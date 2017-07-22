package de.tum.p2p.onion.forwarding.netty.handler.server;

import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendMessage;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendedMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

import static de.tum.p2p.onion.forwarding.netty.context.Route.from;

/**
 * {@code TunnelExtendHandler} handles ONION_TUNNEL_EXTEND messages that are designated
 * to <strong>this very</strong> peer, that means this peer is a one that was requested
 * by other peers to extend the tunnel with (msg.destination = this.peer.address).
 * <p>
 * Otherwise the ONION_TUNNEL_EXTEND message is propagated down to {@link TunnelExtendPropagator}
 * where message is forwarded to the NEXT hop of the tunnel.
 */
public class TunnelExtendHandler extends MessageToMessageDecoder<TunnelExtendMessage> {

    private static final Logger log = LoggerFactory.getLogger(TunnelExtendHandler.class);

    private final OnionAuthorizer onionAuth;
    private final Router router;

    public TunnelExtendHandler(OnionAuthorizer onionAuth, Router router) {
        this.onionAuth = onionAuth;
        this.router = router;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TunnelExtendMessage tunnelExtendMsg, List<Object> out)
            throws Exception {
        val tunnelId = tunnelExtendMsg.tunnelId();
        val requestId = tunnelExtendMsg.requestId();

        val localAddress = (InetSocketAddress) ctx.channel().localAddress();
        val extendDestination = tunnelExtendMsg.destinationSocketAddress();

        log.debug("[{}][{}] ONION_TUNNEL_EXTEND({}) request received by {} (from {}), req_id = {}",
            ctx.channel().localAddress(), tunnelId,
            extendDestination, ctx.channel().localAddress(), ctx.channel().remoteAddress(),
            requestId);

        if (localAddress.equals(extendDestination)) {
            log.debug("[{}][{}] ONION_TUNNEL_EXTEND({}) reached the last node in the tunnel, req_id = {}",
                ctx.channel().localAddress(), tunnelId,
                extendDestination, requestId);

            val hs1 = tunnelExtendMsg.handshake();
            val futureSessionIdHs2Pair = onionAuth.sessionFactory().responseTo(hs1);

            futureSessionIdHs2Pair.thenAccept(sessionIdHs2Pair -> {
                val sessionId = sessionIdHs2Pair.getLeft();
                val hs2 = sessionIdHs2Pair.getRight();

                router.route(from(tunnelId, ctx.channel(), sessionId));

                val tunnelExtendedMsg = TunnelExtendedMessage.of(tunnelId, requestId, hs2);
                ctx.writeAndFlush(tunnelExtendedMsg);
                log.debug("[{}][{}] ONION_TUNNEL_EXTENDED with HS2 has been sent back to {}, req_id = {}",
                    ctx.channel().localAddress(), tunnelId,
                    ctx.channel().remoteAddress(), requestId);
            });
        } else {
            // If 'localAddress' is not a requested tunnel addition, propagate extension request down the tunnel
            out.add(tunnelExtendMsg);
        }
    }
}
