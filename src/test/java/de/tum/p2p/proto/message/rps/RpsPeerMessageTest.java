package de.tum.p2p.proto.message.rps;

import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.IPs.randIPv4;
import static de.tum.p2p.IPs.randIPv6;
import static de.tum.p2p.PublicKeys.testPublicKey;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RpsPeerMessageTest {

    private static final int TEST_RUNS = 20;

    @Parameterized.Parameters
    public static Collection<RpsPeerMessage> data() throws Exception {
        val data = new ArrayList<RpsPeerMessage>(TEST_RUNS);

        val testHostkey = testPublicKey();
        val random = ThreadLocalRandom.current();
        for (int i = 0; i < TEST_RUNS; i++) {
            val randomPort = (short) random.nextInt(0, Short.MAX_VALUE + 1);
            val randomInetAddress = random.nextBoolean() ? randIPv4() : randIPv6();

            data.add(RpsPeerMessage.of(randomPort, randomInetAddress, testHostkey));
        }

        return data;
    }

    private RpsPeerMessage testRpsPeerMessage;

    public RpsPeerMessageTest(RpsPeerMessage testRpsPeerMessage) {
        this.testRpsPeerMessage = testRpsPeerMessage;
    }

    @Test
    public void convertsToBytesAndBackCorrectly() {
        val disassembledMsg = testRpsPeerMessage.bytes();
        val parsedDisassembledMsg = RpsPeerMessage.fromBytes(disassembledMsg);

        assertEquals(testRpsPeerMessage.port(), parsedDisassembledMsg.port());
        assertEquals(testRpsPeerMessage.inetAddress(), parsedDisassembledMsg.inetAddress());
        assertEquals(testRpsPeerMessage.hostkey(), parsedDisassembledMsg.hostkey());
    }
}
