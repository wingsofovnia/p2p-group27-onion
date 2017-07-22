package de.tum.p2p.proto.message.rps;


import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.proto.message.TypedMessage;
import de.tum.p2p.util.Nets.IPVersion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.val;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static de.tum.p2p.util.Keys.parsePublicKey;
import static de.tum.p2p.util.Nets.guessInetAddressVersion;
import static de.tum.p2p.util.Nets.validPort;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * The {@code RpsPeerMessage} is sent by the remote RPS module as a
 * response to the {@link RpsQueryMessage} message.
 * <p>
 * It contains the peer identity and the network address of a peer
 * which is selected by RPS at random. The address may be either IPv4
 * or IPv6
 */
@Accessors(fluent = true)
@ToString @EqualsAndHashCode(callSuper = true)
public class RpsPeerMessage extends TypedMessage {

    private static final byte IPv4_FLAG_BYTE = 0;
    private static final byte IPv6_FLAG_BYTE = 1;

    @Getter
    private final int port;

    private final static byte[] RESERVED = new byte[Short.BYTES - 1]; // 1 byte for the V field (ip version)

    @Getter
    private final InetAddress inetAddress;

    @Getter
    private final PublicKey hostkey;

    public RpsPeerMessage(int port, InetAddress inetAddress, PublicKey hostkey) {
        super(MessageType.RPS_PEER,
            Short.BYTES             // port
                + RESERVED.length       // RESERVED
                + Byte.BYTES            // ipVer
                + sizeOf(inetAddress)   // inetAddress
                + sizeOf(hostkey));     // hostkey

        this.port = validPort(port);
        this.hostkey = notNull(hostkey);

        this.inetAddress = notNull(inetAddress);
    }

    public static RpsPeerMessage of(int port, InetAddress inetAddress, PublicKey hostkey) {
        return new RpsPeerMessage(port, inetAddress, hostkey);
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        typedMessageBuffer.putShort((short) port);
        typedMessageBuffer.put(RESERVED);

        val ipVersion = guessInetAddressVersion(inetAddress);
        val ipVersionByte = ipVersion == IPVersion.IPv4 ? IPv4_FLAG_BYTE : IPv6_FLAG_BYTE;
        typedMessageBuffer.put(ipVersionByte);
        typedMessageBuffer.put(inetAddress.getAddress());

        typedMessageBuffer.put(hostkey.getEncoded());

        return typedMessageBuffer;
    }

    public static RpsPeerMessage fromBytes(byte[] rawTypedMessage) {
        val rawRpsPeerMessage = untype(rawTypedMessage, MessageType.RPS_PEER);

        val parsedPort = Short.toUnsignedInt(rawRpsPeerMessage.getShort());

        rawRpsPeerMessage.position(rawRpsPeerMessage.position() + RESERVED.length); // skip reserved

        val parsedIpVersion = ensureValidIpVersionByte(rawRpsPeerMessage.get());

        InetAddress parsedInetAddress;
        try {
            val parsedIpSize = parsedIpVersion == IPv4_FLAG_BYTE ? IPVersion.IPv4.bytes() : IPVersion.IPv6.bytes();
            val rawParsedInetAddress = new byte[parsedIpSize];
            rawRpsPeerMessage.get(rawParsedInetAddress);

            parsedInetAddress = InetAddress.getByAddress(rawParsedInetAddress);
        } catch (UnknownHostException e) {
            throw new ProtoException("Failed to parse IP address", e);
        }

        val rawParsedHostKey = new byte[rawRpsPeerMessage.capacity() - rawRpsPeerMessage.position()];
        rawRpsPeerMessage.get(rawParsedHostKey);

        PublicKey parsedHostKey;
        try {
            parsedHostKey = parsePublicKey(rawParsedHostKey);
        } catch (InvalidKeySpecException e) {
            throw new ProtoException("Failed to parse host key: " + Arrays.toString(rawParsedHostKey), e);
        }

        return new RpsPeerMessage(parsedPort, parsedInetAddress, parsedHostKey);
    }

    private static byte ensureValidIpVersionByte(byte ipVerByte) {
        if (ipVerByte != IPv4_FLAG_BYTE && ipVerByte != IPv6_FLAG_BYTE)
            throw new IllegalArgumentException("IpVer is unknown. " +
                "Actual = " + Byte.toString(ipVerByte) + ", expected = " +
                Arrays.toString(new byte[]{IPv4_FLAG_BYTE, IPv6_FLAG_BYTE}));

        return ipVerByte;
    }

    private static int sizeOf(InetAddress inetAddress) {
        return inetAddress.getAddress().length;
    }

    private static int sizeOf(PublicKey hostkey) {
        return hostkey.getEncoded().length;
    }
}
