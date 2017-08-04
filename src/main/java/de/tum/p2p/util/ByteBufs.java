package de.tum.p2p.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * {@code ByteBufs} contains util methods for Netty's {@link ByteBuf}ers
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public final class ByteBufs {

    private ByteBufs() {
        throw new AssertionError("No instance for you");
    }

    public static byte[] safeContent(ByteBuf byteBuf) {
        return Unpooled.copiedBuffer(byteBuf).array();
    }
}
