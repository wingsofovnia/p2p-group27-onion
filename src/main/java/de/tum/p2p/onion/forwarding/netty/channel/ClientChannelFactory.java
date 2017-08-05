package de.tum.p2p.onion.forwarding.netty.channel;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.netty.context.OriginatorContext;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.onion.forwarding.netty.handler.TunnelExtendedHandler;
import de.tum.p2p.onion.forwarding.netty.handler.TunnelMessageDecoder;
import de.tum.p2p.onion.forwarding.netty.handler.TunnelMessageEncoder;
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
 *     <li>{@link TunnelExtendedHandler}</li>
 *     <li>{@link io.netty.handler.codec.FixedLengthFrameDecoder} to discard
 *     missized frames</li>
 *     <li>{@link TunnelMessageDecoder}</li>
 *     <li>{@link TunnelMessageEncoder}</li>
 * </ul>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class ClientChannelFactory extends ChannelFactory<Channel> {

    protected ClientChannelFactory(Builder builder) {
        this.bossEventLoop = notNull(builder.bossEventLoop);
        this.channel = notNull(builder.channel);
        this.channelOptions = notNull(builder.channelOptions);

        this.onionAuthorizer = notNull(builder.onionAuthorizer);
        this.routingContext = notNull(builder.routingContext);
        this.originatorContext = notNull(builder.originatorContext);
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
            pipe.addLast(new TunnelExtendedHandler(onionAuthorizer, routingContext, eventBus));
        });
    }

    public static final class Builder {

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
        private RoutingContext routingContext;
        public OriginatorContext originatorContext;
        private EventBus eventBus;

        private LogLevel loggerLevel;

        public Builder bossEventLoop(EventLoopGroup bossEventLoop) {
            this.bossEventLoop = bossEventLoop;
            return this;
        }

        public Builder channel(Class<? extends Channel> channel) {
            this.channel = channel;
            return this;
        }

        public Builder channelOptions(Map<ChannelOption, Object> channelOptions) {
            this.channelOptions = channelOptions;
            return this;
        }

        public Builder onionAuthorizer(OnionAuthorizer onionAuthorizer) {
            this.onionAuthorizer = onionAuthorizer;
            return this;
        }

        public Builder routingContext(RoutingContext routingContext) {
            this.routingContext = routingContext;
            return this;
        }

        public Builder originatorContext(OriginatorContext originatorContext) {
            this.originatorContext = originatorContext;
            return this;
        }

        public Builder eventBus(EventBus eventBus) {
            this.eventBus = eventBus;
            return this;
        }

        public Builder loggerLevel(LogLevel loggerLevel) {
            this.loggerLevel = loggerLevel;
            return this;
        }

        public ClientChannelFactory build() {
            return new ClientChannelFactory(this);
        }
    }
}
