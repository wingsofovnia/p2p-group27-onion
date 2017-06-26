package de.tum.p2p.onion.forwarding.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.val;

import java.nio.ByteOrder;
import java.util.List;

/**
 * A decoder that splits content of the received {@link DatagramPacket}s dynamically
 * by the value of the length field in the message.
 * <p>
 * This is a {@link DatagramPacket}-compatible version of
 * {@link LengthFieldBasedFrameDecoder}.
 *
 * @see LengthFieldBasedFrameDecoder
 */
public class DatagramLengthFieldBasedFrameDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private final AccessableLengthFieldBasedFrameDecoder frameDecoder;

    public DatagramLengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        this.frameDecoder = new AccessableLengthFieldBasedFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }

    public DatagramLengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                                                int lengthAdjustment, int initialBytesToStrip) {
        this.frameDecoder = new AccessableLengthFieldBasedFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength,
            lengthAdjustment, initialBytesToStrip);
    }

    public DatagramLengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                                                int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        this.frameDecoder = new AccessableLengthFieldBasedFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength,
            lengthAdjustment, initialBytesToStrip, failFast);
    }

    public DatagramLengthFieldBasedFrameDecoder(ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset,
                                                int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip,
                                                boolean failFast) {
        this.frameDecoder = new AccessableLengthFieldBasedFrameDecoder(byteOrder, maxFrameLength, lengthFieldOffset,
            lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        val content = msg.content();
        val deframedContent = frameDecoder.decode(ctx, content);

        if (deframedContent != null) {
            out.add(msg.replace((ByteBuf) deframedContent));
        }
    }

    private static final class AccessableLengthFieldBasedFrameDecoder extends LengthFieldBasedFrameDecoder {

        public AccessableLengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
            super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
        }

        public AccessableLengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                                                      int lengthAdjustment, int initialBytesToStrip) {
            super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
        }

        public AccessableLengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                                                      int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
            super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
        }

        public AccessableLengthFieldBasedFrameDecoder(ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset,
                                                      int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip,
                                                      boolean failFast) {
            super(byteOrder, maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment,
                initialBytesToStrip, failFast);
        }

        @Override
        protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
            return super.decode(ctx, in);
        }
    }
}
