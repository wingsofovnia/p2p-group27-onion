package de.tum.p2p.proto.message.onion.forwarding.api;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.nio.ByteBuffer;

@ToString
@EqualsAndHashCode(callSuper = true)
public class OnionTunnelIncomingMessage extends OnionApiMessage {
    private final TunnelId tunnelId;

    public OnionTunnelIncomingMessage(TunnelId tunnelId) {
        super(MessageType.ONION_TUNNEL_READY);
        this.tunnelId = tunnelId;
    }

    public static OnionTunnelIncomingMessage fromBytes(ByteBuffer buffer) throws Exception {
        buffer.getShort();
        int id = buffer.getInt();
        OnionTunnelIncomingMessage message = new OnionTunnelIncomingMessage(TunnelId.wrap(id));
        return message;
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        throw new UnsupportedOperationException();
    }
}
