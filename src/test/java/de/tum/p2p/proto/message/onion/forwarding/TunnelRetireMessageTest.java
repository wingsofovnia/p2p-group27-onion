package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TunnelRetireMessageTest {

    private static final int TEST_RUNS = 10;

    @Parameterized.Parameters
    public static Collection<TunnelRetireMessage> data() throws Exception {
        val data = new ArrayList<TunnelRetireMessage>(TEST_RUNS);

        for (int i = 0; i < TEST_RUNS; i++)
            data.add(new TunnelRetireMessage(TunnelId.random()));

        return data;
    }

    private TunnelRetireMessage msg;

    public TunnelRetireMessageTest(TunnelRetireMessage msg) {
        this.msg = msg;
    }

    @Test
    public void convertBackAndForthCorrectly() {
        val msgBytes = msg.bytes();

        val parsedMsg = TunnelRetireMessage.fromBytes(msgBytes);

        assertEquals(msg.tunnelId(), parsedMsg.tunnelId());
    }
}
