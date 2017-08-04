package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.nio.ByteBuffer;

import static de.tum.p2p.util.ByteBuffers.bufferWrittenBytes;
import static de.tum.p2p.util.Paddings.randPadToArray;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * {@code TraceableTypedTunnelMessage} is traceable version of
 * {@code TypedTunnelMessage}, i.e. contains {@code RequestId} used to
 * communication in request-response manner
 * <p>
 * Packet structure (abstract):
 * <pre>
 * |-------------|-------------|
 * |     LP*     |  MSSG TYPE  |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |    REQ ID   |     ...
 * |-------------|
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
 *
 * @see TunnelMessage
 * @see MessageType
 * @see RequestId
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public abstract class TraceableTypedTunnelMessage extends TypedTunnelMessage {

    /**
     * A size of metadata this message carries
     */
    public static final int META_BYTES = MessageType.BYTES + TunnelId.BYTES + RequestId.BYTES;

    /**
     * Amount of bytes that are free to use by child classes (max - meta)
     */
    public static final int PAYLOAD_BYTES = TunnelMessage.PAYLOAD_BYTES - RequestId.BYTES - MessageType.BYTES;

    @Getter
    protected final RequestId requestId;

    protected TraceableTypedTunnelMessage(TunnelId tunnelId, RequestId requestId, MessageType messageType) {
        super(tunnelId, messageType);
        this.requestId = defaultIfNull(requestId, RequestId.next());
    }

    @Deprecated
    protected TraceableTypedTunnelMessage(TunnelId tunnelId, short requestId, MessageType messageType) {
        super(tunnelId, messageType);
        this.requestId = RequestId.wrap(requestId);
    }

    protected TraceableTypedTunnelMessage(TunnelId tunnelId, MessageType messageType) {
        this(tunnelId, null, messageType);
    }

    @Override
    public byte[] bytes(boolean pad) {
        val tunnelMsgBuffer
            = ByteBuffer.allocate(BYTES)
                .putShort(messageType.code())
                    .putInt(tunnelId.raw())
                        .putShort(requestId.raw());

        val disassembledTunnelMessage = writeMessage(tunnelMsgBuffer);

        if (!pad)
            return bufferWrittenBytes(disassembledTunnelMessage);

        return randPadToArray(disassembledTunnelMessage);
    }

    protected static TraceableTypedTunnelMessage fromBytes(ByteBuffer bytesBuffer, MessageType typeExpected) {
        val rawTypedTunnelMsg = TypedTunnelMessage.fromBytes(bytesBuffer, typeExpected);
        val parsedMessageType = rawTypedTunnelMsg.messageType();
        val parsedTunnelId = rawTypedTunnelMsg.tunnelId();

        val parsedRequestId = RequestId.wrap(bytesBuffer.getShort());

        return new TraceableTypedTunnelMessage(parsedTunnelId, parsedRequestId, parsedMessageType) {
            @Override
            protected ByteBuffer writeMessage(ByteBuffer messageBuffer) {
                throw new UnsupportedOperationException();
            }
        };
    }

    protected static TraceableTypedTunnelMessage fromBytes(ByteBuffer bytesBuffer) {
        return fromBytes(bytesBuffer, null);
    }
}
