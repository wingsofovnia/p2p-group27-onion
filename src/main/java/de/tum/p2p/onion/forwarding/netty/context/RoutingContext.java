package de.tum.p2p.onion.forwarding.netty.context;

import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.val;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.ObjectUtils.anyNotNull;

/**
 * {@code RoutingContext} is a routing table of onions and contains
 * previous and next hops {@link Channel}s mapped to corresponding
 * {@link TunnelId}
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public final class RoutingContext implements Closeable {

    private final Map<TunnelId, Route> routes = new HashMap<>();

    public void serve(TunnelId tunnelId, Channel next, Channel prev, SessionId sessionId) {
        if (!routes.containsKey(tunnelId)) {
            routes.put(tunnelId, new Route(next, prev, sessionId));
            return;
        }

        if (anyNotNull(next, prev, sessionId)) {
            val oldRoute = routes.get(tunnelId);
            if (next != null) {
                oldRoute.next = next;
            } else if (prev != null) {
                oldRoute.prev = prev;
            } else if (sessionId != null) {
                oldRoute.sessionId = sessionId;
            }
        }
    }

    public void forget(TunnelId tunnelId) {
        val removedRoute = routes.remove(tunnelId);
        closeRoute(removedRoute);
    }

    public boolean serves(TunnelId tunnelId) {
        return routes.containsKey(tunnelId);
    }

    public boolean hasPrevHop(TunnelId tunnelId) {
        return routes.containsKey(tunnelId) && routes.get(tunnelId).prev != null;
    }

    public Channel prevHop(TunnelId tunnelId) {
        val route = routes.get(tunnelId);
        if (route == null)
            return null;

        return route.prev;
    }

    public boolean hasNextHop(TunnelId tunnelId) {
        return routes.containsKey(tunnelId) && routes.get(tunnelId).next != null;
    }

    public Channel nextHop(TunnelId tunnelId) {
        val route = routes.get(tunnelId);
        if (route == null)
            return null;

        return route.next;
    }

    public SessionId sessionId(TunnelId tunnelId) {
        val route = routes.get(tunnelId);
        if (route == null)
            return null;

        return route.sessionId;
    }

    public boolean hasSessionId(TunnelId tunnelId) {
        return routes.containsKey(tunnelId) && routes.get(tunnelId).sessionId != null;
    }

    public void setNextHop(TunnelId tunnelId, Channel next) {
        serve(tunnelId, next, null, null);
    }

    public void setPrevHop(TunnelId tunnelId, Channel prev) {
        serve(tunnelId, null, prev, null);
    }

    public void setSessionId(TunnelId tunnelId, SessionId sessionId) {
        serve(tunnelId, null, null, sessionId);
    }

    public void clear() {
        routes.forEach((tunnelId, route) -> closeRoute(route));
        routes.clear();
    }

    private void closeRoute(Route route) {
        closeChannel(route.next);
        closeChannel(route.prev);
    }

    private void closeChannel(Channel channel) {
        if (channel == null)
            return;

        channel.disconnect();
        channel.close().awaitUninterruptibly();
    }

    @Override
    public void close() throws IOException {
        clear();
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    private static final class Route {
        private Channel next;
        private Channel prev;
        private SessionId sessionId;
    }
}
