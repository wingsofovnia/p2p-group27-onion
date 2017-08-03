package de.tum.p2p.proto;

/**
 * {@code ProtoException} is thrown by {@link de.tum.p2p.proto.message.Message}
 * derived classes in case of protocol related errors like serializing or deserializing.
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public class ProtoException extends RuntimeException {

    public ProtoException(String message) {
        super(message);
    }

    public ProtoException(String message, Throwable cause) {
        super(message, cause);
    }
}
