package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static de.tum.p2p.proto.message.MessageType.ONION_TUNNEL_CONNECT;
import static de.tum.p2p.util.ByteBuffers.bufferAllBytes;
import static java.util.Arrays.copyOfRange;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelConnectEncryptedMessage} message carries an encrypted data
 * from {@link TunnelConnectMessage}.
 * <p>
 * Since layered decryption for tunnel building - related messages is not
 * required by the specification, onions just propagate this message to the
 * very end where the last onion expects this message to be encrypted with
 * known session id (1 layer).
 * <p>
 * Packet structure:
 * <pre>
 * |-------------|-------------|
 * |     LP*     |   CONNECT   |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |    REQ ID   |    PSIZE    |
 * |-------------|-------------|
 * |          PAYLOAD          |
 * |---------------------------|
 *
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class TunnelConnectEncryptedMessage extends TraceableTypedTunnelMessage
        implements Peelable<TunnelConnectEncryptedMessage> {

    @Getter
    private final byte[] payload;

    private TunnelConnectEncryptedMessage(TunnelId tunnelId, RequestId requestId, byte[] payload) {
        super(tunnelId, requestId, ONION_TUNNEL_CONNECT);

        this.payload = notNull(payload);
    }

    private TunnelConnectEncryptedMessage(TunnelId tunnelId, byte[] payload) {
        this(tunnelId, RequestId.next(), payload);
    }

    public static CompletableFuture<TunnelConnectEncryptedMessage> fromConnect(TunnelConnectMessage connMsg,
                                                                               OnionAuthorizer onionAuth,
                                                                               SessionId sessionId) {
        val connMsgBytes = connMsg.bytes(false);
        val connMsgNoMetaBytes = copyOfRange(connMsgBytes, TunnelConnectMessage.META_BYTES, connMsgBytes.length);

        return onionAuth.encrypt(connMsgNoMetaBytes, sessionId).thenApply(ciphertext ->
            new TunnelConnectEncryptedMessage(connMsg.tunnelId(), connMsg.requestId(), ciphertext.bytes()));
    }

    public static TunnelConnectEncryptedMessage fromBytes(byte[] bytes) {
        val bytesBuffer = ByteBuffer.wrap(bytes);
        val rawTraceableTypedTunnelMsg = TraceableTypedTunnelMessage.fromBytes(bytesBuffer, ONION_TUNNEL_CONNECT);

        val parsedTunnelId = rawTraceableTypedTunnelMsg.tunnelId();
        val parsedRequestId = rawTraceableTypedTunnelMsg.requestId();

        val parsedPayload = new byte[PAYLOAD_BYTES];
        bytesBuffer.get(parsedPayload);

        return new TunnelConnectEncryptedMessage(parsedTunnelId, parsedRequestId, parsedPayload);
    }

    public TunnelConnectMessage toConnectMessage() {
        val rawTunnelConnectMsgBuffer = ByteBuffer.allocate(BYTES)
            .putShort(messageType().code())
            .putInt(tunnelId().raw())
            .putShort(requestId().raw())
            .put(payload);

        val rawTunnelConnectMsgBytes = bufferAllBytes(rawTunnelConnectMsgBuffer);

        return TunnelConnectMessage.fromBytes(rawTunnelConnectMsgBytes);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer messageBuffer) {
        messageBuffer.put(payload);
        return messageBuffer;
    }

    @Override
    public TunnelConnectEncryptedMessage peel(byte[] peeledPayload) {
        return new TunnelConnectEncryptedMessage(this.tunnelId, this.requestId, peeledPayload);
    }
}
