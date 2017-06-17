package de.tum.p2p.proto.message;

import de.tum.p2p.proto.ProtoException;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.String.format;

/**
 * Represents a {@link Message} implementation that reserves first
 * {@link MessageType#BYTES} bytes to describe Message's Type, defined
 * by {@link MessageType}.
 */
@ToString
@EqualsAndHashCode
public abstract class TypedMessage implements Message {

    private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;

    private final int size;
    private final MessageType messageType;

    protected TypedMessage(MessageType messageType, int size) {
        this.messageType = messageType;
        this.size = size + MessageType.BYTES;
    }

    /**
     * Disassembles message data into bytes
     * @param typedMessageBuffer a ByteBuffer with {@link TypedMessage#messageType} set
     * @return ByteBuffer filled out with disassembled message data
     */
    protected abstract ByteBuffer writeMessage(ByteBuffer typedMessageBuffer);

    @Override
    public final byte[] bytes() {
        val typedMessageBuffer
            = ByteBuffer.allocate(size)
                        .order(BYTE_ORDER)
                        .putShort(messageType.code());

        val enrichedTypedMessageBuffer = writeMessage(typedMessageBuffer);

        return enrichedTypedMessageBuffer.array();
    }

    @Override
    public int size() {
        return this.size;
    }

    public MessageType messageType() {
        return this.messageType;
    }

    public static MessageType guessType(byte[] rawTypedMessage) {
        val rawTypedMessageBuffer = ByteBuffer.wrap(rawTypedMessage);
        val rawTypedMessageTypeCode = rawTypedMessageBuffer.getShort();

        return MessageType.fromCode(rawTypedMessageTypeCode);
    }

    /**
     * Cleans byte array from {@link MessageType} and validates removed type
     * against given type
     * @param rawTypedMessage a raw, disassembled typed message
     * @param type a MessageType the validation will be conducted against.
     * @return a ByteBuffer that represents given raw, disassembled typed message
     * without {@link MessageType}
     */
    protected static ByteBuffer untype(byte[] rawTypedMessage, MessageType type) {
        val rawTypedMessageBuffer = ByteBuffer.wrap(rawTypedMessage);
        val rawTypedMessageTypeCode = rawTypedMessageBuffer.getShort();
        val rawTypedMessageType = MessageType.fromCode(rawTypedMessageTypeCode);

        if (!rawTypedMessageType.equals(type))
            throw new ProtoException(format("Failed untype raw typed message for MessageType given. " +
                    "Expected %s, given %s (%s)", type.name(), rawTypedMessageType.name(), rawTypedMessageTypeCode));

        return rawTypedMessageBuffer.slice();
    }
}
