package de.tum.p2p.onion.forwarding.netty.handler;

import de.tum.p2p.proto.message.onion.forwarding.OnionMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.val;

import java.util.List;

/**
 * {@code OnionMessageEncoder} disassembles {@link OnionMessage}s into bytes
 */
public class OnionMessageEncoder extends MessageToMessageEncoder<OnionMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, OnionMessage msg, List<Object> out) throws Exception {
        val msgBytes = msg.bytes();

        val msgByteBuf = Unpooled.wrappedBuffer(msgBytes);
        out.add(msgByteBuf);
    }
}
