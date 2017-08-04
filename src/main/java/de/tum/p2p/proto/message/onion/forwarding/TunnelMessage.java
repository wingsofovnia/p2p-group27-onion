package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;

import static de.tum.p2p.util.ByteBuffers.bufferWrittenBytes;
import static de.tum.p2p.util.Paddings.randPadToArray;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelMessage} is a base message type for Onion 2 Onion
 * communication via tunnels. It contains {@code TunnelId} and
 * is capable of padding payloads results of {@link TunnelMessage#writeMessage(ByteBuffer)}
 * <p>
 * Packet structure (abstract):
 * <pre>
 * |-------------|-------------|
 * |     LP*     |   RESERVED  |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * | ......................... |
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
 *
 * @see TunnelMessage#TunnelMessage(TunnelId, boolean)
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public abstract class TunnelMessage implements Message {

    private static final int RESERVED = Short.BYTES;

    /**
     * Maximal amount of bytes messages of this type can carry
     */
    public static final int BYTES = 1024;

    /**
     * A size of metadata this message carries
     */
    public static final int META_BYTES = TunnelId.BYTES + RESERVED;

    /**
     * Amount of bytes that are free to use by child classes (max - meta)
     */
    public static final int PAYLOAD_BYTES = TunnelMessage.BYTES - TunnelId.BYTES;

    @Getter
    protected final TunnelId tunnelId;

    private final boolean padByDefault;

    protected TunnelMessage(TunnelId tunnelId, boolean padByDefault) {
        this.tunnelId = notNull(tunnelId);
        this.padByDefault = padByDefault;
    }

    protected TunnelMessage(TunnelId tunnelId) {
        this(tunnelId, true);
    }

    protected abstract ByteBuffer writeMessage(ByteBuffer messageBuffer);

    @Override
    public byte[] bytes() {
        return bytes(padByDefault);
    }

    public byte[] bytes(boolean pad) {
        val tunnelMsgBuffer = ByteBuffer.allocate(BYTES);

        tunnelMsgBuffer.position(tunnelMsgBuffer.position() + RESERVED);

        tunnelMsgBuffer.putInt(tunnelId.raw());

        val disassembledTunnelMessage = writeMessage(tunnelMsgBuffer);

        if (!pad)
            return bufferWrittenBytes(disassembledTunnelMessage);

        return randPadToArray(disassembledTunnelMessage);
    }

    protected static TunnelMessage fromBytes(ByteBuffer bytesBuffer) {
        if (bytesBuffer.remaining() < TunnelMessage.BYTES)
            throw new ProtoException("Too short byte array. Expected to be at least " + TunnelMessage.BYTES);

        bytesBuffer.position(bytesBuffer.position() + RESERVED);

        val parsedTunnelId = TunnelId.wrap(bytesBuffer.getInt());

        return new TunnelMessage(parsedTunnelId) {
            @Override
            protected ByteBuffer writeMessage(ByteBuffer messageBuffer) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int size() {
        return BYTES;
    }
}
