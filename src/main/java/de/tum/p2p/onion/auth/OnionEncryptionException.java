package de.tum.p2p.onion.auth;

import de.tum.p2p.onion.OnionException;

/**
 * Thrown by {@link OnionAuthorizer} in case of problems during
 * data encryption
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public class OnionEncryptionException extends OnionException {

    public OnionEncryptionException(String message) {
        super(message);
    }

    public OnionEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public OnionEncryptionException(Throwable cause) {
        super(cause);
    }
}
