package de.tum.p2p.rps;

import de.tum.p2p.Peer;
import lombok.val;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

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

    /**
     * Gets a particular amount of random {@link Peer}s
     *
     * @param amount of random {@link Peer}s required
     * @return future set of random {@link Peer}s
     * @throws PeerSamplingException in case of errors during RPSing
     */
    default CompletableFuture<List<Peer>> sample(int amount) throws PeerSamplingException {
        val featureRandomSampledPeers = new ArrayList<CompletableFuture<Peer>>(amount);

        while (featureRandomSampledPeers.size() != amount)
            featureRandomSampledPeers.add(sample());

        val featureRandomSampledPeersArr = featureRandomSampledPeers.toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(featureRandomSampledPeersArr)
            .thenApply(v -> featureRandomSampledPeers.stream()
                .map(CompletableFuture::join)
                .collect(toList())
            );
    }
}
