package de.tum.p2p.onion.forwarding.proto.message;

import lombok.val;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

import static org.apache.commons.lang3.ArrayUtils.toObject;
import static org.apache.commons.lang3.ArrayUtils.toPrimitive;
import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public class MessageUnsignedBytesTest {

    private static final int TEST_RUNS = 20;
    private static final int MESSAGE_SIZE = 20;

    @Parameterized.Parameters
    public static Collection<Pair<Integer[], Byte[]>> data() {
        val data = new ArrayList<Pair<Integer[], Byte[]>>(TEST_RUNS);

        for (int i = 0; i < TEST_RUNS; i++) {
            val unsignedRandomMsgBytes = new Integer[MESSAGE_SIZE];

            val randomMsgBytes = toObject(RandomUtils.nextBytes(MESSAGE_SIZE));
            for (int j = 0; j < randomMsgBytes.length; j++) {

                // Make sure all bytes are negative
                if (randomMsgBytes[j] > 0)
                    randomMsgBytes[j] = (byte) -randomMsgBytes[j];

                // Expected output - unsigned bytes
                unsignedRandomMsgBytes[j] = Byte.toUnsignedInt(randomMsgBytes[j]);
            }

            data.add(Pair.of(unsignedRandomMsgBytes, randomMsgBytes));
        }

        return data;
    }

    private Pair<Integer[], Byte[]> testData;

    public MessageUnsignedBytesTest(Pair<Integer[], Byte[]> testData) {
        this.testData = testData;
    }

    @Test
    public void unsignedBytes() {
        val expectedOutput = testData.getLeft();
        val mockedOutput = testData.getRight();

        // Mockito does not support default methods yet :(
        val message = new Message() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public byte[] bytes() {
                return toPrimitive(mockedOutput);
            }
        };

        val unsignedMsgBytes = toObject(
            message.unsignedBytes()
        );

        assertArrayEquals(expectedOutput, unsignedMsgBytes);
    }
}
