package de.tum.p2p;

import de.tum.p2p.voidphone.rps.api.RpsPeerMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.val;

import java.nio.ByteBuffer;

public final class VoidApiMessages {

    private VoidApiMessages() {
        throw new AssertionError("No instance for you");
    }

    public static ByteBuf toByteBuf(RpsPeerMessage message) {
        val buff = ByteBuffer.allocate(message.getSize());
        message.send(buff);
        return Unpooled.wrappedBuffer(buff.array());
    }
}
