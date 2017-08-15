package de.tum.p2p.proto.message.onion.forwarding.composite;

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
public class TunnelRelayMessageTest {

    private static final int TEST_RUNS = 10;

    @Parameterized.Parameters
    public static Collection<TunnelRelayMessage> data() throws Exception {
        val data = new ArrayList<TunnelRelayMessage>(TEST_RUNS);

        val randPayload = new byte[TunnelRelayMessage.PAYLOAD_BYTES];
        for (int i = 0; i < TEST_RUNS; i++) {
            ThreadLocalRandom.current().nextBytes(randPayload);
            data.add(new TunnelRelayMessage(TunnelId.random(), randPayload));
        }

        return data;
    }

    private TunnelRelayMessage msg;

    public TunnelRelayMessageTest(TunnelRelayMessage msg) {
        this.msg = msg;
    }

    @Test
    public void convertBackAndForthCorrectly() {
        val msgBytes = msg.bytes();

        val parsedMsg = TunnelRelayMessage.fromBytes(msgBytes);

        assertEquals(msg.tunnelId(), parsedMsg.tunnelId());
        assertArrayEquals(msg.payload(), parsedMsg.payload());
    }
}
