package de.tum.p2p.onion.forwarding.netty;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.onion.forwarding.netty.event.TunnelDatumReceived;
import de.tum.p2p.onion.forwarding.netty.event.TunnelExtendReceived;
import de.tum.p2p.onion.forwarding.netty.event.TunnelExtendedReceived;
import de.tum.p2p.proto.RequestId;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@code OnionEventBus} encapsulates Guava's {@link EventBus} to provide
 * some useful {@code OnionForwarder} specific functionality
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class OnionEventBus {

    private final EventBus eventBus;

    private final Map<RequestId, CompletableFuture<SessionId>> futureSessions = new ConcurrentHashMap<>();
    private final List<BiConsumer<TunnelId, ByteBuffer>> dataConsumers = new Vector<>();
    private final List<Consumer<TunnelId>> incomingTunnelConsumers = new Vector<>();

    public OnionEventBus(EventBus eventBus) {
        this.eventBus = eventBus;

        registerTunnelExtendedProxyListener(this.eventBus);
        registerDataReceivedProxyListener(this.eventBus);
        registerTunnelExtendProxyListener(this.eventBus);
    }

    public void completeFutureSession(RequestId requestId, CompletableFuture<SessionId> futureSession) {
        futureSessions.put(requestId, futureSession);
    }

    public void register(Object object) {
        eventBus.register(object);
    }

    public void unregister(Object object) {
        eventBus.unregister(object);
    }

    public void registerDataListener(BiConsumer<TunnelId, ByteBuffer> consumer) {
        dataConsumers.add(consumer);
    }

    public void registerIncomingTunnelListener(Consumer<TunnelId> consumer) {
        incomingTunnelConsumers.add(consumer);
    }

    public void unregisterIncomingTunnelListener(BiConsumer<TunnelId, ByteBuffer> consumer) {
        dataConsumers.remove(consumer);
    }

    public void unregisterIncomingTunnelListener(Consumer<TunnelId> consumer) {
        incomingTunnelConsumers.remove(consumer);
    }

    public void post(Object event) {
        eventBus.post(event);
    }

    private void registerTunnelExtendedProxyListener(EventBus eventBus) {
        eventBus.register(new Consumer<TunnelExtendedReceived>() {
            @Subscribe
            public void accept(TunnelExtendedReceived tunnelExtendedReceived) {
                futureSessions.forEach((requestId, futureSession) -> {
                    if (!tunnelExtendedReceived.requestId().equals(requestId))
                        return;

                    futureSession.complete(tunnelExtendedReceived.sessionId());
                });
            }
        });
    }

    private void registerDataReceivedProxyListener(EventBus eventBus) {
        eventBus.register(new Consumer<TunnelDatumReceived>() {
            @Subscribe
            public void accept(TunnelDatumReceived tunnelDatumReceived) {
                dataConsumers.forEach(consumer -> {
                    consumer.accept(tunnelDatumReceived.tunnelId(), tunnelDatumReceived.payload());
                });
            }
        });
    }

    private void registerTunnelExtendProxyListener(EventBus eventBus) {
        eventBus.register(new Consumer<TunnelExtendReceived>() {
            @Subscribe
            public void accept(TunnelExtendReceived tunnelExtendReceived) {
                incomingTunnelConsumers.forEach(consumer -> {
                    consumer.accept(tunnelExtendReceived.tunnelId());
                });
            }
        });
    }
}
