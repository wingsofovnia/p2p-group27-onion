package de.tum.p2p.onion.forwarding.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import lombok.val;

import java.util.List;

/**
 * A {@code MessageToMessageDecoder} inbound handler that discards
 * missized {@link DatagramPacket}.
 */
public class MissizedDatagramDiscarder extends MessageToMessageDecoder<DatagramPacket> {

    private final int expectedSize;

    public MissizedDatagramDiscarder(int expectedSize) {
        this.expectedSize = expectedSize;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        val datagramContent = msg.content();
        val datagramContentSize = datagramContent.readableBytes();

        if (datagramContentSize != expectedSize) {
            ReferenceCountUtil.release(msg);
            return;
        }

        out.add(msg);
    }
}
