package de.tum.p2p.proto.message.onion.forwarding.composite;

import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.proto.RequestId;
import de.tum.p2p.util.Nets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static de.tum.p2p.proto.message.MessageType.ONION_TUNNEL_CONNECT;
import static de.tum.p2p.util.Handshakes.notOversizedHadshake;
import static de.tum.p2p.util.Keys.notOversizedKey;
import static de.tum.p2p.util.Keys.parsePublicKey;
import static de.tum.p2p.util.Nets.guessInetAddressVersion;
import static de.tum.p2p.util.Nets.validPort;
import static java.lang.Short.toUnsignedInt;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelDatum} is a payload that carries a request to the last peer in the
 * tunnel to extend the tunnel by one peer defined in the message. It typically is
 * embedded into {@link TunnelRelayMessage} so that it can be layered encrypted (entirely)
 * and transferred via the tunnel securely without revealing the content by other onions.
 * <p>
 * Payload structure:
 * <pre>
 * |---------------------------|
 * |   CONNECT   |   REQE_ID   |
 * |---------------------------|
 * |     PORT    |  RSVD |IPVER|
 * |---------------------------|
 * |          IP ADDR          |
 * |---------------------------|
 * |   KEY LEN   |   SRC KEY   |
 * |---------------------------|
 * |     SRC KEY (CONT...)     |
 * |---------------------------|
 * |   HS1 LENG  |  HANDSHAKE  |
 * |---------------------------|
 * |     HANDSHAKE(CONT..)     |
 * |---------------------------|
 * </pre>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class TunnelConnect extends TunnelRelayPayload {

    @Getter
    protected final RequestId requestId;

    @Getter // unsigned short
    private final int port;

    private final static int RESERVED = Short.BYTES - 1; // 1 byte for the V field (ip version)

    @Getter
    private final InetAddress destination;

    @Getter
    private final PublicKey sourceKey;

    @Getter
    private final byte[] handshake;

    public TunnelConnect(RequestId requestId, InetAddress dest, int port, PublicKey sourceKey, byte[] handshake) {
        super(ONION_TUNNEL_CONNECT);
        this.requestId = requestId == null ? RequestId.next() : requestId;
        this.destination = notNull(dest);
        this.port = validPort(port);
        this.sourceKey = notOversizedKey(sourceKey);
        this.handshake = notOversizedHadshake(handshake);
    }

    public TunnelConnect(InetAddress dest, int port, PublicKey sourceKey, byte[] handshake) {
        this(null, dest, port, sourceKey, handshake);
    }

    public static TunnelConnect fromBytes(byte[] bytes) {
        try {
            val bytesBuffer = ByteBuffer.wrap(bytes);

            val parsedMessageType = MessageType.fromCode(bytesBuffer.getShort());
            if (parsedMessageType != ONION_TUNNEL_CONNECT)
                throw new IllegalArgumentException("Not an ONION_TUNNEL_CONNECT message");

            val parsedRequestId = RequestId.wrap(bytesBuffer.getShort());

            val parsedPort = toUnsignedInt(bytesBuffer.getShort());
            bytesBuffer.position(bytesBuffer.position() + RESERVED); // skip reserved
            val parsedIpVersionOrdinal = (int) bytesBuffer.get();
            val parsedIpVersion = Nets.IPVersion.fromOrdinal(parsedIpVersionOrdinal);

            if (parsedIpVersion == null)
                throw new ProtoException("Failed to parse InetAddress version - unknown IP version");

            InetAddress parsedDestinationAddress;
            try {
                val rawParsedDestinationAddress = new byte[parsedIpVersion.bytes()];
                bytesBuffer.get(rawParsedDestinationAddress);

                parsedDestinationAddress = InetAddress.getByAddress(rawParsedDestinationAddress);
            } catch (UnknownHostException e) {
                throw new ProtoException("Failed to parse IP address", e);
            }

            val parsedSourceKeySize = toUnsignedInt(bytesBuffer.getShort());
            val rawParsedSourceKey = new byte[parsedSourceKeySize];
            bytesBuffer.get(rawParsedSourceKey);

            PublicKey parsedSourceKey;
            try {
                parsedSourceKey = parsePublicKey(rawParsedSourceKey);
            } catch (InvalidKeySpecException e) {
                throw new ProtoException("Failed to parse source key: " + Arrays.toString(rawParsedSourceKey), e);
            }

            val parsedHandshakeSize = toUnsignedInt(bytesBuffer.getShort());
            val parsedHandshake = new byte[parsedHandshakeSize];
            bytesBuffer.get(parsedHandshake);

            return new TunnelConnect(parsedRequestId, parsedDestinationAddress, parsedPort,
                parsedSourceKey, parsedHandshake);
        } catch (BufferUnderflowException | BufferOverflowException e) {
            throw new ProtoException("Failed to parse ONION_CONNECT message", e);
        }
    }

    @Override
    protected void writePayload(ByteBuffer messageBuffer) {
        messageBuffer.putShort(requestId.raw());

        messageBuffer.putShort((short) port);

        messageBuffer.position(messageBuffer.position() + RESERVED);
        val ipVersion = guessInetAddressVersion(destination);
        val ipVersionByte = (byte) ipVersion.ordinal();
        messageBuffer.put(ipVersionByte);

        messageBuffer.put(destination.getAddress());

        val sourceKeyBytes = sourceKey.getEncoded();
        messageBuffer.putShort((short) sourceKeyBytes.length);
        messageBuffer.put(sourceKeyBytes);

        messageBuffer.putShort((short) handshake.length);
        messageBuffer.put(handshake);
    }

    public InetSocketAddress socketDestination() {
        return new InetSocketAddress(destination, port);
    }
}
