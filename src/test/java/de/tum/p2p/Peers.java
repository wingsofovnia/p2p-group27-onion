package de.tum.p2p;

import lombok.val;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static de.tum.p2p.IPs.randIPv4;
import static de.tum.p2p.IPs.randIPv6;
import static de.tum.p2p.PublicKeys.testPublicKey;
import static de.tum.p2p.util.Nets.localhost;
import static de.tum.p2p.util.Nets.randUnprivilegedPort;
import static java.util.stream.Collectors.toList;

public final class Peers {

    private Peers() {
        throw new AssertionError("No instance for you");
    }

    public static Peer randPeer() {
        val randIp = ThreadLocalRandom.current().nextBoolean() ? randIPv4() : randIPv6();

        return Peer.of(randIp, randUnprivilegedPort(), testPublicKey());
    }

    public static Peer randLocalPeer() {
        return Peer.of(localhost(), randUnprivilegedPort(), testPublicKey());
    }

    public static List<Peer> randLocalPeers(int amount) {
        return randUnprivilegedPort(amount)
            .mapToObj(randPort -> Peer.of(localhost(), randPort, testPublicKey()))
            .collect(toList());
    }
}
