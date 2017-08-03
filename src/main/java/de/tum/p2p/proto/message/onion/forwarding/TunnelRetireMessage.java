package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.val;

import java.nio.ByteBuffer;

/**
 * {@code TunnelRetireMessage} used to notify the tunnel peers that the tunnel
 * was retired therefore they can clean their resources and routing tables.
 * <p>
 * Packet structure:
 * <pre>
 * |-------------|-------------|
 * |     LP*     |    RETIRE   |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
 *
 * @see TunnelMessage
 * @see MessageType
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@EqualsAndHashCode(callSuper = true)
public class TunnelRetireMessage extends TypedTunnelMessage {

    public TunnelRetireMessage(TunnelId tunnelId) {
        super(tunnelId, MessageType.ONION_TUNNEL_RETIRE);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer messageBuffer) {
        return messageBuffer;
    }

    public static TunnelRetireMessage fromBytes(byte[] bytes) {
        val bytesBuffer = ByteBuffer.wrap(bytes);
        val rawTypedTunnelMessage = TypedTunnelMessage.fromBytes(bytesBuffer, MessageType.ONION_TUNNEL_RETIRE);

        val parsedTunnelId = rawTypedTunnelMessage.tunnelId();

        return new TunnelRetireMessage(parsedTunnelId);
    }
}
