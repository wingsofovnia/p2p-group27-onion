package de.tum.p2p.config;

/**
 * Thrown by {@link Configurations} in case of unexpected errors
 * during configurations loading
 */
public class ConfigurationLoadException extends RuntimeException {

    public ConfigurationLoadException(String message) {
        super(message);
    }

    public ConfigurationLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
