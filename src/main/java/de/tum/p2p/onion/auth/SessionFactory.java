package de.tum.p2p.onion.auth;

import de.tum.p2p.Peer;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * {@code Hadshaker} encapsulates Diffie–Hellman key exchange flow specific for
 * Onion Auth specification
 *
 * @see <a href="https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange">
 * Wikipedia - Diffie–Hellman key exchange</a>
 */
public interface SessionFactory {

    /**
     * Computes a (Alice's) public secret to be shared with the destination
     * peer (Bob).
     * <p>
     * The method call corresponds to AUTH SESSION START that returns
     * AUTH SESSION HS1's handshake payload form the spec.
     *
     * @param destination destination
     * @return a Handshake 1 payload (Alice's)
     */
    CompletableFuture<Handshake> start(Peer destination);

    /**
     * Receives a (Alice's) public secret and responses with personal (Bob's)
     * public secret.
     * <p>
     * The method call corresponds to AUTH SESSION INCOMING HS1 that returns
     * AUTH SESSION HS2's handshake payload form the spec.
     *
     * @param hs1 a Handshake 1 payload (Alice's)
     * @return a Handshake 2 payload (Bob's)
     */
    CompletableFuture<Handshake> responseTo(Handshake hs1);

    /**
     * Receives (Bob's) public key and finishes session key establishment.
     * This also must register newly created Session within {@link OnionAuthorizer}
     * that initialized this SessionFactory.
     * <p>
     * The method call corresponds to AUTH SESSION INCOMING HS2  form the spec.
     *
     * @param hs2 a Handshake 2 payload (Bob's)
     */
    void complete(Handshake hs2);
}
