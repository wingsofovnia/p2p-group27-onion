package de.tum.p2p.rps;

import de.tum.p2p.P2PException;

/**
 * Thrown by {@link RandomPeerSampler} in case of errors during RPSing
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public class PeerSamplingException extends P2PException {
    public PeerSamplingException(String message) {
        super(message);
    }

    public PeerSamplingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PeerSamplingException(Throwable cause) {
        super(cause);
    }
}
