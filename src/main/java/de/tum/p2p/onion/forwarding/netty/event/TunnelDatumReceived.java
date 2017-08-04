package de.tum.p2p.onion.forwarding.netty.event;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.onion.forwarding.TunnelDatumMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.ByteBuffer;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * The {@code TunnelDatumReceived} is issued by {@code TunnelDatumHandler}
 * on receiving {@code TunnelDatumMessage} so that {@code OnionForwarder} can
 * notify its listeners about incoming data
 *
 * @see com.google.common.eventbus.EventBus
 * @see de.tum.p2p.onion.forwarding.netty.OnionEventBus
 * @see de.tum.p2p.onion.forwarding.netty.handler.TunnelDatumHandler
 * @see TunnelDatumMessage
 * @see de.tum.p2p.onion.forwarding.netty.NettyOnionForwarder#forward(TunnelId, ByteBuffer)
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class TunnelDatumReceived {

    private TunnelId tunnelId;

    private ByteBuffer payload;

    public TunnelDatumReceived(TunnelId tunnelId, ByteBuffer payload) {
        this.tunnelId = notNull(tunnelId);
        this.payload = notNull(payload);
    }

    public static TunnelDatumReceived of(TunnelId tunnelId, ByteBuffer payload) {
        return new TunnelDatumReceived(tunnelId, payload);
    }
}
