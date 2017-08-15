package de.tum.p2p.proto.message.onion.forwarding.composite;

import de.tum.p2p.IPs;
import de.tum.p2p.proto.RequestId;
import de.tum.p2p.util.Nets;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.PublicKeys.testPublicKey;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TunnelConnectTest {

    private static final int TEST_RUNS = 10;

    @Parameterized.Parameters
    public static Collection<TunnelConnect> data() throws Exception {
        val data = new ArrayList<TunnelConnect>(TEST_RUNS);

        val randHandshake = new byte[Short.SIZE];
        for (int i = 0; i < TEST_RUNS; i++) {
            ThreadLocalRandom.current().nextBytes(randHandshake);
            data.add(new TunnelConnect(RequestId.next(), IPs.randIP(), Nets.randUnprivilegedPort(), testPublicKey(), randHandshake));
        }

        return data;
    }

    private TunnelConnect payload;

    public TunnelConnectTest(TunnelConnect payload) {
        this.payload = payload;
    }

    @Test
    public void convertBackAndForthCorrectly() {
        val payloadBytes = payload.bytes();

        val parsedPayload = TunnelConnect.fromBytes(payloadBytes);

        assertEquals(payload.requestId(), parsedPayload.requestId());
        assertEquals(payload.destination(), parsedPayload.destination());
        assertEquals(payload.port(), parsedPayload.port());
        assertEquals(payload.sourceKey(), parsedPayload.sourceKey());
        assertArrayEquals(payload.handshake(), parsedPayload.handshake());
    }
}
