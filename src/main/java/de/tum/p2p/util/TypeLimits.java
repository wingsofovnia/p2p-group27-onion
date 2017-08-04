package de.tum.p2p.util;

import java.math.BigInteger;

import static java.lang.Math.abs;

/**
 * {@code TypeLimits} contains definition of unsigned types, missed in Java
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public final class TypeLimits {

    private TypeLimits() {
        throw new AssertionError("No instance for you!");
    }

    public static int USHRT_MAX = Short.MAX_VALUE + abs(Short.MIN_VALUE);

    public static long UINT_MAX = Integer.MAX_VALUE + abs(Integer.MIN_VALUE);

    public static BigInteger ULONG_MAX = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(abs(Long.MIN_VALUE)));
}
