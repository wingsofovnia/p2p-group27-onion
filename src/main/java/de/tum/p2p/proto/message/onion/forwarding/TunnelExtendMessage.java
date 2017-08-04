package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static de.tum.p2p.proto.message.MessageType.ONION_TUNNEL_EXTEND;
import static de.tum.p2p.util.Handshakes.notOversizedHadshake;
import static de.tum.p2p.util.Keys.notOversizedKey;
import static de.tum.p2p.util.Keys.parsePublicKey;
import static java.lang.Short.toUnsignedInt;

/**
 * {@code TunnelExtendMessage} is received by the onion that is requested to be
 * a new peer in the tunnel. The onion response with {@link TunnelExtendedMessage}
 * then if it accepts the proposition.
 * <p>
 * Packet structure:
 * <pre>
 * |-------------|-------------|
 * |     LP*     |    EXTEND   |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |    REQ ID   |   KEY LEN   |
 * |-------------|-------------|
 * |          SRC KEY          |
 * |-------------|-------------|
 * |   HS1 LENG  |  HANDSHAKE  |
 * |-------------|-------------|
 * |     HANDSHAKE(CONT..)     |
 * |-------------|-------------|
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
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
        this.sourceKey = notOversizedKey(sourceKey);
        this.handshake = notOversizedHadshake(handshake);
    }

    public static TunnelExtendMessage fromBytes(byte[] bytes) {
        val bytesBuffer = ByteBuffer.wrap(bytes);
        val rawTraceableTypedTunnelMsg = TraceableTypedTunnelMessage.fromBytes(bytesBuffer, ONION_TUNNEL_EXTEND);

        val parsedTunnelId = rawTraceableTypedTunnelMsg.tunnelId();
        val parsedRequestId = rawTraceableTypedTunnelMsg.requestId();

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
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer messageBuffer) {
        val sourceKeyBytes = sourceKey.getEncoded();
        messageBuffer.putShort((short) sourceKeyBytes.length);
        messageBuffer.put(sourceKeyBytes);

        messageBuffer.putShort((short) handshake.length);
        messageBuffer.put(handshake);

        return messageBuffer;
    }
}
