package de.tum.p2p.proto.message.onion.forwarding;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.util.Nets.IPVersion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.val;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static de.tum.p2p.util.ByteArrayPaddings.pad;
import static de.tum.p2p.util.Handshakes.notOversizedHadshake;
import static de.tum.p2p.util.Keys.notOversizedKey;
import static de.tum.p2p.util.Keys.parsePublicKey;
import static de.tum.p2p.util.Nets.guessInetAddressVersion;
import static de.tum.p2p.util.Nets.validPort;
import static java.lang.Short.toUnsignedInt;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@code TunnelExtendMessage} represents and intention to extend the tunnel
 * further. It contains the address of the next onion H2 along with the first
 * half of the Diffie-Hellman handshake.
 */
@Accessors(fluent = true)
@ToString @EqualsAndHashCode(callSuper = true)
public class TunnelExtendMessage extends OnionMessage {

    private static final AtomicInteger messageIdCounter = new AtomicInteger();

    @Getter
    private final TunnelId tunnelId;

    @Getter
    private final int requestId;

    @Getter // unsigned short
    private final int port;

    private final static byte[] RESERVED = new byte[Short.BYTES - 1]; // 1 byte for the V field (ip version)

    @Getter
    private final InetAddress destination;

    @Getter
    private final PublicKey sourceKey;

    @Getter
    private final byte[] handshake;

    protected TunnelExtendMessage(TunnelId tunnelId, int requestId, InetAddress dest, int port,
                                  PublicKey sourceKey, byte[] handshake) {
        super(MessageType.ONION_TUNNEL_EXTEND);

        this.tunnelId = tunnelId;
        this.requestId = requestId;
        this.destination = notNull(dest);
        this.port = validPort(port);
        this.sourceKey = notOversizedKey(sourceKey);
        this.handshake = notOversizedHadshake(handshake);
    }

    public TunnelExtendMessage(TunnelId tunnelId, InetAddress dest, int port, PublicKey sourceKey, byte[] handshake) {
        this(tunnelId, messageIdCounter.getAndIncrement(), dest, port, sourceKey, handshake);
    }

    public static TunnelExtendMessage of(TunnelId tunnelId, InetAddress dest, int port, PublicKey sourceKey, byte[] handshake) {
        return new TunnelExtendMessage(tunnelId, dest, port, sourceKey, handshake);
    }

    public TunnelExtendMessage(TunnelId tunnelId, InetAddress dest, int port, PublicKey sourceKey, ByteBuffer handshake) {
        this(tunnelId, messageIdCounter.getAndIncrement(), dest, port, sourceKey, handshake.array());
    }

    public static TunnelExtendMessage of(TunnelId tunnelId, InetAddress dest, int port, PublicKey sourceKey, ByteBuffer handshake) {
        return new TunnelExtendMessage(tunnelId, dest, port, sourceKey, handshake);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        typedMessageBuffer.putInt(tunnelId.raw());
        typedMessageBuffer.putInt(requestId);

        typedMessageBuffer.putShort((short) port);
        for (val rb : RESERVED) typedMessageBuffer.put(rb);

        val ipVersion = guessInetAddressVersion(destination);
        val ipVersionByte = (byte) ipVersion.ordinal();
        typedMessageBuffer.put(ipVersionByte);
        for (val ib : destination.getAddress()) typedMessageBuffer.put(ib);

        val sourceKeyBytes = sourceKey.getEncoded();
        typedMessageBuffer.putShort((short) sourceKeyBytes.length);
        for (val kb : sourceKeyBytes) typedMessageBuffer.put(kb);

        typedMessageBuffer.putShort((short) handshake.length);
        val freeSpaceLeft = typedMessageBuffer.capacity() - typedMessageBuffer.position();
        val paddedHandshake = pad(handshake, freeSpaceLeft);
        for (val hb : paddedHandshake) typedMessageBuffer.put(hb);

        return typedMessageBuffer;
    }

    public static TunnelExtendMessage fromBytes(byte[] rawTypedMessage) {
        val rawTunnelExtendMessage = untype(rawTypedMessage, MessageType.ONION_TUNNEL_EXTEND);

        val parsedTunnelId = rawTunnelExtendMessage.getInt();
        val parsedRequestId = rawTunnelExtendMessage.getInt();

        val parsedPort = toUnsignedInt(rawTunnelExtendMessage.getShort());

        rawTunnelExtendMessage.position(rawTunnelExtendMessage.position() + RESERVED.length); // skip reserved

        val parsedIpVersionOrdinal = (int) rawTunnelExtendMessage.get();
        val parsedIpVersion = IPVersion.fromOrdinal(parsedIpVersionOrdinal);

        if (parsedIpVersion == null)
            throw new ProtoException("Failed to parse InetAddress version - unknown IP version");

        InetAddress parsedDestinationAddress;
        try {
            val rawParsedDestinationAddress = new byte[parsedIpVersion.bytes()];
            rawTunnelExtendMessage.get(rawParsedDestinationAddress);

            parsedDestinationAddress = InetAddress.getByAddress(rawParsedDestinationAddress);
        } catch (UnknownHostException e) {
            throw new ProtoException("Failed to parse IP address", e);
        }

        val parsedSourceKeySize = toUnsignedInt(rawTunnelExtendMessage.getShort());
        val rawParsedSourceKey = new byte[parsedSourceKeySize];
        rawTunnelExtendMessage.get(rawParsedSourceKey);

        PublicKey parsedSourceKey;
        try {
            parsedSourceKey = parsePublicKey(rawParsedSourceKey);
        } catch (InvalidKeySpecException e) {
            throw new ProtoException("Failed to parse source key: " + Arrays.toString(rawParsedSourceKey), e);
        }

        val parsedHandshakeSize = toUnsignedInt(rawTunnelExtendMessage.getShort());
        val parsedHandshake = new byte[parsedHandshakeSize];
        rawTunnelExtendMessage.get(parsedHandshake);

        return new TunnelExtendMessage(TunnelId.wrap(parsedTunnelId), parsedRequestId,
            parsedDestinationAddress, parsedPort,
            parsedSourceKey, parsedHandshake);
    }

    public InetSocketAddress destinationSocketAddress() {
        return new InetSocketAddress(destination(), port);
    }
}
