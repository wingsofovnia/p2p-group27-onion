package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.RequestId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static de.tum.p2p.proto.message.MessageType.ONION_TUNNEL_EXTEND;
import static de.tum.p2p.util.Keys.parsePublicKey;
import static java.lang.Short.toUnsignedInt;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelExtendMessage} is received by the onion that is requested to be
 * a new peer in the tunnel. The onion response with {@link TunnelExtendedMessage}
 * then if it accepts the proposition.
 * <p>
 * Packet structure:
 * <pre>
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |  MESG_TYPE  |   REQE_ID   |
 * |---------------------------|
 * |   KEY LEN   |   SRC KEY   |
 * |---------------------------|
 * |      SRC KEY (CONT..)     |
 * |---------------------------|
 * |   HS1 LENG  |  HANDSHAKE  |
 * |---------------------------|
 * |     HANDSHAKE(CONT..)     |
 * |-------------|-------------|
 * </pre>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class TunnelExtendMessage extends TraceableTypedTunnelMessage {

    @Getter
    private final PublicKey sourceKey;

    @Getter
    private final byte[] handshake;

    public TunnelExtendMessage(TunnelId tunnelId, RequestId requestId, PublicKey sourceKey, byte[] handshake) {
        super(tunnelId, requestId, ONION_TUNNEL_EXTEND);
        this.sourceKey = notNull(sourceKey);
        this.handshake = notNull(handshake);
    }

    public TunnelExtendMessage(TunnelId tunnelId, PublicKey sourceKey, byte[] handshake) {
        this(tunnelId, null, sourceKey, handshake);
    }

    public static TunnelExtendMessage fromBytes(byte[] bytes) {
        try {
            val bytesBuffer = ByteBuffer.wrap(bytes);

            val parsedTunnelId = TunnelId.wrap(bytesBuffer.getInt());
            val messageType = MessageType.fromCode(bytesBuffer.getShort());

            if (messageType != ONION_TUNNEL_EXTEND)
                throw new IllegalArgumentException("Not a ONION_TUNNEL_EXTEND message");

            val parsedRequestId = RequestId.wrap(bytesBuffer.getShort());

            val parsedSourceKeySize = toUnsignedInt(bytesBuffer.getShort());
            val rawParsedSourceKey = new byte[parsedSourceKeySize];
            bytesBuffer.get(rawParsedSourceKey);

            PublicKey parsedSourceKey;
            try {
                parsedSourceKey = parsePublicKey(rawParsedSourceKey);
            } catch (InvalidKeySpecException e) {
                throw new ProtoException("Failed to parse source key: " + Arrays.toString(rawParsedSourceKey), e);
            }

            val parsedHandshakeSize = toUnsignedInt(bytesBuffer.getShort());
            val parsedHandshake = new byte[parsedHandshakeSize];
            bytesBuffer.get(parsedHandshake);

            return new TunnelExtendMessage(parsedTunnelId, parsedRequestId, parsedSourceKey, parsedHandshake);
        } catch (BufferUnderflowException | BufferOverflowException e) {
            throw new ProtoException("Failed to parse ONION_TUNNEL_EXTEND message", e);
        }
    }

    @Override
    protected void writeBody(ByteBuffer messageBuffer) {
        val sourceKeyBytes = sourceKey.getEncoded();
        messageBuffer.putShort((short) sourceKeyBytes.length);
        messageBuffer.put(sourceKeyBytes);

        messageBuffer.putShort((short) handshake.length);
        messageBuffer.put(handshake);
    }
}
