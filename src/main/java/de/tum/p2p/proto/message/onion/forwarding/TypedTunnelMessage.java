package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;

import static de.tum.p2p.util.ByteBuffers.bufferConsumedBytes;
import static de.tum.p2p.util.Paddings.randPadToArray;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TypedTunnelMessage} is a typed version of {@code TunnelMessage},
 * i.e. contains {@code MessageType}
 * <p>
 * Packet structure (abstract):
 * <pre>
 * |-------------|-------------|
 * |     LP*     |  MSSG TYPE  |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * | ......................... |
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
 *
 * @see TunnelMessage
 * @see MessageType
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public abstract class TypedTunnelMessage extends TunnelMessage {

    /**
     * A size of metadata this message carries
     */
    public static final int META_BYTES = MessageType.BYTES + TunnelId.BYTES;

    /**
     * Amount of bytes that are free to use by child classes (max - meta)
     */
    public static final int PAYLOAD_BYTES = TunnelMessage.PAYLOAD_BYTES - MessageType.BYTES;

    @Getter
    protected final MessageType messageType;

    protected TypedTunnelMessage(TunnelId tunnelId, MessageType messageType) {
        super(tunnelId);
        this.messageType = notNull(messageType);
    }

    public static MessageType guessType(byte[] bytes) {
        if (bytes.length < MessageType.BYTES)
            return MessageType.UNKNOWN;

        val bytesBuffer = ByteBuffer.wrap(bytes);

        return MessageType.fromCode(bytesBuffer.getShort());
    }

    @Override
    public byte[] bytes(boolean pad) {
        val tunnelMsgBuffer
            = ByteBuffer.allocate(BYTES)
                .putShort(messageType.code())
                    .putInt(tunnelId.raw());

        val disassembledTunnelMessage = writeMessage(tunnelMsgBuffer);

        if (!pad)
            return bufferConsumedBytes(disassembledTunnelMessage);

        return randPadToArray(disassembledTunnelMessage);
    }

    protected static TypedTunnelMessage fromBytes(ByteBuffer bytesBuffer, MessageType typeExpected) {
        if (bytesBuffer.remaining() < TunnelMessage.BYTES)
            throw new ProtoException("Too short byte array. Expected to be at least " + TunnelMessage.BYTES);

        val parsedMessageType = MessageType.fromCode(bytesBuffer.getShort());
        if (typeExpected != null && typeExpected != parsedMessageType)
            throw new ProtoException("Given bytes doesn't represent message of type " + typeExpected.name());

        val parsedTunnelId = TunnelId.wrap(bytesBuffer.getInt());

        return new TypedTunnelMessage(parsedTunnelId, parsedMessageType) {
            @Override
            protected ByteBuffer writeMessage(ByteBuffer messageBuffer) {
                throw new UnsupportedOperationException();
            }
        };
    }

    protected static TypedTunnelMessage fromBytes(ByteBuffer bytesBuffer) {
        return fromBytes(bytesBuffer, null);
    }
}
