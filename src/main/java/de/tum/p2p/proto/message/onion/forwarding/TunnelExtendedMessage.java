package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;

import static de.tum.p2p.proto.message.MessageType.ONION_TUNNEL_EXTENDED;
import static de.tum.p2p.util.ByteBuffers.bufferAllBytes;
import static de.tum.p2p.util.Handshakes.notOversizedHadshake;
import static java.lang.Short.toUnsignedInt;

/**
 * {@code TunnelExtendedMessage} is a message that may be propagated down
 * the tunnel to the originator by a new peer who decided to be a member of
 * the tunnel on {@link TunnelExtendMessage} request.
 * <p>
 * Packet structure:
 * <pre>
 * |-------------|-------------|
 * |     LP*     |  EXTEND_ED  |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |    REQ ID   |   HS2 LEN   |
 * |-------------|-------------|
 * |         HANDSHAKE         |
 * |-------------|-------------|
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
 *
 * @see TunnelExtendedMessage
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Accessors(fluent = true)
@ToString @EqualsAndHashCode(callSuper = true)
public class TunnelExtendedMessage extends TraceableTypedTunnelMessage {

    @Getter
    private final byte[] handshake;

    public TunnelExtendedMessage(TunnelId tunnelId, RequestId requestId, byte[] handshake) {
        super(tunnelId, requestId, ONION_TUNNEL_EXTENDED);
        this.handshake = notOversizedHadshake(handshake);
    }

    public TunnelExtendedMessage(TunnelId tunnelId, RequestId requestId, ByteBuffer handshake) {
        this(tunnelId, requestId, bufferAllBytes(handshake));
    }

    public TunnelExtendedMessage(TunnelId tunnelId, byte[] handshake) {
        this(tunnelId, null, handshake);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        typedMessageBuffer.putShort((short) handshake.length);
        typedMessageBuffer.put(handshake);

        return typedMessageBuffer;
    }

    public static TunnelExtendedMessage fromBytes(byte[] bytes) {
        val bytesBuffer = ByteBuffer.wrap(bytes);
        val rawTraceableTypedTunnelMsg = TraceableTypedTunnelMessage.fromBytes(bytesBuffer, ONION_TUNNEL_EXTENDED);

        val parsedTunnelId = rawTraceableTypedTunnelMsg.tunnelId();
        val parsedRequestId = rawTraceableTypedTunnelMsg.requestId();

        val parsedHandshakeSize = toUnsignedInt(bytesBuffer.getShort());
        val parsedHandshake = new byte[parsedHandshakeSize];
        bytesBuffer.get(parsedHandshake);

        return new TunnelExtendedMessage(parsedTunnelId, parsedRequestId, parsedHandshake);
    }
}
