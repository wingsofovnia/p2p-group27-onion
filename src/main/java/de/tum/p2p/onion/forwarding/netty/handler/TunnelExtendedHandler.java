package de.tum.p2p.onion.forwarding.netty.handler;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.onion.forwarding.netty.event.TunnelExtendedReceived;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendedMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * {@code TunnelExtendedHandler} handles ONION_TUNNEL_EXTEND<strong>ED</strong> messages
 * and make sure the message arrive to the first peer which will complete tunnel extension,
 * i.e. the handler will propagate the message down the tunnel till there is a prev hop.
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Slf4j
public class TunnelExtendedHandler extends SimpleChannelInboundHandler<TunnelExtendedMessage> {

    private final OnionAuthorizer onionAuth;
    private final RoutingContext routingContext;
    private final EventBus eventBus;

    public TunnelExtendedHandler(OnionAuthorizer onionAuth, RoutingContext routingContext, EventBus eventBus) {
        this.onionAuth = onionAuth;
        this.routingContext = routingContext;
        this.eventBus = eventBus;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelExtendedMessage tunnelExtendedMsg) throws Exception {
        val tunnelId = tunnelExtendedMsg.tunnelId();
        val requestId = tunnelExtendedMsg.requestId();

        if (!routingContext.hasPrevHop(tunnelId)) {
            log.debug("ONION_TUNNEL_EXTENDED request received by {} from {} with request id = {}",
                ctx.channel().localAddress(), ctx.channel().remoteAddress(), requestId);

            val hs2 = tunnelExtendedMsg.handshake();
            val futureSessionId = onionAuth.sessionFactory().confirm(hs2);

            futureSessionId.thenAccept(sessionId -> {
                eventBus.post(TunnelExtendedReceived.of(tunnelId, sessionId, requestId));

                log.debug("TunnelExtendedReceived(req_id: {}, tunnelId: {}) event has been dispatched by {}",
                    requestId, tunnelId, ctx.channel().localAddress());
            });
        } else {
            // If 'me' is not a peer who requested tunnel addition, propagate extension request up the tunnel
            val prevKnownTunnelChannel = routingContext.prevHop(tunnelId);

            prevKnownTunnelChannel.writeAndFlush(tunnelExtendedMsg)
                .addListener((ChannelFutureListener) transfer -> {
                    if (!transfer.isSuccess())
                        throw new OnionTunnelingException("Failed to propagate extension confirmation " +
                            "for tunnel " + tunnelId, transfer.cause());

                    log.debug("ONION_TUNNEL_EXTENDED has been propagated down the {} tunnel by {} to {}",
                        tunnelId, ctx.channel().localAddress(), ctx.channel().remoteAddress());
                });
        }
    }
}
