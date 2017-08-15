package de.tum.p2p.proto.message.onion.forwarding.composite;

import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public class TunnelDatumTest {

    private static final int TEST_RUNS = 10;

    @Parameterized.Parameters
    public static Collection<TunnelDatum> data() throws Exception {
        val data = new ArrayList<TunnelDatum>(TEST_RUNS);

        val randPayload = new byte[TunnelDatum.PAYLOAD_BYTES];
        for (int i = 0; i < TEST_RUNS; i++) {
            if (ThreadLocalRandom.current().nextBoolean())
                data.add(new TunnelDatum(Double.SIZE));
            else {
                ThreadLocalRandom.current().nextBytes(randPayload);
                data.add(new TunnelDatum(randPayload));
            }
        }

        return data;
    }

    private TunnelDatum payload;

    public TunnelDatumTest(TunnelDatum payload) {
        this.payload = payload;
    }

    @Test
    public void convertBackAndForthCorrectly() {
        val payloadBytes = payload.bytes();

        val parsedPayload = TunnelDatum.fromBytes(payloadBytes);

        assertArrayEquals(payload.payload(), parsedPayload.payload());
    }
}
