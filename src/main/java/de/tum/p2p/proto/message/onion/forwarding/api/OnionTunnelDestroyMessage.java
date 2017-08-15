package de.tum.p2p.proto.message.onion.forwarding.api;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.nio.ByteBuffer;

@ToString
@EqualsAndHashCode(callSuper = true)
public class OnionTunnelDestroyMessage extends OnionApiMessage {

    private final TunnelId tunnelId;

    public OnionTunnelDestroyMessage(TunnelId id) {
        super(MessageType.ONION_TUNNEL_DESTROY);
        this.tunnelId = id;
    }

    public TunnelId getTunnelId() {
        return tunnelId;
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        throw new UnsupportedOperationException();
    }

    public static OnionTunnelDestroyMessage fromBytes(ByteBuffer buffer) throws ProtoException {
        buffer.getShort();
        int id = buffer.getInt();
        if (0 != buffer.remaining()) throw new ProtoException ("Buffer size not expected");
        return new OnionTunnelDestroyMessage(TunnelId.wrap(id));
    }
}
