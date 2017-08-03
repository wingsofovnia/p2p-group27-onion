package de.tum.p2p.onion.forwarding;

import lombok.EqualsAndHashCode;

import java.util.concurrent.ThreadLocalRandom;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelId} encapsulates an id that is used to identify a tunnel
 * withing an onion
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
@EqualsAndHashCode
public class TunnelId {

    public static final int BYTES = Integer.BYTES;

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
