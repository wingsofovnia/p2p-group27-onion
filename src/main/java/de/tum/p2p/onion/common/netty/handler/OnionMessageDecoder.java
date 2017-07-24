package de.tum.p2p.onion.common.netty.handler;

import de.tum.p2p.proto.message.TypedMessage;
import de.tum.p2p.proto.message.onion.forwarding.TunnelDatumMessage;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendMessage;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendedMessage;
import de.tum.p2p.proto.message.onion.forwarding.TunnelRetireMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import lombok.val;

import java.util.List;

import static de.tum.p2p.util.netty.ByteBufs.safeContent;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code OnionMessageDecoder} decodes ONION_TUNNEL_* messages
 * from bytes to corresponding message type.
 */
public class OnionMessageDecoder extends ByteToMessageDecoder {

    private final byte[] hmacKey;

    public OnionMessageDecoder(byte[] hmacKey) {
        this.hmacKey = notNull(hmacKey);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        val inBytes = safeContent(in);

        switch (TypedMessage.guessType(inBytes)) {
            case ONION_TUNNEL_EXTEND:
                out.add(TunnelExtendMessage.fromBytes(inBytes));
                break;

            case ONION_TUNNEL_EXTENDED:
                out.add(TunnelExtendedMessage.fromBytes(inBytes));
                break;

            case ONION_TUNNEL_DATUM:
                out.add(TunnelDatumMessage.fromBytes(inBytes, hmacKey));
                break;

            case ONION_TUNNEL_RETIRE:
                out.add(TunnelRetireMessage.fromBytes(inBytes));
                break;

            default:
                throw new UnsupportedMessageTypeException("Unknown onion message type");
        }
     }
}
