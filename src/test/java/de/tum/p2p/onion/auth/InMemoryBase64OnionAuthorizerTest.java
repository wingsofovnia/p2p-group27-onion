package de.tum.p2p.onion.auth;

import de.tum.p2p.Peer;
import lombok.val;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class InMemoryBase64OnionAuthorizerTest {

    @Test
    public void twoInMemoryBase64OnionAuthsEstablishSameSessionIdFromHandshakes() {

        val auth1 = new InMemoryBase64OnionAuthorizer();
        val auth1sessionFactory = auth1.sessionFactory();

        val auth2 = new InMemoryBase64OnionAuthorizer();
        val auth2sessionFactory = auth2.sessionFactory();

        val auth1SessionIdAndHs1 = auth1sessionFactory.start(mock(Peer.class)).join();
        val auth1Auth2Hs1 = auth1SessionIdAndHs1.getValue();

        val auth2SessionIdAndHs2 = auth2sessionFactory.responseTo(auth1Auth2Hs1).join();
        val auth2SessionId = auth2SessionIdAndHs2.getKey();
        val auth2Auth1Hs2 = auth2SessionIdAndHs2.getValue();

        val auth1SessionId = auth1sessionFactory.confirm(auth2Auth1Hs2).join();

        assertEquals(auth1SessionId, auth2SessionId);
    }

    @Test
    public void encryptsAndDecryptsOneLayeredPlaintextCorrectly() {
        val auth1 = new InMemoryBase64OnionAuthorizer();
        val auth2 = new InMemoryBase64OnionAuthorizer();

        val auth1Auth2Hs1 = auth1.sessionFactory().start(mock(Peer.class)).join().getValue();
        val sessionId = auth2.sessionFactory().responseTo(auth1Auth2Hs1).join().getKey();

        val plaintext = randPayload();

        val ciphertext = auth1.encrypt(plaintext, sessionId).join().bytesBuffer();
        val deciphertext = auth2.decrypt(ciphertext, sessionId).join();

        assertEquals(plaintext, deciphertext.bytesBuffer());
        assertTrue(deciphertext.isPlaintext());
    }

    private static ByteBuffer randPayload() {
        val payload = new byte[Byte.SIZE];
        ThreadLocalRandom.current().nextBytes(payload);

        return ByteBuffer.wrap(payload);
    }
}
