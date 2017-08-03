package de.tum.p2p.onion.auth;

import de.tum.p2p.Peer;
import lombok.experimental.var;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.util.ByteBuffers.bufferAllBytes;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code InMemoryBase64OnionAuthorizer} is a simple in-memory fake implementation
 * of {@link OnionAuthorizer} used for testing purposes. It uses Base64 for
 * encoding/decoding and handshakes of type (layer_numers; session_id) to for
 * session establishment.
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public class InMemoryBase64OnionAuthorizer implements OnionAuthorizer {

    private final List<SessionId> sessionIds = new ArrayList<>();

    private SessionFactory sessionFactory = new SessionFactory() {
        @Override
        public CompletableFuture<Pair<SessionId, ByteBuffer>> start(Peer destination) {
            val randSessionId = randSessionId();
            val randSessionIdHs1 = sessionIdToHandshake(randSessionId);

            rememberSessionId(randSessionId);

            return completedFuture(Pair.of(randSessionId, randSessionIdHs1));
        }

        @Override
        public CompletableFuture<Pair<SessionId, ByteBuffer>> responseTo(ByteBuffer hs1) {
            val sessionIdFromHs2 = handshakeToSessionId(hs1);

            rememberSessionId(sessionIdFromHs2);

            return completedFuture(Pair.of(sessionIdFromHs2, hs1));
        }

        @Override
        public CompletableFuture<SessionId> confirm(ByteBuffer hs2) {
            return completedFuture(handshakeToSessionId(hs2));
        }

        private SessionId randSessionId() {
            return SessionId.wrap(ThreadLocalRandom.current().nextInt(Short.MAX_VALUE));
        }

        private ByteBuffer sessionIdToHandshake(SessionId sessionId) {
            val rawSessionIdBuffer = ByteBuffer.allocate(SessionId.BYTES).putShort(sessionId.raw());
            rawSessionIdBuffer.clear();

            return rawSessionIdBuffer.asReadOnlyBuffer();
        }

        private SessionId handshakeToSessionId(ByteBuffer handshake) {
            return SessionId.wrap(handshake.duplicate().getShort());
        }
    };

    @Override
    public SessionFactory sessionFactory() {
        return sessionFactory;
    }

    public void sessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = notNull(sessionFactory);
    }

    @Override
    public CompletableFuture<Ciphertext> encrypt(ByteBuffer plaintext, List<SessionId> sessions)
            throws OnionEncryptionException {
        ensureExistingSessions(sessions);

        val plaintextBytes = bufferAllBytes(plaintext);

        if (sessions.size() == 0)
            return completedFuture(Ciphertext.wrapLayered(plaintextBytes, 0));

        val layers = sessions.size();

        var encodedPlaintextBytes = plaintextBytes;
        for (int i = 0; i < layers; i++) {
            encodedPlaintextBytes = encodeBase64(encodedPlaintextBytes);
        }

        val encodedPlaintextWithLayersBytes = prependLayersCounter(encodedPlaintextBytes, layers);

        return completedFuture(Ciphertext.wrapLayered(encodedPlaintextWithLayersBytes, layers));
    }

    @Override
    public CompletableFuture<Deciphertext> decrypt(ByteBuffer ciphertext, SessionId sessionId)
            throws OnionDecryptionException {
        ensureExistingSession(sessionId);

        val ciphertextLayers = ciphertext.getInt();

        val ciphertextBytes = new byte[ciphertext.limit() - ciphertext.position()];
        ciphertext.get(ciphertextBytes);

        val peeledCiphertextBytes = decodeBase64(ciphertextBytes);

        if (ciphertextLayers == 1) { // it was the last layer
            return completedFuture(Deciphertext.ofPlaintext(peeledCiphertextBytes));
        } else {
            val peeledCiphertextWithLayersBytes = prependLayersCounter(peeledCiphertextBytes, ciphertextLayers - 1);
            return completedFuture(Deciphertext.of(peeledCiphertextWithLayersBytes));
        }
    }

    private byte[] prependLayersCounter(byte[] payload, int layers) {
        return bufferAllBytes(ByteBuffer.allocate(Integer.BYTES + payload.length)
            .putInt(layers)
            .put(payload));
    }

    private void rememberSessionId(SessionId sessionId) {
        sessionIds.add(sessionId);
    }

    private void ensureExistingSession(SessionId sessionId) {
        if (!sessionIds.contains(sessionId))
            throw new IllegalArgumentException("Unknown sessionId = " + sessionId);
    }

    private void ensureExistingSessions(Collection<SessionId> sessionIds) {
        if (!this.sessionIds.containsAll(sessionIds))
            throw new IllegalArgumentException("Some sessions id are unknown");
    }
}
