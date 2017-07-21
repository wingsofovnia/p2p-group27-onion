package de.tum.p2p.onion.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import static org.apache.commons.lang3.Validate.notNull;

@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class SessionId {

    private final Short id;

    public SessionId(Short id) {
        this.id = notNull(id);
    }

    public static SessionId wrap(Short id) {
        return new SessionId(id);
    }
}
