package de.tum.p2p.onion.forwarding;

import de.tum.p2p.Peer;
import de.tum.p2p.proto.message.Message;
import de.tum.p2p.proto.message.onion.forwarding.DatumOnionMessage;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The Onion Forwarder is responsible for forwarding data between
 * API connections and Onion Tunnels.
 */
public interface OnionForwarder extends Closeable {

    /**
     * Drills a data tunnel that will be used for data propagation through peers
     * <p>
     * Tunnel building is done in the following steps:
     * <ol>
     * <li>Samples {@code hops} random peers using {@link de.tum.p2p.rps.RandomPeerSampler}</li>
     * <li>Establish secure connection with each sampled peer with
     * {@link de.tum.p2p.onion.auth.OnionAuthorizer}</li>
     * <li>Remembers sampled peers and corresponding {@link de.tum.p2p.onion.auth.SessionId}s
     * as a Tunnel</li>
     * </ol>
     *
     * @param destination a {@link Peer} requested Tunnel should ends with
     * @return a future {@link TunnelId} built
     * @throws OnionTunnelingException in case of unexpected error during Tunnel building
     * @see <a href="https://en.wikipedia.org/wiki/Onion_routing">
     * Wikipedia - Onion Routing</a>
     */
    CompletableFuture<TunnelId> createTunnel(Peer destination) throws OnionTunnelingException;

    /**
     * Instructs {@link OnionForwarder} that Tunnel is no longer in use and it can
     * be destroyed.
     *
     * @param tunnel a data Tunnel to be destroyed
     * @throws OnionTunnelingException in case of unexpected error during Tunnel destroying
     */
    void destroyTunnel(TunnelId tunnel) throws OnionTunnelingException;

    /**
     * Forwards data through the data Tunnel given
     *
     * @param tunnel  a data Tunnel to be used as a data pipe
     * @param message a msg that should be forwarded
     * @throws OnionDataForwardingException in case of unexpected error during data forwarding
     */
    void forward(TunnelId tunnel, Message message) throws OnionDataForwardingException;

    /**
     * Generates cover traffic which is sent to a random destination, used to
     * fabricate cover traffic mimicking the characteristics of real traffic.
     *
     * @param size amount of random bytes on the Tunnel established
     *             to a random destination
     * @throws OnionCoverInterferenceException in case of sending
     */
    void cover(int size) throws OnionCoverInterferenceException;

    /**
     * Registers a listener for incoming {@link DatumOnionMessage}.
     * <p>
     * {@code OnionForwarder} will fire {@link Consumer#accept(Object)} on each incoming
     * {@link DatumOnionMessage}.
     *
     * @param datumOnionMessageConsumer listener implementation (lambda)
     */
    void onDatumArrival(final Consumer<DatumOnionMessage> datumOnionMessageConsumer);

    /**
     * Generates a {@link Peer} info object with peer connection detains and public key
     * @return a Peer metadata
     */
    Peer peer();
}
