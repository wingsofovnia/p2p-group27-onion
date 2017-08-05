package de.tum.p2p.proto.message.onion.forwarding.composite;

import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.onion.forwarding.TunnelMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelRelayMessage} is a container for encrypted payloads
 * {@link TunnelRelayPayload} that are peeled out on each round of
 * tunnel building or datum forwarding.
 * <p>
 * Packet structure:
 * <pre>
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |    PSIZE    |  R_PAYLOAD  |
 * |---------------------------|
 * |   RELAY PAYLOAD(CONT..)   |
 * |---------------------------|
 * </pre>
 *
 * @see TunnelConnect
 * @see TunnelDatum
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class TunnelRelayMessage extends TunnelMessage {

    @Getter
    private final byte[] payload;

    public TunnelRelayMessage(TunnelId tunnelId, TunnelRelayPayload payload) {
        this(tunnelId, payload.bytes());
    }

    private TunnelRelayMessage(TunnelId tunnelId, byte[] payload) {
        super(tunnelId);
        this.payload = notNull(payload);
    }

    public static TunnelRelayMessage fromBytes(byte[] bytes) {
        try {
            val bytesBuffer = ByteBuffer.wrap(bytes);

            val parsedTunnelId = TunnelId.wrap(bytesBuffer.getInt());

            val parsedPsize = bytesBuffer.getShort();
            val parsedPayload = new byte[parsedPsize];
            bytesBuffer.get(parsedPayload);

            return new TunnelRelayMessage(parsedTunnelId, parsedPayload);
        } catch (BufferUnderflowException | BufferOverflowException e) {
            throw new ProtoException("Failed to parse tunnel relay message", e);
        }
    }

    @Override
    protected void writeBody(ByteBuffer messageBuffer) {
        if (payload.length > PAYLOAD_BYTES)
            throw new ProtoException("Failed to bytefy Relay Message - Payload is too big. " +
                "Expected max = " + PAYLOAD_BYTES + ", actual = " + payload.length);

        messageBuffer.putChar((char) payload.length);
        messageBuffer.put(payload);
    }

    public TunnelRelayMessage peel(byte[] peeledPayload) {
        return new TunnelRelayMessage(tunnelId, peeledPayload);
    }

    public static final class Encrypted {

        private TunnelId tunnelId;

        private TunnelRelayPayload payload;

        private OnionAuthorizer onionAuthorizer;

        private List<SessionId> sessionIds;

        public Encrypted tunnelId(TunnelId tunnelId) {
            this.tunnelId = tunnelId;
            return this;
        }

        public Encrypted payload(TunnelRelayPayload payload) {
            this.payload = payload;
            return this;
        }

        public Encrypted encrypt(OnionAuthorizer onionAuthorizer, List<SessionId> sessionIds) {
            this.onionAuthorizer = onionAuthorizer;
            this.sessionIds = sessionIds;
            return this;
        }

        public Encrypted encrypt(OnionAuthorizer onionAuthorizer, SessionId sessionId) {
            this.onionAuthorizer = onionAuthorizer;
            this.sessionIds = Collections.singletonList(sessionId);
            return this;
        }

        public CompletableFuture<TunnelRelayMessage> build() {
            return onionAuthorizer.encrypt(payload.bytes(), sessionIds).thenApply(ciphertext -> {
                return new TunnelRelayMessage(tunnelId, ciphertext.bytes());
            });
        }
    }
}
