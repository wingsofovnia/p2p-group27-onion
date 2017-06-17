package de.tum.p2p.onion.forwarding;

import de.tum.p2p.onion.OnionException;

/**
 * Thrown to indicate that {@link OnionForwarder} tried to fabricate
 * cover traffic while holding real data Tunnel connection
 */
public class OnionCoverInterferenceException extends OnionException {

    public OnionCoverInterferenceException(String message) {
        super(message);
    }

    public OnionCoverInterferenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public OnionCoverInterferenceException(Throwable cause) {
        super(cause);
    }
}
