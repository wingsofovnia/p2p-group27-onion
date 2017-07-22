package de.tum.p2p.onion.forwarding.netty.event;

import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This event is issued to {@link com.google.common.eventbus.EventBus} by
 * {@link de.tum.p2p.onion.forwarding.netty.handler.client.TunnelExtendedHandler}
 * so {@link de.tum.p2p.onion.forwarding.OnionForwarder} can react accordingly.
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class TunnelExtendedReceived {

    private final TunnelId tunnelId;

    private final SessionId sessionId;

    private final Integer requestId;

    public TunnelExtendedReceived(TunnelId tunnelId, SessionId sessionId, Integer requestId) {
        this.tunnelId = notNull(tunnelId);
        this.sessionId = notNull(sessionId);
        this.requestId = notNull(requestId);
    }

    public static TunnelExtendedReceived of(TunnelId tunnelId, SessionId sessionId, Integer requestId) {
        return new TunnelExtendedReceived(tunnelId, sessionId, requestId);
    }
}
