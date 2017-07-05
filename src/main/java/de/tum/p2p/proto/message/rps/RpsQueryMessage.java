package de.tum.p2p.proto.message.rps;

import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.proto.message.TypedMessage;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.nio.ByteBuffer;

/**
 * A {@code RpsQueryMessage} is used to ask a remote RPS to reply with
 * a random peer. This message is short and consists of header only.
 * <p>
 * This is {@code OUTBOUND/REQUEST} only message, therefore there is no
 * fromBytes method.
 */

@ToString @EqualsAndHashCode(callSuper = true)
public class RpsQueryMessage extends TypedMessage {

    public RpsQueryMessage() {
        super(MessageType.RPS_QUERY, 0);
    }

    public static RpsQueryMessage me() {
        return new RpsQueryMessage();
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        return typedMessageBuffer; // RPS QUERY message carries no payload
    }
}
