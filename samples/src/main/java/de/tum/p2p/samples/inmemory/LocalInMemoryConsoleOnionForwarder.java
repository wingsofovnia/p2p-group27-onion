package de.tum.p2p.samples.inmemory;

import de.tum.p2p.Peer;
import de.tum.p2p.onion.auth.InMemoryBase64OnionAuthorizer;
import de.tum.p2p.onion.auth.OnionAuthorizer;
import de.tum.p2p.onion.forwarding.OnionForwarder;
import de.tum.p2p.onion.forwarding.Tunnel;
import de.tum.p2p.onion.forwarding.netty.NettyOnionForwarder;
import de.tum.p2p.rps.InMemoryRandomPeerSampler;
import de.tum.p2p.rps.RandomPeerSampler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static de.tum.p2p.samples.FakeKeys.fakePublicKey;
import static de.tum.p2p.util.Nets.localhost;
import static de.tum.p2p.util.Nets.randUnprivilegedPort;
import static java.lang.System.out;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * {@code LocalInMemoryConsoleOnionForwarder} shows how Netty implementation of
 * {@link OnionForwarder} can be used to build tunnel, forward data (incl. listening
 * for the incoming data on the destination onion) and destroy tunnel.
 * <p>
 * Fake in-memory {@link OnionAuthorizer} and {@link RandomPeerSampler} are used to
 * make the setup fully local and standalone.
 * <p>
 * Usage:
 * <pre>
 * ./gradlew clean jar
 * java -jar samples/build/libs/onion-forwarding-samples-0.1-SNAPSHOT.jar ${NUMBER_OF_ONIONS_>3}
 * </pre>
 * <p>
 * Example:
 * <pre>
 * $ java -jar samples/build/libs/onion-forwarding-samples-0.1-SNAPSHOT.jar 3
 * Number of local onions = 3, intermediate hops = 1
 * Random local peers = localhost/127.0.0.1:16453, localhost/127.0.0.1:6247, localhost/127.0.0.1:39516
 * Please select origin onion to build a tunnel
 * 0) localhost/127.0.0.1:16453
 * 1) localhost/127.0.0.1:6247
 * 2) localhost/127.0.0.1:39516
 * Type: 0
 * Please select destination onion to build a tunnel
 * 1) localhost/127.0.0.1:6247
 * 2) localhost/127.0.0.1:39516
 * Type: 2
 * Building the tunnel ...
 * Listener: Onion (localhost/127.0.0.1:39516) incoming tunnel #909156460
 * Tunnel #909156460 has been successfully built
 *
 * Feel free to type any string to forward it from the (localhost/127.0.0.1:16453) -> (localhost/127.0.0.1:39516). Type 'exit' to exit.
 * > test
 * Sending "test" from (localhost/127.0.0.1:16453) to (localhost/127.0.0.1:39516) via tunnel #909156460
 * Listener: Datum "test" arrived to (localhost/127.0.0.1:39516) via tunnel #909156460
 * > exit
 * Closing onions ...
 * $ ...
 * </pre>
 */
public class LocalInMemoryConsoleOnionForwarder {
    static {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";

    private static final int DEFAULT_ONION_NODES = 3;

    public static void main(String[] args) throws InterruptedException, IOException {
        final int onionNodes = args.length > 0 ? Integer.valueOf(args[0]) : DEFAULT_ONION_NODES;
        final int onionHops = onionNodes - 2;
        out.println("Number of local onions = " + onionNodes + ", intermediate hops = " + onionHops);

        // Setup local fake peers
        final List<Peer> randLocalPeers = randUnprivilegedPort(onionNodes)
            .mapToObj(randPort -> Peer.of(localhost(), randPort, fakePublicKey()))
            .collect(toList());
        out.println("Random local peers = " + randLocalPeers.stream()
            .map(Peer::socketAddress)
            .map(InetSocketAddress::toString)
            .collect(joining(", ")));

        // Setup local fake in-memory OnionAuth and RPS
        final RandomPeerSampler inMemoryRps = new InMemoryRandomPeerSampler(randLocalPeers);
        final OnionAuthorizer inMemoryBase64OnionAuth = new InMemoryBase64OnionAuthorizer();

        // Launch onion forwarder
        final List<OnionForwarder> onionFwds = new ArrayList<>(onionNodes);
        for (int i = 0; i < onionNodes; i++) {
            final Peer onionPeer = randLocalPeers.get(i);

            onionFwds.add(new NettyOnionForwarder.Builder()
                .port(onionPeer.port())
                .onionAuthorizer(inMemoryBase64OnionAuth)
                .randomPeerSampler(inMemoryRps)
                .publicKey(onionPeer.publicKey())
                .intermediateHops(onionHops)
                .build());
        }

        try (final Scanner in = new Scanner(System.in)) {
            // Asking user for origin and destination onions
            out.println("Please select origin onion to build a tunnel");
            for (int i = 0; i < onionNodes; i++) {
                out.println(i + ") " + randLocalPeers.get(i).socketAddress().toString());
            }
            out.print("Type: ");
            final int origPeerIndex = in.nextInt();
            final Peer originPeer = randLocalPeers.get(origPeerIndex);
            final OnionForwarder originOnion = onionFwds.stream().filter(fwd -> fwd.peer().equals(originPeer)).findFirst().get();

            out.println("Please select destination onion to build a tunnel");
            for (int i = 0; i < onionNodes; i++) {
                if (i == origPeerIndex)
                    continue;
                out.println(i + ") " + randLocalPeers.get(i).socketAddress().toString());
            }
            out.print("Type: ");
            final Peer destPeer = randLocalPeers.get(in.nextInt());
            final OnionForwarder destOnion = onionFwds.stream().filter(fwd -> fwd.peer().equals(destPeer)).findFirst().get();

            // Setup destination onion listeners for incoming data and tunnel
            destOnion.addIncomingTunnelObserver(tunnelId
                -> out.println(ANSI_RED + "Listener:" + ANSI_RESET +
                    " Onion (" + destPeer.socketAddress() +") incoming tunnel #" + tunnelId));
            destOnion.addIncomingDataObserver(((tunnelId, byteBuffer)
                -> out.println(ANSI_RED + "Listener:" + ANSI_RESET +
                    " Datum \"" + new String(byteBuffer.array()) + "\" arrived to (" + destPeer.socketAddress() +") " +
                        "via tunnel #" + tunnelId)));

            out.println("Building the tunnel ...");
            final Tunnel tunnel = originOnion.createTunnel(destPeer).join();
            out.println("Tunnel #" + tunnel.id() + " has been successfully built");

            out.println("\nFeel free to type any string to forward it from the (" + originPeer.socketAddress() + ") " +
                "-> (" + destPeer.socketAddress() + "). Type 'exit' to exit.");

            out.print("> ");
            String str;
            while(in.hasNextLine()) {
                str = in.next();
                if (str.length() == 0)
                    continue;
                else if ("exit".equals(str)) {
                    break;
                }

                out.println("Sending \"" + str + "\" from (" + originPeer.socketAddress() + ") to (" + destPeer.socketAddress() + ") via tunnel #" + tunnel.id());
                originOnion.forward(tunnel, ByteBuffer.wrap(str.getBytes()));
                Thread.sleep(500);
                out.print("> ");
            }
        } finally {
            out.println("Closing onions ...");
            for (OnionForwarder onionForwarder : onionFwds) {
                onionForwarder.close();
            }
        }
    }
}
