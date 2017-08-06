package de.tum.p2p.onion.forwarding.netty.handler;

import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.OnionDataForwardingException;
import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.proto.message.onion.forwarding.composite.TunnelConnect;
import de.tum.p2p.proto.message.onion.forwarding.composite.TunnelDatum;
import de.tum.p2p.proto.message.onion.forwarding.composite.TunnelRelayMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

/**
 * {@code TunnelRelayHandler} performs onion decryption of {@link TunnelRelayMessage}'s
 * {@link de.tum.p2p.proto.message.onion.forwarding.composite.TunnelRelayPayload}s. As
 * soon as the last layer is peeled out, the payload is extracted and propagated down the
 * Netty's channel so further handlers can process payloads separately.
 *
 * @see TunnelDatumHandler
 * @see TunnelConnectHandler
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Slf4j
public class TunnelRelayHandler extends SimpleChannelInboundHandler<TunnelRelayMessage> {

    private final OnionAuthorizer onionAuth;
    private final RoutingContext routingContext;

    public TunnelRelayHandler(OnionAuthorizer onionAuth, RoutingContext routingContext) {
        this.onionAuth = onionAuth;
        this.routingContext = routingContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelRelayMessage relay) throws Exception {
        val tunnelId = relay.tunnelId();
        val relayPayload = relay.payload();

        val futureDeciphertext = onionAuth.decrypt(relayPayload, routingContext.sessionId(tunnelId));

        futureDeciphertext.thenAccept(deciphertext -> {
            val decryptedPayload = deciphertext.bytes();
            val peeledRelay = relay.peel(decryptedPayload);

            if (!deciphertext.isPlaintext() && !routingContext.hasNextHop(tunnelId))
                throw new OnionTunnelingException("This onion was supposed to peel last crypto layer, " +
                    "but not a plaintext returned by onionAuth");

            if (routingContext.hasNextHop(tunnelId)) {
                routingContext.nextHop(tunnelId).writeAndFlush(peeledRelay);
                log.debug("Encrypted relay message has been propagated down the {} tunnel by {} to {}",
                    tunnelId, ctx.channel().localAddress(), ctx.channel().remoteAddress());
                return;
            }

            val rawRelayPayload = peeledRelay.payload();

            switch (MessageType.fromBytes(rawRelayPayload)) {
                case ONION_TUNNEL_COVER:
                case ONION_TUNNEL_DATUM:
                    // FIXME: Netty SimpleChannelInboundHandler doesn't recognize generic type parameters
                    // FIXME: therefore here another Pair tuple imp is used.
                    ctx.fireChannelRead(new javafx.util.Pair<>(tunnelId, TunnelDatum.fromBytes(rawRelayPayload)));
                    break;

                case ONION_TUNNEL_CONNECT:
                    ctx.fireChannelRead(Pair.of(tunnelId, TunnelConnect.fromBytes(rawRelayPayload)));
                    break;

                default:
                    log.error("Unknown message type of relay payload from {} after last decrypt round, tunnel {}.",
                        ctx.channel().remoteAddress(), tunnelId);
                    throw new OnionDataForwardingException("Unknown message type of relay payload after last decrypt round");
            }
        });
    }
}
