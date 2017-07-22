package de.tum.p2p.onion.forwarding;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.concurrent.ThreadLocalRandom;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
@Getter @Accessors(fluent = true)
public class TunnelId {

    private final Integer id;

    public TunnelId(Integer id) {
        this.id = notNull(id);
    }

    public static TunnelId wrap(Integer id) {
        return new TunnelId(id);
    }

    public static TunnelId random() {
        return TunnelId.wrap(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
    }

    public Integer raw() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
