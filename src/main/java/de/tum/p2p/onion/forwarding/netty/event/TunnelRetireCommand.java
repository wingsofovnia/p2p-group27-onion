package de.tum.p2p.onion.forwarding.netty.event;

import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class TunnelRetireCommand {

    private final TunnelId tunnelId;

    public TunnelRetireCommand(TunnelId tunnelId) {
        this.tunnelId = tunnelId;
    }

    public static TunnelRetireCommand of(TunnelId tunnelId) {
        return new TunnelRetireCommand(tunnelId);
    }
}
