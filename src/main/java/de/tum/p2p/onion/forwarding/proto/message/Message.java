package de.tum.p2p.onion.forwarding.proto.message;

/**
 * A representation of a basic unit of data that travels
 * via network between application APIs
 */
public interface Message {

    /**
     * Returns a length of byte array that represents the
     * content of the Message
     * @return size of the Message in BYTES
     */
    int size();

    /**
     * Converts message to an array of bytes
     * @return Message as byte array
     */
    byte[] bytes();
}
