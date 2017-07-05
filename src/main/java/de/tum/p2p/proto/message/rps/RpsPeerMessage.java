package de.tum.p2p.proto.message.rps;


import de.tum.p2p.proto.ProtoException;
import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.proto.message.TypedMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.val;
import org.apache.commons.lang3.Validate;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

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

    private static final byte IPv4 = 0;
    private static final int IPv4_BYTES = 4;

    private static final byte IPv6 = 1;
    private static final int IPv6_BYTES = 16;

    @Getter
    private final short port;

    private final static byte[] RESERVED = new byte[Short.BYTES - 1]; // 1 byte for the V field (ip version)

    private final byte ipVer;

    @Getter
    private final InetAddress inetAddress;

    @Getter
    private final PublicKey hostkey;

    public RpsPeerMessage(short port, InetAddress inetAddress, PublicKey hostkey) {
        super(MessageType.RPS_PEER,
            Short.BYTES             // port
                + RESERVED.length       // RESERVED
                + Byte.BYTES            // ipVer
                + sizeOf(inetAddress)   // inetAddress
                + sizeOf(hostkey));     // hostkey

        Validate.isTrue(port > 0, "Port cant be negative or eq 0");

        this.port = port;
        this.hostkey = notNull(hostkey);

        this.inetAddress = notNull(inetAddress);
        this.ipVer = (inetAddress instanceof Inet4Address) ? IPv4 : IPv6;
    }

    public static RpsPeerMessage of(short port, InetAddress inetAddress, PublicKey hostkey) {
        return new RpsPeerMessage(port, inetAddress, hostkey);
    }

    public boolean isIPv4() {
        return ipVer == IPv4;
    }

    public boolean isIPv6() {
        return ipVer == IPv6;
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        typedMessageBuffer.putShort(port);

        for (val resByte : RESERVED) typedMessageBuffer.put(resByte);

        typedMessageBuffer.put(ipVer);
        for (val ipByte : inetAddress.getAddress()) typedMessageBuffer.put(ipByte);

        for (val keyByte : hostkey.getEncoded()) typedMessageBuffer.put(keyByte);

        return typedMessageBuffer;
    }

    public static RpsPeerMessage fromBytes(byte[] rawTypedMessage, String keyAlgorithm) {
        try {
            return fromBytes(rawTypedMessage, KeyFactory.getInstance(keyAlgorithm));
        } catch (NoSuchAlgorithmException e) {
            throw new ProtoException("Failed to factory KeyFactory", e);
        }
    }

    public static RpsPeerMessage fromBytes(byte[] rawTypedMessage, KeyFactory keyFactory) {
        val rawRpsPeerMessage = untype(rawTypedMessage, MessageType.RPS_PEER);

        val parsedPort = rawRpsPeerMessage.getShort();

        rawRpsPeerMessage.position(rawRpsPeerMessage.position() + RESERVED.length); // skip reserved

        val parsedIpVersion = ensureValidIpVersion(rawRpsPeerMessage.get());

        InetAddress parsedInetAddress;
        if (parsedIpVersion == IPv4) {
            val rawParsedInetAddress = new byte[IPv4_BYTES];
            rawRpsPeerMessage.get(rawParsedInetAddress);

            try {
                parsedInetAddress = Inet4Address.getByAddress(rawParsedInetAddress);
            } catch (UnknownHostException e) {
                throw new ProtoException("Failed to parse IPv4 address", e);
            }
        } else {
            val rawParsedInetAddress = new byte[IPv6_BYTES];
            rawRpsPeerMessage.get(rawParsedInetAddress);

            try {
                parsedInetAddress = Inet6Address.getByAddress(rawParsedInetAddress);
            } catch (UnknownHostException e) {
                throw new ProtoException("Failed to parse IPv6 address", e);
            }
        }

        val rawParsedHostKey = new byte[rawRpsPeerMessage.capacity() - rawRpsPeerMessage.position()];
        rawRpsPeerMessage.get(rawParsedHostKey);

        PublicKey parsedHostKey;
        try {
            parsedHostKey = keyFactory.generatePublic(new X509EncodedKeySpec(rawParsedHostKey));
        } catch (InvalidKeySpecException e) {
            throw new ProtoException("Failed to parse host key: " + Arrays.toString(rawParsedHostKey), e);
        }

        return new RpsPeerMessage(parsedPort, parsedInetAddress, parsedHostKey);
    }

    private static byte ensureValidIpVersion(byte ipVer) {
        if (ipVer != IPv4 && ipVer != IPv6)
            throw new IllegalArgumentException("IpVer is unknown. " +
                "Actual = " + Byte.toString(ipVer) + ", expected = " + Arrays.toString(new byte[]{IPv4, IPv6}));

        return ipVer;
    }

    private static int sizeOf(InetAddress inetAddress) {
        return inetAddress.getAddress().length;
    }

    private static int sizeOf(PublicKey hostkey) {
        return hostkey.getEncoded().length;
    }
}
