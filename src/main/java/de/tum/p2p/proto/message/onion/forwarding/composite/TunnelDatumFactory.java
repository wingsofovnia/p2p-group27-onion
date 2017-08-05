package de.tum.p2p.proto.message.onion.forwarding.composite;

import lombok.val;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static de.tum.p2p.proto.message.onion.forwarding.composite.TunnelRelayPayload.PAYLOAD_BYTES;
import static java.lang.Integer.min;
import static java.lang.Math.ceil;
import static java.util.stream.Collectors.toList;

/**
 * {@code TunnelDatumPayloadFactory} used to create {@link TunnelDatum}
 * that may be of size bigger then {@code TunnelDatumPayload} can carry. Therefore
 * this factory allows to partition the data and create multiple datum messages.
 *
 * @see TunnelDatum
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class TunnelDatumFactory {

    public static List<TunnelDatum> ofMany(ByteBuffer data) {
        return partitions(data).map(TunnelDatum::new).collect(toList());
    }

    private static Stream<byte[]> partitions(ByteBuffer dataBuffer) {
        val dataBufferDupe = dataBuffer.duplicate();

        val partitionAmount = (int) ceil((double) dataBufferDupe.remaining() / PAYLOAD_BYTES);

        val partitions = new ArrayList<byte[]>(partitionAmount);
        for (int i = 0; i < partitionAmount; i++) {
            val payloadSize = min(dataBufferDupe.remaining(), PAYLOAD_BYTES);

            val payload = new byte[payloadSize];
            dataBufferDupe.get(payload);

            partitions.add(payload);
        }

        return partitions.stream();
    }
}
