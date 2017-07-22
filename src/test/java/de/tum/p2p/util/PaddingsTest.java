package de.tum.p2p.util;

import lombok.val;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.util.Paddings.pad;
import static de.tum.p2p.util.Paddings.randPad;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PaddingsTest {

    @Test
    public void padsByteBuffWithGivenLimit() throws Exception {
        val capacity = ThreadLocalRandom.current().nextInt(Character.SIZE, Short.MAX_VALUE);

        val byteBuff = ByteBuffer.allocate(capacity);

        val occupancy = capacity / 2;
        val randBytes = new byte[occupancy];
        ThreadLocalRandom.current().nextBytes(randBytes);
        byteBuff.put(randBytes);

        assertEquals(occupancy, byteBuff.position());

        val paddingLimit = occupancy + occupancy / 2;
        pad(byteBuff, paddingLimit);

        assertEquals(paddingLimit, byteBuff.position());
    }

    @Test
    public void padsByteBuffRandomly() {
        val capacity = ThreadLocalRandom.current().nextInt(Character.BYTES, Character.SIZE);

        val byteBuff = ByteBuffer.allocate(capacity);

        val occupancy = capacity / 2;
        val occupancyIndex = occupancy - 1;

        for (int i = 0; i < occupancy; i++) byteBuff.put((byte) 1); // skip with ones
        assertEquals(1, byteBuff.get(occupancyIndex));

        val paddingLimit = occupancy + occupancy / 2;
        randPad(byteBuff, paddingLimit);

        assertEquals(paddingLimit, byteBuff.position());

        // Boundaries
        assertEquals(1, byteBuff.get(occupancyIndex));
        assertNotEquals(1, byteBuff.get(occupancyIndex + 1));
        assertNotEquals(1, byteBuff.get(paddingLimit));
    }
}
