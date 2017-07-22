package de.tum.p2p.util;

import io.netty.buffer.ByteBuf;
import lombok.val;

public final class ByteBufs {

    private ByteBufs() {
        throw new AssertionError("No instance for you");
    }

    public static byte[] safeContent(ByteBuf byteBuf) {
        val buffer = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buffer);

        return buffer;
    }
}
