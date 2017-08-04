package de.tum.p2p;

/**
 * {@code P2PException} is the superclass of those exceptions
 * that can be thrown during the normal operation of the whole
 * P2P application
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class P2PException extends RuntimeException {

    public P2PException(String message) {
        super(message);
    }

    public P2PException(String message, Throwable cause) {
        super(message, cause);
    }

    public P2PException(Throwable cause) {
        super(cause);
    }
}
