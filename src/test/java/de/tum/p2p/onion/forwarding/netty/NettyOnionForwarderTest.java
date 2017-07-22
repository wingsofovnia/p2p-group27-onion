package de.tum.p2p.onion.forwarding.netty;

import de.tum.p2p.onion.auth.*;
import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.rps.InMemoryRandomPeerSampler;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;

import static de.tum.p2p.Peers.randLocalPeers;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class NettyOnionForwarderTest {

    @Test
    public void buildsTunnelsCorrectly() throws ExecutionException, InterruptedException, TimeoutException {
        val randDistinctPeers = randLocalPeers(3);
        val hmac = rand5Bytes();

        val rps = new InMemoryRandomPeerSampler(randDistinctPeers);

        val peer1 = randDistinctPeers.get(0);
        val peer1router = new Router();

        val peer2 = randDistinctPeers.get(1);

        val peer3 = randDistinctPeers.get(2);
        val peer3router = new Router();

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
            .router(peer1router)
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
            .router(peer3router)
            //.loggerLevel(LogLevel.DEBUG)
            .intermediateHops(1)
            .listen();

        val p1p3TunnelId = peer1onion.createTunnel(peer3).get(5, TimeUnit.SECONDS);

        assertNotNull(p1p3TunnelId);

        verifyAliseOnionMock(p1p3aliseOnionAuth);
        verifyBobOnionMock(p1p3bobOnionAuth);

        assertEquals(peer2.socketAddress(), peer1router.routeNext(p1p3TunnelId).get().remoteAddress());
        assertFalse(peer1router.routePrev(p1p3TunnelId).isPresent());
    }

    @Test(expected = OnionTunnelingException.class)
    public void throwsExceptionOnSendingDataViaRetiredTunnel() throws InterruptedException, ExecutionException, TimeoutException {
        val randDistinctPeers = randLocalPeers(3);
        val hmac = rand5Bytes();

        val rps = new InMemoryRandomPeerSampler(randDistinctPeers);

        val peer1 = randDistinctPeers.get(0);
        val peer2 = randDistinctPeers.get(1);
        val peer3 = randDistinctPeers.get(2);

        val rootPeer = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer1.port())
            .onionAuthorizer(randOnionAuthMock())
            .randomPeerSampler(rps)
            .hmacKey(hmac)
            .publicKey(peer1.publicKey())
            .intermediateHops(1)
            .listen();

        // peer2onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer2.port())
            .onionAuthorizer(randOnionAuthMock())
            .randomPeerSampler(rps)
            .hmacKey(hmac)
            .publicKey(peer2.publicKey())
            .intermediateHops(1)
            .listen();

        // peer3onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer3.port())
            .onionAuthorizer(randOnionAuthMock())
            .randomPeerSampler(rps)
            .hmacKey(hmac)
            .publicKey(peer3.publicKey())
            .intermediateHops(1)
            .listen();

        val tunnelId = rootPeer.createTunnel(peer3).get(5, TimeUnit.SECONDS);
        assertNotNull(tunnelId);

        rootPeer.destroyTunnel(tunnelId);

        Thread.sleep(500);
        rootPeer.forward(tunnelId, null);
    }

    @Test
    public void forwardsDataWithMockedCryptoCorrectly() throws InterruptedException, ExecutionException, TimeoutException {
        val randDistinctPeers = randLocalPeers(3);
        val hmac = rand5Bytes();

        val rps = new InMemoryRandomPeerSampler(randDistinctPeers);

        val peer1 = randDistinctPeers.get(0);
        val peer1oauth = randOnionAuthMock();
        val peer2 = randDistinctPeers.get(1);
        val peer2oauth = randOnionAuthMock();
        val peer3 = randDistinctPeers.get(2);
        val peer3oauth = randOnionAuthMock();

        val rootPeer = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer1.port())
            .onionAuthorizer(peer1oauth)
            .randomPeerSampler(rps)
            .hmacKey(hmac)
            .publicKey(peer1.publicKey())
            .intermediateHops(1)
            .listen();

        // peer2onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer2.port())
            .onionAuthorizer(peer2oauth)
            .randomPeerSampler(rps)
            .hmacKey(hmac)
            .publicKey(peer2.publicKey())
            .intermediateHops(1)
            .listen();

        // peer3onion
        val destPeer = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer3.port())
            .onionAuthorizer(peer3oauth)
            .randomPeerSampler(rps)
            .hmacKey(hmac)
            .publicKey(peer3.publicKey())
            .intermediateHops(1)
            .listen();

        val tunnelIdBuilt = rootPeer.createTunnel(peer3).get(5, TimeUnit.SECONDS);

        val dataToForward = new byte[] {1, 2, 3};

        val lock = new CountDownLatch(1);
        destPeer.subscribe(((tunnelId, byteBuffer) -> {

            assertEquals(tunnelIdBuilt, tunnelId);
            assertArrayEquals(dataToForward, byteBuffer.array());

            lock.countDown();
        }));

        rootPeer.forward(tunnelIdBuilt, ByteBuffer.wrap(dataToForward));

        lock.await(5, TimeUnit.SECONDS);

        verify(peer1oauth, times(1)).encrypt(any(ByteBuffer.class), any(List.class));
        verify(peer2oauth, times(1)).decrypt(any(byte[].class), any());
        verify(peer3oauth, times(1)).decrypt(any(byte[].class), any());
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

        when(onionAuth.encrypt(any(ByteBuffer.class), any()))
            .then(invocation -> completedFuture(Ciphertext.wrap(((ByteBuffer) invocation.getArguments()[0]).array())));
        when(onionAuth.decrypt(any(byte[].class), any()))
            .then(invocation -> completedFuture(Deciphertext.ofPlaintext((byte[]) invocation.getArguments()[0])));

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
