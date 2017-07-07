package de.tum.p2p.onion.auth;

import de.tum.p2p.Peer;

import java.nio.ByteBuffer;

/**
 * {@code Hadshaker} encapsulates Diffie–Hellman key exchange flow specific for
 * Onion Auth specification
 *
 * @see <a href="https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange">
 * Wikipedia - Diffie–Hellman key exchange</a>
 */
public interface SessionFactory {

    /**
     * Computes a (Alice's) public key to be shared with the destination
     * peer (Bob)
     *
     * @param destination destination
     * @return a Handshake 1 payload (Alice's)
     */
    ByteBuffer createHandshake(Peer destination);

    /**
     * Receives (Bob's) public key and finishes session key establishment.
     * This also must register newly created Session within {@link OnionAuthorizer}
     * that initialized this SessionFactory.
     *
     * @param handshake a Handshake 2 payload (from Bob)
     * @return a new Session with shared secret between parties (Alice and Bob)
     */
    Session receiveHandshake(ByteBuffer handshake);
}
