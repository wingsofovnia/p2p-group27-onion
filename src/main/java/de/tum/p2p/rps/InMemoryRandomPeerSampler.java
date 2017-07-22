package de.tum.p2p.rps;

import de.tum.p2p.Peer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * An in memory implementation of {@link RandomPeerSampler}
 */
public class InMemoryRandomPeerSampler implements RandomPeerSampler {

    private final List<Peer> peers;

    public InMemoryRandomPeerSampler(List<Peer> peers) {
        this.peers = new ArrayList<>(peers);
    }

    @Override
    public CompletableFuture<Peer> sample() throws PeerSamplingException {
        Collections.shuffle(peers);
        return CompletableFuture.completedFuture(peers.get(0));
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }
}
