package de.tum.p2p.onion.forwarding.netty.event;

import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.onion.forwarding.RequestId;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendedMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * The {@code TunnelExtendedReceived} is issued by {@code TunnelExtendedHandler}
 * on receiving {@code TunnelExtendedMessage} so that {@code OnionForwarder} can
 * disconnect tunnel and cleanup.
 *
 * @see com.google.common.eventbus.EventBus
 * @see de.tum.p2p.onion.forwarding.netty.OnionEventBus
 * @see de.tum.p2p.onion.forwarding.netty.handler.TunnelExtendedHandler
 * @see TunnelExtendedMessage
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class TunnelExtendedReceived {

    private final TunnelId tunnelId;

    private final SessionId sessionId;

    private final RequestId requestId;

    public TunnelExtendedReceived(TunnelId tunnelId, SessionId sessionId, RequestId requestId) {
        this.tunnelId = notNull(tunnelId);
        this.sessionId = notNull(sessionId);
        this.requestId = notNull(requestId);
    }

    public static TunnelExtendedReceived of(TunnelId tunnelId, SessionId sessionId, RequestId requestId) {
        return new TunnelExtendedReceived(tunnelId, sessionId, requestId);
    }
}
