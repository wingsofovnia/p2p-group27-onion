package de.tum.p2p.onion.forwarding.netty.channel;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.netty.context.OriginatorContext;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.onion.forwarding.netty.handler.*;
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
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code ServerChannelFactory} is used to create TCP server connection for
 * {@link de.tum.p2p.onion.forwarding.OnionForwarder} to handle tunneling requests
 * from remote onions.
 * <p>
 * The {@code ServerChannelFactory}'s pipeline includes:
 * <ul>
 *     <li>{@link TunnelDatumEncryptedHandler}</li>
 *     <li>{@link TunnelExtendHandler}</li>
 *     <li>{@link TunnelRetireHandler}</li>
 *     <li>{@link TunnelConnectHandler}</li>
 *     <li>{@link TunnelDatumHandler}</li>
 *     <li>{@link io.netty.handler.codec.FixedLengthFrameDecoder} to discard
 *     missized frames</li>
 *     <li>{@link de.tum.p2p.onion.forwarding.netty.handler.TunnelMessageDecoder}
 *     and {@link de.tum.p2p.onion.forwarding.netty.handler.TunnelMessageEncoder}</li>
 * </ul>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class ServerChannelFactory extends ChannelFactory<ServerChannel> {

    private final EventLoopGroup workerEventLoop;

    private final ClientChannelFactory clientChannelFactory;

    protected ServerChannelFactory(Builder builder) {
        this.bossEventLoop = notNull(builder.bossEventLoop);
        this.workerEventLoop = notNull(builder.workerEventLoop);

        this.channel = notNull(builder.channel);
        this.channelOptions = notNull(builder.channelOptions);

        this.onionAuthorizer = notNull(builder.onionAuthorizer);
        this.clientChannelFactory = notNull(builder.clientChannelFactory);
        this.routingContext = notNull(builder.routingContext);

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
        return messagingChannel(pipe -> {
            pipe.addLast(new TunnelDatumEncryptedHandler(onionAuthorizer, routingContext));
            pipe.addLast(new TunnelExtendHandler(routingContext, onionAuthorizer));
            pipe.addLast(new TunnelRetireHandler(routingContext));
            pipe.addLast(new TunnelConnectHandler(routingContext, onionAuthorizer, clientChannelFactory));
            pipe.addLast(new TunnelDatumHandler(eventBus));
        });
    }

    public static final class Builder {

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

        private ClientChannelFactory clientChannelFactory;

        private OnionAuthorizer onionAuthorizer;
        private RoutingContext routingContext;
        public OriginatorContext originatorContext;

        private EventBus eventBus;

        private LogLevel loggerLevel;

        public Builder bossEventLoop(EventLoopGroup bossEventLoop) {
            this.bossEventLoop = bossEventLoop;
            return this;
        }

        public Builder workerEventLoop(EventLoopGroup workerEventLoop) {
            this.workerEventLoop = workerEventLoop;
            return this;
        }

        public Builder channel(Class<? extends ServerChannel> channel) {
            this.channel = channel;
            return this;
        }

        public Builder channelOptions(Map<ChannelOption, Object> channelOptions) {
            this.channelOptions = channelOptions;
            return this;
        }

        public Builder clientChannelFactory(ClientChannelFactory clientChannelFactory) {
            this.clientChannelFactory = clientChannelFactory;
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

        public ServerChannelFactory build() {
            return new ServerChannelFactory(this);
        }
    }
}
