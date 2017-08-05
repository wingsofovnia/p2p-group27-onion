package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.val;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static de.tum.p2p.proto.message.MessageType.ONION_TUNNEL_RETIRE;

/**
 * {@code TunnelRetireMessage} used to notify the tunnel peers that the tunnel
 * was retired therefore they can clean their resources and routing tables.
 * <p>
 * Packet structure:
 * <pre>
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |    RETIRE   |
 * |-------------|
 * </pre>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@EqualsAndHashCode(callSuper = true)
public class TunnelRetireMessage extends TypedTunnelMessage {

    public TunnelRetireMessage(TunnelId tunnelId) {
        super(tunnelId, ONION_TUNNEL_RETIRE);
    }

    public static TunnelRetireMessage fromBytes(byte[] bytes) {
        try {
            val bytesBuffer = ByteBuffer.wrap(bytes);

            val parsedTunnelId = TunnelId.wrap(bytesBuffer.getInt());
            val messageType = MessageType.fromCode(bytesBuffer.getShort());

            if (messageType != ONION_TUNNEL_RETIRE)
                throw new IllegalArgumentException("Not a ONION_TUNNEL_RETIRE message");

            return new TunnelRetireMessage(parsedTunnelId);
        } catch (BufferUnderflowException | BufferOverflowException e) {
            throw new ProtoException("Failed to parse ONION_TUNNEL_RETIRE message", e);
        }
    }

    @Override
    protected void writeBody(ByteBuffer messageBuffer) {
        // Retire message doesn't carry any additional payload
    }
}
