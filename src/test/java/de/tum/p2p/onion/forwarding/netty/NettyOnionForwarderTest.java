package de.tum.p2p.onion.forwarding.netty;

import de.tum.p2p.Peer;
import de.tum.p2p.onion.auth.InMemoryBase64OnionAuthorizer;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.netty.context.OriginatorContext;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.rps.InMemoryRandomPeerSampler;
import de.tum.p2p.rps.RandomPeerSampler;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.tum.p2p.Peers.randLocalPeers;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("ALL")
public class NettyOnionForwarderTest {

    private List<Peer> randomPeers;
    private RandomPeerSampler rps;

    @Before
    public void prepareMocks() {
        this.randomPeers = randLocalPeers(3);
        this.rps = new InMemoryRandomPeerSampler(randomPeers);
    }

    @Test
    public void buildOneTunnelCorrectly() throws ExecutionException, InterruptedException, TimeoutException {
        val peer1 = randomPeers.get(0);
        val peer1OriginContext = new OriginatorContext();
        val peer1auth = spiedInMemoryBase64OnionAuthorizer();

        val peer2 = randomPeers.get(1);
        val peer2router = new RoutingContext();
        val peer2auth = spiedInMemoryBase64OnionAuthorizer();

        val peer3 = randomPeers.get(2);
        val peer3router = new RoutingContext();
        val peer3auth = spiedInMemoryBase64OnionAuthorizer();

        val peer1onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer1.port())
            .onionAuthorizer(peer1auth)
            .randomPeerSampler(rps)
            .publicKey(peer1.publicKey())
            .originatorContext(peer1OriginContext)
            .intermediateHops(1)
            .listen();

        // peer2onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer2.port())
            .onionAuthorizer(peer2auth)
            .randomPeerSampler(rps)
            .routingContext(peer2router)
            .publicKey(peer2.publicKey())
            .intermediateHops(1)
            .listen();

        // peer3onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer3.port())
            .onionAuthorizer(peer3auth)
            .randomPeerSampler(rps)
            .publicKey(peer3.publicKey())
            .routingContext(peer3router)
            .intermediateHops(1)
            .listen();

        val p1p3TunnelId = peer1onion.createTunnel(peer3).join();

        assertNotNull(p1p3TunnelId);

        // Verify session establishment
        verify(peer1auth, atLeastOnce()).sessionFactory();
        verify(peer2auth, atLeastOnce()).sessionFactory();
        verify(peer3auth, atLeastOnce()).sessionFactory();

        verify(peer1auth.sessionFactory(), times(1)).start(peer2);
        verify(peer1auth.sessionFactory(), times(1)).start(peer3);
        verify(peer1auth.sessionFactory(), times(2)).confirm(any(byte[].class));

        verify(peer2auth.sessionFactory(), times(1)).responseTo(any(byte[].class));
        verify(peer3auth.sessionFactory(), times(1)).responseTo(any(byte[].class));

