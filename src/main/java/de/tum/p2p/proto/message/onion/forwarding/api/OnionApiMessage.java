package de.tum.p2p.proto.message.onion.forwarding.api;

import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.proto.message.TypedMessage;

/**
 * {@code OnionApiMessage} is a {@link TypedMessage} of fixed size {@link #BYTES}
 * that is used for P2P UDP communication by {@link de.tum.p2p.onion.forwarding.OnionForwarder}
 *
 * @see TypedMessage
 */
public abstract class OnionApiMessage extends TypedMessage {

    public static final int BYTES = 1024 - LENGTH_PREFIX_BYTES;

    private final boolean enableAutoPadding;

    public OnionApiMessage(MessageType messageType, boolean enableAutoPadding) {
        super(messageType, BYTES - MessageType.BYTES);
        this.enableAutoPadding = enableAutoPadding;
    }

    public OnionApiMessage(MessageType messageType) {
        this(messageType, true);
    }
}
