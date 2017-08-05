package de.tum.p2p.proto.message;

import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.toPrimitive;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TunnelDestroyMessageTest {

    // Python eq ~ list(bytearray(struct.pack("!Hi", 563, tunnelId)))
    // Warning! this will print UNSIGNED writePayload, therefore:
    // for tunnelId = 999, python will list [2, 51, 0, 0, 3, 231]
    //                 while Java will list [2, 51, 0, 0, 3, -25]
    private static final List<Pair<Message, Byte[]>> GOOD_ENCODED_TUNNEL_DESTROY_MSGS
        = new ArrayList<Pair<Message, Byte[]>>() {{
            add(Pair.of(new TunnelDestroyMessage(111), new Byte[] {2, 51, 0, 0, 0, 111}));
            add(Pair.of(new TunnelDestroyMessage(999), new Byte[] {2, 51, 0, 0, 3, -25}));
            add(Pair.of(new TunnelDestroyMessage(1), new Byte[] {2, 51, 0, 0, 0, 1}));
    }};

    @Test
    public void bytefiesTunnelDestroyMsgsCorrectly() {
        GOOD_ENCODED_TUNNEL_DESTROY_MSGS.forEach(msgBytesPair -> {
            val actualMsgBytes = msgBytesPair.getLeft().bytes();
            val expectedMsgBytes = toPrimitive(msgBytesPair.getRight());

            assertArrayEquals(expectedMsgBytes, actualMsgBytes);
        });
    }

    @Test
    public void debytefiesTunnelDestroyMsgsCorrectly() {
        GOOD_ENCODED_TUNNEL_DESTROY_MSGS.forEach(msgBytesPair -> {
            val rawMsgBytes = msgBytesPair.getRight();
            val actualMsg = TunnelDestroyMessage.fromBytes(toPrimitive(rawMsgBytes));
            val expectedMsg = msgBytesPair.getLeft();

            assertEquals(expectedMsg, actualMsg);
        });
    }

    @Test
    public void returnsCorrectMessageSizeBytes() {
        assertEquals(Integer.BYTES + MessageType.BYTES,
            new TunnelDestroyMessage(1).size());
    }
}
