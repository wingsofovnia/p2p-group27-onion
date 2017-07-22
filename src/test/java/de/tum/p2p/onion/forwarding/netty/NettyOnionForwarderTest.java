package de.tum.p2p.onion.forwarding.netty;

import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.auth.SessionFactory;
import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.rps.InMemoryRandomPeerSampler;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.tum.p2p.Peers.randLocalPeers;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class NettyOnionForwarderTest {

    @Test
    public void buildsTunnelsCorrectly() throws ExecutionException, InterruptedException, TimeoutException {
        val randDistinctPeers = randLocalPeers(3);
        val hmac = rand5Bytes();

        val rps = new InMemoryRandomPeerSampler(randDistinctPeers);

        val peer1 = randDistinctPeers.get(0);
        val peer1router = new TunnelRouter();

        val peer2 = randDistinctPeers.get(1);

        val peer3 = randDistinctPeers.get(2);
        val peer3router = new TunnelRouter();

        val p1p3sessionId = randSessionId();
        val p1p3hs1 = rand5Bytes();
        val p1p3hs2 = rand5Bytes();
        val p1p3aliseOnionAuth = aliseOnionAuthMock(p1p3sessionId, p1p3hs1);
        val p1p3bobOnionAuth = bobOnionAuthMock(p1p3sessionId, p1p3hs2);

        val peer1onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer1.port())
            .onionAuthorizer(p1p3aliseOnionAuth)
            .randomPeerSampler(rps)
            .hmacKey(hmac)
            .publicKey(peer1.publicKey())
            .tunnelRouter(peer1router)
            //.loggerLevel(LogLevel.DEBUG)
            .intermediateHops(1)
            .listen();

        // peer2onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer2.port())
            .onionAuthorizer(randOnionAuthMock())
            .randomPeerSampler(rps)
            .hmacKey(hmac)
            .publicKey(peer2.publicKey())
            //.loggerLevel(LogLevel.DEBUG)
            .intermediateHops(1)
            .listen();

        // peer3onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer3.port())
            .onionAuthorizer(p1p3bobOnionAuth)
            .randomPeerSampler(rps)
            .hmacKey(hmac)
            .publicKey(peer3.publicKey())
            .tunnelRouter(peer3router)
            //.loggerLevel(LogLevel.DEBUG)
            .intermediateHops(1)
            .listen();


        val futureP1p3TunnelId = peer1onion.createTunnel(peer3);
        try {
            val p1p3TunnelId = futureP1p3TunnelId.get(5, TimeUnit.MILLISECONDS);

            verifyAliseOnionMock(p1p3aliseOnionAuth);
            verifyBobOnionMock(p1p3bobOnionAuth);

            assertEquals(peer2.socketAddress(), peer1router.getNext(p1p3TunnelId).remoteAddress());
            assertFalse(peer1router.resolvePrev(p1p3TunnelId).isPresent());
        } catch (TimeoutException e) {
            futureP1p3TunnelId.cancel(true);
        }


    }

    private static OnionAuthorizer aliseOnionAuthMock(SessionId id, byte[] hs1) {
        val onionAuth = mock(OnionAuthorizer.class);
        val sessionFactory = mock(SessionFactory.class);
        when(onionAuth.sessionFactory()).thenReturn(sessionFactory);

        when(sessionFactory.start(any())).thenReturn(completedFuture(Pair.of(id, ByteBuffer.wrap(hs1))));
        when(sessionFactory.confirm(any(byte[].class))).thenReturn(completedFuture(id));

        return onionAuth;
    }

    private static void verifyAliseOnionMock(OnionAuthorizer onionAuthorizer) {
        verify(onionAuthorizer, atLeastOnce()).sessionFactory();

        val sessionFactory = onionAuthorizer.sessionFactory();
        verify(sessionFactory, atLeastOnce()).start(any());
        verify(sessionFactory, atLeastOnce()).confirm(any(byte[].class));
    }

    private static OnionAuthorizer bobOnionAuthMock(SessionId id, byte[] hs2) {
        val onionAuth = mock(OnionAuthorizer.class);
        val sessionFactory = mock(SessionFactory.class);
        when(onionAuth.sessionFactory()).thenReturn(sessionFactory);

        when(sessionFactory.responseTo(any(byte[].class))).thenReturn(completedFuture(Pair.of(id, ByteBuffer.wrap(hs2))));
        when(sessionFactory.confirm(any(byte[].class))).thenReturn(completedFuture(id));

        return onionAuth;
    }

    private static void verifyBobOnionMock(OnionAuthorizer onionAuthorizer) {
        verify(onionAuthorizer, atLeastOnce()).sessionFactory();

        val sessionFactory = onionAuthorizer.sessionFactory();
        verify(sessionFactory, atLeastOnce()).responseTo(any(byte[].class));
    }

    private static OnionAuthorizer randOnionAuthMock() {
        val onionAuth = mock(OnionAuthorizer.class);
        val sessionFactory = mock(SessionFactory.class);
        when(onionAuth.sessionFactory()).thenReturn(sessionFactory);

        when(sessionFactory.start(any()))
            .thenReturn(completedFuture(Pair.of(randSessionId(), randHandshake())));

        when(sessionFactory.responseTo(any(byte[].class)))
            .thenReturn(completedFuture(Pair.of(randSessionId(), randHandshake())));

        when(sessionFactory.confirm(any(byte[].class)))
            .thenReturn(completedFuture(randSessionId()));

        return onionAuth;
    }

    private static ByteBuffer randHandshake() {
        return ByteBuffer.wrap(rand5Bytes());
    }

    private static byte[] rand5Bytes() {
        val bytes = new byte[5];
        ThreadLocalRandom.current().nextBytes(bytes);

        return bytes;
    }

    private static SessionId randSessionId() {
        return SessionId.wrap((short) ThreadLocalRandom.current().nextInt());
    }

}
