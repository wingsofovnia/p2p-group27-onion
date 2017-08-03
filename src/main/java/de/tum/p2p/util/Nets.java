package de.tum.p2p.util;

import de.tum.p2p.onion.OnionException;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.val;
import org.apache.commons.lang3.Validate;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.stream.IntStream;

import static de.tum.p2p.util.TypeLimits.USHRT_MAX;

/**
 * {@code Nets} contains useful utility methods for validating
 * network related stuff, generating random ports and ip version
 * detection.
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public final class Nets {

    private static final int UNPRIVILEGED_PORT_LIMIT = 1024;

    public enum IPVersion {
        IPv4(4), IPv6(16);

        private final int bytes;

        IPVersion(int bytes) {
            this.bytes = bytes;
        }

        public int bytes() {
            return bytes;
        }

        public static IPVersion fromOrdinal(int ordinal) {
            if (ordinal < 0)
                throw new IllegalArgumentException("Invalid ordinal, must be > 0");

            val myVals = IPVersion.values();

            if (ordinal > myVals.length)
                return null;

            return myVals[ordinal];
        }
    }

    private Nets() {
        throw new AssertionError("No instance for you");
    }

    public static int validPort(int port, String msg) {
        Validate.inclusiveBetween(0, USHRT_MAX, port, msg);
        return port;
    }

    public static int validPort(int port) {
        Validate.inclusiveBetween(0, USHRT_MAX, port);
        return port;
    }

    public static int validPort(short port, String msg) {
        return validPort(Short.toUnsignedInt(port), msg);
    }

    public static int validPort(short port) {
        return validPort(Short.toUnsignedInt(port));
    }

    public static IPVersion guessInetAddressVersion(InetAddress address) {
        if(address instanceof Inet6Address){
            return IPVersion.IPv6;
        } else if (address instanceof Inet4Address) {
            return IPVersion.IPv4;
        } else {
            throw new IllegalArgumentException("Unknown InetAddress version");
        }
    }

    public static InetAddress localhost() {
        try {
            return InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new OnionException(e);
        }
    }

    public static int randUnprivilegedPort() {
        return ThreadLocalRandom.current().nextInt(UNPRIVILEGED_PORT_LIMIT, USHRT_MAX);
    }

    public static IntStream randUnprivilegedPort(int amount) {
        val randPorts = new HashSet<Integer>();
        while (randPorts.size() != amount)
            randPorts.add(randUnprivilegedPort());

        return randPorts.stream().mapToInt(v -> v);
    }
}