        // Verify routes
        assertEquals(peer2.socketAddress(), peer1OriginContext.entry(p1p3TunnelId).remoteAddress());
        assertEquals(peer3.socketAddress(), peer2router.nextHop(p1p3TunnelId).remoteAddress());
    }

    @Test
    public void buildsMultipleTunnelsCorrectly() {
        val peer1 = randomPeers.get(0);
        val peer2 = randomPeers.get(1);
        val peer3 = randomPeers.get(2);

        val peer1onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer1.port())
            .onionAuthorizer(spiedInMemoryBase64OnionAuthorizer())
            .randomPeerSampler(rps)
            .publicKey(peer1.publicKey())
            .intermediateHops(1)
            .listen();

        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer2.port())
            .onionAuthorizer(spiedInMemoryBase64OnionAuthorizer())
            .randomPeerSampler(rps)
            .publicKey(peer2.publicKey())
            .intermediateHops(1)
            .listen();

        val peer3onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer3.port())
            .onionAuthorizer(spiedInMemoryBase64OnionAuthorizer())
            .randomPeerSampler(rps)
            .publicKey(peer3.publicKey())
            .intermediateHops(1)
            .listen();

        val p1p3TunnelId = peer1onion.createTunnel(peer3).join();
        val p1p2TunnelId = peer1onion.createTunnel(peer2).join();
        val p3p2TunnelId = peer3onion.createTunnel(peer2).join();
        val p3p1TunnelId = peer3onion.createTunnel(peer1).join();

        assertNotNull(p1p3TunnelId);
        assertNotNull(p1p2TunnelId);
        assertNotNull(p3p2TunnelId);
        assertNotNull(p3p1TunnelId);
    }

    @Test
    public void retiresTunnelCorrectly() throws ExecutionException, InterruptedException, TimeoutException {
        val peer1 = randomPeers.get(0);
        val peer1originContext = new OriginatorContext();
        val peer1auth = spiedInMemoryBase64OnionAuthorizer();

        val peer2 = randomPeers.get(1);
        val peer2router = new RoutingContext();
        val peer2auth = spiedInMemoryBase64OnionAuthorizer();

        val peer3 = randomPeers.get(2);
        val peer3router = new RoutingContext();
        val peer3auth = spiedInMemoryBase64OnionAuthorizer();

        val peer1onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer1.port())
            .onionAuthorizer(peer1auth)
            .originatorContext(peer1originContext)
            .randomPeerSampler(rps)
            .publicKey(peer1.publicKey())
            .intermediateHops(1)
            .listen();

        // peer2onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer2.port())
            .onionAuthorizer(peer2auth)
            .randomPeerSampler(rps)
            .routingContext(peer2router)
            .publicKey(peer2.publicKey())
            //.loggerLevel(LogLevel.DEBUG)
            .intermediateHops(1)
            .listen();

        // peer3onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer3.port())
            .onionAuthorizer(peer3auth)
            .randomPeerSampler(rps)
            .publicKey(peer3.publicKey())
            .routingContext(peer3router)
            //.loggerLevel(LogLevel.DEBUG)
            .intermediateHops(1)
            .listen();

        val p1p3TunnelId = peer1onion.createTunnel(peer3).join();
        peer1onion.destroyTunnel(p1p3TunnelId);
        Thread.sleep(Duration.ofSeconds(1).toMillis());

        // Verify hops being removed from all tunnels
        assertFalse(peer1originContext.serves(p1p3TunnelId));

        assertFalse(peer2router.hasNextHop(p1p3TunnelId));
        assertFalse(peer2router.hasPrevHop(p1p3TunnelId));

        assertFalse(peer3router.hasNextHop(p1p3TunnelId));
        assertFalse(peer3router.hasPrevHop(p1p3TunnelId));
    }

    @Test
    public void forwardsDataCorrectly() throws InterruptedException {
        val dataToForward = new byte[] {1, 2, 3};

        val peer1 = randomPeers.get(0);
        val peer1auth = spiedInMemoryBase64OnionAuthorizer();

        val peer2 = randomPeers.get(1);
        val peer2auth = spiedInMemoryBase64OnionAuthorizer();

        val peer3 = randomPeers.get(2);
        val peer3auth = spiedInMemoryBase64OnionAuthorizer();

        val peer1onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer1.port())
            .onionAuthorizer(peer1auth)
            .randomPeerSampler(rps)
            .publicKey(peer1.publicKey())
            .intermediateHops(1)
            .listen();

        // peer2onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer2.port())
            .onionAuthorizer(peer2auth)
            .randomPeerSampler(rps)
            .publicKey(peer2.publicKey())
            .intermediateHops(1)
            .listen();

        // peer3onion
        val peer3onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer3.port())
            .onionAuthorizer(peer3auth)
            .randomPeerSampler(rps)
            .publicKey(peer3.publicKey())
            .intermediateHops(1)
            .listen();

        val p1p3TunnelId = peer1onion.createTunnel(peer3).join();

        assertNotNull(p1p3TunnelId);

        val lock = new CountDownLatch(1);
        peer3onion.subscribe((tunnelId, byteBuffer) -> {

            assertEquals(p1p3TunnelId, tunnelId);
            assertArrayEquals(dataToForward, byteBuffer.array());

            lock.countDown();
        });

        peer1onion.forward(p1p3TunnelId, ByteBuffer.wrap(dataToForward));
        if (!lock.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS))
            fail("Message didn't arrive");
    }

    @Test
    public void forwardsDataBidirectionallyCorrectly() throws InterruptedException {
        val peer1 = randomPeers.get(0);
        val peer2 = randomPeers.get(1);
        val peer3 = randomPeers.get(2);

        val peer1onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer1.port())
            .onionAuthorizer(spiedInMemoryBase64OnionAuthorizer())
            .randomPeerSampler(rps)
            .publicKey(peer1.publicKey())
            .intermediateHops(1)
            .listen();

        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer2.port())
            .onionAuthorizer(spiedInMemoryBase64OnionAuthorizer())
            .randomPeerSampler(rps)
            .publicKey(peer2.publicKey())
            .intermediateHops(1)
            .listen();

        val peer3onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer3.port())
            .onionAuthorizer(spiedInMemoryBase64OnionAuthorizer())
            .randomPeerSampler(rps)
            .publicKey(peer3.publicKey())
            .intermediateHops(1)
            .listen();

        val p1p3TunnelId = peer1onion.createTunnel(peer3).join();
        val p3p1TunnelId = peer3onion.createTunnel(peer1).join();

        assertNotNull(p1p3TunnelId);
        assertNotNull(p3p1TunnelId);

        val p1p3data = new byte[] {1, 2, 3};
        val p3p1data = new byte[] {3, 2, 1};

        val p1p3TunnelDatumLock = new CountDownLatch(1);
        peer3onion.subscribe((tunnelId, byteBuffer) -> {

            assertEquals(p1p3TunnelId, tunnelId);
            assertArrayEquals(p1p3data, byteBuffer.array());

            p1p3TunnelDatumLock.countDown();
        });

        val p3p1TunnelDatumLock = new CountDownLatch(1);
        peer1onion.subscribe((tunnelId, byteBuffer) -> {

            assertEquals(p3p1TunnelId, tunnelId);
            assertArrayEquals(p3p1data, byteBuffer.array());

            p3p1TunnelDatumLock.countDown();
        });

        peer1onion.forward(p1p3TunnelId, ByteBuffer.wrap(p1p3data));
        peer3onion.forward(p3p1TunnelId, ByteBuffer.wrap(p3p1data));

        if (!p1p3TunnelDatumLock.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS))
            fail("Message didn't arrive to peer3onion");

        if (!p3p1TunnelDatumLock.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS))
            fail("Message didn't arrive to peer1onion");
    }

    @Test
    public void receivesCoverDatumButDoesntTriggerSubscribers() throws InterruptedException {
        val peer1 = randomPeers.get(0);
        val peer1auth = spiedInMemoryBase64OnionAuthorizer();
        val peer1rps = spy(rps);

        val peer2 = randomPeers.get(1);
        val peer2auth = spiedInMemoryBase64OnionAuthorizer();

        val peer3 = randomPeers.get(2);
        val peer3auth = spiedInMemoryBase64OnionAuthorizer();

        val peer1onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer1.port())
            .onionAuthorizer(peer1auth)
            .randomPeerSampler(rps)
            .publicKey(peer1.publicKey())
            .intermediateHops(1)
            .listen();

        // peer2onion
        new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer2.port())
            .onionAuthorizer(peer2auth)
            .randomPeerSampler(rps)
            .publicKey(peer2.publicKey())
            .intermediateHops(1)
            .listen();

        // peer3onion
        val peer3onion = new NettyOnionForwarder.NettyRemoteOnionForwarderBuilder()
            .port(peer3.port())
            .onionAuthorizer(peer3auth)
            .randomPeerSampler(rps)
            .publicKey(peer3.publicKey())
            .intermediateHops(1)
            .listen();

        // Make sure cover will target peer3onion, not random, so we can validate it
        doReturn(completedFuture(peer3)).when(peer1rps).sampleNot(any(Peer.class));

        val lock = new CountDownLatch(1);
        peer3onion.subscribe((tunnelId, byteBuffer) -> {
            lock.countDown();
        });

        peer1onion.cover(Byte.SIZE);

        if (lock.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS))
            fail("Cover didn't arrive");
    }

    private static OnionAuthorizer spiedInMemoryBase64OnionAuthorizer() {
        val auth = spy(new InMemoryBase64OnionAuthorizer());
        auth.sessionFactory(spy(auth.sessionFactory()));

        return auth;
    }
}
