package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static de.tum.p2p.util.ByteBuffers.bufferAllBytes;
import static java.util.Arrays.copyOfRange;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelDatumEncryptedMessage} message carries an encrypted data
 * from {@link TunnelDatumMessage}. It can carry either a cover datum or
 * regular one with real data.
 * <p>
 * Each peer peels one layer of encryption so that the latest peer can
 * decrypt the {@link TunnelDatumMessage} and process accordingly.
 * <p>
 * Packet structure:
 * <pre>
 * |-------------|-------------|
 * |     LP*     |   RESERVED  |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |    PSIZE    |   PAYLOAD   |
 * |---------------------------|
 * |      PAYLOAD(CONT...)     |
 * |---------------------------|
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class TunnelDatumEncryptedMessage extends TunnelMessage implements Peelable<TunnelDatumEncryptedMessage> {

    @Getter
    private final byte[] payload;

    private TunnelDatumEncryptedMessage(TunnelId tunnelId, byte[] payload) {
        super(tunnelId);
        this.payload = notNull(payload);
    }

    public static CompletableFuture<TunnelDatumEncryptedMessage> fromDatum(TunnelDatumMessage datumMsg,
                                                                           OnionAuthorizer onionAuth,
                                                                           List<SessionId> sessionIds) {

        val datumMsgBytes = datumMsg.bytes(false);
        val datumMsgBytesNoMetaBytes = copyOfRange(datumMsgBytes, TunnelDatumMessage.META_BYTES, datumMsgBytes.length);

        return onionAuth.encrypt(datumMsgBytesNoMetaBytes, sessionIds).thenApply(ciphertext ->
            new TunnelDatumEncryptedMessage(datumMsg.tunnelId(), ciphertext.bytes()));
    }

    public static TunnelDatumEncryptedMessage fromBytes(byte[] bytes) {
        val bytesBuffer = ByteBuffer.wrap(bytes);
        val rawTunnelMsg = TunnelMessage.fromBytes(bytesBuffer);

        val parsedTunnelId = rawTunnelMsg.tunnelId();
        val parsedPayloadSize = bytesBuffer.getInt();

        val parsedPayload = new byte[parsedPayloadSize];
        bytesBuffer.get(parsedPayload);

        return new TunnelDatumEncryptedMessage(parsedTunnelId, parsedPayload);
    }

    public TunnelDatumMessage toDatumMessage() {
        val rawTunnelDatumMsgBuffer = ByteBuffer.allocate(BYTES)
            .putShort(MessageType.ONION_TUNNEL_DATUM.code())
            .putInt(tunnelId.raw())
            .put(payload);

        val rawTunnelDatumMsgBytes = bufferAllBytes(rawTunnelDatumMsgBuffer);

        return TunnelDatumMessage.fromBytes(rawTunnelDatumMsgBytes);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer messageBuffer) {
        messageBuffer.putInt(payload.length);
        messageBuffer.put(payload);
        return messageBuffer;
    }

    @Override
    public TunnelDatumEncryptedMessage peel(byte[] peeledPayload) {
        return new TunnelDatumEncryptedMessage(this.tunnelId, peeledPayload);
    }
}
