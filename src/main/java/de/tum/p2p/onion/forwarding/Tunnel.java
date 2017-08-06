package de.tum.p2p.onion.forwarding;

import de.tum.p2p.Peer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.security.PublicKey;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code Tunnel} encapsulates data about Tunnel build by
 * {@link OnionForwarder#createTunnel(Peer)}
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@EqualsAndHashCode @ToString
@Getter @Accessors(fluent = true)
public class Tunnel {

    private final TunnelId id;

    private final PublicKey destinationKey;

    private final int hops;

    public Tunnel(TunnelId id, PublicKey destinationKey, int hops) {
        this.id = notNull(id);
        this.destinationKey = notNull(destinationKey);
        this.hops = hops;
    }

    public static Tunnel of(TunnelId tunnelId, PublicKey destinationKey, int hops) {
        return new Tunnel(tunnelId, destinationKey, hops);
    }
}
