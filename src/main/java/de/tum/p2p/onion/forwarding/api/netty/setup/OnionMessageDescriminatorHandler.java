package de.tum.p2p.onion.forwarding.api.netty.setup;

import de.tum.p2p.onion.forwarding.OnionForwarder;
import de.tum.p2p.proto.message.onion.forwarding.api.OnionCoverMessage;
import de.tum.p2p.proto.message.onion.forwarding.api.OnionTunnelBuildMessage;
import de.tum.p2p.proto.message.TypedMessage;
import de.tum.p2p.proto.message.onion.forwarding.api.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

@Slf4j
public class OnionMessageDescriminatorHandler extends ByteToMessageDecoder {

    private OnionForwarder forwarder;

    public OnionMessageDescriminatorHandler(OnionForwarder forwarder) {
        this.forwarder = forwarder;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            val buffer = Unpooled.copiedBuffer(in);
            val rawIn = buffer.array();
            val inMsgType = TypedMessage.guessType(rawIn);

            log.trace("REGISTERED MESSAGE: " + inMsgType.name());
            switch (inMsgType) {
                case ONION_COVER:
                    out.add(OnionCoverMessage.fromBytes(buffer.nioBuffer()));
                    break;
                case ONION_TUNNEL_BUILD:
                    out.add(OnionTunnelBuildMessage.fromBytes(in.nioBuffer()));
                    break;
                case ONION_TUNNEL_READY:
                    out.add(OnionTunnelReadyMessage.fromBytes(in.nioBuffer()));
                    break;
                case ONION_TUNNEL_INCOMING:
                    out.add(OnionTunnelIncomingMessage.fromBytes(in.nioBuffer()));
                    break;
                case ONION_TUNNEL_DESTROY:
                    out.add(OnionTunnelDestroyMessage.fromBytes(in.nioBuffer()));
                    break;
                case ONION_TUNNEL_DATA:
                    out.add(OnionTunnelDataMessage.fromBytes(in.nioBuffer()));
                    break;
                case ONION_TUNNEL_ERROR:
                    out.add(OnionTunnelErrorMessage.fromBytes(in.nioBuffer()));
                    break;
                default:
                    ReferenceCountUtil.release(in);
                    throw new Exception("BAD PROTOCOL, DROPPED PACKAGE");
            }
        } catch (Exception e) {
            log.error("BAD PROTOCOL, PACKET DOES NOT FOLLOW PROTOCOL, TERMINATE!");
            ctx.close();
        }
    }
}
