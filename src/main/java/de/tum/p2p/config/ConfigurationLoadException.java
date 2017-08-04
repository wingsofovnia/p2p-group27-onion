package de.tum.p2p.config;

/**
 * Thrown by {@link Configurations} in case of unexpected errors
 * during configurations loading
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
public class ConfigurationLoadException extends RuntimeException {

    public ConfigurationLoadException(String message) {
        super(message);
    }

    public ConfigurationLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
