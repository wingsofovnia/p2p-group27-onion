package de.tum.p2p;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.Validate;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PublicKey;

import static de.tum.p2p.util.Nets.validPort;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Encapsulates information about peer in the network such
 * as address and public key.
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class Peer {

    private final InetAddress address;

    private final int port;

    private final PublicKey publicKey;

    public Peer(InetAddress address, int port, PublicKey publicKey) {
        Validate.isTrue(port > 0, "Port cant be negative or eq 0");

        this.address = notNull(address);
        this.port = validPort(port);
        this.publicKey = publicKey;
    }

    public Peer(InetAddress address, int port) {
        this(address, port, null);
    }

    public static Peer of(InetAddress address, int port, PublicKey publicKey) {
        return new Peer(address, port, publicKey);
    }

    public static Peer of(InetAddress address, int port) {
        return new Peer(address, port);
    }

    public InetSocketAddress socketAddress() {
        return new InetSocketAddress(address, port);
    }
}
