package de.tum.p2p.config;

import lombok.val;
import org.ini4j.Wini;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.apache.commons.lang3.ArrayUtils.toObject;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * The {@code Configurations} class represents a persistent set of
 * properties located in an application config file of .INI format.
 * The {@code Configurations} can loaded from a stream or {@link File}.
 * <p>
 * Example:
 * <pre>
 *     // Loads properties from config.ini
 *     Configurations cfg = Configurations.load("config.ini");
 *
 *     // Gets a File from HOSTKEY_KEY property
 *     Optional&lt;File&gt; hostKeyFile = cfg.hostKey();
 * </pre>
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class Configurations {

    private static final String DEFAULT_CONFIG_FILEPATH = "./config.ini";

    private static final String NO_SECTION_SELECTION = "?";
    private static final String HOSTKEY_KEY = "HOSTKEY";

    private static final String ONION_SECTION_SELECTION = "ONION";
    private static final String ONION_IP_ADDRESS_KEY = "api_address";
    private static final String ONION_PORT_KEY = "port";
    private static final String ONION_MIN_HOPS_KEY = "min_hops";
    private static final String ONION_DATUM_HMAC_SECRET = "hmac_secret";

    private final Wini appConfig;

    private Configurations() {
        throw new AssertionError("No de.tum.p2p.config.Configurations instance for you");
    }

    private Configurations(InputStream configFileStream) {
        try {
            this.appConfig = new Wini(configFileStream);
        } catch (IOException e) {
            throw new ConfigurationLoadException("Failed to load configurations", e);
        }
    }

    /**
     * Loads {@link Configurations#HOSTKEY_KEY} property's value and wraps into {@link File}
     *
     * @return {@link Configurations#HOSTKEY_KEY} property as {@link File}
     */
    public Optional<File> hostKey() {
        val hostKeyPathStr = appConfig.get(NO_SECTION_SELECTION, HOSTKEY_KEY, String.class);

        if (isBlank(hostKeyPathStr))
            return Optional.empty();

        return Optional.of(new File(hostKeyPathStr));
    }

    /**
     * Loads {@link Configurations#ONION_IP_ADDRESS_KEY} property's value from
     * {@link Configurations#ONION_SECTION_SELECTION} ini section and wraps into {@link InetAddress}
     *
     * @return {@link Configurations#ONION_IP_ADDRESS_KEY} property as {@link InetAddress}
     */
    public Optional<InetAddress> onionInetAddress() {
        val onionInetAddressStr = appConfig.get(ONION_SECTION_SELECTION, ONION_IP_ADDRESS_KEY, String.class);

        if (isBlank(onionInetAddressStr))
            return Optional.empty();

        try {
            return Optional.of(InetAddress.getByName(onionInetAddressStr));
        } catch (UnknownHostException e) {
            throw new ConfigurationLoadException("Failed to parse Onion Ip Address", e);
        }
    }

    /**
     * Loads {@link Configurations#ONION_PORT_KEY} property's value from
     * {@link Configurations#ONION_SECTION_SELECTION} ini section
     *
     * @return {@link Configurations#ONION_IP_ADDRESS_KEY} property
     */
    public Optional<Integer> onionPort() {
        val onionPortStr = appConfig.get(ONION_SECTION_SELECTION, ONION_PORT_KEY, String.class);

        if (isBlank(onionPortStr))
            return Optional.empty();

        return Optional.ofNullable(Integer.valueOf(onionPortStr));
    }

    /**
     * Loads {@link Configurations#ONION_MIN_HOPS_KEY} property's value from
     * {@link Configurations#ONION_SECTION_SELECTION} ini section
     *
     * @return {@link Configurations#ONION_MIN_HOPS_KEY} property
     */
    public Optional<Integer> onionMinHops() {
        val onionHopsStr = appConfig.get(ONION_SECTION_SELECTION, ONION_MIN_HOPS_KEY, String.class);

        if (isBlank(onionHopsStr))
            return Optional.empty();

        return Optional.ofNullable(Integer.valueOf(onionHopsStr));
    }

    /**
     * Loads {@link Configurations#ONION_DATUM_HMAC_SECRET} property's value from
     * {@link Configurations#ONION_SECTION_SELECTION} ini section
     *
     * @return {@link Configurations#ONION_DATUM_HMAC_SECRET} property
     */
    public Optional<Byte[]> onionHmacSecret() {
        val onionHmacSecret = appConfig.get(ONION_SECTION_SELECTION, ONION_DATUM_HMAC_SECRET, String.class);

        if (isBlank(onionHmacSecret))
            return Optional.empty();

        return Optional.ofNullable(toObject(getBytesUtf8(onionHmacSecret)));
    }

    /**
     * Loads Configurations from default location, specified by
     * {@link Configurations#DEFAULT_CONFIG_FILEPATH} constant.
     *
     * @return a {@link Configurations} instance populated with
     * settings from default location config
     */
    public static Configurations load() {
        return load(DEFAULT_CONFIG_FILEPATH);
    }

    public static Configurations load(String configFilePath) {
        return load(new File(configFilePath));
    }

    public static Configurations load(File configFile) {
        try {
            return load(new FileInputStream(configFile));
        } catch (FileNotFoundException e) {
            throw new ConfigurationLoadException("Failed to load configurations", e);
        }
    }

    public static Configurations load(InputStream configFileStream) {
        return new Configurations(configFileStream);
    }
}
