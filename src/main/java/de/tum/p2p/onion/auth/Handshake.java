package de.tum.p2p.onion.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

/**
 * Encapsulates a handshake's payload used by {@link Handshaker} in
 * Diffie–Hellman key exchange flow
 */
@EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class Handshake {

    private final byte[] payload;

    public Handshake(byte[] payload) {
        if (isEmpty(payload))
            throw new IllegalArgumentException("Handshake payload should not be empty or null");

        this.payload = payload;
    }

    public static Handshake from(byte[] payload) {
        return new Handshake(payload);
    }

    @Override
    public String toString() {
        return Arrays.toString(payload);
    }
}
