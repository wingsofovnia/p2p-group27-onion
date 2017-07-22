package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.proto.message.TypedMessage;

import java.nio.ByteBuffer;

import static de.tum.p2p.util.Paddings.randPad;

/**
 * {@code OnionMessage} is a {@link TypedMessage} of fixed size {@link #BYTES}
 * that is used for P2P UDP communication by {@link de.tum.p2p.onion.forwarding.OnionForwarder}
 *
 * @see TypedMessage
 */
public abstract class OnionMessage extends TypedMessage {

    public static final int BYTES = 1024 - LENGTH_PREFIX_BYTES;

    private final boolean enableAutoPadding;

    public OnionMessage(MessageType messageType, boolean enableAutoPadding) {
        super(messageType, BYTES - MessageType.BYTES);

        this.enableAutoPadding = enableAutoPadding;
    }

    public OnionMessage(MessageType messageType) {
        this(messageType, true);
    }

    @Override
    protected ByteBuffer enhanceMessage(ByteBuffer typedMessageBuffer) {
        return randPad(typedMessageBuffer);
    }
}
