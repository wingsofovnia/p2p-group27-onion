package de.tum.p2p;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.net.InetAddress;
import java.security.PublicKey;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Encapsulates information about peer in the network such
 * as address and public key.
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class Peer {

    private final InetAddress address;

    private final PublicKey publicKey;

    public Peer(InetAddress address, PublicKey publicKey) {
        this.address = notNull(address);
        this.publicKey = notNull(publicKey);
    }
}
