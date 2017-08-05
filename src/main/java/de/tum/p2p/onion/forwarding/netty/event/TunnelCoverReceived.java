package de.tum.p2p.onion.forwarding.netty.event;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.onion.forwarding.netty.handler.TunnelDatumHandler;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * The {@code TunnelCoverReceived} is issued by {@code TunnelDatumHandler}
 * on receiving a <strong>cover</strong> {@code TunnelDatum}.
 * <p>
 * This event is ignored by {@code OnionForwarder} but is useful for testing
 * purposes or logging.
 *
 * @see com.google.common.eventbus.EventBus
 * @see de.tum.p2p.onion.forwarding.netty.OnionEventBus
 * @see TunnelDatumHandler
 * @see de.tum.p2p.proto.message.onion.forwarding.composite.TunnelDatum
 * @see de.tum.p2p.onion.forwarding.netty.NettyOnionForwarder#cover(int)
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class TunnelCoverReceived {

    private final TunnelId tunnelId;

    public TunnelCoverReceived(TunnelId tunnelId) {
        this.tunnelId = tunnelId;
    }

    public static TunnelCoverReceived from(TunnelId tunnelId) {
        return new TunnelCoverReceived(tunnelId);
    }
}
