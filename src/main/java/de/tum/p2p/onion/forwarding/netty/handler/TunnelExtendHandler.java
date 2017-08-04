package de.tum.p2p.onion.forwarding.netty.handler;

import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendMessage;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendedMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

/**
 * {@code TunnelExtendHandler} handles incoming {@link TunnelExtendMessage} received
 * by the onion that is requested to be a new peer in the tunnel. The handler forms
 * a {@link TunnelExtendedMessage} and propagates up the tunnel.
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Slf4j
public class TunnelExtendHandler extends MessageToMessageDecoder<TunnelExtendMessage> {

    private final RoutingContext routingContext;
    private final OnionAuthorizer onionAuth;

    public TunnelExtendHandler(RoutingContext routingContext, OnionAuthorizer onionAuth) {
        this.routingContext = routingContext;
        this.onionAuth = onionAuth;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TunnelExtendMessage extendMsg, List<Object> out) throws Exception {
        val tunnelId = extendMsg.tunnelId();
        val requestId = extendMsg.requestId();

        val hs1 = extendMsg.handshake();
        val futureSessionIdHs2Pair = onionAuth.sessionFactory().responseTo(hs1);

        futureSessionIdHs2Pair.thenAccept(sessionIdHs2Pair -> {
            val sessionId = sessionIdHs2Pair.getLeft();
            val hs2 = sessionIdHs2Pair.getRight();

            routingContext.setPrevHop(tunnelId, ctx.channel());
            routingContext.setSessionId(tunnelId, sessionId);

            val tunnelExtendedMsg = new TunnelExtendedMessage(tunnelId, requestId, hs2);
            ctx.writeAndFlush(tunnelExtendedMsg)
                .addListener((ChannelFutureListener) transfer -> {
                    if (!transfer.isSuccess())
                        throw new OnionTunnelingException("Failed to response with HS2", transfer.cause());

                    log.debug("Tunnel Extend_ed (ack) has been sent back via tunnel {}, peer = {}", tunnelId,
                        ctx.channel().remoteAddress());
                });
        });
    }
}
