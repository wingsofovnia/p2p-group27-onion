package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TunnelDatumMessageTest {

    private static final byte[] HMAC_SECRET = getBytesUtf8(UUID.randomUUID().toString());

    private static final int TEST_RUNS = 20;


    @Parameterized.Parameters
    public static Collection<TunnelDatumMessage> data() {
        val data = new ArrayList<TunnelDatumMessage>(TEST_RUNS);

        val random = ThreadLocalRandom.current();
        for (int i = 0; i < TEST_RUNS; i++) {
            val randomTunnelId = TunnelId.wrap(random.nextInt());

            val randomPayload = new byte[random.nextInt(1, TunnelDatumMessage.MAX_PAYLOAD_BYTES)];
            random.nextBytes(randomPayload);

            data.add(TunnelDatumMessage.of(randomTunnelId, randomPayload, HMAC_SECRET));
        }

        return data;
    }

    private TunnelDatumMessage testDatumOnionMessage;

    public TunnelDatumMessageTest(TunnelDatumMessage testDatumOnionMessage) {
        this.testDatumOnionMessage = testDatumOnionMessage;
    }

    @Test
    public void convertsToBytesAndBackCorrectly() {
        val disassembledMsg = testDatumOnionMessage.bytes();
        val parsedDisassembledMsg = TunnelDatumMessage.fromBytes(disassembledMsg, HMAC_SECRET);

        assertEquals(testDatumOnionMessage.tunnelId(), parsedDisassembledMsg.tunnelId());
        assertArrayEquals(testDatumOnionMessage.payload(), parsedDisassembledMsg.payload());
    }
}
