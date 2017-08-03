package de.tum.p2p.util;

import lombok.val;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.util.ByteBuffers.bufferAllBytes;

/**
 * {@code Paddings} contains useful utility methods for padding
 * and unpadding byte arrays
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public final class Paddings {

    private Paddings() {
        throw new AssertionError("No instance for you");
    }

    /**
     * Removes trailing zeros from byte array
     *
     * @param bytes zero padded byte array
     * @return trimmed byte array
     */
    public static byte[] trim(byte[] bytes) {
        int paddingIndex = bytes.length - 1;
        while (paddingIndex >= 0 && bytes[paddingIndex] == 0) {
            --paddingIndex;
        }

        return Arrays.copyOf(bytes, paddingIndex + 1);
    }

    /**
     * Pads given array with zeros
     *
     * @param bytes  array to pad
     * @param length of new padded array
     * @return padded array
     */
    public static byte[] pad(byte[] bytes, int length) {
        if (length < bytes.length)
            throw new IllegalArgumentException("Given length (" + length + ") is < array itself (" + bytes.length + ")");

        if (bytes.length == length)
            return bytes;

        val paddedBytes = new byte[length];
        System.arraycopy(bytes, 0, paddedBytes, 0, bytes.length);

        return paddedBytes;
    }

    /**
     * Pads given array with <strong>random</strong> values
     *
     * @param bytes  array to pad
     * @param length of new padded array
     * @return padded array
     */
    public static byte[] randPad(byte[] bytes, int length) {
        if (bytes.length == length)
            return bytes;

        val randPadTrail = new byte[length - bytes.length];
        ThreadLocalRandom.current().nextBytes(randPadTrail);

        return ArrayUtils.addAll(bytes, randPadTrail);
    }

    /**
     * Pads given bytenBuffer with zeros
     *
     * @param byteBuffer to pad
     * @param length     required final length (= buffer.position)
     * @return padded buffer
     */
    public static ByteBuffer pad(ByteBuffer byteBuffer, int length) {
        val byteBufferDupe = byteBuffer.duplicate();

        if (length > byteBufferDupe.limit())
            throw new IllegalArgumentException("Given length is bigger then byteBuffer limit");
        else if (byteBufferDupe.position() == length)
            return byteBufferDupe;

        byteBufferDupe.position(length);

        return byteBufferDupe;
    }

    public static byte[] padToArray(ByteBuffer byteBuffer, int length) {
        return bufferAllBytes(pad(byteBuffer, length));
    }

    public static ByteBuffer pad(ByteBuffer byteBuffer) {
        return pad(byteBuffer, byteBuffer.limit());
    }

    public static byte[] padToArray(ByteBuffer byteBuffer) {
        return padToArray(byteBuffer, byteBuffer.limit());
    }

    /**
     * Pads given bytenBuffer with <strong>random</strong> values
     *
     * @param byteBuffer to pad
     * @param length     required final length (= buffer.position)
     * @return padded buffer
     */
    public static ByteBuffer randPad(ByteBuffer byteBuffer, int length) {
        val byteBufferDupe = byteBuffer.duplicate();

        if (length > byteBufferDupe.limit())
            throw new IllegalArgumentException("Given length is bigger then byteBuffer limit");
        else if (byteBufferDupe.position() == length)
            return byteBufferDupe;

        val randPadTrail = new byte[length - byteBufferDupe.position()];
        ThreadLocalRandom.current().nextBytes(randPadTrail);

        byteBufferDupe.put(randPadTrail);

        return byteBufferDupe;
    }

    public static byte[] randPadToArray(ByteBuffer byteBuffer, int length) {
        return bufferAllBytes(randPad(byteBuffer, length));
    }

    public static ByteBuffer randPad(ByteBuffer byteBuffer) {
        return randPad(byteBuffer, byteBuffer.limit());
    }

    public static byte[] randPadToArray(ByteBuffer byteBuffer) {
        return randPadToArray(byteBuffer, byteBuffer.limit());
    }
}
