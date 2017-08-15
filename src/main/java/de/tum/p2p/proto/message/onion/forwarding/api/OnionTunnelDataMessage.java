package de.tum.p2p.proto.message.onion.forwarding.api;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class OnionTunnelDataMessage extends OnionApiMessage {

    private final TunnelId tunnelId;
    private final ByteBuffer data;

    public OnionTunnelDataMessage(TunnelId id, ByteBuffer data) {
        super(MessageType.ONION_TUNNEL_DESTROY);
        this.tunnelId = id;
        this.data = data;
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        throw new UnsupportedOperationException();
    }

    public static OnionTunnelDataMessage fromBytes(ByteBuffer buffer) throws Exception {
        buffer.getShort();
        int id = buffer.getInt();
        byte[] data = new byte[buffer.remaining()];
        return new OnionTunnelDataMessage(TunnelId.wrap(id), buffer.get(data));
    }
}
