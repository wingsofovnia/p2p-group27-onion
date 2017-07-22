package de.tum.p2p.onion.forwarding.netty.channel;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.onion.forwarding.netty.handler.OnionMessageDecoder;
import de.tum.p2p.onion.forwarding.netty.handler.OnionMessageEncoder;
import de.tum.p2p.proto.message.Message;
import de.tum.p2p.proto.message.onion.forwarding.OnionMessage;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import lombok.val;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

abstract class ChannelFactory<T extends Channel> {

    static {
        // Enable Netty to use Sl4j
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    // Length-prefix framing
    protected static final int FRAME_LENGTH_PREFIX_LENGTH = Message.LENGTH_PREFIX_BYTES;
    protected static final int FRAME_LENGTH = OnionMessage.BYTES + Message.LENGTH_PREFIX_BYTES;

    protected EventLoopGroup bossEventLoop;
    protected Class<? extends T> channel;
    protected Map<ChannelOption, Object> channelOptions;

    protected byte[] hmacKey;

    protected OnionAuthorizer onionAuthorizer;
    protected Router router;
    protected EventBus eventBus;

    protected LogLevel loggerLevel;

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

    protected ChannelInitializer messagingChannel(Consumer<ChannelPipeline> domainHandlers) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                val pipe = ch.pipeline();

                if (loggerLevel != null)
                    pipe.addLast(new LoggingHandler(loggerLevel));

                pipe.addLast(new FixedLengthFrameDecoder(FRAME_LENGTH));
                pipe.addLast(new LengthFieldBasedFrameDecoder(FRAME_LENGTH, 0,
                    FRAME_LENGTH_PREFIX_LENGTH, -FRAME_LENGTH_PREFIX_LENGTH, FRAME_LENGTH_PREFIX_LENGTH, true));
                pipe.addLast(new LengthFieldPrepender(FRAME_LENGTH_PREFIX_LENGTH, true));

                pipe.addLast(new OnionMessageDecoder(hmacKey));
                pipe.addLast(new OnionMessageEncoder());

                domainHandlers.accept(pipe);
            }
        };
    }
}
