package de.tum.p2p.onion.forwarding.netty;

import de.tum.p2p.onion.OnionException;
import de.tum.p2p.onion.forwarding.TunnelId;
import io.netty.channel.Channel;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TunnelRouter holds routes to next and previous peers for
 * {@link NettyOnionForwarder} so that it knows where it should
 * forward ONION_TUNNEL_DATUM/ONION_TUNNEL_EXTEND messages
 */
@EqualsAndHashCode @ToString
public class TunnelRouter implements Closeable {

    private final Map<TunnelId, Channel> tunnelPrevHops = new ConcurrentHashMap<>();
    private final Map<TunnelId, Channel> tunnelNextHops = new ConcurrentHashMap<>();

    public TunnelRouter routePrev(TunnelId tunnelId, Channel channel) {
        tunnelPrevHops.put(tunnelId, channel);
        return this;
    }

    public TunnelRouter routeNext(TunnelId tunnelId, Channel channel) {
        tunnelNextHops.put(tunnelId, channel);
        return this;
    }

    public Optional<Channel> resolveNext(TunnelId tunnelId) {
        return Optional.ofNullable(tunnelNextHops.get(tunnelId));
    }

    public Optional<Channel> resolvePrev(TunnelId tunnelId) {
        return Optional.ofNullable(tunnelPrevHops.get(tunnelId));
    }

    public Channel getNext(TunnelId tunnelId) {
        return resolveNext(tunnelId).get();
    }

    public Channel getPrev(TunnelId tunnelId) {
        return resolvePrev(tunnelId).get();
    }

    public void remove(Integer tunnelId) {
        tunnelPrevHops.remove(tunnelId);
        tunnelNextHops.remove(tunnelId);
    }

    @Override
    public void close() throws IOException {
        tunnelNextHops.values().forEach(channel -> {
            try {
                channel.disconnect();
                channel.close().syncUninterruptibly();
            } catch (Exception e) {
                throw new OnionException("Failed to close tunnel routeNext routes (channels)", e);
            }
        });

        tunnelPrevHops.values().forEach(channel -> {
            try {
                channel.disconnect();
                channel.close().syncUninterruptibly();
            } catch (Exception e) {
                throw new OnionException("Failed to close tunnel routePrev routes (channels)", e);
            }
        });
    }
}
