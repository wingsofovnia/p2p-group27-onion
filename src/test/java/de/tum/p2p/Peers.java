package de.tum.p2p;

import lombok.val;

import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.IPs.randIPv4;
import static de.tum.p2p.IPs.randIPv6;
import static de.tum.p2p.PublicKeys.testPublicKey;

public final class Peers {

    private Peers() {
        throw new AssertionError("No instance for you");
    }

    public static Peer randPeer() {
        val rand = ThreadLocalRandom.current();
        val randomPort = (short) rand.nextInt(1, Short.MAX_VALUE);
        val randomInetAddress = rand.nextBoolean() ? randIPv4() : randIPv6();

        return Peer.of(randomInetAddress, randomPort, testPublicKey());
    }
}
