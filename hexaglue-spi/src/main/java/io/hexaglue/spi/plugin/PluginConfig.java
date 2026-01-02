package io.hexaglue.spi.plugin;

import java.util.Optional;

/**
 * Type-safe access to plugin configuration.
 *
 * <p>Configuration values are read from {@code hexaglue.yaml} under the plugin's namespace.
 */
public interface PluginConfig {

    /**
     * Returns a string configuration value.
     *
     * @param key the configuration key
     * @return the value, or empty if not configured
     */
    Optional<String> getString(String key);

    /**
     * Returns a boolean configuration value.
     *
     * @param key the configuration key
     * @return the value, or empty if not configured
     */
    Optional<Boolean> getBoolean(String key);

    /**
     * Returns an integer configuration value.
     *
     * @param key the configuration key
     * @return the value, or empty if not configured
     */
    Optional<Integer> getInteger(String key);

    /**
     * Returns a string value with a default.
     *
     * @param key the configuration key
     * @param defaultValue the default value if not configured
     * @return the configured value or the default
     */
    default String getString(String key, String defaultValue) {
        return getString(key).orElse(defaultValue);
    }

    /**
     * Returns a boolean value with a default.
     *
     * @param key the configuration key
     * @param defaultValue the default value if not configured
     * @return the configured value or the default
     */
    default boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key).orElse(defaultValue);
    }
}
