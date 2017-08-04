package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static de.tum.p2p.proto.message.onion.forwarding.TunnelDatumMessage.PAYLOAD_BYTES;
import static java.lang.Integer.min;
import static java.lang.Math.ceil;
import static java.util.stream.Collectors.toList;

/**
 * {@code TunnelDatumMessageFactory} used to create {@link TunnelDatumMessage}
 * that may be of size bigger then {@code TunnelDatumMessage} can carry. Therefore
 * this factory allows to partition the data and create multiple datum messages.
 *
 * @see TunnelDatumMessage
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class TunnelDatumMessageFactory {

    public static List<TunnelDatumMessage> ofMany(TunnelId tunnelId, ByteBuffer data) {
        return partitions(data)
            .map(payload -> new TunnelDatumMessage(tunnelId, byteBufferToArray(payload)))
            .collect(toList());
    }

    private static Stream<ByteBuffer> partitions(ByteBuffer data) {
        val partitionAmount = (int) ceil((double) data.remaining() / PAYLOAD_BYTES);

        val partitions = new ArrayList<ByteBuffer>(partitionAmount);
        for (int i = 0; i < partitionAmount; i++) {
            val payloadSize = min(data.remaining(), PAYLOAD_BYTES);

            val payload = new byte[payloadSize];
            data.get(payload);

            partitions.add(ByteBuffer.wrap(payload));
        }

        return partitions.stream();
    }

    private static byte[] byteBufferToArray(ByteBuffer data) {
        val payload = new byte[data.remaining()];
        data.duplicate().get(payload);

        return payload;
    }
}
