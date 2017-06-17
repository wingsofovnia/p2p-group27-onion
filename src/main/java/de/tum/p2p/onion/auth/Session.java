package de.tum.p2p.onion.auth;

import de.tum.p2p.Peer;
import lombok.*;
import lombok.experimental.Accessors;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Encapsulates information about sessions such as origin
 * & destination {@link Peer}s and session identifier
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class Session {

    private final Peer origin;

    private final Peer destination;

    private final Integer sessionId;

    public Session(Peer origin, Peer destination, Integer sessionId) {
        this.origin = notNull(origin);
        this.destination = notNull(destination);
        this.sessionId = notNull(sessionId);
    }
}
