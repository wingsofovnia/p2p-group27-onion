package de.tum.p2p.onion.forwarding;

import de.tum.p2p.onion.OnionException;

/**
 * Thrown to indicate that {@link OnionForwarder} came across
 * error during initialization of {@link OnionForwarder} itself
 */
public class OnionInitializationException extends OnionException {

    public OnionInitializationException(String message) {
        super(message);
    }

    public OnionInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public OnionInitializationException(Throwable cause) {
        super(cause);
    }
}
