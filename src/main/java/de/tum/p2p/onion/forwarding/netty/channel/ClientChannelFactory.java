package de.tum.p2p.onion.forwarding.netty.channel;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.netty.TunnelRouter;
import de.tum.p2p.onion.forwarding.netty.handler.client.TunnelExtendedHandler;
import de.tum.p2p.onion.forwarding.netty.handler.client.TunnelExtendedPropagator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import lombok.val;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code ClientChannelFactory} is used to create client TCP connections
 * between {@link de.tum.p2p.onion.forwarding.OnionForwarder} peers.
 * <p>
 * The {@code ClientChannelFactory}'s pipeline includes:
 * <ul>
 *     <li>{@link io.netty.handler.codec.LengthFieldBasedFrameDecoder}</li>
 *     <li>{@link io.netty.handler.codec.LengthFieldPrepender}</li>
 *     <li>{@link io.netty.handler.codec.FixedLengthFrameDecoder} to discard
 *     missized frames</li>
 *     <li>{@link de.tum.p2p.onion.forwarding.netty.handler.OnionMessageDecoder}
 *     and {@link de.tum.p2p.onion.forwarding.netty.handler.OnionMessageEncoder}</li>
 *     <li>{@link TunnelExtendedHandler}</li>
 *     <li>{@link TunnelExtendedPropagator}</li>
 * </ul>
 */
public class ClientChannelFactory extends ChannelFactory<Channel> {

    protected ClientChannelFactory(ClientChannelFactoryBuilder builder) {
        this.bossEventLoop = notNull(builder.bossEventLoop);
        this.channel = notNull(builder.channel);
        this.channelOptions = notNull(builder.channelOptions);

        this.hmacKey = notNull(builder.hmacKey);

        this.onionAuthorizer = notNull(builder.onionAuthorizer);
        this.tunnelRouter = notNull(builder.tunnelRouter);
        this.eventBus = notNull(builder.eventBus);

        this.loggerLevel = builder.loggerLevel;
    }

    public CompletableFuture<Channel> connect(InetSocketAddress socketAddress) {
        val bootstrap = new Bootstrap();

        bootstrap
            .group(bossEventLoop)
            .channel(channel)
            .handler(clientPipeline());

        channelOptions.forEach(bootstrap::option);

        val futureNettyChannel = bootstrap.connect(socketAddress);
        return toCompletableFuture(futureNettyChannel);
    }

    public CompletableFuture<Channel> connect(InetAddress inetAddress, int port) {
        return connect(new InetSocketAddress(inetAddress, port));
    }

    private ChannelInitializer clientPipeline() {
        return messagingChannel(pipe -> {
            pipe.addLast(new TunnelExtendedHandler(onionAuthorizer, tunnelRouter, eventBus));
            pipe.addLast(new TunnelExtendedPropagator(tunnelRouter));
        });
    }

    public static final class ClientChannelFactoryBuilder {

        private static final EventLoopGroup DEFAULT_BOSS_EVENT_LOOP = new NioEventLoopGroup();
        private static final Class<? extends Channel> DEFAULT_CHANNEL = NioSocketChannel.class;
        private static final Map<ChannelOption, Object> DEFAULT_CHANNEL_OPTS = new HashMap<ChannelOption, Object>() {{
            put(ChannelOption.SO_KEEPALIVE, true);
        }};

        private EventLoopGroup bossEventLoop = DEFAULT_BOSS_EVENT_LOOP;

        private Class<? extends Channel> channel = DEFAULT_CHANNEL;
        private Map<ChannelOption, Object> channelOptions = DEFAULT_CHANNEL_OPTS;

        private byte[] hmacKey;

        private OnionAuthorizer onionAuthorizer;
        private TunnelRouter tunnelRouter;
        private EventBus eventBus;

        private LogLevel loggerLevel;

        public ClientChannelFactoryBuilder bossEventLoop(EventLoopGroup bossEventLoop) {
            this.bossEventLoop = bossEventLoop;
            return this;
        }

        public ClientChannelFactoryBuilder channel(Class<? extends Channel> channel) {
            this.channel = channel;
            return this;
        }

        public ClientChannelFactoryBuilder channelOptions(Map<ChannelOption, Object> channelOptions) {
            this.channelOptions = channelOptions;
            return this;
        }

        public ClientChannelFactoryBuilder hmacKey(byte[] hmacKey) {
            this.hmacKey = hmacKey;
            return this;
        }

        public ClientChannelFactoryBuilder onionAuthorizer(OnionAuthorizer onionAuthorizer) {
            this.onionAuthorizer = onionAuthorizer;
            return this;
        }

        public ClientChannelFactoryBuilder tunnelRouter(TunnelRouter tunnelRouter) {
            this.tunnelRouter = tunnelRouter;
            return this;
        }

        public ClientChannelFactoryBuilder eventBus(EventBus eventBus) {
            this.eventBus = eventBus;
            return this;
        }

        public ClientChannelFactoryBuilder loggerLevel(LogLevel loggerLevel) {
            this.loggerLevel = loggerLevel;
            return this;
        }

        public ClientChannelFactory build() {
            return new ClientChannelFactory(this);
        }
    }
}
