package de.tum.p2p.rps;

import de.tum.p2p.Peer;
import lombok.val;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * {@code RandomPeerSampler} is geared towards helping
 * find {@link Peer} at random.
 */
public interface RandomPeerSampler {

    /**
     * Gets a random {@link Peer} in the network.
     * <p>
     * RPS should sample random peers from the currently online peers.
     * Therefore the peer sent in this message is very likely to be online,
     * but no strict guarantee could be made about its presence.
     *
     * @return a random {@link Peer}
     * @throws PeerSamplingException in case of errors during RPSing
     */
    Peer sample() throws PeerSamplingException;

    /**
     * Gets a particular amount of <b>distinct</b> random {@link Peer}s
     *
     * @param amount of random {@link Peer}s required
     * @return list of <b>distinct</b> random {@link Peer}s
     * @throws PeerSamplingException in case of errors during RPSing
     */
    default List<Peer> sample(int amount) throws PeerSamplingException {
        val randomPeers = new HashSet<Peer>(amount);

        while (randomPeers.size() != amount)
            randomPeers.add(sample());

        return new ArrayList<>(randomPeers);
    }
}
