package de.tum.p2p.onion.auth;

import de.tum.p2p.onion.OnionException;

/**
 * Thrown by {@link OnionAuthorizer} in case of problems during
 * data encryption
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class OnionDecryptionException extends OnionException {

    public OnionDecryptionException(String message) {
        super(message);
    }

    public OnionDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public OnionDecryptionException(Throwable cause) {
        super(cause);
    }
}
