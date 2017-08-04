package de.tum.p2p.onion.forwarding.netty.channel;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.netty.context.OriginatorContext;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.onion.forwarding.netty.handler.TunnelMessageDecoder;
import de.tum.p2p.onion.forwarding.netty.handler.TunnelMessageEncoder;
import de.tum.p2p.proto.message.onion.forwarding.TunnelMessage;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import lombok.val;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A base class for configuration channel factories
 *
 * @param <T> a socket channel that will be used for bootstrapping
 *            the channel
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
abstract class ChannelFactory<T extends Channel> {

    static {
        // Enable Netty to use Sl4j
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    protected static final int FRAME_LENGTH = TunnelMessage.BYTES;

    protected EventLoopGroup bossEventLoop;
    protected Class<? extends T> channel;
    protected Map<ChannelOption, Object> channelOptions;

    protected OnionAuthorizer onionAuthorizer;
    protected OriginatorContext originatorContext;
    protected RoutingContext routingContext;
    protected EventBus eventBus;

    protected LogLevel loggerLevel;

    /**
     * Convers Netty's {@link ChannelFuture} to Java8's {@link CompletableFuture}
     *
     * @param nettyChannelFuture ChannelFuture
     * @return CompletableFuture[Channel]
     */
    protected CompletableFuture<Channel> toCompletableFuture(ChannelFuture nettyChannelFuture) {
        val completableChannelFuture = new CompletableFuture<Channel>();

        nettyChannelFuture.addListener((ChannelFuture f) -> {
            if (f.isCancelled()) {
                completableChannelFuture.cancel(false);
            } else if (f.cause() != null) {
                completableChannelFuture.completeExceptionally(f.cause());
            } else {
                completableChannelFuture.complete(f.channel());
            }
        });

        return completableChannelFuture;
    }

    /**
     * Builds a messaging pipe with prefedined {@link FixedLengthFrameDecoder},
     * {@link TunnelMessageEncoder} and {@link TunnelMessageDecoder}.
     *
     * @param domainHandlers a pipe configurator
     * @return configured ChannelInitializer
     */
    protected ChannelInitializer messagingChannel(Consumer<ChannelPipeline> domainHandlers) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                val pipe = ch.pipeline();

                if (loggerLevel != null)
                    pipe.addLast(new LoggingHandler(loggerLevel));

                pipe.addLast(new FixedLengthFrameDecoder(FRAME_LENGTH));

                pipe.addLast(new TunnelMessageEncoder());
                pipe.addLast(new TunnelMessageDecoder());

                domainHandlers.accept(pipe);
            }
        };
    }
}
