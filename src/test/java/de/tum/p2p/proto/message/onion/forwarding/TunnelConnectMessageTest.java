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
public class TunnelConnectMessageTest {

    private static final int TEST_RUNS = 20;

    @Parameterized.Parameters
    public static Collection<TunnelConnectMessage> data() {
        val data = new ArrayList<TunnelConnectMessage>(TEST_RUNS);

        val random = ThreadLocalRandom.current();
        for (int i = 0; i < TEST_RUNS; i++) {
            val randomTunnelId = TunnelId.wrap(random.nextInt());
            val randomReqId = RequestId.wrap(random.nextInt(Short.MAX_VALUE));
            val randomInetAddress = random.nextBoolean() ? randIPv4() : randIPv6();
            val randomPort = random.nextInt(0, Short.MAX_VALUE);

            val randomHandshake = new byte[random.nextInt(Byte.MAX_VALUE, TypedTunnelMessage.PAYLOAD_BYTES / 2)];
            random.nextBytes(randomHandshake);

            data.add(new TunnelConnectMessage(randomTunnelId, randomReqId, randomInetAddress, randomPort, testPublicKey(), randomHandshake));
        }

        return data;
    }

    private TunnelConnectMessage tunnelConnectMessage;

    public TunnelConnectMessageTest(TunnelConnectMessage tunnelConnectMessage) {
        this.tunnelConnectMessage = tunnelConnectMessage;
    }

    @Test
    public void convertsToBytesAndBackCorrectly() {
        val disassembledMsg = tunnelConnectMessage.bytes();
        val parsedTunnelConnectMessage = TunnelConnectMessage.fromBytes(disassembledMsg);

        assertEquals(tunnelConnectMessage.tunnelId(), parsedTunnelConnectMessage.tunnelId());
        assertEquals(tunnelConnectMessage.requestId(), parsedTunnelConnectMessage.requestId());
        assertEquals(tunnelConnectMessage.port(), parsedTunnelConnectMessage.port());
        assertEquals(tunnelConnectMessage.destination(), parsedTunnelConnectMessage.destination());
        assertEquals(tunnelConnectMessage.sourceKey(), parsedTunnelConnectMessage.sourceKey());
        assertArrayEquals(tunnelConnectMessage.handshake(), parsedTunnelConnectMessage.handshake());
    }
}
