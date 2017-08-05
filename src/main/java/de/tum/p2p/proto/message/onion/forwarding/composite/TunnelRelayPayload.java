package de.tum.p2p.proto.message.onion.forwarding.composite;

import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;

import static de.tum.p2p.util.ByteBuffers.bufferWrittenBytes;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Payload structure:
 * <pre>
 * |---------------------------|
 * |  MESG_TYPE  |     ...     |
 * |---------------------------|
 * | ......................... |
 * |---------------------------|
 * </pre>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public abstract class TunnelRelayPayload {

    /**
     * Maximal amount of bytes messages of this type can carry
     */
    public static final int BYTES = TunnelRelayMessage.PAYLOAD_BYTES;

    /**
     * A size of metadata this message carries
     */
    public static final int META_BYTES = MessageType.BYTES;

    /**
     * Amount of bytes that are free to use for data (max - meta)
     */
    public static final int PAYLOAD_BYTES = BYTES - META_BYTES;

    @Getter
    protected final MessageType messageType;

    protected TunnelRelayPayload(MessageType messageType) {
        this.messageType = notNull(messageType);
    }

    protected abstract void writePayload(ByteBuffer messageBuffer);

    public byte[] bytes() {
        val msgBytesBuffer = ByteBuffer.allocate(BYTES);

        // META_BYTES
        msgBytesBuffer.putShort(messageType.code());

        // PAYLOAD_BYTES from childs
        writePayload(msgBytesBuffer);

        return bufferWrittenBytes(msgBytesBuffer);
    }
}
