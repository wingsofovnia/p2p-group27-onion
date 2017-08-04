package de.tum.p2p.util;

import lombok.val;

import static de.tum.p2p.util.TypeLimits.USHRT_MAX;
import static java.lang.String.format;

/**
 * {@code Handshakes} contains useful utility methods for validating
 * handshake payloads
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public final class Handshakes {

    public static final int MAX_HANDSHAKE_BYTES = USHRT_MAX;

    private Handshakes() {
        throw new AssertionError("No instance for you");
    }

    public static byte[] notOversizedHadshake(byte[] handshake) {
        val handshakeLength = handshake.length;

        if (handshakeLength > MAX_HANDSHAKE_BYTES)
            throw new IllegalArgumentException(format("Too big handshake. Max supported = %d, actual = %d.",
                MAX_HANDSHAKE_BYTES, handshakeLength));

        return handshake;
    }
}
