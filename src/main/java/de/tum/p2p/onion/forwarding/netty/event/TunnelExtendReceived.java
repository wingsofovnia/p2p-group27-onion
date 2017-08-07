package de.tum.p2p.onion.forwarding.netty.event;

import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * The {@code TunnelCoverReceived} is issued by {@code TunnelExtendHandler}
 * after a confirmation of being the new hop in the tunnel required. In
 * other words, this message is used to signal a new incoming tunnel connection.
 *
 * @see de.tum.p2p.onion.forwarding.netty.handler.TunnelExtendHandler
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class TunnelExtendReceived {

    private final TunnelId tunnelId;

    public TunnelExtendReceived(TunnelId tunnelId) {
        this.tunnelId = tunnelId;
    }

    public static TunnelExtendReceived from(TunnelId tunnelId) {
        return new TunnelExtendReceived(tunnelId);
    }
}
