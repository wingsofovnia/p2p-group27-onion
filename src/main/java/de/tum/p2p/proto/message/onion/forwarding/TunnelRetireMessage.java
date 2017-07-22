package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;

@Getter @Accessors(fluent = true)
@ToString @EqualsAndHashCode(callSuper = true)
public class TunnelRetireMessage extends OnionMessage {

    private final TunnelId tunnelId;

    public TunnelRetireMessage(TunnelId tunnelId) {
        super(MessageType.ONION_TUNNEL_RETIRE);
        this.tunnelId = tunnelId;
    }

    public static TunnelRetireMessage of(TunnelId tunnelId) {
        return new TunnelRetireMessage(tunnelId);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        return typedMessageBuffer.putInt(tunnelId.raw());
    }

    public static TunnelRetireMessage fromBytes(byte[] rawTypedMessage) {
        val rawTunnelRetireMessage = untype(rawTypedMessage, MessageType.ONION_TUNNEL_RETIRE);
        val tunnelId = rawTunnelRetireMessage.getInt();

        return new TunnelRetireMessage(TunnelId.wrap(tunnelId));
    }
}
