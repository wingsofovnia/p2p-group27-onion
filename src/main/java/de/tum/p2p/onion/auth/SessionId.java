package de.tum.p2p.onion.auth;

import lombok.EqualsAndHashCode;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code SessionId} an id that is used to identify a tunnel' session
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@EqualsAndHashCode
public class SessionId {

    public static final int BYTES = Short.BYTES;

    private final Short id;

    public SessionId(Short id) {
        this.id = notNull(id);
    }

    public static SessionId wrap(Short id) {
        return new SessionId(id);
    }

    public static SessionId wrap(Integer id) {
        return new SessionId(id.shortValue());
    }

    public Short raw() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
