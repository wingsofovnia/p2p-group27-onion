package de.tum.p2p;

import de.tum.p2p.proto.message.rps.RpsPeerMessageTest;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public final class PublicKeys {

    private static final String HOSTKEY_ALG = "RSA";
    private static final String HOSTKEY_NAME = "test_public_key.der";

    private PublicKeys() {
        throw new AssertionError("No instance for you");
    }

    public static PublicKey testPublicKey() {
        try {
            val hostkeyResource = RpsPeerMessageTest.class.getClassLoader().getResource(HOSTKEY_NAME);
            val hostkeyPath = Paths.get(hostkeyResource.toURI());
            val hostkeyBytes = Files.readAllBytes(hostkeyPath);

            val keyFactory = KeyFactory.getInstance(HOSTKEY_ALG);
            return keyFactory.generatePublic(new X509EncodedKeySpec(hostkeyBytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyFactory testKeyFactory() {
        try {
            return KeyFactory.getInstance(HOSTKEY_ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
