package de.tum.p2p.onion.auth;

import de.tum.p2p.Peer;
import de.tum.p2p.proto.message.Message;

import java.util.List;
import java.util.Optional;

/**
 * {@code OnionAuthorizer} encapsulates authentication mechanisms
 * used while building onion tunnels. It implements establishing
 * session keys given hostkeys of hops, and onion layer-encryption
 * and decryption of payload data.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange">
 * Wikipedia - Diffie–Hellman key exchange</a>
 */
public interface OnionAuthorizer {

    /**
     * Returns all active {@link Session}s that passes Diffie–Hellman key
     * exchange
     *
     * @return list of active sessions
     */
    List<Session> sessions();

    /**
     * Creates and instance of {@link Handshaker} that is used to pass
     * Diffie–Hellman key exchange and create new {@link Session}
     * @return
     */
    Handshaker handshaker();

    /**
     * Queries all active {@link Session}s for one with specific origin and
     * destination {@link Peer}s
     *
     * @param origin a data Tunnel's origin {@link Peer}
     * @param destination a data Tunnel's destination {@link Peer}
     * @return a corresponding {@link Session}
     */
    default Optional<Session> session(Peer origin, Peer destination) {
        return sessions().stream().filter(s -> s.origin().equals(origin))
                                  .filter(s -> s.destination().equals(destination))
                                  .findFirst();
    }

    /**
     * Queries all active {@link Session}s for one with specific origin {@link Peer}
     *
     * @param destination a data Tunnel's destination {@link Peer}
     * @return a corresponding {@link Session}
     */
    default Optional<Session> session(Peer destination) {
        return sessions().stream().filter(s -> s.destination().equals(destination))
                                  .findAny();
    }

    /**
     * Layer-encrypts a message for given {@link Session}s.
     *
     * @param message a payload to encrypt
     * @param layers  {@link Session}s used for encryption
     * @return encrypted payload
     * @throws OnionEncryptionException in case of problems during data encryption
     */
    Message encrypt(Message message, Session... layers) throws OnionEncryptionException;

    default Message encrypt(Message message,  List<Session> layers) throws OnionEncryptionException {
        return encrypt(message, layers.toArray(new Session[layers.size()]));
    }

    /**
     * Layer-decrypts a message for given {@link Session}s.
     *
     * @param ciphertext a payload to decrypt
     * @param layers     {@link Session}s used for decryption
     * @return decrypted Message
     * @throws OnionDecryptionException in case of problems during data encryption
     */
    Message decrypt(Message ciphertext, Session... layers) throws OnionDecryptionException;

    default Message decrypt(Message ciphertext, List<Session> layers) throws OnionDecryptionException {
        return decrypt(ciphertext, layers.toArray(new Session[layers.size()]));
    }
}
