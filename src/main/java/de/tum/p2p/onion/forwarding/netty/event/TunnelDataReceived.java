package de.tum.p2p.onion.forwarding.netty.event;

import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.ByteBuffer;

import static org.apache.commons.lang3.Validate.notNull;

@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class TunnelDataReceived {

    private TunnelId tunnelId;

    private ByteBuffer payload;

    public TunnelDataReceived(TunnelId tunnelId, ByteBuffer payload) {
        this.tunnelId = notNull(tunnelId);
        this.payload = notNull(payload);
    }

    public static TunnelDataReceived of(TunnelId tunnelId, ByteBuffer payload) {
        return new TunnelDataReceived(tunnelId, payload);
    }
}
