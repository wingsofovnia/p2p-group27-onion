package de.tum.p2p.onion.forwarding.netty.handler;

import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.proto.message.onion.forwarding.TunnelDatumEncryptedMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

/**
 * {@code TunnelConnectHandler} handles encrypted datum message in {@link TunnelDatumEncryptedMessage}.
 * It peels one layer of encryption and then may propagate the encrypted datum down the tunnel or
 * unwrap it to {@link de.tum.p2p.proto.message.onion.forwarding.TunnelDatumMessage} so that the
 * {@link TunnelDatumHandler} can consume it and publish datum arrival event.
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@Slf4j
public class TunnelDatumEncryptedHandler extends MessageToMessageDecoder<TunnelDatumEncryptedMessage> {

    private final OnionAuthorizer onionAuth;
    private final RoutingContext routingContext;

    public TunnelDatumEncryptedHandler(OnionAuthorizer onionAuth, RoutingContext routingContext) {
        this.onionAuth = onionAuth;
        this.routingContext = routingContext;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, TunnelDatumEncryptedMessage blindMsg, List<Object> out) throws Exception {
        val tunnelId = blindMsg.tunnelId();
        val blindPayload = blindMsg.payload();

        val deciphertext = onionAuth.decrypt(blindPayload, routingContext.sessionId(tunnelId)).join();

        val decryptedPayload = deciphertext.bytes();
        val peeledBlindMsg = blindMsg.peel(decryptedPayload);

        if (!deciphertext.isPlaintext() && !routingContext.hasNextHop(tunnelId))
            throw new OnionTunnelingException("This onion was supposed to peel last crypto layer, " +
                "but not a plaintext returned by onionAuth");

        if (routingContext.hasNextHop(tunnelId)) {
            routingContext.nextHop(tunnelId).writeAndFlush(peeledBlindMsg);
            log.debug("Encrypted datum message has been propagated down the {} tunnel by {} to {}",
                tunnelId, ctx.channel().localAddress(), ctx.channel().remoteAddress());
            return;
        }


        val decryptedDatum = peeledBlindMsg.toDatumMessage();
        out.add(decryptedDatum);

        log.debug("TunnelDatumEncryptedHandler decrypted the last layer of encrpt. of a message received from {} tunnel",
            tunnelId);
    }
}
