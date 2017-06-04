package de.tum.p2p.onion.forwarding.config;

import lombok.val;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ConfigurationsTest {

    private static final String GOOD_HOSTKEY_CFG = "HOSTKEY = ../hostkey.pem";
    private static final File GOOD_HOSTKEY_VAL = new File("../hostkey.pem");
    private static final List<String> BAD_HOSTKEY_CFGS = new ArrayList<String>() {{
        add("HOSTKEY =");
        add("HOSTKEZZ = ../hostkey.pem");
    }};

    private static final String GOOD_ONION_API_ADDRESS_CFG = "[ONION]\n" + "api_address = " + InetAddress.getLoopbackAddress().getHostAddress();
    private static final InetAddress GOOD_ONION_API_ADDRESS_VAL = InetAddress.getLoopbackAddress();
    private static final List<String> BAD_ONION_API_ADDRESS_CFGS = new ArrayList<String>() {{
        add("[ONION]\n" + "api_address = ");
        add("[ONION]\n" + "api_addrezz = 127.0.0.1");
    }};

    private static final String GOOD_ONION_PORT_CFG = "[ONION]\n" + "port = 80";
    private static final Integer GOOD_ONION_PORT_VAL = 80;
    private static final List<String> BAD_ONION_PORT_CFGS = new ArrayList<String>() {{
        add("[ONION]\n" + "port =");
        add("[ONION]\n" + "pozt = 80");
    }};

    private static final String GOOD_ONION_MIN_HOPS_CFG = "[ONION]\n" + "min_hops = 2";
    private static final Integer GOOD_ONION_MIN_HOPS_VAL = 2;
    private static final List<String> BAD_ONION_MIN_HOPS_CFGS = new ArrayList<String>() {{
        add("[ONION]\n" + "min_hops =");
        add("[ONION]\n" + "min_hopz = 2");
    }};

    @Test(expected = ConfigurationLoadException.class)
    public void throwExceptionOnLoadingMissingConfigFile() {
        Configurations.load("NOSUCHFILELOL");
    }

    @Test
    public void loadsGoodHostkeyCfgCorrectly() {
        val cfg = Configurations.load(configFileStreamFromString(GOOD_HOSTKEY_CFG));
        val goodHostKey = cfg.hostKey();

        assertTrue(goodHostKey.isPresent());
        assertEquals(GOOD_HOSTKEY_VAL, goodHostKey.get());
    }

    @Test
    public void emptyOptionalOnBadHostkeyCfg() {
        BAD_HOSTKEY_CFGS.forEach(bc -> {
            val cfg = Configurations.load(configFileStreamFromString(bc));
            val goodHostKey = cfg.hostKey();

            assertFalse(goodHostKey.isPresent());
        });
    }

    @Test
    public void loadsGoodInetAddressCfgCorrectly() {
        val cfg = Configurations.load(configFileStreamFromString(GOOD_ONION_API_ADDRESS_CFG));
        val inetAddress = cfg.onionInetAddress();

        assertTrue(inetAddress.isPresent());
        assertEquals(GOOD_ONION_API_ADDRESS_VAL, inetAddress.get());
    }

    @Test
    public void emptyOptionalOnBadInetAddressCfg() {
        BAD_ONION_API_ADDRESS_CFGS.forEach(bc -> {
            val cfg = Configurations.load(configFileStreamFromString(bc));
            val inetAddress = cfg.onionInetAddress();

            assertFalse(inetAddress.isPresent());
        });
    }

    @Test
    public void loadsGoodPortCfgCorrectly() {
        val cfg = Configurations.load(configFileStreamFromString(GOOD_ONION_PORT_CFG));
        val onionPort = cfg.onionPort();

        assertTrue(onionPort.isPresent());
        assertEquals(GOOD_ONION_PORT_VAL, onionPort.get());
    }

    @Test
    public void emptyOptionalOnBadPortCfg() {
        BAD_ONION_PORT_CFGS.forEach(bc -> {
            val cfg = Configurations.load(configFileStreamFromString(bc));
            val onionPort = cfg.onionPort();

            assertFalse(onionPort.isPresent());
        });
    }

    @Test
    public void loadsGoodMinHopsCfgCorrectly() {
        val cfg = Configurations.load(configFileStreamFromString(GOOD_ONION_MIN_HOPS_CFG));
        val onionMinHops = cfg.onionMinHops();

        assertTrue(onionMinHops.isPresent());
        assertEquals(GOOD_ONION_MIN_HOPS_VAL, onionMinHops.get());
    }

    @Test
    public void emptyOptionalOnBadMinHopsCfg() {
        BAD_ONION_MIN_HOPS_CFGS.forEach(bc -> {
            val cfg = Configurations.load(configFileStreamFromString(bc));
            val onionMinHops = cfg.onionMinHops();

            assertFalse(onionMinHops.isPresent());
        });
    }

    private static InputStream configFileStreamFromString(String configFileContent) {
        return new ByteArrayInputStream(configFileContent.getBytes(StandardCharsets.UTF_8));
    }
}
