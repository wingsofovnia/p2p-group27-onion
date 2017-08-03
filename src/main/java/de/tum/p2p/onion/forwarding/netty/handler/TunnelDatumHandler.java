package de.tum.p2p.onion.forwarding.netty.handler;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.forwarding.netty.event.TunnelCoverReceived;
import de.tum.p2p.onion.forwarding.netty.event.TunnelDatumReceived;
import de.tum.p2p.proto.message.onion.forwarding.TunnelDatumMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * {@code TunnelConnectHandler} handles plaintext {@link TunnelDatumMessage} received from
 * unwrapping {@code TunnelDatumEncryptedMessage} by {@link TunnelDatumEncryptedHandler}.
 * It checks whether the datum is cover or not and then notify {@code OnionForwarder}'s data listeners.
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@Slf4j
public class TunnelDatumHandler extends SimpleChannelInboundHandler<TunnelDatumMessage> {

    private final EventBus eventBus;

    public TunnelDatumHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelDatumMessage datumMsg) throws Exception {
        if (datumMsg.isCover()) {
            eventBus.post(TunnelCoverReceived.from(datumMsg.tunnelId()));
            log.debug("Listeners has been notified about incoming cover data");
        } else {
            eventBus.post(TunnelDatumReceived.of(datumMsg.tunnelId(), ByteBuffer.wrap(datumMsg.payload())));
            log.debug("Listeners has been notified about incoming data {}", datumMsg.toString());
        }
    }
}
