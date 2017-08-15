package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.RequestId;
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

    private static final int TEST_RUNS = 10;

    @Parameterized.Parameters
    public static Collection<TunnelExtendedMessage> data() throws Exception {
        val data = new ArrayList<TunnelExtendedMessage>(TEST_RUNS);

        val randHandshake = new byte[Short.SIZE];
        for (int i = 0; i < TEST_RUNS; i++) {
            ThreadLocalRandom.current().nextBytes(randHandshake);
            data.add(new TunnelExtendedMessage(TunnelId.random(), RequestId.next(), randHandshake));
        }

        return data;
    }

    private TunnelExtendedMessage msg;

    public TunnelExtendedMessageTest(TunnelExtendedMessage msg) {
        this.msg = msg;
    }

    @Test
    public void convertBackAndForthCorrectly() {
        val msgBytes = msg.bytes();

        val parsedMsg = TunnelExtendedMessage.fromBytes(msgBytes);

        assertEquals(msg.tunnelId(), parsedMsg.tunnelId());
        assertEquals(msg.requestId(), parsedMsg.requestId());
        assertArrayEquals(msg.handshake(), parsedMsg.handshake());
    }
}
