package de.tum.p2p.onion.forwarding.netty.handler;

import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.channel.ClientChannelFactory;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.proto.message.onion.forwarding.TunnelConnectEncryptedMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

/**
 * {@code TunnelConnectHandler} handles {@link TunnelConnectEncryptedMessage}s, i.e.
 * decides whether the onion should propagate the tunnel request down the tunnel
 * or create a {@link de.tum.p2p.proto.message.onion.forwarding.TunnelExtendMessage} and
 * ask the peer to be a new member of the tunnel.
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Slf4j
public class TunnelConnectHandler extends MessageToMessageDecoder<TunnelConnectEncryptedMessage> {

    private final RoutingContext routingContext;
    private final OnionAuthorizer onionAuth;
    private final ClientChannelFactory clientChannelFactory;

    public TunnelConnectHandler(RoutingContext routingContext, OnionAuthorizer onionAuth, ClientChannelFactory clientChannelFactory) {
        this.routingContext = routingContext;
        this.onionAuth = onionAuth;
        this.clientChannelFactory = clientChannelFactory;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TunnelConnectEncryptedMessage ecrConnMsg, List<Object> out)
            throws Exception {
        val tunnelId = ecrConnMsg.tunnelId();

        if (routingContext.hasNextHop(tunnelId)) {
            routingContext.nextHop(tunnelId).writeAndFlush(ecrConnMsg)
                .addListener((ChannelFutureListener) transfer -> {
                    if (!transfer.isSuccess())
                        throw new OnionTunnelingException("Failed to propagate extension request " +
                            "for tunnel " + tunnelId, transfer.cause());
                });

            log.debug("Encrypted ONION_TUNNEL_CONNECT has been propagated down the {} tunnel by {} to {}",
                tunnelId, ctx.channel().localAddress(), ctx.channel().remoteAddress());
            return;
        }

        val currentSessionId = routingContext.sessionId(tunnelId);
        val futureDecryptedPayload = onionAuth.decrypt(ecrConnMsg.payload(), currentSessionId);

        futureDecryptedPayload.thenAccept(decryptedPayload -> {
            val peeledConnectMsg = ecrConnMsg.peel(decryptedPayload.bytes());

            val connMsg = peeledConnectMsg.toConnectMessage();

            val nextHop = connMsg.socketDestination();
            clientChannelFactory.connect(nextHop).thenAccept(channel -> {
                routingContext.setNextHop(tunnelId, channel);

                channel.writeAndFlush(connMsg.deriveExtendMessage());
                log.debug("ONION_TUNNEL_EXTEND confirmation with HS has been sent via tunnel {} from {} to {}",
                    tunnelId, ctx.channel().localAddress(), ctx.channel().remoteAddress());
            });
        }).exceptionally(throwable -> {
            log.error("Failed to decrypt ONION_TUNNEL_CONNECT designated to me ({}) received from {} via tunnel {}",
                ctx.channel().localAddress(), ctx.channel().remoteAddress(), tunnelId, throwable);
            return null;
        });
    }
}
