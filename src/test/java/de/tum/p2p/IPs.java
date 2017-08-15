package de.tum.p2p;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;

public final class IPs {

    private static final ThreadLocalRandom rand = ThreadLocalRandom.current();

    private static final String[] IPv6_SAMPLE_ADDRESSES = new String[] {
        "2001::cafe:3f22",
        "1080:0:0:0:8:800:200C:417A",
        "::FFFF:129.144.52.38",
        "1080::8:800:200C:417A",
        "2001::cafe:9676"
    };

    private IPs() {
        throw new AssertionError("No instance for you");
    }

    public static InetAddress randIPv6() {
        try {
            return Inet6Address.getByName(IPv6_SAMPLE_ADDRESSES[rand.nextInt(0, IPv6_SAMPLE_ADDRESSES.length)]);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static InetAddress randIPv4() {
        try {
            return Inet4Address.getByName(
                rand.nextInt(256) + "." +
                rand.nextInt(256) + "." +
                rand.nextInt(256) + "." +
                rand.nextInt(256)
            );
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static InetAddress randIP() {
        return ThreadLocalRandom.current().nextBoolean() ? randIPv4() : randIPv6();
    }
}
