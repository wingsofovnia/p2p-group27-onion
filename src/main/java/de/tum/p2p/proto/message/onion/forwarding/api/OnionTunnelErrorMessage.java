package de.tum.p2p.proto.message.onion.forwarding.api;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.nio.ByteBuffer;

@ToString
@EqualsAndHashCode(callSuper = true)
public class OnionTunnelErrorMessage extends OnionApiMessage {

    private final TunnelId tunnelId;

    public OnionTunnelErrorMessage(TunnelId id) {
        super(MessageType.ONION_TUNNEL_DESTROY);
        this.tunnelId = id;
    }

    public TunnelId getTunnelId() {
        return tunnelId;
    }
    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        return null;
    }

    public static OnionTunnelErrorMessage fromBytes(ByteBuffer buffer) throws Exception {
        buffer.getShort();
        buffer.getShort();
        buffer.getShort();
        int id = buffer.getInt();
        return new OnionTunnelErrorMessage(TunnelId.wrap(id));
    }
}
