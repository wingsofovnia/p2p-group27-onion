package de.tum.p2p.onion.forwarding;

import de.tum.p2p.onion.OnionException;

/**
 * Thrown to indicate that {@link OnionForwarder} came across
 * error during building a data Tunnel
 */
public class OnionTunnelingException extends OnionException {

    public OnionTunnelingException(String message) {
        super(message);
    }

    public OnionTunnelingException(String message, Throwable cause) {
        super(message, cause);
    }

    public OnionTunnelingException(Throwable cause) {
        super(cause);
    }
}
