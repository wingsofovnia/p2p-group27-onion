package de.tum.p2p.onion.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class SessionId {

    private final Short id;

    public SessionId(Short id) {
        this.id = notNull(id);
    }

    public static SessionId wrap(Short id) {
        return new SessionId(id);
    }

    public Short raw() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
