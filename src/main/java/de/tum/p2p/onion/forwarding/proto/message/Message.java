package de.tum.p2p.onion.forwarding.proto.message;

import lombok.val;

import static java.lang.Byte.toUnsignedInt;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

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

    /**
     * Converts message to an array of UNSIGNED bytes
     * using {@link Byte#toUnsignedInt(byte)}
     * @return Message as unsigned byte array described by ints
     */
    default int[] unsignedBytes() {
        val signedBytes = bytes();

        if (isEmpty(signedBytes))
            return new int[0];

        val unsignedBytes = new int[signedBytes.length];
        for (int i = 0; i < signedBytes.length; i++) {
            unsignedBytes[i] = toUnsignedInt(signedBytes[i]);
        }

        return unsignedBytes;
    }
}
