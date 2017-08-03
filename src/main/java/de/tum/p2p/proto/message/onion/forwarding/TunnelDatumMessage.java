package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Objects.deepEquals;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelDatumMessage} carries plaintext payloads of data that
 * the onion was requested to forward. It also used to represent a cover
 * data, discriminated by {@link TunnelDatumMessage#isCover} flag.
 * <p>
 * <strong>This message type is not used for transporting data because of
 * encryption required. See {@link TunnelDatumEncryptedMessage}</strong>.
 * <p>
 * Packet structure:
 * <pre>
 * |-------------|-------------|
 * |     LP*     |    DATUM    |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |    RSV  |COV|    PSIZE    |
 * |---------------------------|
 * |          PAYLOAD          |
 * |---------------------------|
 * |         MD5  HASH         |
 * |---------------------------|
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
 *
 * @see TunnelDatumEncryptedMessage
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class TunnelDatumMessage extends TypedTunnelMessage {

    private static final int HASH_LENGTH = 16; // md5

    private static final byte[] RESERVED = new byte[Short.BYTES - 1]; // 1 byte for COVER flag

    public static final int PAYLOAD_BYTES = TypedTunnelMessage.PAYLOAD_BYTES
        - Short.BYTES   // RESERVED + COVER flag
        - Integer.BYTES // payload length
        - HASH_LENGTH;

    @Getter
    private final boolean isCover;

    @Getter
    private final byte[] payload;

    public TunnelDatumMessage(TunnelId tunnelId, byte[] payload) {
        super(tunnelId, MessageType.ONION_TUNNEL_DATUM);
        this.payload = notNull(payload);
        this.isCover = false;
    }

    public TunnelDatumMessage(TunnelId tunnelId, byte[] payload, boolean isCover) {
        super(tunnelId, MessageType.ONION_TUNNEL_DATUM);
        this.payload = notNull(payload);
        this.isCover = isCover;
    }

    public TunnelDatumMessage(TunnelId tunnelId, int size) {
        super(tunnelId, MessageType.ONION_TUNNEL_DATUM);
        this.isCover = true;

        this.payload = new byte[size];
        ThreadLocalRandom.current().nextBytes(this.payload);
    }

    public static TunnelDatumMessage fromBytes(byte[] bytes) {
        val bytesBuffer = ByteBuffer.wrap(bytes);
        val rawTypedTunnelMessage = TypedTunnelMessage.fromBytes(bytesBuffer, MessageType.ONION_TUNNEL_DATUM);

        val parsedTunnelId = rawTypedTunnelMessage.tunnelId();
        bytesBuffer.position(bytesBuffer.position() + RESERVED.length);
        val parsedIsCover = bytesBuffer.get() > 0;

        val payloadSize = bytesBuffer.getInt();
        val parsedPayload = new byte[payloadSize];
        bytesBuffer.get(parsedPayload);

        val parsedPayloadHash = new byte[HASH_LENGTH];
        bytesBuffer.get(parsedPayloadHash);

        if (!deepEquals(md5(parsedPayload), parsedPayloadHash))
            throw new ProtoException(MessageType.ONION_TUNNEL_DATUM.name() + " MD5 hash check failed");

        return new TunnelDatumMessage(parsedTunnelId, parsedPayload, parsedIsCover);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer messageBuffer) {
        messageBuffer.position(messageBuffer.position() + RESERVED.length);
        messageBuffer.put((byte) (isCover ? 1 : 0));
        messageBuffer.putInt(payload.length);
        messageBuffer.put(payload);
        messageBuffer.put(md5(payload));
        return messageBuffer;
    }
}
