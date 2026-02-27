/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.spi.plugin;

import java.util.Map;
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
     * Returns an integer map configuration value.
     *
     * <p>Useful for structured configuration like exception-to-status-code mappings.
     *
     * @param key the configuration key
     * @return the map, or empty if not configured
     * @since 3.1.0
     */
    default Optional<Map<String, Integer>> getIntegerMap(String key) {
        return Optional.empty();
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
