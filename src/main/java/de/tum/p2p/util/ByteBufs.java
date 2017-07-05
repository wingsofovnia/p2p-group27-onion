package de.tum.p2p.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class ByteBufs {

    public static byte[] safeContent(ByteBuf byteBuf) {
        return Unpooled.copiedBuffer(byteBuf).array();
    }
}
