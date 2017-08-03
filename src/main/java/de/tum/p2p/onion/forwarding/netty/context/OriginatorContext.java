package de.tum.p2p.onion.forwarding.netty.context;

import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.val;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code OriginatorContext} holds information about created tunnel
 * and theirs entry {@link Channel} where {@code OnionForwarder} can
 * forward data
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public class OriginatorContext implements Closeable {

    private final Map<TunnelId, Tunnel> tunnels = new HashMap<>();

    public void serve(TunnelId tunnelId, Channel entry, List<SessionId> sessionIds) {
        if (!tunnels.containsKey(tunnelId)) {
            tunnels.put(tunnelId, new Tunnel(notNull(entry), sessionIds));
            return;
        }

        val oldTunnel = tunnels.get(tunnelId);
        oldTunnel.sessionIds = notEmpty(sessionIds);
    }

    public void serve(TunnelId tunnelId, Channel entry) {
        serve(tunnelId, entry, new ArrayList<>());
    }

    public boolean serves(TunnelId tunnelId) {
        return tunnels.containsKey(tunnelId);
    }

    public void appendSession(TunnelId tunnelId, SessionId sessionId) {
        if (tunnels.containsKey(tunnelId))
            throw new IllegalArgumentException("No tunnel found");

        tunnels.get(tunnelId).sessionIds.add(sessionId);
    }

    public void appendSession(TunnelId tunnelId, List<SessionId> sessionIds) {
        if (!tunnels.containsKey(tunnelId))
            throw new IllegalArgumentException("No tunnel found");

        tunnels.get(tunnelId).sessionIds.addAll(sessionIds);
    }

    public Channel entry(TunnelId tunnelId) {
        val tunnel = tunnels.get(tunnelId);
        if (tunnel == null)
            return null;

        return tunnel.entry;
    }

    public List<SessionId> sessionIds(TunnelId tunnelId) {
        val tunnel = tunnels.get(tunnelId);
        if (tunnel == null)
            return null;

        return tunnel.sessionIds;
    }

    public Set<TunnelId> tunnels() {
        return tunnels.keySet();
    }

    public boolean isEmpty() {
        return tunnels.isEmpty();
    }

    @Override
    public void close() throws IOException {
        tunnels.forEach((tunnelId, tunnel) -> {
            closeTunnel(tunnel);
        });
        tunnels.clear();
    }

    public void forget(TunnelId tunnelId) {
        val removedTunnel = tunnels.remove(tunnelId);
        closeTunnel(removedTunnel);
    }

    private void closeTunnel(Tunnel tunnel) {
        tunnel.entry.disconnect();
        tunnel.entry.close().awaitUninterruptibly();
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    private static final class Tunnel {
        private Channel entry;
        private List<SessionId> sessionIds;
    }
}
