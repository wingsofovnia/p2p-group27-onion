package de.tum.p2p.onion.forwarding.netty.handler.server;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.OnionDataForwardingException;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.onion.forwarding.netty.event.TunnelDataReceived;
import de.tum.p2p.proto.message.onion.forwarding.TunnelDatumMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.val;

/**
 * {@code TunnelDatumHandler} processes data forwarder by {@code OnionForwarder} in
 * order to notify listeners for incoming data (in case this peer is the last node
 * in the tunnel) or send to next peer
 */
public class TunnelDatumHandler extends SimpleChannelInboundHandler<TunnelDatumMessage> {

    private final OnionAuthorizer onionAuthorizer;
    private final Router router;
    private final EventBus eventBus;

    public TunnelDatumHandler(OnionAuthorizer onionAuthorizer, Router router, EventBus eventBus) {
        this.onionAuthorizer = onionAuthorizer;
        this.router = router;
        this.eventBus = eventBus;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelDatumMessage tunnelDatumMsg) throws Exception {
        val tunnelId = tunnelDatumMsg.tunnelId();
        val encryptedPayload = tunnelDatumMsg.payload();

        val sessionId = router.routeSessionId(tunnelId).orElseThrow(()
            -> new OnionDataForwardingException("Failed to forward data via tunnel " + tunnelId + " - no sessionId"));

        val futureDecryptedPayload = onionAuthorizer.decrypt(encryptedPayload, sessionId);

        if (router.hasRouteNext(tunnelId)) {
            // Not mine, propagate down the tunnel
            futureDecryptedPayload.thenAccept(deciphertext -> {
                router.routeNext(tunnelId).ifPresent(channel -> {
                    val peeledDatumMsg = tunnelDatumMsg.derive(deciphertext.bytes());

                    channel.writeAndFlush(peeledDatumMsg);
                });
            });

        } else {
            // Should be me mine, notify the world
            futureDecryptedPayload.thenAccept(deciphertext -> {
                eventBus.post(TunnelDataReceived.of(tunnelId, deciphertext.bytesBuffer()));
            });
        }

    }
}
