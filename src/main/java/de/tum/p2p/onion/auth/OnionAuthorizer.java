package de.tum.p2p.onion.auth;

import de.tum.p2p.Peer;
import lombok.val;

import java.nio.ByteBuffer;
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
     * Returns all active {@link Session}s that passed Diffie–Hellman key
     * exchange
     *
     * @return list of active sessions
     */
    List<Session> sessions();

    /**
     * Creates and instance of {@link SessionFactory} that is used to perform
     * Diffie–Hellman key exchange and create new {@link Session} (shared secret)
     *
     * @return a new SessionFactory instance
     */
    SessionFactory sessionFactory();

    /**
     * Queries all active {@link Session}s for one with specific origin and
     * destination {@link Peer}s
     *
     * @param origin      a data Tunnel's origin {@link Peer}
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
     * @param message  a payload to encrypt
     * @param session  {@link Session}s used for encryption
     * @param sessions additional {@link Session}s used for layered encryption
     * @return encrypted payload
     * @throws OnionEncryptionException in case of problems during data encryption
     */
    ByteBuffer encrypt(ByteBuffer message, Session session, Session... sessions) throws OnionEncryptionException;

    default ByteBuffer encrypt(ByteBuffer message, List<Session> sessions) throws OnionEncryptionException {
        if (sessions.isEmpty())
            throw new IllegalArgumentException("At least one session is required for encryption");

        if (sessions.size() == 1)
            return encrypt(message, sessions.get(0));

        val sessionArg = sessions.get(0);
        val sessionsArg = sessions.stream().skip(1).toArray(Session[]::new);

        return encrypt(message, sessionArg, sessionsArg);
    }

    /**
     * Layer-decrypts a message for given {@link Session}s.
     *
     * @param ciphertext a payload to decrypt
     * @param session    {@link Session}s used for decryption
     * @param sessions   additional {@link Session}s used for layered decryption
     * @return decrypted Message
     * @throws OnionDecryptionException in case of problems during data encryption
     */
    ByteBuffer decrypt(ByteBuffer ciphertext, Session session, Session... sessions) throws OnionDecryptionException;

    default ByteBuffer decrypt(ByteBuffer ciphertext, List<Session> sessions) throws OnionDecryptionException {
        if (sessions.isEmpty())
            throw new IllegalArgumentException("At least one session is required for decryption");

        if (sessions.size() == 1)
            return decrypt(ciphertext, sessions.get(0));

        val sessionArg = sessions.get(0);
        val sessionsArg = sessions.stream().skip(1).toArray(Session[]::new);

        return decrypt(ciphertext, sessionArg, sessionsArg);
    }
}
