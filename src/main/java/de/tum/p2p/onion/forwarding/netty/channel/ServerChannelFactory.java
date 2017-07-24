package de.tum.p2p.onion.forwarding.netty.channel;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.common.netty.OnionPipelineBuilder;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.onion.forwarding.netty.handler.server.TunnelDatumHandler;
import de.tum.p2p.onion.forwarding.netty.handler.server.TunnelExtendHandler;
import de.tum.p2p.onion.forwarding.netty.handler.server.TunnelExtendPropagator;
import de.tum.p2p.onion.forwarding.netty.handler.server.TunnelRetireHandler;
import de.tum.p2p.rps.RandomPeerSampler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import lombok.val;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static de.tum.p2p.util.Nets.localhost;
import static de.tum.p2p.util.netty.Channels.toCompletableFuture;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code ServerChannelFactory} is used to create TCP server connection for
 * {@link de.tum.p2p.onion.forwarding.OnionForwarder} to handle tunneling requests
 * from remote onions.
 * <p>
 * The {@code ServerChannelFactory}'s pipeline includes:
 * <ul>
 *     <li>{@link io.netty.handler.codec.LengthFieldBasedFrameDecoder}</li>
 *     <li>{@link io.netty.handler.codec.LengthFieldPrepender}</li>
 *     <li>{@link io.netty.handler.codec.FixedLengthFrameDecoder} to discard
 *     missized frames</li>
 *     <li>{@link de.tum.p2p.onion.common.netty.handler.OnionMessageDecoder}
 *     and {@link de.tum.p2p.onion.common.netty.handler.OnionMessageEncoder}</li>
 *     <li>{@link TunnelExtendHandler}</li>
 *     <li>{@link TunnelExtendPropagator}</li>
 * </ul>
 */
public class ServerChannelFactory {

    private final EventLoopGroup bossEventLoop;
    private final EventLoopGroup workerEventLoop;
    private final Class<? extends ServerChannel> channel;
    private final Map<ChannelOption, Object> channelOptions;

    private byte[] hmacKey;

    private final OnionAuthorizer onionAuthorizer;
    private final RandomPeerSampler randomPeerSampler;
    private final ClientChannelFactory clientChannelFactory;

    private final Router router;
    private final EventBus eventBus;

    private final LogLevel loggerLevel;

    protected ServerChannelFactory(ServerChannelFactoryBuilder builder) {
        this.bossEventLoop = notNull(builder.bossEventLoop);
        this.workerEventLoop = notNull(builder.workerEventLoop);

        this.channel = notNull(builder.channel);
        this.channelOptions = notNull(builder.channelOptions);

        this.hmacKey = notNull(builder.hmacKey);

        this.randomPeerSampler = notNull(builder.randomPeerSampler);
        this.onionAuthorizer = notNull(builder.onionAuthorizer);
        this.clientChannelFactory = notNull(builder.clientChannelFactory);
        this.router = notNull(builder.router);

        this.eventBus = notNull(builder.eventBus);

        this.loggerLevel = builder.loggerLevel;
    }

    public CompletableFuture<Channel> bind(InetSocketAddress socketAddress) {
        val bootstrap = new ServerBootstrap();

        bootstrap
            .group(bossEventLoop, workerEventLoop)
            .channel(channel)
            .childHandler(serverPipeline());

        channelOptions.forEach(bootstrap::childOption);

        val nettyChannelFuture = bootstrap.bind(socketAddress);
        return toCompletableFuture(nettyChannelFuture);
    }

    public CompletableFuture<Channel> bind(InetAddress inetAddress, int port) {
        return bind(new InetSocketAddress(inetAddress, port));
    }

    public CompletableFuture<Channel> bind(int port) {
        return bind(localhost(), port);
    }

    private ChannelInitializer serverPipeline() {
        return new OnionPipelineBuilder()
            .hmacKey(hmacKey)
            .handler(new TunnelDatumHandler(onionAuthorizer, router, eventBus))
            .handler(new TunnelRetireHandler(router))
            .handler(new TunnelExtendHandler(onionAuthorizer, router))
            .handler(new TunnelExtendPropagator(clientChannelFactory, router))
            .build();

    }

    public static final class ServerChannelFactoryBuilder {

        private static final EventLoopGroup DEFAULT_BOSS_EVENT_LOOP = new NioEventLoopGroup();
        private static final EventLoopGroup DEFAULT_WORKER_EVENT_LOOP = new NioEventLoopGroup();
        private static final Class<? extends ServerChannel> DEFAULT_CHANNEL = NioServerSocketChannel.class;
        private static final Map<ChannelOption, Object> DEFAULT_CHANNEL_OPTS = new HashMap<ChannelOption, Object>() {{
            put(ChannelOption.SO_KEEPALIVE, true);
        }};

        private EventLoopGroup bossEventLoop = DEFAULT_BOSS_EVENT_LOOP;
        private EventLoopGroup workerEventLoop = DEFAULT_WORKER_EVENT_LOOP;

        private Class<? extends ServerChannel> channel = DEFAULT_CHANNEL;
        private Map<ChannelOption, Object> channelOptions = DEFAULT_CHANNEL_OPTS;

        private byte[] hmacKey;

        private RandomPeerSampler randomPeerSampler;
        private ClientChannelFactory clientChannelFactory;

        private OnionAuthorizer onionAuthorizer;
        private Router router;

        private EventBus eventBus;

        private LogLevel loggerLevel;

        public ServerChannelFactoryBuilder bossEventLoop(EventLoopGroup bossEventLoop) {
            this.bossEventLoop = bossEventLoop;
            return this;
        }

        public ServerChannelFactoryBuilder workerEventLoop(EventLoopGroup workerEventLoop) {
            this.workerEventLoop = workerEventLoop;
            return this;
        }

        public ServerChannelFactoryBuilder channel(Class<? extends ServerChannel> channel) {
            this.channel = channel;
            return this;
        }

        public ServerChannelFactoryBuilder channelOptions(Map<ChannelOption, Object> channelOptions) {
            this.channelOptions = channelOptions;
            return this;
        }

        public ServerChannelFactoryBuilder hmacKey(byte[] hmacKey) {
            this.hmacKey = hmacKey;
            return this;
        }

        public ServerChannelFactoryBuilder randomPeerSampler(RandomPeerSampler randomPeerSampler) {
            this.randomPeerSampler = randomPeerSampler;
            return this;
        }

        public ServerChannelFactoryBuilder clientChannelFactory(ClientChannelFactory clientChannelFactory) {
            this.clientChannelFactory = clientChannelFactory;
            return this;
        }

        public ServerChannelFactoryBuilder onionAuthorizer(OnionAuthorizer onionAuthorizer) {
            this.onionAuthorizer = onionAuthorizer;
            return this;
        }

        public ServerChannelFactoryBuilder router(Router router) {
            this.router = router;
            return this;
        }

        public ServerChannelFactoryBuilder eventBus(EventBus eventBus) {
            this.eventBus = eventBus;
            return this;
        }

        public ServerChannelFactoryBuilder loggerLevel(LogLevel loggerLevel) {
            this.loggerLevel = loggerLevel;
            return this;
        }

        public ServerChannelFactory build() {
            return new ServerChannelFactory(this);
        }
    }
}
