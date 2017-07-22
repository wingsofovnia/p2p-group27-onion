package de.tum.p2p.onion.forwarding.netty.handler.client;

import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendedMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * {@code TunnelExtendedPropagator} propagates ONION_TUNNEL_EXTEND<strong>ED</strong>
 * back to the PREV hop of the tunnel.
 *
 * @see TunnelExtendedHandler
 */
public class TunnelExtendedPropagator extends MessageToMessageDecoder<TunnelExtendedMessage> {

    private static final Logger log = LoggerFactory.getLogger(TunnelExtendedPropagator.class);

    private final Router router;

    public TunnelExtendedPropagator(Router router) {
        this.router = router;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TunnelExtendedMessage tunnelExtendedMsg, List<Object> out)
            throws Exception {
        val tunnelId = tunnelExtendedMsg.tunnelId();
        val requestId = tunnelExtendedMsg.requestId();

        val prevKnownTunnelChannel = router.routePrev(tunnelId).orElseThrow(()
                -> new OnionTunnelingException("Cannot propagate ONION_TUNNEL_EXTENDED back, route not found"));

        log.debug("[{}][{}] ONION_TUNNEL_EXTENDED is above being propagated BACK to {} by {}, req_id = {}",
            ctx.channel().localAddress(), tunnelId,
            prevKnownTunnelChannel.remoteAddress(), ctx.channel().localAddress(),
            requestId);

        prevKnownTunnelChannel.writeAndFlush(tunnelExtendedMsg)
            .addListener((ChannelFutureListener) transfer -> {
                if (!transfer.isSuccess())
                    throw new OnionTunnelingException("Failed to propagate extension confirmation " +
                        "for tunnel " + tunnelId, transfer.cause());
            });

        log.debug("[{}][{}] ONION_TUNNEL_EXTENDED has been passed through via {}, req_id = {}",
            ctx.channel().localAddress(), tunnelId,
            prevKnownTunnelChannel.remoteAddress(),
            requestId);
    }
}
