package de.tum.p2p.onion.forwarding.netty;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import de.tum.p2p.Peer;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.*;
import de.tum.p2p.onion.forwarding.netty.channel.ClientChannelFactory;
import de.tum.p2p.onion.forwarding.netty.channel.ServerChannelFactory;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.onion.forwarding.netty.event.TunnelDataReceived;
import de.tum.p2p.onion.forwarding.netty.event.TunnelExtendedReceived;
import de.tum.p2p.proto.message.onion.forwarding.TunnelDatumMessageFactory;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendMessage;
import de.tum.p2p.proto.message.onion.forwarding.TunnelRetireMessage;
import de.tum.p2p.rps.RandomPeerSampler;
import io.netty.channel.*;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static de.tum.p2p.onion.forwarding.netty.context.Route.to;
import static de.tum.p2p.util.Nets.localhost;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

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
 *
 * <strong>TODO:</strong>
 * <ol>
 *     <li>Can we improve asynchronicity of peer sampling?</li>
 *     <li>Can we build tunnel asynchronously {@link #extendTunnel(TunnelId, Peer)}?</li>
 *     <li>Decouple message handlers from internal structures (tunnelRouter etc?)</li>
 *     <li>Message Factory (route rid of hmac  here)/parser</li>
 *     <li>Update subscripbe (sender? tunnelId? more parameters)</li>
 * </ol>
 */
public class NettyOnionForwarder implements OnionForwarder {

    private static final Logger log = LoggerFactory.getLogger(NettyOnionForwarder.class);

    static {
        // Enable Netty to use Sl4j
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    private static final Duration SYNC_CHANNEL_GET_TIMEOUT = Duration.ofDays(2);

    private static final Integer MIN_INTERMEDIATE_HOPS_WARN = 3;
    private final Integer intermediateHops;

    private final byte[] hmac;

    private final RandomPeerSampler rps;
    private final OnionAuthorizer onionAuthorizer;

    private final Router router;

    private final Channel serverChannel;
    private final ClientChannelFactory clientChannelFactory;

    private final EventBus eventBus;
    private final List<BiConsumer<TunnelId, ByteBuffer>> dataConsumers = new ArrayList<>();

    private final Peer me;

    private NettyOnionForwarder(NettyRemoteOnionForwarderBuilder builder) {
        Validate.isTrue(builder.intermediateHops > 0, "At least one intermediate hop is required");
        if (builder.intermediateHops < MIN_INTERMEDIATE_HOPS_WARN)
            log.warn("Amount of required intermediate hops is very low. " +
                "Consider increasing intermediate hops count {}+ to improve security", MIN_INTERMEDIATE_HOPS_WARN);

        this.intermediateHops = builder.intermediateHops;

        this.hmac = builder.hmacKey;

        this.rps = builder.randomPeerSampler;
        this.onionAuthorizer = builder.onionAuthorizer;

        this.router = builder.router;

        try {
            this.serverChannel = builder.buildServerChannelFactory()
                .bind(builder.inetAddress, builder.port)
                .get(SYNC_CHANNEL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            log.info("Onion Server Channel has been initialized on {}", serverChannel.localAddress());

            this.clientChannelFactory = builder.buildClientChannelFactory();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new OnionInitializationException("Failed to initialize onion server channel");
        }

        this.eventBus = builder.eventBus;
        attachDataConsumers(eventBus);

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
            // tunnelHeadPeer peer is the fist random peer of a new future tunnel.
            // It will receive all ONION_TUNNEL_EXTEND msgs from this O(orig) and propagate them to the tunnel's end
            val tunnelHeadPeer = tunnelPeers.iterator().next();

            if (log.isDebugEnabled()) {
                log.debug("[{}][{}] Next tunnel #{}: {} 0-> {}",
                    me.socketAddress(), tunnelId, tunnelId, me.socketAddress(),
                    tunnelPeers.stream().map(Peer::socketAddress).map(Object::toString).collect(joining(" -> ")));
            }

            // Establish a client connection with the tunnelHeadPeer
            val futureTunnelHeadChannel = clientChannelFactory.connect(tunnelHeadPeer.socketAddress());

            // Bore the tunnel
            return futureTunnelHeadChannel.thenApplyAsync(tunnelHeadChannel -> {
                // Remember connection to head peer of the tunnel
                router.route(to(tunnelId, tunnelHeadChannel));

                // Synchronously extend tunnel peer by peer. Synchronicity is required
                // in order to ensure natural sessionIds order
                return tunnelPeers.stream().map(peer -> {
                    try {
                        return extendTunnel(tunnelId, peer)
                                .get(SYNC_CHANNEL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        throw new OnionTunnelingException("Failed to extend tunnel " + tunnelId +
                            " with " + peer.socketAddress(), e);
                    }
                }).collect(toList());
            });
        });

        return futureTunnelHopSessionIds.thenApply((sessionIds) -> {
            log.debug("[{}][{}] Tunnel has been persisted withing the onion",
                me.socketAddress(), tunnelId);

            val route = router.route(tunnelId).orElseThrow(()
                -> new OnionTunnelingException("Failed to persist tunnel session ids - " + tunnelId + " not found"));
            route.attachSessionIds(sessionIds);

            return tunnelId;
        });
    }

    private CompletableFuture<SessionId> extendTunnel(TunnelId tunnelId, Peer newHop) {
        log.trace("[{}][{}] Extending tunnel by new peer {}", me.socketAddress(), tunnelId, newHop.socketAddress());

        val sessionFactory = onionAuthorizer.sessionFactory();
        val futureTunnelSession = new CompletableFuture<SessionId>();

        sessionFactory.start(newHop)
            .thenAccept(sessionIdHs1Pair -> {
                val tunnelExtendMsg = TunnelExtendMessage.of(tunnelId, newHop.address(), newHop.port(),
                    me.publicKey(), sessionIdHs1Pair.getRight());

                val tunnelHeadChannel = router.routeNext(tunnelId).orElseThrow(()
                    -> new OnionTunnelingException("Failed to retrieve next hop of the tunnel " + tunnelId));

                eventBus.register(new Consumer<TunnelExtendedReceived>() {
                    @Subscribe
                    public void accept(TunnelExtendedReceived tunnelExtendedReceived) {
                        if (!tunnelId.equals(tunnelExtendedReceived.tunnelId()))
                            return;

                        if (!tunnelExtendedReceived.requestId().equals(tunnelExtendMsg.requestId()))
                            return;

                        futureTunnelSession.complete(tunnelExtendedReceived.sessionId());

                        log.trace("[{}][{}] Tunnel session for req_id = {} has been established and persisted",
                            me.socketAddress(), tunnelId, tunnelExtendedReceived.requestId());
                    }
                });

                tunnelHeadChannel.writeAndFlush(tunnelExtendMsg)
                    .addListener(transfer -> {
                        if (!transfer.isSuccess())
                            throw new OnionTunnelingException("Failed to extend tunnel", transfer.cause());
                    });

                log.trace("[{}][{}] ONION_TUNNEL_EXTEND({}) has been sent to {} via {}, req_id = {}",
                    me.socketAddress(), tunnelId, tunnelExtendMsg.destinationSocketAddress(),
                    tunnelHeadChannel.remoteAddress(), tunnelHeadChannel.localAddress(), tunnelExtendMsg.requestId());
            });


        return futureTunnelSession;
    }

    @Override
    public void destroyTunnel(TunnelId tunnelId) throws OnionTunnelingException {
        router.routeNext(tunnelId).ifPresent(tunnelNextHop -> {
            val tunnelRetireMsg = TunnelRetireMessage.of(tunnelId);

            tunnelNextHop.writeAndFlush(tunnelRetireMsg)
                .addListener((ChannelFutureListener) transfer -> {
                    if (!transfer.isSuccess())
                        throw new OnionTunnelingException("Failed to sent tunnel retire request " +
                            "to next hop", transfer.cause());

                    router.forget(tunnelId);
                });
        });
    }

    @Override
    public void forward(TunnelId tunnelId, ByteBuffer data) throws OnionDataForwardingException {
        val route = router.route(tunnelId).orElseThrow(()
            -> new OnionTunnelingException("Unknown tunnel " + tunnelId));

        val routeNextHop = route.getNext();
        val routeSessionIds = route.sessionIds();

        TunnelDatumMessageFactory.ofMany(tunnelId, data, (plaintext)
            -> onionAuthorizer.encrypt(plaintext, routeSessionIds).join().bytesBuffer(), hmac)
                .forEach(routeNextHop::writeAndFlush);
    }

    @Override
    public void cover(int size) throws OnionCoverInterferenceException {
        if (!router.isEmpty())
            throw new OnionCoverInterferenceException("Generation of cover traffic when " +
                "there are active tunnels is prohibited");

        val coverData = new byte[size];
        ThreadLocalRandom.current().nextBytes(coverData);

        rps.sample()
            .thenCompose(randPeer -> clientChannelFactory.connect(randPeer.socketAddress()))
            .thenAccept(channel -> channel.writeAndFlush(coverData));
    }

    @Override
    public void subscribe(BiConsumer<TunnelId, ByteBuffer> consumer) {
        dataConsumers.add(consumer);
    }

    @Override
    public void unsubscribe(BiConsumer<TunnelId, ByteBuffer> consumer) {
        dataConsumers.remove(consumer);
    }

    @Override
    public Peer peer() {
        return me;
    }

    @Override
    public void close() throws IOException {
        try {
            this.router.close();

            this.serverChannel.disconnect();
            this.serverChannel.close().syncUninterruptibly();
        } catch (Exception e) {
            throw new IOException("Failed to close onion server channel", e);
        }
    }

    private void attachDataConsumers(EventBus eventBus) {
        eventBus.register(new Consumer<TunnelDataReceived>() {
            @Subscribe
            public void accept(TunnelDataReceived tunnelDataReceived) {
                dataConsumers.forEach(consumer -> {
                    consumer.accept(tunnelDataReceived.tunnelId(), tunnelDataReceived.payload());
                });
            }
        });
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

        private byte[] hmacKey;
        private PublicKey publicKey;

        private Integer intermediateHops;
        private RandomPeerSampler randomPeerSampler;
        private OnionAuthorizer onionAuthorizer;

        private Router router = new Router();

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

        public NettyRemoteOnionForwarderBuilder hmacKey(byte[] hmacKey) {
            this.hmacKey = hmacKey;
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

        public NettyRemoteOnionForwarderBuilder router(Router router) {
            this.router = router;
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
                .hmacKey(hmacKey)
                .randomPeerSampler(randomPeerSampler)
                .clientChannelFactory(buildClientChannelFactory())
                .onionAuthorizer(onionAuthorizer)
                .router(router)
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
                .hmacKey(hmacKey)
                .onionAuthorizer(onionAuthorizer)
                .router(router)
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
