package de.tum.p2p.util;

import lombok.val;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.util.ByteBuffers.bufferAllBytes;
import static de.tum.p2p.util.ByteBuffers.bufferWrittenBytes;
import static de.tum.p2p.util.ByteBuffers.randPadRemaining;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ByteBuffersTest {

    @Test
    public void extractsAllUnderlyingBytesCorrectly() {
        val randByteBuff = randPadRemaining(randomSizedByteBuffer());

        assertArrayEquals(randByteBuff.array(), bufferAllBytes(randByteBuff));
    }

    @Test
    public void extractsWrittenBytesCorrectly() throws Exception {
        val byteBuff = randomSizedByteBuffer();

        val writtenBytes = new byte[byteBuff.limit() / 2];
        ThreadLocalRandom.current().nextBytes(writtenBytes);
        byteBuff.put(writtenBytes);

        assertArrayEquals(writtenBytes, bufferWrittenBytes(byteBuff));
    }

    @Test
    public void padsByteBuffRandomly() {
        val byteBuff = randomSizedByteBuffer();
        val capacity = byteBuff.capacity();

        val occupancy = capacity / 2;

        for (int i = 0; i < occupancy; i++) byteBuff.put((byte) 1); // skip with ones
        assertEquals(1, byteBuff.get(occupancy - 1));

        val paddedByteBuff = randPadRemaining(byteBuff);

        assertEquals(capacity, paddedByteBuff.position());

        for (int i = occupancy - 1; i < capacity; i++) {
            assertNotEquals(0, paddedByteBuff.get(i));
        }
    }

    private static ByteBuffer randomSizedByteBuffer() {
        val capacity = ThreadLocalRandom.current().nextInt(Character.BYTES, Character.SIZE);

        return ByteBuffer.allocate(capacity);
    }
}
