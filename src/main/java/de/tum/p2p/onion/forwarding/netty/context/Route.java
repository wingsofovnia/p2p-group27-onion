package de.tum.p2p.onion.forwarding.netty.context;

import de.tum.p2p.onion.OnionException;
import de.tum.p2p.onion.auth.SessionId;
import de.tum.p2p.onion.forwarding.TunnelId;
import io.netty.channel.Channel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.Validate.notNull;

@Accessors(fluent = true)
@ToString @EqualsAndHashCode
public class Route implements Closeable {

    @Getter
    private final TunnelId tunnelId;

    @Setter
    private Channel prev;

    @Setter
    private Channel next;

    private List<SessionId> sessionIds = new ArrayList<>();

    Route(TunnelId tunnelId, Channel prev, Channel next, List<SessionId> sessionIds) {
        this.tunnelId = notNull(tunnelId);
        this.prev = prev;
        this.next = next;

        if (sessionIds != null)
            this.sessionIds.addAll(sessionIds);
    }

    public static Route to(TunnelId tunnelId, Channel next) {
        return new Route(tunnelId, null, next, null);
    }

    public static Route to(TunnelId tunnelId, Channel next, SessionId sessionId) {
        return new Route(tunnelId, null, next, Collections.singletonList(sessionId));
    }

    public static Route from(TunnelId tunnelId, Channel prev) {
        return new Route(tunnelId, prev, null, null);
    }

    public static Route from(TunnelId tunnelId, Channel prev, SessionId sessionId) {
        return new Route(tunnelId, prev, null, Collections.singletonList(sessionId));
    }

    public static Route between(TunnelId tunnelId, Channel prev, Channel next) {
        return new Route(tunnelId, prev, next, null);
    }

    public static Route between(TunnelId tunnelId, Channel prev, Channel next, SessionId sessionId) {
        return new Route(tunnelId, prev, next, Collections.singletonList(sessionId));
    }

    public Optional<Channel> next() {
        return Optional.ofNullable(next);
    }

    public Optional<Channel> prev() {
        return Optional.ofNullable(prev);
    }

    public Channel getNext() {
        return this.next;
    }

    public Channel getPrev() {
        return this.prev;
    }

    public void attachSessionId(SessionId sessionId) {
        this.sessionIds.add(sessionId);
    }

    public void attachSessionIds(List<SessionId> sessionIds) {
        this.sessionIds.addAll(sessionIds);
    }

    public void deattachSessionId(SessionId sessionId) {
        this.sessionIds.remove(sessionId);
    }

    public List<SessionId> sessionIds() {
        return Collections.unmodifiableList(this.sessionIds);
    }

    public Optional<SessionId> firstSessionId() {
        return Optional.ofNullable(this.sessionIds.get(0));
    }

    public SessionId getFirstSessionId() {
        return this.sessionIds.get(0);
    }

    @Override
    public void close() throws IOException {
        retire();
    }

    public void retire() {
        if (prev != null)
            closeChannel(prev);

        if (next != null)
            closeChannel(next);
    }

    private void closeChannel(Channel channel) {
        try {
            channel.disconnect();
            channel.close().syncUninterruptibly();
        } catch (Exception e) {
            throw new OnionException("Failed to close tunnel routeNext routes (channels)", e);
        }
    }
}
