package de.tum.p2p.rps;

import de.tum.p2p.Peer;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * {@code RandomPeerSampler} is geared towards helping
 * find {@link Peer} at random.
 */
public interface RandomPeerSampler extends Closeable {

    /**
     * Gets a random {@link Peer} in the network.
     * <p>
     * RPS should sample random peers from the currently online peers.
     * Therefore the peer sent in this message is very likely to be online,
     * but no strict guarantee could be made about its presence.
     *
     * @return a future random {@link Peer}
     * @throws PeerSamplingException in case of errors during RPSing
     */
    CompletableFuture<Peer> sample() throws PeerSamplingException;
}
