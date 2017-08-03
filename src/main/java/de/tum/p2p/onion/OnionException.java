package de.tum.p2p.onion;

import de.tum.p2p.P2PException;

/**
 * {@code OnionException} is the superclass of those exceptions
 * that can be thrown during the normal operation of the
 * {@link de.tum.p2p.onion.forwarding.OnionForwarder} or
 * {@link de.tum.p2p.onion.auth.OnionAuthorizer} modules
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public class OnionException extends P2PException {

    public OnionException(String message) {
        super(message);
    }

    public OnionException(String message, Throwable cause) {
        super(message, cause);
    }

    public OnionException(Throwable cause) {
        super(cause);
    }
}
