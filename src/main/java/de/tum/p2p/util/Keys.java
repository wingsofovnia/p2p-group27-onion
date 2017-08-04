package de.tum.p2p.util;

import lombok.val;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.Function;

import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.toObject;
import static org.apache.commons.lang3.ArrayUtils.toPrimitive;

/**
 * {@code Keys} contains util methods for validating and parsing keys
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public final class Keys {

    /**
     * According to the specification, the hostkey is 4096 bits long (512 bytes)
     */
    public static final int PUBLIC_KEY_MAX_BYTES = 512;

    /**
     * According to the specification, the hostkey is generated with RSA
     */
    public static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final Function<Byte[], KeySpec> toKeySpec = bytes -> new X509EncodedKeySpec(toPrimitive(bytes));

    private Keys() {
        throw new AssertionError("No instance for you");
    }

    public static <T extends Key> T notOversizedKey(T publicKey, Class<T> keyType) {
        val keyLength = publicKey.getEncoded().length;

        if (keyLength > PUBLIC_KEY_MAX_BYTES)
            throw new IllegalArgumentException(format("Too long public key. Max supported = %d, actual = %d.",
                PUBLIC_KEY_MAX_BYTES, keyLength));

        return publicKey;
    }

    public static PublicKey notOversizedKey(PublicKey publicKey) {
        return notOversizedKey(publicKey, PublicKey.class);
    }

    public static PrivateKey notOversizedKey(PrivateKey privateKey) {
        return notOversizedKey(privateKey, PrivateKey.class);
    }

    public static KeyFactory keyFactory() {
        try {
            return KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to build KeyFactory", e);
        }
    }

    public static PublicKey parsePublicKey(Byte[] publicKeyBytes) throws InvalidKeySpecException {
        return keyFactory().generatePublic(toKeySpec.apply(publicKeyBytes));
    }

    public static PublicKey parsePublicKey(byte[] publicKeyBytes) throws InvalidKeySpecException {
        return parsePublicKey(toObject(publicKeyBytes));
    }
}
