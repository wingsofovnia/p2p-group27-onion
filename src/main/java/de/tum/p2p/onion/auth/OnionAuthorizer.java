package de.tum.p2p.onion.auth;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.ArrayUtils.addAll;

/**
 * {@code OnionAuthorizer} encapsulates authentication and encryption
 * mechanisms used while building onion tunnels. It implements establishing
 * session keys given hostkeys of hops, and onion layer-encryption &
 * decryption of payload data.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange">
 * Wikipedia - Diffie–Hellman key exchange</a>
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public interface OnionAuthorizer {


    /**
     * Creates and instance of {@link SessionFactory} that is used to perform
     * Diffie–Hellman key exchange and create new {@link SessionId} (shared secret)
     *
     * @return a new SessionFactory instance
     */
    SessionFactory sessionFactory();

    /**
     * Encapsulates plaintext in layers of encryption, analogous to layers of an onion.
     *
     * @param plaintext a plaintext to encrypt
     * @param sessions  {@link SessionId}s used for encryption
     * @return a ciphertext
     * @throws OnionEncryptionException in case of problems during data encryption
     */
    CompletableFuture<Ciphertext> encrypt(ByteBuffer plaintext, List<SessionId> sessions) throws OnionEncryptionException;

    default CompletableFuture<Ciphertext> encrypt(ByteBuffer plaintext, SessionId sessionId, SessionId... sessions)
            throws OnionEncryptionException {
        return encrypt(plaintext, asList(addAll(sessions, sessionId)));
    }

    default CompletableFuture<Ciphertext> encrypt(byte[] plaintext, SessionId sessionId, SessionId... sessions)
        throws OnionEncryptionException {
        return encrypt(ByteBuffer.wrap(plaintext), sessionId, sessions);
    }

    default CompletableFuture<Ciphertext> encrypt(byte[] plaintext, List<SessionId> sessions)
            throws OnionEncryptionException {

        return encrypt(ByteBuffer.wrap(plaintext), sessions);
    }

    /**
     * Peels away a single layer of encryption made by
     * {@link OnionAuthorizer#encrypt(ByteBuffer, SessionId, SessionId...)}
     *
     * @param ciphertext a ciphertext to decrypt
     * @param sessionId  a {@link SessionId} used for decryption one layer
     * @return decrypted ciphertext
     * @throws OnionDecryptionException in case of problems during data encryption
     */
    CompletableFuture<Deciphertext> decrypt(ByteBuffer ciphertext, SessionId sessionId) throws OnionDecryptionException;

    default CompletableFuture<Deciphertext> decrypt(Ciphertext ciphertext, SessionId sessionId)
            throws OnionDecryptionException {

        return decrypt(ciphertext.bytesBuffer(), sessionId);
    }

    default CompletableFuture<Deciphertext> decrypt(byte[] ciphertext, SessionId sessionId)
            throws OnionDecryptionException {

        return decrypt(ByteBuffer.wrap(ciphertext), sessionId);
    }
}
