package de.tum.p2p.proto.message.onion.forwarding.api;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.NonNull;

import de.tum.p2p.proto.message.MessageType;
import java.nio.ByteBuffer;

@ToString
@EqualsAndHashCode(callSuper = true)
public class OnionCoverMessage extends OnionApiMessage {

    @NonNull
    private final int coverSize;

    public OnionCoverMessage(int coverSize) {
        super(MessageType.ONION_COVER);
        this.coverSize = coverSize;
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        throw new UnsupportedOperationException();
    }

    public static OnionCoverMessage fromBytes(ByteBuffer buffer) {
        buffer.getShort();
        int coverSize = buffer.getInt();
        return new OnionCoverMessage(coverSize);
    }
}
