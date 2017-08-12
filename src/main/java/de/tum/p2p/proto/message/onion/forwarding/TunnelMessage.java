package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;

import static de.tum.p2p.util.ByteBuffers.bufferWrittenBytes;
import static de.tum.p2p.util.ByteBuffers.randPadRemaining;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelMessage} is a base message type for Onion 2 Onion communication
 * via tunnels. It contains {@code TunnelId} and is capable of padding payloads
 * results of {@link TunnelMessage#writeBody(ByteBuffer)}
 * <p>
 * Packet structure (abstract):
 * <pre>
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * | ......................... |
 * |---------------------------|
 * </pre>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public abstract class TunnelMessage implements Message {

    /**
     * Maximal amount of bytes messages of this type can carry
     */
    public static final int BYTES = 1024;

    /**
     * A size of metadata this message carries
     */
    public static final int META_BYTES = TunnelId.BYTES;

    /**
     * Amount of bytes that are free to use by child classes (max - meta)
     */
    public static final int PAYLOAD_BYTES = BYTES - META_BYTES;

    @Getter
    protected final TunnelId tunnelId;

    protected TunnelMessage(TunnelId tunnelId) {
        this.tunnelId = notNull(tunnelId);
    }

    protected void writeMeta(ByteBuffer messageBuffer) {
        messageBuffer.putInt(tunnelId.raw());
    }

    protected abstract void writeBody(ByteBuffer messageBuffer);

    @Override
    public final byte[] bytes() {
        val msgBytesBuffer = ByteBuffer.allocate(BYTES);

        writeMeta(msgBytesBuffer);
        writeBody(msgBytesBuffer);

        if (msgBytesBuffer.remaining() == 0)
            return msgBytesBuffer.array();

        val paddedMsgBytesBuffer = randPadRemaining(msgBytesBuffer);

        return bufferWrittenBytes(paddedMsgBytesBuffer);
    }

    @Override
    public int size() {
        return BYTES;
    }
}
