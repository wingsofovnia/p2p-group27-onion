package de.tum.p2p.onion.forwarding.proto.message;

import lombok.val;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.onion.forwarding.proto.message.TypedMessage.guessType;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;

public class TypedMessageTest {

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
}
