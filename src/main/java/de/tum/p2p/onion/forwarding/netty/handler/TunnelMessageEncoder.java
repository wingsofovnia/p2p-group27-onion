package de.tum.p2p.onion.forwarding.netty.handler;

import de.tum.p2p.proto.message.onion.forwarding.TunnelMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

/**
 * {@code OnionMessageDecoder} decodes ONION_TUNNEL_* messages
 * from bytes to corresponding message type.
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Slf4j
public class TunnelMessageEncoder extends MessageToMessageEncoder<TunnelMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TunnelMessage msg, List<Object> out) throws Exception {
        val msgBytes = msg.bytes();

        val msgByteBuf = Unpooled.wrappedBuffer(msgBytes);
        out.add(msgByteBuf);
    }
}
