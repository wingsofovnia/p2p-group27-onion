package de.tum.p2p.onion.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.ByteBuffer;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Encapsulates the result of encryption performed on plaintext
 * using {@link OnionAuthorizer#encrypt(ByteBuffer, SessionId, SessionId...)}
 */
@ToString @EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class Ciphertext {

    private final byte[] bytes;

    private final int layers;

    public Ciphertext(byte[] bytes, int layers) {
        this.bytes = notNull(bytes);
        this.layers = layers;
    }

    public Ciphertext(byte[] bytes) {
        this(bytes, 1);
    }

    public static Ciphertext wrap(byte[] bytes) {
        return new Ciphertext(bytes);
    }

    public static Ciphertext wrapLayered(byte[] bytes, int layers) {
        return new Ciphertext(bytes, layers);
    }

    public ByteBuffer bytesBuffer() {
        return ByteBuffer.wrap(bytes);
    }
}
