package de.tum.p2p.proto.message;

import de.tum.p2p.proto.ProtoException;
import lombok.val;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.proto.message.TypedMessage.guessType;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypedMessageTest {

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionOn0SizedMessage() {
        new TypedMessage(MessageType.ONION_COVER, 0) {
            @Override
            protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
                return null;
            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionOnNegativeSizedMessage() {
        new TypedMessage(MessageType.ONION_COVER, -1) {
            @Override
            protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
                return null;
            }
        };
    }

    @Test
    public void guessesAllAvailableMessageTypesCorrectly() throws Exception {
        val msgTypesAvailable = stream(MessageType.values());

        msgTypesAvailable.forEach(type -> {
            val rawTypedMsg
                = ByteBuffer.allocate(MessageType.BYTES
                    + ThreadLocalRandom.current().nextInt(10))
                        .putShort(type.code()).array();

            assertEquals(type, guessType(rawTypedMsg));
        });
    }

    @Test(expected = ProtoException.class)
    public void validatesUndersizedMessagesDuringByting() {
        val msgBodyValue = ThreadLocalRandom.current().nextInt();
        val actualMsgBodySize = Integer.BYTES;
        val underestimatedMsgBodySize = Short.BYTES;
        assertTrue(actualMsgBodySize > underestimatedMsgBodySize);

        new TypedMessage(MessageType.ONION_COVER, underestimatedMsgBodySize) {

            @Override
            protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
                typedMessageBuffer.putInt(msgBodyValue);
                return typedMessageBuffer;
            }
        }.bytes();
    }

    @Test(expected = ProtoException.class)
    public void validatesOversizedMessagesDuringByting() {
        val msgBodyValue = ThreadLocalRandom.current().nextFloat();
        val actualMsgBodySize = Float.BYTES;
        val overestimatedMsgBodySize = Long.BYTES;
        assertTrue(actualMsgBodySize < overestimatedMsgBodySize);

        new TypedMessage(MessageType.ONION_COVER, overestimatedMsgBodySize) {

            @Override
            protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
                typedMessageBuffer.putFloat(msgBodyValue);
                return typedMessageBuffer;
            }
        }.bytes();
    }
}
