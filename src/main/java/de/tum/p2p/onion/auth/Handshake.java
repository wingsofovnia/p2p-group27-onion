package de.tum.p2p.onion.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Encapsulates a handshake's payload used by {@link SessionFactory} in
 * Diffie–Hellman key exchange flow
 */
@EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class Handshake {

    private final Integer sessionId;

    private final byte[] payload;

    public Handshake(Integer sessionId, byte[] payload) {
        this.sessionId = notNull(sessionId);
        this.payload = notNull(payload);
    }
}
