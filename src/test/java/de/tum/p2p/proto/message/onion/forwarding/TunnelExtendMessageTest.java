package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TunnelExtendMessageTest {

    private static final int TEST_RUNS = 20;

    @Parameterized.Parameters
    public static Collection<TunnelExtendMessage> data() {
        val data = new ArrayList<TunnelExtendMessage>(TEST_RUNS);

        val random = ThreadLocalRandom.current();
        for (int i = 0; i < TEST_RUNS; i++) {
            val randomTunnelId = TunnelId.wrap(random.nextInt());
            val randomInetAddress = random.nextBoolean() ? randIPv4() : randIPv6();
            val randomPort = random.nextInt(0, Short.MAX_VALUE);

            val randomHandshake = new byte[random.nextInt(Byte.MAX_VALUE, OnionMessage.BYTES / 2)];
            random.nextBytes(randomHandshake);

            data.add(TunnelExtendMessage.of(randomTunnelId, randomInetAddress, randomPort, testPublicKey(), randomHandshake));
        }

        return data;
    }

    private TunnelExtendMessage testTunnelExtendMessage;

    public TunnelExtendMessageTest(TunnelExtendMessage testTunnelExtendMessage) {
        this.testTunnelExtendMessage = testTunnelExtendMessage;
    }

    @Test
    public void convertsToBytesAndBackCorrectly() {
        val disassembledMsg = testTunnelExtendMessage.bytes();
        val parsedTunnelExtendMessage = TunnelExtendMessage.fromBytes(disassembledMsg);

        assertEquals(testTunnelExtendMessage.tunnelId(), parsedTunnelExtendMessage.tunnelId());
        assertEquals(testTunnelExtendMessage.requestId(), parsedTunnelExtendMessage.requestId());
        assertEquals(testTunnelExtendMessage.port(), parsedTunnelExtendMessage.port());
        assertEquals(testTunnelExtendMessage.destination(), parsedTunnelExtendMessage.destination());
        assertEquals(testTunnelExtendMessage.sourceKey(), parsedTunnelExtendMessage.sourceKey());
        assertArrayEquals(testTunnelExtendMessage.handshake(), parsedTunnelExtendMessage.handshake());
    }
}
