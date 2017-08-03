package de.tum.p2p.util;

import lombok.val;

import java.nio.ByteBuffer;

/**
 * {@code ByteBuffers} contains util methods for extracting
 * raw byte arrays from {@link ByteBuffer}.
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public final class ByteBuffers {

    private ByteBuffers() {
        throw new AssertionError("No instance for you");
    }

    public static byte[] bufferRemainingBytes(ByteBuffer byteBuffer) {
        val byteBufferDupe = byteBuffer.duplicate();

        val bytes = new byte[byteBufferDupe.limit() - byteBufferDupe.position()];
        byteBufferDupe.get(bytes);

        return bytes;
    }

    public static byte[] bufferAllBytes(ByteBuffer byteBuffer) {
        val byteBufferDupe = byteBuffer.duplicate();
        byteBufferDupe.position(0);

        return bufferRemainingBytes(byteBufferDupe);
    }

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
}
