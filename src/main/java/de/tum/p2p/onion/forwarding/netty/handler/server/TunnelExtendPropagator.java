package de.tum.p2p.onion.forwarding.netty.handler.server;

import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.channel.ClientChannelFactory;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * {@code TunnelExtendPropagator} propagates ONION_TUNNEL_EXTEND to the NEXT hop of the tunnel.
 *
 * @see TunnelExtendHandler
 */
public class TunnelExtendPropagator extends MessageToMessageDecoder<TunnelExtendMessage> {

    private static final Logger log = LoggerFactory.getLogger(TunnelExtendPropagator.class);

    private final ClientChannelFactory clientChannelFactory;
    private final Router router;

    public TunnelExtendPropagator(ClientChannelFactory clientChannelFactory, Router router) {
        this.clientChannelFactory = clientChannelFactory;
        this.router = router;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TunnelExtendMessage tunnelExtendMsg, List<Object> out)
            throws Exception {
        val tunnelId = tunnelExtendMsg.tunnelId();
        val requestId = tunnelExtendMsg.requestId();
        val extendDestination = tunnelExtendMsg.destinationSocketAddress();

        log.debug("[{}][{}] ONION_TUNNEL_EXTEND({}) is above being propagated to {} by {}, req_id = {}",
            ctx.channel().localAddress(), tunnelId,
            extendDestination, extendDestination, ctx.channel().localAddress(),
            requestId);

        val nextKnownTunnelChannel = router.routeNext(tunnelId);
        if (nextKnownTunnelChannel.isPresent()) {
            // There are some peers behind us and before the peer we are requested to extend by

            // Ask peer behind this peer to propagate extension request further
            nextKnownTunnelChannel.ifPresent(c -> c.writeAndFlush(tunnelExtendMsg));

            log.debug("[{}][{}] ONION_TUNNEL_EXTEND({}) has been passed through via {}, req_id = {}",
                ctx.channel().localAddress(), tunnelId,
                extendDestination, nextKnownTunnelChannel.map(Channel::remoteAddress),
                requestId);
        } else {
            // This peer is on the tail of the tunnel.

            // This peer is responsible for propagating extension request (with hs1) to a new peer
            val futureNextTunnelChannel = clientChannelFactory.connect(extendDestination);

            futureNextTunnelChannel.thenAccept(nextTunnelChannel -> {
                nextTunnelChannel.writeAndFlush(tunnelExtendMsg)
                    .addListener((ChannelFutureListener) transfer -> {
                        if (!transfer.isSuccess())
                            throw new OnionTunnelingException("Failed to extend the tunnel " + tunnelId, transfer.cause());

                        val tunnelRoute = router.route(tunnelId).orElseThrow(()
                            -> new OnionTunnelingException("Cant update router after tunnel extension, prev not found"));
                        tunnelRoute.next(nextTunnelChannel);
                    });

                log.debug("[{}][{}] ONION_TUNNEL_EXTEND({}) has been passed to the destination peer {}, req_id = {}",
                    ctx.channel().localAddress(), tunnelId,
                    extendDestination, nextTunnelChannel.remoteAddress(),
                    requestId);
            });
        }
    }
}
