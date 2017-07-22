package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;

import static de.tum.p2p.util.ByteArrayPaddings.pad;
import static de.tum.p2p.util.Handshakes.notOversizedHadshake;
import static java.lang.Short.toUnsignedInt;

/**
 * {@code TunnelExtendedMessage} represents a confirmation of tunnel
 * extension and carries a handshake HS2 from a remote peer on the
 * tunnel's end (Bob)
 */
@Accessors(fluent = true) @Getter
@ToString @EqualsAndHashCode(callSuper = true)
public class TunnelExtendedMessage extends OnionMessage {

    private final TunnelId tunnelId;

    private final int requestId;

    private final byte[] handshake;

    public TunnelExtendedMessage(TunnelId tunnelId, int requestId, byte[] handshake) {
        super(MessageType.ONION_TUNNEL_EXTENDED);

        this.tunnelId = tunnelId;
        this.requestId = requestId;
        this.handshake = notOversizedHadshake(handshake);
    }

    public static TunnelExtendedMessage of(TunnelId tunnelId, int requestId, byte[] handshake) {
        return new TunnelExtendedMessage(tunnelId, requestId, handshake);
    }

    public TunnelExtendedMessage(TunnelId tunnelId, int requestId, ByteBuffer handshake) {
        this(tunnelId, requestId, handshake.array());
    }

    public static TunnelExtendedMessage of(TunnelId tunnelId, int requestId, ByteBuffer handshake) {
        return new TunnelExtendedMessage(tunnelId, requestId, handshake);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        typedMessageBuffer.putInt(tunnelId.raw());
        typedMessageBuffer.putInt(requestId);

        typedMessageBuffer.putShort((short) handshake.length);
        val freeSpaceLeft = typedMessageBuffer.capacity() - typedMessageBuffer.position();
        val paddedHandshake = pad(handshake, freeSpaceLeft);
        for (val hb : paddedHandshake) typedMessageBuffer.put(hb);

        return typedMessageBuffer;
    }

    public static TunnelExtendedMessage fromBytes(byte[] rawTypedMessage) {
        val rawTunnelExtendedMessage = untype(rawTypedMessage, MessageType.ONION_TUNNEL_EXTENDED);

        val parsedTunnelId = rawTunnelExtendedMessage.getInt();
        val parsedRequestId = rawTunnelExtendedMessage.getInt();

        val parsedHandshakeSize = toUnsignedInt(rawTunnelExtendedMessage.getShort());
        val parsedHandshake = new byte[parsedHandshakeSize];
        rawTunnelExtendedMessage.get(parsedHandshake);

        return TunnelExtendedMessage.of(TunnelId.wrap(parsedTunnelId), parsedRequestId, parsedHandshake);
    }
}
