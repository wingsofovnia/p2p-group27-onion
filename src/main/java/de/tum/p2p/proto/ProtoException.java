package de.tum.p2p.proto;

public class ProtoException extends RuntimeException {

    public ProtoException(String message) {
        super(message);
    }

    public ProtoException(String message, Throwable cause) {
        super(message, cause);
    }
}
