package de.tum.p2p.onion.forwarding.netty;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.Peer;
import de.tum.p2p.onion.OnionException;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.*;
import de.tum.p2p.onion.forwarding.netty.channel.ClientChannelFactory;
import de.tum.p2p.onion.forwarding.netty.channel.ServerChannelFactory;
import de.tum.p2p.onion.forwarding.netty.context.OriginatorContext;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.proto.message.onion.forwarding.*;
import de.tum.p2p.rps.RandomPeerSampler;
import io.netty.channel.*;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import lombok.experimental.var;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import static de.tum.p2p.proto.message.onion.forwarding.TunnelDatumEncryptedMessage.fromDatum;
import static de.tum.p2p.util.ByteBuffers.bufferAllBytes;
import static de.tum.p2p.util.Nets.localhost;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.stream.Collectors.joining;

/**
 * Netty implementation of the Onion Forwarder who is responsible for
 * forwarding data between onions.
 * <p>
 * Each Netty Onion Forwarder holds a server socket channel and may hold
 * 1 client socket per tunnel that points to next onion's server this onion
 * should route ONION_TUNNEL_DATUM/ONION_TUNNEL_EXTEND messages:
 * <pre>
 *    O(1)                    O(2)
 * |--------|              |--------|
 * |  SSO1  |------*   +...|  SSO2  | ...
 * |--------|  +...|...+   |--------|
 * |  CSO1  |..+   *-------|  CSO2  | ...
 * |--------|              |--------|
 * </pre>
 * <ul>
 *     <li>SS - Server Socket</li>
 *     <li>CS - Client Socket</li>
 * </ul>
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@Slf4j
public class NettyOnionForwarder implements OnionForwarder {

    static {
        // Enable Netty to use Sl4j
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    private static final Duration SYNC_CHANNEL_GET_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration RETIRE_COVER_TUNNEL_TIMEOUT = Duration.ofSeconds(2);

    private static final Integer MIN_INTERMEDIATE_HOPS_WARN = 3;
    private final Integer intermediateHops;

    private final RandomPeerSampler rps;
    private final OnionAuthorizer onionAuthorizer;

    private final OriginatorContext originatorContext;
    private final RoutingContext routingContext;

    private final Channel serverChannel;
    private final ClientChannelFactory clientChannelFactory;

    private final OnionEventBus eventBus;

    private final Peer me;

    private NettyOnionForwarder(NettyRemoteOnionForwarderBuilder builder) {
        Validate.isTrue(builder.intermediateHops > 0, "At least one intermediate hop is required");
        if (builder.intermediateHops < MIN_INTERMEDIATE_HOPS_WARN)
            log.warn("Amount of required intermediate hops is very low. " +
                "Consider increasing intermediate hops count {}+ to improve security", MIN_INTERMEDIATE_HOPS_WARN);

        this.intermediateHops = builder.intermediateHops;

        this.rps = builder.randomPeerSampler;
        this.onionAuthorizer = builder.onionAuthorizer;

        this.originatorContext = builder.originatorContext;
        this.routingContext = builder.routingContext;

        try {
            this.serverChannel = builder.buildServerChannelFactory()
                .bind(builder.inetAddress, builder.port)
                .get(SYNC_CHANNEL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            log.info("Onion Server Channel has been initialized on {}", serverChannel.localAddress());

            this.clientChannelFactory = builder.buildClientChannelFactory();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new OnionInitializationException("Failed to initialize onion server channel");
        }

        this.eventBus = new OnionEventBus(builder.eventBus);

        this.me = Peer.of(builder.inetAddress, builder.port, builder.publicKey);
    }

    @Override
    public CompletableFuture<TunnelId> createTunnel(Peer destination) throws OnionTunnelingException {
        val tunnelId = TunnelId.random();

        // Random Route peers (not me/dest)
        //             vvvvvvvvvvvvvvvvvvvvvvvvvvvv
        // O(me/orig) ... O(inter1) ... O(interN) ... O(dest)
        val futureTunnelInterHopPeers = rps.sampleDistinctExclusive(intermediateHops, asList(me, destination));

        // All hops the data sent to the future tunnel must pass through
        //             vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
        // O(me/orig) ... O(inter1) ... O(interN) ... O(dest)
        val futureTunnelHopPeers = futureTunnelInterHopPeers.<Peer, List<Peer>>thenCombine(completedFuture(destination),
            (randPeers, dest) -> new ArrayList<Peer>() {{
                addAll(randPeers);
                add(dest);
            }});

        val futureTunnelHopSessionIds = futureTunnelHopPeers.<List<SessionId>>thenCompose(tunnelPeers -> {
            // tunnelEntryPeer peer is the fist random peer of a new future tunnel.
            val tunnelEntryPeer = tunnelPeers.iterator().next();

            if (log.isDebugEnabled()) {
                log.debug("Next tunnel #{}: {} 0-> {}", tunnelId, me.socketAddress(),
                    tunnelPeers.stream().map(Peer::socketAddress).map(Object::toString).collect(joining(" -> ")));
            }

            // Establish a client connection with the entry peer
            val futureTunnelEntryChannel = clientChannelFactory.connect(tunnelEntryPeer.socketAddress());

            return futureTunnelEntryChannel.thenCompose(tunnelEntryChannel -> {
                // Remember connection to head peer of the tunnel
                originatorContext.serve(tunnelId, tunnelEntryChannel);

                val sessionIds = new ArrayList<SessionId>();

                // Boring the tunnel
                var futurePrevSessionId = extendTunnel(tunnelId, tunnelEntryPeer, null);
                for (val peer : tunnelPeers.subList(1, tunnelPeers.size())) {
                    futurePrevSessionId = futurePrevSessionId.thenCompose(sessionId -> {
                        sessionIds.add(sessionId);

                        return extendTunnel(tunnelId, peer, sessionId);
                    });
                }

                return futurePrevSessionId.thenApply(lastSessionId -> {
                    sessionIds.add(lastSessionId);
                    return sessionIds;
                });
            });
        });

        return futureTunnelHopSessionIds.thenApply((sessionIds) -> {
            log.debug("Tunnel #{} has been persisted withing the onion {}", tunnelId, me.socketAddress());

            originatorContext.appendSession(tunnelId, sessionIds);

            return tunnelId;
        });
    }
    @Deprecated
    private CompletableFuture<SessionId> extendTunnel(TunnelId tunnelId, Peer newHop, SessionId sessionId) {
        log.trace("Extending tunnel {} by new peer {}", tunnelId, newHop.socketAddress());

        val sessionFactory = onionAuthorizer.sessionFactory();
        val futureTunnelSession = new CompletableFuture<SessionId>();

        sessionFactory.start(newHop)
            .thenAccept(sessionIdHs1Pair -> {
                val requestId = RequestId.next();
                val handshake1 = sessionIdHs1Pair.getRight();

                TunnelMessage tunnelExtendMsg;

                if (sessionId == null) {
                    tunnelExtendMsg =
                        new TunnelExtendMessage(tunnelId, requestId, me.publicKey(), bufferAllBytes(handshake1));
                } else {
                    val plainConnMsg = new TunnelConnectMessage(tunnelId, requestId, newHop.address(),
                        newHop.port(), me.publicKey(), bufferAllBytes(handshake1));


                    tunnelExtendMsg = TunnelConnectEncryptedMessage.fromConnect(plainConnMsg, onionAuthorizer, sessionId).join();
                    /*tunnelExtendMsg = new TunnelExtendMessage.Builder()
                        .encryption(onionAuthorizer, sessionId)
                        .tunnelId(tunnelId)
                        .requestId(requestId)
                        .destination(newHop.address())
                        .port(newHop.port())
                        .sourceKey(me.publicKey())
                        .handshake(handshake1)
                        .build();*/
                }

                eventBus.completeFutureSession(requestId, futureTunnelSession);

                val tunnelEntryChannel = originatorContext.entry(tunnelId);
                tunnelEntryChannel.writeAndFlush(tunnelExtendMsg)
                    .addListener(transfer -> {
                        if (!transfer.isSuccess())
                            throw new OnionTunnelingException("Failed to extend tunnel", transfer.cause());
                    });

                log.trace("ONION_TUNNEL_EXTEND({}) has been sent to {} via {}, req_id = {}", newHop.socketAddress(),
                    tunnelEntryChannel.remoteAddress(), tunnelEntryChannel.localAddress(), requestId);
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });


        return futureTunnelSession;
    }

    @Override
    public void destroyTunnel(TunnelId tunnelId) throws OnionTunnelingException {
        if (!originatorContext.serves(tunnelId))
            throw new OnionTunnelingException("Failed to destroy the tunnel - not found");

        val tunnelRetireMsg = new TunnelRetireMessage(tunnelId);
        originatorContext.entry(tunnelId).writeAndFlush(tunnelRetireMsg)
            .addListener((ChannelFutureListener) transfer -> {
                if (!transfer.isSuccess())
                    throw new OnionTunnelingException("Failed to sent tunnel retire request " +
                        "to next hop", transfer.cause());

                originatorContext.forget(tunnelId);

                log.debug("Tunnel {} has been removed from originator context of peer {}",
                    tunnelId, me.socketAddress());
            });
    }

    @Override
    public void forward(TunnelId tunnelId, ByteBuffer data) throws OnionDataForwardingException {
        if (!originatorContext.serves(tunnelId))
            throw new OnionDataForwardingException("Failed to forward data - tunnel not found");

        val hopsSessionIds = originatorContext.sessionIds(tunnelId);

        TunnelDatumMessageFactory.ofMany(tunnelId, data).stream()
            .map(datum -> fromDatum(datum, onionAuthorizer, hopsSessionIds))
            .forEach(futureEncryptedDatum -> {
                futureEncryptedDatum.thenAccept(encryptedDatum -> {
                    originatorContext.entry(tunnelId).writeAndFlush(encryptedDatum);

                    log.debug("Datum chuck has been pushed by peer {} via tunnel {}", me.socketAddress(), tunnelId);
                });
            });
    }

    @Override
    public void cover(int size) throws OnionCoverInterferenceException {
        if (!originatorContext.isEmpty())
            throw new OnionCoverInterferenceException("Generation of cover traffic when " +
                "there are active tunnels is prohibited");
        else if (size > TunnelDatumMessage.PAYLOAD_BYTES)
            throw new IllegalArgumentException("Too big cover size. " +
                "Max TunnelDatumMessage payload length = " + TunnelDatumMessage.PAYLOAD_BYTES);

        rps.sampleNot(me).thenCompose(this::createTunnel)
            .thenAccept(tunnelId -> {
                val hopsSessionIds = originatorContext.sessionIds(tunnelId);

                val coverDatum = new TunnelDatumMessage(tunnelId, size);
                val futureEncryptedDatum = fromDatum(coverDatum, onionAuthorizer, hopsSessionIds);

                futureEncryptedDatum.thenAccept(encryptedDatum -> {
                    originatorContext.entry(tunnelId).writeAndFlush(encryptedDatum);
                    log.debug("Cover spam issued by peer {} via cover-tunnel {}", me.socketAddress(), tunnelId);

                    commonPool().execute(() -> {
                        try {
                            Thread.sleep(RETIRE_COVER_TUNNEL_TIMEOUT.toMillis());
                            destroyTunnel(tunnelId);
                        } catch (InterruptedException e) {
                            throw new OnionException("Failed to wait until cover tunnel timeout expire to close it", e);
                        }
                    });
                });
            });
    }

    @Override
    public void subscribe(BiConsumer<TunnelId, ByteBuffer> consumer) {
        eventBus.registerDataListener(consumer);
    }

    @Override
    public void unsubscribe(BiConsumer<TunnelId, ByteBuffer> consumer) {
        eventBus.unregisterDataListener(consumer);
    }

    @Override
    public Peer peer() {
        return me;
    }

    @Override
    public void close() throws IOException {
        try {
            this.routingContext.close();

            this.serverChannel.disconnect();
            this.serverChannel.close().syncUninterruptibly();
        } catch (Exception e) {
            throw new IOException("Failed to close onion server channel", e);
        }
    }

    public static class NettyRemoteOnionForwarderBuilder {

        private EventLoopGroup clientBossEventLoop;
        private EventLoopGroup serverBossEventLoop;
        private EventLoopGroup serverWorkerEventLoop;

        private Class<? extends Channel> clientChannel;
        private Map<ChannelOption, Object> clientChannelOptions;
        private Class<? extends ServerChannel> serverChannel;
        private Map<ChannelOption, Object> serverChannelOptions;

        private InetAddress inetAddress = localhost();
        private int port;

        private PublicKey publicKey;

        private Integer intermediateHops;
        private RandomPeerSampler randomPeerSampler;
        private OnionAuthorizer onionAuthorizer;

        public OriginatorContext originatorContext = new OriginatorContext();
        private RoutingContext routingContext = new RoutingContext();

        private EventBus eventBus = new EventBus();
        private LogLevel loggerLevel;

        public NettyRemoteOnionForwarderBuilder clientBossEventLoop(EventLoopGroup clientBossEventLoop) {
            this.clientBossEventLoop = clientBossEventLoop;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder serverBossEventLoop(EventLoopGroup serverBossEventLoop) {
            this.serverBossEventLoop = serverBossEventLoop;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder serverWorkerEventLoop(EventLoopGroup serverWorkerEventLoop) {
            this.serverWorkerEventLoop = serverWorkerEventLoop;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder clientChannel(Class<? extends Channel> clientChannel) {
            this.clientChannel = clientChannel;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder clientChannelOptions(Map<ChannelOption, Object> clientChannelOptions) {
            this.clientChannelOptions = clientChannelOptions;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder serverChannel(Class<? extends ServerChannel> serverChannel) {
            this.serverChannel = serverChannel;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder serverChannelOptions(Map<ChannelOption, Object> serverChannelOptions) {
            this.serverChannelOptions = serverChannelOptions;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder publicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder inetAddress(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder port(int port) {
            this.port = port;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder intermediateHops(Integer intermediateHops) {
            this.intermediateHops = intermediateHops;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder randomPeerSampler(RandomPeerSampler randomPeerSampler) {
            this.randomPeerSampler = randomPeerSampler;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder onionAuthorizer(OnionAuthorizer onionAuthorizer) {
            this.onionAuthorizer = onionAuthorizer;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder originatorContext(OriginatorContext originatorContext) {
            this.originatorContext = originatorContext;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder routingContext(RoutingContext routingContext) {
            this.routingContext = routingContext;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder eventBus(EventBus eventBus) {
            this.eventBus = eventBus;
            return this;
        }

        public NettyRemoteOnionForwarderBuilder loggerLevel(LogLevel loggerLevel) {
            this.loggerLevel = loggerLevel;
            return this;
        }

        private ServerChannelFactory buildServerChannelFactory() {
            val serverChannelFactoryBuilder = new ServerChannelFactory.ServerChannelFactoryBuilder();

            if (nonNull(serverBossEventLoop))
                serverChannelFactoryBuilder.bossEventLoop(serverBossEventLoop);
            if (nonNull(serverWorkerEventLoop))
                serverChannelFactoryBuilder.workerEventLoop(serverWorkerEventLoop);

            if (nonNull(serverChannel))
                serverChannelFactoryBuilder.channel(serverChannel);
            if (nonNull(serverChannelOptions))
                serverChannelFactoryBuilder.channelOptions(serverChannelOptions);

            serverChannelFactoryBuilder
                .clientChannelFactory(buildClientChannelFactory())
                .onionAuthorizer(onionAuthorizer)
                .routingContext(routingContext)
                .originatorContext(originatorContext)
                .eventBus(eventBus);

            if (nonNull(loggerLevel))
                serverChannelFactoryBuilder.loggerLevel(loggerLevel);

            return serverChannelFactoryBuilder.build();
        }

        private ClientChannelFactory buildClientChannelFactory() {
            val clientChannelFactoryBuilder= new ClientChannelFactory.ClientChannelFactoryBuilder();

            if (nonNull(clientBossEventLoop))
                clientChannelFactoryBuilder.bossEventLoop(serverBossEventLoop);

            if (nonNull(clientChannel))
                clientChannelFactoryBuilder.channel(clientChannel);
            if (nonNull(clientChannelOptions))
                clientChannelFactoryBuilder.channelOptions(clientChannelOptions);

            clientChannelFactoryBuilder
                .onionAuthorizer(onionAuthorizer)
                .originatorContext(originatorContext)
                .routingContext(routingContext)
                .eventBus(eventBus);

            if (nonNull(loggerLevel))
                clientChannelFactoryBuilder.loggerLevel(loggerLevel);

            return clientChannelFactoryBuilder.build();
        }

        public NettyOnionForwarder listen() {
            return new NettyOnionForwarder(this);
        }

        public NettyOnionForwarder build() {
            return listen();
        }
    }
}
