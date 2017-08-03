package de.tum.p2p.rps;

import de.tum.p2p.Peer;
import lombok.experimental.var;
import lombok.val;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * {@code RandomPeerSampler} is geared towards helping find {@link Peer} at random.
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
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

    default CompletableFuture<Peer> sampleNot(Peer exclusion) throws PeerSamplingException {
        return sampleNot(Collections.singletonList(exclusion));
    }

    default CompletableFuture<Peer> sampleNot(List<Peer> exclusions) throws PeerSamplingException {
        return supplyAsync(() -> {
            try {
                var sample = sample().get();

                while (exclusions.contains(sample))
                    sample = sample().get();

                return sample;
            } catch (InterruptedException | ExecutionException e) {
                throw new PeerSamplingException("Failed to sample a peer", e);
            }
        });
    }

    /**
     * Gets a particular amount of random {@link Peer}s
     *
     * @param amount of random {@link Peer}s required
     * @return future list of random {@link Peer}s
     * @throws PeerSamplingException in case of errors during RPSing
     */
    default List<CompletableFuture<Peer>> sample(int amount) throws PeerSamplingException {
        val featureRandomSampledPeers = new ArrayList<CompletableFuture<Peer>>(amount);

        while (featureRandomSampledPeers.size() != amount)
            featureRandomSampledPeers.add(sample());

        return featureRandomSampledPeers;
    }

    /**
     * Gets a particular amount of random {@link Peer}s that are different from given exclusions.
     *
     * @param amount     of random {@link Peer}s required
     * @param exclusions Peers to be excluded from random sampling
     * @return list of random {@link Peer}s
     * @throws PeerSamplingException in case of errors during RPSing
     */
    default CompletableFuture<List<Peer>> sampleExclusive(int amount, List<Peer> exclusions) throws PeerSamplingException {
        return supplyAsync(() -> {
            val randomSampledPeers = new ArrayList<Peer>(amount);

            try {
                while (randomSampledPeers.size() != amount) {
                    val sample = sample().get();

                    if (!exclusions.contains(sample))
                        randomSampledPeers.add(sample);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new PeerSamplingException("Failed to sample a peer", e);
            }

            return randomSampledPeers;
        });
    }

    /**
     * Gets a particular amount of <strong>distinct</strong> random {@link Peer}s
     *
     * @param amount of random {@link Peer}s required
     * @return list of <strong>distinct</strong> random {@link Peer}s
     * @throws PeerSamplingException in case of errors during RPSing
     */
    default CompletableFuture<Set<Peer>> sampleDistinct(int amount) throws PeerSamplingException {
        return sampleDistinctExclusive(amount, Collections.emptyList());
    }

    /**
     * Gets a particular amount of <strong>distinct</strong> random {@link Peer}s that
     * are different from given excludes.
     *
     * @param amount   of random {@link Peer}s required
     * @param excludes Peers to be excluded from random sampling
     * @return list of <strong>distinct</strong> random {@link Peer}s
     * @throws PeerSamplingException in case of errors during RPSing
     */
    default CompletableFuture<Set<Peer>> sampleDistinctExclusive(int amount, List<Peer> excludes)
            throws PeerSamplingException {

        return supplyAsync(() -> {
            val randomSampledPeers = new HashSet<Peer>(amount);

            try {
                while (randomSampledPeers.size() != amount) {
                    val sample = sample().get();

                    if (!excludes.contains(sample))
                        randomSampledPeers.add(sample);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new PeerSamplingException("Failed to sample a peer", e);
            }

            return randomSampledPeers;
        });
    }
}
