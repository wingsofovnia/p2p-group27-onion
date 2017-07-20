package de.tum.p2p.onion.auth;

import de.tum.p2p.Peer;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code OnionAuthorizer} encapsulates authentication and encryption
 * mechanisms used while building onion tunnels. It implements establishing
 * session keys given hostkeys of hops, and onion layer-encryption &
 * decryption of payload data.
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
    default Optional<Session> findSession(Peer origin, Peer destination) {
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
    default Optional<Session> findSession(Peer destination) {
        return sessions().stream().filter(s -> s.destination().equals(destination))
                                  .findAny();
    }

    /**
     * Encapsulates plaintext in layers of encryption, analogous to layers of an onion.
     *
     * @param plaintext a plaintext to encrypt
     * @param session   {@link Session}s used for encryption
     * @param sessions  additional {@link Session}s used for layered encryption
     * @return a ciphertext
     * @throws OnionEncryptionException in case of problems during data encryption
     */
    CompletableFuture<Ciphertext> encrypt(ByteBuffer plaintext, Session session, Session... sessions)
            throws OnionEncryptionException;

    default CompletableFuture<Ciphertext> encrypt(byte[] plaintext, Session session, Session... sessions)
            throws OnionEncryptionException {

        return encrypt(ByteBuffer.wrap(plaintext), session, sessions);
    }

    default CompletableFuture<Ciphertext> encrypt(ByteBuffer plaintext, List<Session> sessions)
            throws OnionEncryptionException {

        if (sessions.isEmpty())
            throw new IllegalArgumentException("At least one session is required for encryption");

        if (sessions.size() == 1)
            return encrypt(plaintext, sessions.get(0));

        val sessionArg = sessions.get(0);
        val sessionsArg = sessions.stream().skip(1).toArray(Session[]::new);

        return encrypt(plaintext, sessionArg, sessionsArg);
    }

    default CompletableFuture<Ciphertext> encrypt(byte[] plaintext, List<Session> sessions)
            throws OnionEncryptionException {

        return encrypt(ByteBuffer.wrap(plaintext), sessions);
    }

    /**
     * Peels away a single layer of encryption made by
     * {@link OnionAuthorizer#encrypt(ByteBuffer, Session, Session...)}
     *
     * @param ciphertext a ciphertext to decrypt
     * @param session    a {@link Session} used for decryption one layer
     * @return decrypted ciphertext
     * @throws OnionDecryptionException in case of problems during data encryption
     */
    CompletableFuture<Deciphertext> decrypt(ByteBuffer ciphertext, Session session) throws OnionDecryptionException;

    default CompletableFuture<Deciphertext> decrypt(Ciphertext ciphertext, Session session)
            throws OnionDecryptionException {

        return decrypt(ciphertext.bytesBuffer(), session);
    }

    default CompletableFuture<Deciphertext> decrypt(byte[] ciphertext, Session session)
            throws OnionDecryptionException {

        return decrypt(ByteBuffer.wrap(ciphertext), session);
    }
}
