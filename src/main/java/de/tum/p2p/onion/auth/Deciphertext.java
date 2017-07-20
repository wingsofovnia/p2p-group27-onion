package de.tum.p2p.onion.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.ByteBuffer;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Encapsulates the result of decryption performed
 * by {@link OnionAuthorizer}
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class Deciphertext {

    private final byte[] bytes;

    private final boolean isPlaintext;

    public Deciphertext(byte[] bytes, boolean isPlaintext) {
        this.bytes = notNull(bytes);
        this.isPlaintext = isPlaintext;
    }

    public Deciphertext(byte[] bytes) {
        this(bytes, false);
    }

    public static Deciphertext of(byte[] payload) {
        return new Deciphertext(payload);
    }

    public static Deciphertext ofPlaintext(byte[] plaintext) {
        return new Deciphertext(plaintext, true);
    }

    public ByteBuffer bytesBuffer() {
        return ByteBuffer.wrap(bytes);
    }
}
