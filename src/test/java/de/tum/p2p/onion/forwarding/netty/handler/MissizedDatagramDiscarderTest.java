package de.tum.p2p.onion.forwarding.netty.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class MissizedDatagramDiscarderTest {

    private static final int TEST_RUNS = 20;

    @Parameterized.Parameters
    public static Collection<DatagramPacket> data() {
        val data = new ArrayList<DatagramPacket>(TEST_RUNS);

        val random = ThreadLocalRandom.current();
        for (int i = 0; i < TEST_RUNS; i++) {
            val randomPayload = new byte[random.nextInt(2, 64)];
            random.nextBytes(randomPayload);

            val datagramMock = new DatagramPacket(Unpooled.wrappedBuffer(randomPayload), mock(InetSocketAddress.class));

            data.add(datagramMock);
        }

        return data;
    }

    private DatagramPacket testDatagramPacket;

    public MissizedDatagramDiscarderTest(DatagramPacket testDatagramPacket) {
        this.testDatagramPacket = testDatagramPacket;
    }

    @Test
    public void discardsMissizedDatagram() throws Exception {
        val actualSize = testDatagramPacket.content().readableBytes();
        val spoiledSize = actualSize - ThreadLocalRandom.current().nextInt(1, actualSize);

        val discarder = new AccessableMissizedDatagramDiscarder(spoiledSize);
        val discarderOut = new ArrayList<Object>();

        discarder.decode(mock(ChannelHandlerContext.class), testDatagramPacket, discarderOut);

        assertTrue(discarderOut.isEmpty());
    }

    @Test
    public void dontDiscardCorrectlySizedDatagram() throws Exception {
        val packetSize = testDatagramPacket.content().readableBytes();

        val discarder = new AccessableMissizedDatagramDiscarder(packetSize);
        val discarderOut = new ArrayList<Object>();

        discarder.decode(mock(ChannelHandlerContext.class), testDatagramPacket, discarderOut);

        assertFalse(discarderOut.isEmpty());
        assertEquals(testDatagramPacket, discarderOut.get(0));
    }

    private static final class AccessableMissizedDatagramDiscarder extends MissizedDatagramDiscarder {

        public AccessableMissizedDatagramDiscarder(int expectedSize) {
            super(expectedSize);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
            super.decode(ctx, msg, out);
        }
    }
}
