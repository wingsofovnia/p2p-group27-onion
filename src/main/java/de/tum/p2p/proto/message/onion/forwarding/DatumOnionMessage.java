package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static de.tum.p2p.util.Paddings.randPad;
import static org.apache.commons.codec.digest.HmacUtils.hmacSha256;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Represents a message used for relying data between Onions.
 * <p>
 * This implementation applies HMAC SHA-256 to payload to enable
 * confidentiality, integrity, and authenticity (Authenticated Encryption).
 *
 * @see <a href="https://goo.gl/e21VV">
 *     Wikipedia - Hash-based message authentication code</a>
 */
@Accessors(fluent = true)
@ToString @EqualsAndHashCode(callSuper = true)
public class DatumOnionMessage extends OnionMessage {

    private static final int HMAC_BYTES = 32; // SHA-256

    public static final int MAX_PAYLOAD_BYTES
        = OnionMessage.BYTES - MessageType.BYTES
            - Integer.BYTES  // tunnelId
            - Integer.BYTES    // payloadSize
            - HMAC_BYTES;

    @Getter
    private final int tunnelId;

    @Getter
    private final byte[] payload;

    private final transient byte[] hmacKey;

    public DatumOnionMessage(int tunnelId, byte[] payload, byte[] hmacKey) {
        super(MessageType.ONION_TUNNEL_DATUM, false);

        if (payload.length > MAX_PAYLOAD_BYTES)
            throw new IllegalArgumentException("Payload is too long! Max expected = " + MAX_PAYLOAD_BYTES
                + " actual = " + payload.length);

        this.tunnelId = tunnelId;
        this.hmacKey = notNull(hmacKey);
        this.payload = payload;
    }

    public static DatumOnionMessage of(int tunnelId, byte[] payload, byte[] hmacKey) {
        return new DatumOnionMessage(tunnelId, payload, hmacKey);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        typedMessageBuffer.putInt(tunnelId);
        typedMessageBuffer.putInt(payload.length);

        val paddedPayload = randPad(payload, MAX_PAYLOAD_BYTES);
        typedMessageBuffer.put(paddedPayload);

        val payloadHmac = hmacSha256(hmacKey, payload);
        typedMessageBuffer.put(payloadHmac);

        return typedMessageBuffer;
    }

    public static DatumOnionMessage fromBytes(byte[] rawTypedMessage, byte[] hmacKey) {
        val rawDatumOnionMessage = untype(rawTypedMessage, MessageType.ONION_TUNNEL_DATUM);

        val parsedTunnelId = rawDatumOnionMessage.getInt();
        val parsedPayloadSize = rawDatumOnionMessage.getInt();

        val parsedPayload = new byte[parsedPayloadSize];
        rawDatumOnionMessage.get(parsedPayload);

        val parsedHmac = new byte[HMAC_BYTES];
        rawDatumOnionMessage.position(rawDatumOnionMessage.capacity() - HMAC_BYTES); // skip padding
        rawDatumOnionMessage.get(parsedHmac);

        val checkHmac = hmacSha256(hmacKey, parsedPayload);
        if (!Arrays.equals(checkHmac, parsedHmac))
            throw new ProtoException("Failed to parse DatumOnionMessage - invalid hmac. \n" +
                "Expected / Actual:\n"
                + Arrays.toString(checkHmac) + "\n"
                + Arrays.toString(parsedHmac) + "\n"
                + "For payload: \n" + Arrays.toString(parsedPayload));

        return new DatumOnionMessage(parsedTunnelId, parsedPayload, hmacKey);
    }
}
