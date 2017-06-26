package de.tum.p2p.onion.forwarding.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.internal.StringUtil;
import lombok.val;

import java.nio.ByteOrder;
import java.util.List;

/**
 * An encoder that prepends the length of the message. The length value is
 * prepended as a binary form.
 * <p>
 * This is a {@link DatagramPacket}-compatible version of
 * {@link LengthFieldPrepender}.
 *
 * @see LengthFieldPrepender
 */
public class DatagramLengthFieldPrepender extends MessageToMessageEncoder<DatagramPacket> {

    private final AccessableLengthFieldPrepender lengthFieldPrepender;

    public DatagramLengthFieldPrepender(int lengthFieldLength) {
        this.lengthFieldPrepender = new AccessableLengthFieldPrepender(lengthFieldLength);
    }

    public DatagramLengthFieldPrepender(int lengthFieldLength, boolean lengthIncludesLengthFieldLength) {
        this.lengthFieldPrepender = new AccessableLengthFieldPrepender(lengthFieldLength,
            lengthIncludesLengthFieldLength);
    }

    public DatagramLengthFieldPrepender(int lengthFieldLength, int lengthAdjustment) {
        this.lengthFieldPrepender = new AccessableLengthFieldPrepender(lengthFieldLength, lengthAdjustment);
    }

    public DatagramLengthFieldPrepender(int lengthFieldLength, int lengthAdjustment,
                                        boolean lengthIncludesLengthFieldLength) {
        this.lengthFieldPrepender = new AccessableLengthFieldPrepender(lengthFieldLength, lengthAdjustment,
            lengthIncludesLengthFieldLength);
    }

    public DatagramLengthFieldPrepender(ByteOrder byteOrder, int lengthFieldLength, int lengthAdjustment,
                                        boolean lengthIncludesLengthFieldLength) {
        this.lengthFieldPrepender = new AccessableLengthFieldPrepender(byteOrder, lengthFieldLength, lengthAdjustment,
            lengthIncludesLengthFieldLength);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        val datagramBody = msg.content();

        lengthFieldPrepender.encode(ctx, datagramBody, out);
        if (out.size() != 2) {
            throw new EncoderException(StringUtil.simpleClassName(lengthFieldPrepender)
                + " must produce two messages (length prefix and data itself.");
        }

        Object prefix = out.get(0);
        Object content = out.get(1);
        if ((prefix instanceof ByteBuf) && (content instanceof ByteBuf)) {
            // Replace the ByteBufs with a DatagramPacket.
            val prefixedDatagramBody = Unpooled.copiedBuffer((ByteBuf) prefix, (ByteBuf) content);
            out.set(0, msg.replace(prefixedDatagramBody));
            out.remove(1);
        } else {
            throw new EncoderException(StringUtil.simpleClassName(lengthFieldPrepender)
                + " must produce objects of ByteBufs type only.");
        }
    }

    private static final class AccessableLengthFieldPrepender extends LengthFieldPrepender {

        public AccessableLengthFieldPrepender(int lengthFieldLength) {
            super(lengthFieldLength);
        }

        public AccessableLengthFieldPrepender(int lengthFieldLength, boolean lengthIncludesLengthFieldLength) {
            super(lengthFieldLength, lengthIncludesLengthFieldLength);
        }

        public AccessableLengthFieldPrepender(int lengthFieldLength, int lengthAdjustment) {
            super(lengthFieldLength, lengthAdjustment);
        }

        public AccessableLengthFieldPrepender(int lengthFieldLength, int lengthAdjustment, boolean lengthIncludesLengthFieldLength) {
            super(lengthFieldLength, lengthAdjustment, lengthIncludesLengthFieldLength);
        }

        public AccessableLengthFieldPrepender(ByteOrder byteOrder, int lengthFieldLength, int lengthAdjustment, boolean lengthIncludesLengthFieldLength) {
            super(byteOrder, lengthFieldLength, lengthAdjustment, lengthIncludesLengthFieldLength);
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            super.encode(ctx, msg, out);
        }
    }
}
