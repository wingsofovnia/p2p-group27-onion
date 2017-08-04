package de.tum.p2p.onion.forwarding;

import de.tum.p2p.onion.OnionException;

/**
 * Thrown to indicate that {@link OnionForwarder} came across
 * error during data forwarding through a data Tunnel
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class OnionDataForwardingException extends OnionException {

    public OnionDataForwardingException(String message) {
        super(message);
    }

    public OnionDataForwardingException(String message, Throwable cause) {
        super(message, cause);
    }

    public OnionDataForwardingException(Throwable cause) {
        super(cause);
    }
}
