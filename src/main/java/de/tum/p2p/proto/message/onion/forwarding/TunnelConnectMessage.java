package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.util.Nets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
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
 * {@code TunnelConnectMessage} message receives an Onion that is
 * required to connect to next peer and extend the tunnel by 1.
 * <p>
 * This message is usually create by decryption from by
 * {@link TunnelConnectEncryptedMessage#toConnectMessage()}.
 * <p>
 * Packet structure:
 * <pre>
 * |-------------|-------------|
 * |     LP*     |   CONNECT   |
 * |---------------------------|
 * |         TUNNEL ID         |
 * |---------------------------|
 * |    REQ ID   |     PORT    |
 * |-------------|-------------|
 * |  RSVD |IPVER|     IP      |
 * |-------------|-------------|
 * |   IP(CONT)  |   KEY LEN   |
 * |-------------|-------------|
 * |          SRC KEY          |
 * |-------------|-------------|
 * |   HS1 LENG  |  HANDSHAKE  |
 * |-------------|-------------|
 * |     HANDSHAKE(CONT..)     |
 * |-------------|-------------|
 * </pre>
 * *LP - Frame Length Prefixing is a Netty's responsibility and is not included
 * in the message class itself
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class TunnelConnectMessage extends TraceableTypedTunnelMessage {

    @Getter // unsigned short
    private final int port;

    private final static byte[] RESERVED = new byte[Short.BYTES - 1]; // 1 byte for the V field (ip version)

    @Getter
    private final InetAddress destination;

    @Getter
    private final PublicKey sourceKey;

    @Getter
    private final byte[] handshake;

    public TunnelConnectMessage(TunnelId tunnelId, RequestId requestId, InetAddress dest, int port,
                                PublicKey sourceKey, byte[] handshake) {
        super(tunnelId, requestId, ONION_TUNNEL_CONNECT);
        this.destination = notNull(dest);
        this.port = validPort(port);
        this.sourceKey = notOversizedKey(sourceKey);
        this.handshake = notOversizedHadshake(handshake);
    }

    public TunnelConnectMessage(TunnelId tunnelId, InetAddress dest, int port, PublicKey sourceKey, byte[] handshake) {
        this(tunnelId, null, dest, port, sourceKey, handshake);
    }

    public static TunnelConnectMessage fromBytes(byte[] bytes) {
        val bytesBuffer = ByteBuffer.wrap(bytes);
        val rawTraceableTypedTunnelMsg = TraceableTypedTunnelMessage.fromBytes(bytesBuffer, ONION_TUNNEL_CONNECT);

        val parsedTunnelId = rawTraceableTypedTunnelMsg.tunnelId();
        val parsedRequestId = rawTraceableTypedTunnelMsg.requestId();

        val parsedPort = toUnsignedInt(bytesBuffer.getShort());

        bytesBuffer.position(bytesBuffer.position() + RESERVED.length); // skip reserved

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

        return new TunnelConnectMessage(parsedTunnelId, parsedRequestId, parsedDestinationAddress, parsedPort,
            parsedSourceKey, parsedHandshake);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer messageBuffer) {
        messageBuffer.putShort((short) port);
        messageBuffer.put(RESERVED);

        val ipVersion = guessInetAddressVersion(destination);
        val ipVersionByte = (byte) ipVersion.ordinal();
        messageBuffer.put(ipVersionByte);
        messageBuffer.put(destination.getAddress());

        val sourceKeyBytes = sourceKey.getEncoded();
        messageBuffer.putShort((short) sourceKeyBytes.length);
        messageBuffer.put(sourceKeyBytes);

        messageBuffer.putShort((short) handshake.length);
        messageBuffer.put(handshake);

        return messageBuffer;
    }

    public InetSocketAddress socketDestination() {
        return new InetSocketAddress(destination, port);
    }

    public TunnelExtendMessage deriveExtendMessage() {
        return new TunnelExtendMessage(tunnelId, requestId, sourceKey, handshake);
    }
}
