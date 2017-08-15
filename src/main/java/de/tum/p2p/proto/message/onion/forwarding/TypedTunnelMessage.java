package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TypedTunnelMessage} is a typed version of {@code TunnelMessage},
 * i.e. contains {@code MessageType}
 * <p>
 * Packet structure (abstract):
 * <pre>
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |  MESG_TYPE  |     ...     |
 * |---------------------------|
 * | ......................... |
 * |---------------------------|
 * </pre>
 *
 * @see TunnelMessage
 * @see MessageType
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public abstract class TypedTunnelMessage extends TunnelMessage {

    /**
     * A size of metadata this message carries
     */
    public static final int META_BYTES = TunnelMessage.BYTES + MessageType.BYTES;

    /**
     * Amount of bytes that are free to use by child classes (max - meta)
     */
    public static final int PAYLOAD_BYTES = BYTES - META_BYTES;

    @Getter
    protected final MessageType messageType;

    protected TypedTunnelMessage(TunnelId tunnelId, MessageType messageType) {
        super(tunnelId);
        this.messageType = notNull(messageType);
    }

    @Override
    protected void writeHeaders(ByteBuffer messageBuffer) {
        messageBuffer.putInt(tunnelId.raw());
        messageBuffer.putShort(messageType.code());
    }

    public static MessageType guessType(byte[] bytes) {
        if (bytes.length < MessageType.BYTES)
            return MessageType.UNKNOWN;

        val bytesBuffer = ByteBuffer.wrap(bytes);
        bytesBuffer.position(TunnelId.BYTES);

        return MessageType.fromCode(bytesBuffer.getShort());
    }
}
