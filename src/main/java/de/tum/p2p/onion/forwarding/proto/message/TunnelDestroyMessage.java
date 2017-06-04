package de.tum.p2p.onion.forwarding.proto.message;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;

import java.nio.ByteBuffer;

@ToString
@EqualsAndHashCode(callSuper = true)
public class TunnelDestroyMessage extends TypedMessage {

    private static final MessageType messageType = MessageType.ONION_TUNNEL_DESTROY;

    private final int tunnelId;

    protected TunnelDestroyMessage(int tunnelId) {
        super(messageType, Integer.BYTES);
        this.tunnelId = tunnelId;
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        return typedMessageBuffer.putInt(tunnelId);
    }

    public static TunnelDestroyMessage fromBytes(byte[] rawTypedMessage) {
        val rawTunnelDestroyMessage = untype(rawTypedMessage, messageType);
        val tunnelId = rawTunnelDestroyMessage.getInt();

        return new TunnelDestroyMessage(tunnelId);
    }
}
