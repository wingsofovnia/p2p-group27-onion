package de.tum.p2p.rps.remote.netty;

import de.tum.p2p.Peer;
import de.tum.p2p.rps.VoidRpsServer;
import io.netty.handler.logging.LogLevel;
import lombok.val;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static de.tum.p2p.Peers.randPeer;
import static org.junit.Assert.assertTrue;

public class NettyRemoteRandomPeerSamplerTest {

    private static final InetAddress RPS_SERVER_HOST;
    private static final int RPS_SERVER_PORT = 9989;

    private static final int RANDOM_PEERS_AMOUNT = 20;
    private static final List<Peer> RANDOM_PEERS;

    static {
        try {
            RPS_SERVER_HOST = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        RANDOM_PEERS = new ArrayList<Peer>() {{
           for (int i = 0; i < RANDOM_PEERS_AMOUNT; i++)
               add(randPeer());
        }};
    }

    private static final int RANDOM_PEERS_SAMPLINGS = RANDOM_PEERS_AMOUNT * 2;

    @Test
    public void samplesRandomPeerCorrectly() throws Exception {
        try (val voidRpsServer = new VoidRpsServer(RPS_SERVER_HOST, RPS_SERVER_PORT, RANDOM_PEERS)) {
            val remoteRPSbuilder
                = new NettyRemoteRandomPeerSampler.Builder()
                    .inetAddress(RPS_SERVER_HOST)
                        .port(RPS_SERVER_PORT);

            try (val rps = remoteRPSbuilder.build()) {
                for (int i = 0; i < RANDOM_PEERS_SAMPLINGS; i++) {
                    val randPeer = rps.sample().get();

                    assertTrue(RANDOM_PEERS.contains(randPeer));
                }
            }
        }
    }

    @Test
    public void samplesMultipleRandomPeersCorrectly() throws Exception {
        try (val voidRpsServer = new VoidRpsServer(RPS_SERVER_HOST, RPS_SERVER_PORT, RANDOM_PEERS)) {
            val remoteRPSbuilder
                = new NettyRemoteRandomPeerSampler.Builder()
                .inetAddress(RPS_SERVER_HOST)
                .loggerLevel(LogLevel.INFO)
                .port(RPS_SERVER_PORT);

            try (val rps = remoteRPSbuilder.build()) {
                val futureRandomSampledPeers = rps.sample(RANDOM_PEERS_AMOUNT);
                 val randomSampledPeers = futureRandomSampledPeers.get();

                randomSampledPeers.forEach(randPeer
                    -> assertTrue(RANDOM_PEERS.contains(randPeer)));
            }
        }
    }
}
