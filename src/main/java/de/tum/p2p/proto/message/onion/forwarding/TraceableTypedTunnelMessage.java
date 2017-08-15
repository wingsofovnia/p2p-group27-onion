package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.RequestId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.ByteBuffer;

/**
 * {@code TraceableTypedTunnelMessage} is traceable version of {@code TypedTunnelMessage},
 * i.e. contains {@code RequestId} used to communication in request-response manner
 * <p>
 * Packet structure (abstract):
 * <pre>
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |  MESG_TYPE  |   REQE_ID   |
 * |---------------------------|
 * | ......................... |
 * |---------------------------|
 * </pre>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public abstract class TraceableTypedTunnelMessage extends TypedTunnelMessage {

    /**
     * A size of metadata this message carries
     */
    public static final int META_BYTES = TypedTunnelMessage.BYTES + RequestId.BYTES;

    /**
     * Amount of bytes that are free to use by child classes (max - meta)
     */
    public static final int PAYLOAD_BYTES = BYTES - META_BYTES;

    @Getter
    protected final RequestId requestId;

    protected TraceableTypedTunnelMessage(TunnelId tunnelId, RequestId requestId, MessageType messageType) {
        super(tunnelId, messageType);
        this.requestId = requestId == null ? RequestId.next() : requestId;
    }

    protected TraceableTypedTunnelMessage(TunnelId tunnelId, MessageType messageType) {
        this(tunnelId, null, messageType);
    }

    @Override
    protected void writeHeaders(ByteBuffer messageBuffer) {
        messageBuffer.putInt(tunnelId.raw());
        messageBuffer.putShort(messageType.code());
        messageBuffer.putShort(requestId.raw());
    }
}
