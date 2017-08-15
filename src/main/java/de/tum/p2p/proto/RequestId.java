package de.tum.p2p.proto;

import lombok.EqualsAndHashCode;

import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code RequestId} encapsulates an id of a request used
 * for mapping messages in communication of request-response manner
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@EqualsAndHashCode
public class RequestId {

    public static final int BYTES = Short.BYTES;

    private static final AtomicInteger requestIdCounter = new AtomicInteger();

    private final Short id;

    public RequestId(Short id) {
        this.id = notNull(id);
    }

    public RequestId(Integer id) {
        this.id = id.shortValue();
    }

    public static RequestId wrap(Short id) {
        return new RequestId(id);
    }

    public static RequestId wrap(Integer id) {
        return new RequestId(id);
    }

    /**
     * Returns an increment of internal request id counter
     * @return new unique counter withing this onion
     */
    public static RequestId next() {
        return new RequestId(requestIdCounter.getAndIncrement());
    }

    public Short raw() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
