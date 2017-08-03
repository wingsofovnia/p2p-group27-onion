package de.tum.p2p.onion.forwarding.netty.handler;

import de.tum.p2p.proto.message.onion.forwarding.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

import static de.tum.p2p.util.ByteBufs.safeContent;

/**
 * {@code OnionMessageDecoder} decodes ONION_TUNNEL_* messages
 * from bytes to corresponding message type.
 * <p>
 * If {@link TypedTunnelMessage#guessType(byte[])} fails to determine
 * type, decoder will treat such messages as {@link TunnelDatumEncryptedMessage}
 * (coz it doesn't have a type in a header).
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@Slf4j
public class TunnelMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        val inBytes = safeContent(in);

        switch (TypedTunnelMessage.guessType(inBytes)) {
            case ONION_TUNNEL_EXTEND:
                out.add(TunnelExtendMessage.fromBytes(inBytes));
                break;

            case ONION_TUNNEL_CONNECT:
                out.add(TunnelConnectEncryptedMessage.fromBytes(inBytes));
                break;

            case ONION_TUNNEL_EXTENDED:
                out.add(TunnelExtendedMessage.fromBytes(inBytes));
                break;

            case ONION_TUNNEL_RETIRE:
                out.add(TunnelRetireMessage.fromBytes(inBytes));
                break;

            default:
                try {
                    out.add(TunnelDatumEncryptedMessage.fromBytes(inBytes));
                } catch (Exception e) {
                    throw new UnsupportedMessageTypeException("Unknown onion message type", e);
                }
        }
     }
}
