package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.proto.message.TypedMessage;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * {@code OnionMessage} is a {@link TypedMessage} of fixed size {@link #BYTES}
 * that is used for P2P UDP communication by {@link de.tum.p2p.onion.forwarding.OnionForwarder}
 *
 * @see TypedMessage
 */
@Getter @Accessors(fluent = true)
public abstract class OnionMessage extends TypedMessage {

    public static final int BYTES = 1024 - LENGTH_PREFIX_BYTES;

    public OnionMessage(MessageType messageType) {
        super(messageType, BYTES - MessageType.BYTES);
    }
}
