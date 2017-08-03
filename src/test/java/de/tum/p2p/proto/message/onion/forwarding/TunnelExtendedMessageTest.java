package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TunnelExtendedMessageTest {

    private static final int TEST_RUNS = 20;

    @Parameterized.Parameters
    public static Collection<TunnelExtendedMessage> data() {
        val data = new ArrayList<TunnelExtendedMessage>(TEST_RUNS);

        val random = ThreadLocalRandom.current();
        for (int i = 0; i < TEST_RUNS; i++) {
            val randomTunnelId = TunnelId.wrap(random.nextInt());
            val randomRequestId = RequestId.wrap(random.nextInt(Short.MAX_VALUE));

            val randomHandshake = new byte[random.nextInt(Byte.MAX_VALUE, TypedTunnelMessage.PAYLOAD_BYTES / 2)];
            random.nextBytes(randomHandshake);

            data.add(new TunnelExtendedMessage(randomTunnelId, randomRequestId, randomHandshake));
        }

        return data;
    }

    private TunnelExtendedMessage testTunnelExtendedMessage;

    public TunnelExtendedMessageTest(TunnelExtendedMessage testTunnelExtendedMessage) {
        this.testTunnelExtendedMessage = testTunnelExtendedMessage;
    }

    @Test
    public void convertsToBytesAndBackCorrectly() {
        val disassembledMsg = testTunnelExtendedMessage.bytes();
        val parsedTunnelExtendedMessage = TunnelExtendedMessage.fromBytes(disassembledMsg);

        assertEquals(testTunnelExtendedMessage.tunnelId(), parsedTunnelExtendedMessage.tunnelId());
        assertEquals(testTunnelExtendedMessage.requestId(), parsedTunnelExtendedMessage.requestId());
        assertArrayEquals(testTunnelExtendedMessage.handshake(), parsedTunnelExtendedMessage.handshake());
    }
}
