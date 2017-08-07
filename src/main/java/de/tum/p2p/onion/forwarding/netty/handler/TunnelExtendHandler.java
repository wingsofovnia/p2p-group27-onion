package de.tum.p2p.onion.forwarding.netty.handler;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.onion.forwarding.netty.event.TunnelExtendReceived;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendMessage;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendedMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * {@code TunnelExtendHandler} handles incoming {@link TunnelExtendMessage} received
 * by the onion that is requested to be a new peer in the tunnel. The handler generates
 * HS2, forms a {@link TunnelExtendedMessage} and propagates the message up the tunnel.
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Slf4j
public class TunnelExtendHandler extends SimpleChannelInboundHandler<TunnelExtendMessage> {

    private final RoutingContext routingContext;
    private final OnionAuthorizer onionAuth;
    private final EventBus eventBus;

    public TunnelExtendHandler(RoutingContext routingContext, OnionAuthorizer onionAuth, EventBus eventBus) {
        this.routingContext = routingContext;
        this.onionAuth = onionAuth;
        this.eventBus = eventBus;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelExtendMessage extendMsg) throws Exception {
        val tunnelId = extendMsg.tunnelId();
        val requestId = extendMsg.requestId();

        val hs1 = extendMsg.handshake();
        val futureSessionIdHs2Pair = onionAuth.sessionFactory().responseTo(hs1);

        futureSessionIdHs2Pair.thenAccept(sessionIdHs2Pair -> {
            val sessionId = sessionIdHs2Pair.getLeft();
            val hs2 = sessionIdHs2Pair.getRight();

            routingContext.setPrevHop(tunnelId, ctx.channel());
            routingContext.setSessionId(tunnelId, sessionId);

            eventBus.post(TunnelExtendReceived.from(tunnelId));

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
