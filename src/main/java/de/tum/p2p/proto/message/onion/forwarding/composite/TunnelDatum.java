package de.tum.p2p.proto.message.onion.forwarding.composite;

import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.proto.message.MessageType.ONION_TUNNEL_COVER;
import static de.tum.p2p.proto.message.MessageType.ONION_TUNNEL_DATUM;
import static java.util.Objects.deepEquals;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelDatum} is a payload that carries voip data and typically is embedded
 * into {@link TunnelRelayMessage} so that it can be layered encrypted (entirely)
 * and transferred via the tunnel securely without revealing the content by other onions.
 * <p>
 * The TunnelDatum can be either of type ONION_TUNNEL_DATUM or ONION_TUNNEL_COVER
 * so that the destination peer can distinguish fake cover from real data.
 * <p>
 * Payload structure:
 * <pre>
 * |---------------------------|
 * |  DATUM/CVR  |    PSIZE    |
 * |---------------------------|
 * |          PAYLOAD          |
 * |---------------------------|
 * |         MD5  HASH         |
 * |---------------------------|
 * </pre>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class TunnelDatum extends TunnelRelayPayload {

    private static final int HASH_LENGTH = 16; // md5

    @Getter
    private final byte[] payload;

    private TunnelDatum(MessageType messageType, byte[] payload) {
        super(ensureDatumOrCoverMessageType(messageType));
        this.payload = notNull(payload);
    }

    public TunnelDatum(byte[] payload) {
        super(ONION_TUNNEL_DATUM);
        this.payload = notNull(payload);
    }

    public TunnelDatum(int coverSize) {
        super(ONION_TUNNEL_COVER);
        this.payload = generateCoverPayload(coverSize);
    }

    public static TunnelDatum fromBytes(byte[] bytes) {
        try {
            val bytesBuffer = ByteBuffer.wrap(bytes);

            val parsedMessageType = MessageType.fromCode(bytesBuffer.getShort());

            val payloadSize = bytesBuffer.getShort();
            val parsedPayload = new byte[payloadSize];
            bytesBuffer.get(parsedPayload);

            val parsedPayloadHash = new byte[HASH_LENGTH];
            bytesBuffer.get(parsedPayloadHash);

            if (!deepEquals(md5(parsedPayload), parsedPayloadHash))
                throw new ProtoException(ONION_TUNNEL_DATUM.name() + " MD5 hash check failed");

            return new TunnelDatum(parsedMessageType, parsedPayload);
        } catch (BufferUnderflowException | BufferOverflowException e) {
            throw new ProtoException("Failed to parse ONION_TUNNEL_DATUM/ONION_TUNNEL_COVER message", e);
        }
    }

    @Override
    protected void writePayload(ByteBuffer messageBuffer) {
        messageBuffer.putShort((short) payload.length);
        messageBuffer.put(payload);
        messageBuffer.put(md5(payload));
    }

    public boolean isCover() {
        return messageType == ONION_TUNNEL_COVER;
    }

    public boolean isDatum() {
        return messageType == ONION_TUNNEL_DATUM;
    }

    private static byte[] generateCoverPayload(int size) {
        if (size > PAYLOAD_BYTES)
            throw new IllegalArgumentException("Cover data cannot exceed max payload size = " + PAYLOAD_BYTES);
        else if (size <= 0)
            throw new IllegalArgumentException("Cover size must me positive");

        val payload = new byte[size];
        ThreadLocalRandom.current().nextBytes(payload);

        return payload;
    }

    private static MessageType ensureDatumOrCoverMessageType(MessageType messageType) {
        if (messageType != ONION_TUNNEL_DATUM
                && messageType != ONION_TUNNEL_COVER)
            throw new IllegalArgumentException("Message type must be ONION_TUNNEL_DATUM or ONION_TUNNEL_COVER");

        return messageType;
    }
}
