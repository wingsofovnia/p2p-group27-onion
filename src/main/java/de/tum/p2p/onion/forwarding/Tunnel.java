package de.tum.p2p.onion.forwarding;

import de.tum.p2p.Peer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Encapsulates information about data Tunnels such as hops
 * {@link Peer}s and Tunnel identifier
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class Tunnel {

    private final Integer tunnelId;

    private final List<Peer> peers;

    public Tunnel(Integer tunnelId, List<Peer> peers) {
        this.tunnelId = notNull(tunnelId);
        this.peers = notEmpty(peers);
    }
}
