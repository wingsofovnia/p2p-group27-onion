package de.tum.p2p.onion.common.netty;

import de.tum.p2p.onion.common.netty.handler.OnionMessageDecoder;
import de.tum.p2p.onion.common.netty.handler.OnionMessageEncoder;
import de.tum.p2p.proto.message.Message;
import de.tum.p2p.proto.message.onion.forwarding.OnionMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code OnionPipelineBuilder} allows to construct typical Netty pipeline for
 * Onion channels while keeping capability of adding custom handlers via
 * {@link OnionPipelineBuilder#handler(ChannelHandler)}
 * <p>
 * Apart from custom handlers, {@code OnionPipelineBuilder} consists of:
 * <ul>
 *     <li>{@link io.netty.handler.codec.LengthFieldBasedFrameDecoder}</li>
 *     <li>{@link io.netty.handler.codec.LengthFieldPrepender}</li>
 *     <li>{@link io.netty.handler.codec.FixedLengthFrameDecoder} to discard
 *     missized frames</li>
 *     <li>{@link de.tum.p2p.onion.common.netty.handler.OnionMessageDecoder}
 *     and {@link de.tum.p2p.onion.common.netty.handler.OnionMessageEncoder}</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 * val onionPipeline = new OnionPipelineBuilder()
 *      .logLevel(LogLevel.DEBUG)
 *      .hmacKey(hmacKey)
 *      .handler(new MyCustomHandler())
 *      .handler(new MySecondCustomHandler())
 *      .build();
 *
 * val bootstrap = new Bootstrap();
 *
 * bootstrap
 *      //....
 *      .handler(onionPipeline);
 * </pre>
 */
@Slf4j
public final class OnionPipelineBuilder {

    private static final int FRAME_LENGTH_PREFIX_LENGTH = Message.LENGTH_PREFIX_BYTES;
    private static final int FRAME_LENGTH = OnionMessage.BYTES + Message.LENGTH_PREFIX_BYTES;

    private final List<ChannelHandler> handlers = new ArrayList<>();

    private byte[] hmacKey;

    private LogLevel logLevel = log.isDebugEnabled() ? LogLevel.DEBUG : null;

    public OnionPipelineBuilder hmacKey(byte[] hmacKey) {
        this.hmacKey = hmacKey;
        return this;
    }

    public OnionPipelineBuilder handler(ChannelHandler handler) {
        this.handlers.add(notNull(handler));
        return this;
    }

    public OnionPipelineBuilder logLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public ChannelInitializer build() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                val pipe = ch.pipeline();

                if (logLevel != null)
                    pipe.addLast(new LoggingHandler(logLevel));

                pipe.addLast(new FixedLengthFrameDecoder(FRAME_LENGTH));
                pipe.addLast(new LengthFieldBasedFrameDecoder(FRAME_LENGTH, 0,
                    FRAME_LENGTH_PREFIX_LENGTH, -FRAME_LENGTH_PREFIX_LENGTH, FRAME_LENGTH_PREFIX_LENGTH, true));
                pipe.addLast(new LengthFieldPrepender(FRAME_LENGTH_PREFIX_LENGTH, true));

                pipe.addLast(new OnionMessageDecoder(hmacKey));
                pipe.addLast(new OnionMessageEncoder());

                handlers.forEach(pipe::addLast);
            }
        };
    }
}
