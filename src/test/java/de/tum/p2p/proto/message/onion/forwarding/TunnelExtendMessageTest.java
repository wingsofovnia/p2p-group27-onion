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

import static de.tum.p2p.PublicKeys.testPublicKey;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TunnelExtendMessageTest {

    private static final int TEST_RUNS = 10;

    @Parameterized.Parameters
    public static Collection<TunnelExtendMessage> data() throws Exception {
        val data = new ArrayList<TunnelExtendMessage>(TEST_RUNS);

        val randHandshake = new byte[Short.SIZE];
        for (int i = 0; i < TEST_RUNS; i++) {
            ThreadLocalRandom.current().nextBytes(randHandshake);
            data.add(new TunnelExtendMessage(TunnelId.random(), RequestId.next(), testPublicKey(), randHandshake));
        }

        return data;
    }

    private TunnelExtendMessage msg;

    public TunnelExtendMessageTest(TunnelExtendMessage msg) {
        this.msg = msg;
    }

    @Test
    public void convertBackAndForthCorrectly() {
        val msgBytes = msg.bytes();

        val parsedMsg = TunnelExtendMessage.fromBytes(msgBytes);

        assertEquals(msg.tunnelId(), parsedMsg.tunnelId());
        assertEquals(msg.requestId(), parsedMsg.requestId());
        assertEquals(msg.sourceKey(), parsedMsg.sourceKey());
        assertArrayEquals(msg.handshake(), parsedMsg.handshake());
    }
}
