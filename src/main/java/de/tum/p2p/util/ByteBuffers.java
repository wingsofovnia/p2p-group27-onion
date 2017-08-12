package de.tum.p2p.util;

import lombok.val;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@code ByteBuffers} contains util methods for extracting
 * raw byte arrays from {@link ByteBuffer}.
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public final class ByteBuffers {

    private ByteBuffers() {
        throw new AssertionError("No instance for you");
    }

    /**
     * Returns all bytes of given ByteBuffer from 0 to byteBuffer.limit()
     *
     * @param byteBuffer source of bytes
     * @return a byte array of [0, byteBuffer.limit()] size
     */
    public static byte[] bufferAllBytes(ByteBuffer byteBuffer) {
        val byteBufferDupe = byteBuffer.duplicate();

        val bytes = new byte[byteBufferDupe.capacity()];
        byteBufferDupe.position(0);
        byteBufferDupe.get(bytes);

        return bytes;
    }

    /**
     * Returns a byte array that contains all bytes consumed from given
     * ByteBuffer, i.e. from 0 to current position.
     *
     * @param byteBuffer source of bytes
     * @return a byte array of [0, byteBuffer.position()] size
     */
    public static byte[] bufferConsumedBytes(ByteBuffer byteBuffer) {
        if (byteBuffer.position() == 0)
            return new byte[0];

        val bytesConsumed = byteBuffer.position();

        val byteBufferDupe = byteBuffer.duplicate();
        byteBufferDupe.clear();

        val bytes = new byte[bytesConsumed];
        byteBufferDupe.get(bytes);

        return bytes;
    }

    /**
     * Returns a byte array that contains all bytes written to given
     * ByteBuffer, i.e. from 0 to current position.
     * <p>
     * This method is an alias of {@link ByteBuffers#bufferConsumedBytes(ByteBuffer)}
     *
     * @param byteBuffer source of bytes
     * @return a byte array of [0, byteBuffer.position()] size
     */
    public static byte[] bufferWrittenBytes(ByteBuffer byteBuffer) {
        return bufferConsumedBytes(byteBuffer);
    }

    /**
     * Pads remaining space of given bytenBuffer with
     * <strong>random</strong> values
     *
     * @param byteBuffer to pad
     * @return padded buffer
     */
    public static ByteBuffer randPadRemaining(ByteBuffer byteBuffer) {
        val byteBufferDupe = byteBuffer.duplicate();

        if (byteBufferDupe.remaining() == 0)
            return byteBufferDupe;

        val randPadTrail = new byte[byteBuffer.remaining()];
        ThreadLocalRandom.current().nextBytes(randPadTrail);

        byteBufferDupe.put(randPadTrail);

        return byteBufferDupe;
    }
}
