package de.tum.p2p.proto.message;

import de.tum.p2p.proto.ProtoException;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import org.apache.commons.lang3.Validate;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.String.format;
import static java.util.Arrays.copyOfRange;

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

    protected TypedMessage(MessageType messageType, int msgBodySize) {
        Validate.isTrue(msgBodySize >= 0, "Message body size cannot be less or equal 0");

        this.messageType = messageType;
        this.size = msgBodySize + MessageType.BYTES;
    }

    /**
     * Disassembles message data into bytes
     * @param typedMessageBuffer a ByteBuffer with {@link TypedMessage#messageType} set
     * @return ByteBuffer filled out with disassembled message data
     */
    protected abstract ByteBuffer writeMessage(ByteBuffer typedMessageBuffer);

    /**
     * Enhances message before futher processing by {@link #bytes()}.
     * <p>
     * Goes strictly after {@link #writeMessage(ByteBuffer)} before any validation by
     * {@link #bytes()}
     *
     * @param typedMessageBuffer a ByteBuffer to enhance with message content
     * @return enhanced message content as a ByteBuffer
     */
    protected ByteBuffer enhanceMessage(ByteBuffer typedMessageBuffer) {
        return typedMessageBuffer;
    }

    @Override
    public final byte[] bytes() {
        val typedMessageBuffer
            = ByteBuffer.allocate(size)
                        .order(BYTE_ORDER)
                        .putShort(messageType.code());

        try {
            val disassembledTypedMessageBuffer = writeMessage(typedMessageBuffer);
            val enhancedTypedMessageBuffer = enhanceMessage(disassembledTypedMessageBuffer);

            val rawTypedMessage = convertToExactByteArray(enhancedTypedMessageBuffer);

            if (rawTypedMessage.length != size())
                throw new ProtoException("Actual size of disassembled message doesn't match the declared value: "
                    + "expected = " + size() + ", actual = " + rawTypedMessage.length);

            return rawTypedMessage;
        } catch (BufferOverflowException | BufferUnderflowException e) {
            throw new ProtoException("Actual size of disassembled message doesn't match the declared value", e);
        }
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

    private static byte[] convertToExactByteArray(ByteBuffer byteBuffer) {
        val fullSizedArray = byteBuffer.array();
        return copyOfRange(fullSizedArray, 0, byteBuffer.position());
    }
}
