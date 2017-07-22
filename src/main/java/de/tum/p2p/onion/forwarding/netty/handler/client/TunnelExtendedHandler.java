package de.tum.p2p.onion.forwarding.netty.handler.client;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.onion.forwarding.netty.event.TunnelExtendedReceived;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendedMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * {@code TunnelExtendedHandler} handles ONION_TUNNEL_EXTEND<strong>ED</strong> messages
 * that are designated to <strong>this very</strong> peer, that means this peer is a one
 * that requested to extend the tunnel.
 * <p>
 * Otherwise the ONION_TUNNEL_EXTEND<strong>ED</strong> message is propagated down to the
 * {@code TunnelExtendedPropagator} where message is forwarded BACK to the PREV hop of the tunnel.
 *
 * @see TunnelExtendedPropagator
 */
public class TunnelExtendedHandler extends MessageToMessageDecoder<TunnelExtendedMessage> {

    private static final Logger log = LoggerFactory.getLogger(TunnelExtendedHandler.class);

    private final OnionAuthorizer onionAuth;
    private final Router router;
    private final EventBus eventBus;

    public TunnelExtendedHandler(OnionAuthorizer onionAuth, Router router, EventBus eventBus) {
        this.onionAuth = onionAuth;
        this.router = router;
        this.eventBus = eventBus;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TunnelExtendedMessage tunnelExtendedMsg, List<Object> out) throws Exception {
        val tunnelId = tunnelExtendedMsg.tunnelId();
        val requestId = tunnelExtendedMsg.requestId();

        if (!router.hasRoutePrev(tunnelId)) {
            log.debug("[{}][{}] ONION_TUNNEL_EXTENDED request received by {} (from {}), req_id = {}",
                ctx.channel().localAddress(), tunnelId,
                ctx.channel().localAddress(), ctx.channel().remoteAddress(),
                requestId);

            val hs2 = tunnelExtendedMsg.handshake();
            val futureSessionId = onionAuth.sessionFactory().confirm(hs2);

            futureSessionId.thenAccept(sessionId -> {
                eventBus.post(TunnelExtendedReceived.of(tunnelId, sessionId, requestId));

                log.debug("[{}][{}] TunnelExtendedReceived(req_id: {}, tunnelId: {}) event has been dispatched",
                    ctx.channel().localAddress(), tunnelId,
                    requestId, tunnelId);
            });
        } else {
            // If 'me' is not a peer who requested tunnel addition, propagate extension request up the tunnel
            out.add(tunnelExtendedMsg);
        }
    }
}
