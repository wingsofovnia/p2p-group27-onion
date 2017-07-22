package de.tum.p2p.onion.forwarding.netty.context;

import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.onion.forwarding.netty.NettyOnionForwarder;
import io.netty.channel.Channel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Router holds routes to next, previous peers and session idsfor
 * {@link NettyOnionForwarder} so that it knows where it should
 * route ONION_TUNNEL_DATUM/ONION_TUNNEL_EXTEND messages
 */
@EqualsAndHashCode @ToString
public class Router implements Closeable {

    private final List<Route> routes = new ArrayList<>();

    public void route(Route route) {
        if (hasRoute(route.tunnelId()))
            throw new IllegalArgumentException("One route object per tunnel please");

        routes.add(route);
    }

    public void forget(Route route) {
        val removed = routes.remove(route);

        if (removed)
            route.retire();
    }

    public void forget(TunnelId tunnelId) {
        route(tunnelId).ifPresent(route -> {
            routes.remove(route);
            route.retire();
        });
    }

    public boolean forgetIf(Predicate<? super Route> filter) {
        return routes.removeIf(filter);
    }

    public Optional<Route> route(TunnelId tunnelId) {
        return routes.stream()
            .filter(fwd -> fwd.tunnelId().equals(tunnelId))
            .findAny();
    }

    public Optional<Channel> routeNext(TunnelId tunnelId) {
        return route(tunnelId).map(Route::getNext);
    }

    public Optional<SessionId> routeSessionId(TunnelId tunnelId) {
        return route(tunnelId).map(Route::getFirstSessionId);
    }

    public Optional<Channel> routePrev(TunnelId tunnelId) {
        return route(tunnelId).map(Route::getPrev);
    }

    public boolean hasRoute(TunnelId tunnelId) {
        return route(tunnelId).isPresent();
    }

    public boolean hasRouteNext(TunnelId tunnelId) {
        return routeNext(tunnelId).isPresent();
    }

    public boolean hasRoutePrev(TunnelId tunnelId) {
        return routePrev(tunnelId).isPresent();
    }

    public int size() {
        return routes.size();
    }

    public boolean isEmpty() {
        return routes.isEmpty();
    }

    public void clear() {
        routes.clear();
    }

    @Override
    public void close() throws IOException {
        for (val fwd : routes)
            fwd.close();
    }
}
