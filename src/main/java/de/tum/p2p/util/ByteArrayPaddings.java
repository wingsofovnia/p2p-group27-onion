package de.tum.p2p.util;

import lombok.val;

import java.util.Arrays;

/**
 * {@code ByteArrayPaddings} contains useful utility methods for padding
 * and unpadding byte arrays
 */
public final class ByteArrayPaddings {

    private ByteArrayPaddings() {
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
}
