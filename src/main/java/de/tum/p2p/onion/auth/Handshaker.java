package de.tum.p2p.onion.auth;

import de.tum.p2p.Peer;

/**
 * {@code Hadshaker} encapsulates Diffie–Hellman key exchange flow
 *
 * @see <a href="https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange">
 *     Wikipedia - Diffie–Hellman key exchange</a>
 */
public interface Handshaker {

    /**
     * Computes a "public key" to be shared with the destination peer
     *
     * @param destination destination
     * @return a Handshake 1 (Alice) payload
     */
    Handshake makeHandshake(Peer destination);

    /**
     * Receives (Bob) "public key" and finishes session key establishment
     *
     * @param handshake a Handshake 2 (Bob) payload
     */
    void receiveHandshake(Handshake handshake);
}
