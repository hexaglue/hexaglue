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

package io.hexaglue.core.plugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared storage for plugin outputs.
 *
 * <p>Allows plugins to share data with other plugins that depend on them.
 * Each plugin's outputs are keyed by the plugin ID and output key.
 */
final class PluginOutputStore {

    private final Map<String, Map<String, Object>> outputs = new ConcurrentHashMap<>();

    /**
     * Stores an output value for a plugin.
     *
     * @param pluginId the plugin storing the output
     * @param key the output key
     * @param value the value to store
     */
    void set(String pluginId, String key, Object value) {
        outputs.computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    /**
     * Retrieves an output value from a plugin.
     *
     * @param pluginId the plugin that stored the output
     * @param key the output key
     * @param type the expected type
     * @param <T> the value type
     * @return the output value, or empty if not found or wrong type
     */
    // Suppressed: safe cast to T after type.isInstance() check guarantees the value is of type T
    @SuppressWarnings("unchecked")
    <T> Optional<T> get(String pluginId, String key, Class<T> type) {
        Map<String, Object> pluginOutputs = outputs.get(pluginId);
        if (pluginOutputs == null) {
            return Optional.empty();
        }

        Object value = pluginOutputs.get(key);
        if (value == null) {
            return Optional.empty();
        }

        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }

        return Optional.empty();
    }

    /**
     * Retrieves all outputs stored by a plugin.
     *
     * @param pluginId the plugin that stored the outputs
     * @return an immutable copy of all outputs, or empty map if none
     */
    Map<String, Object> getAll(String pluginId) {
        Map<String, Object> pluginOutputs = outputs.get(pluginId);
        return pluginOutputs != null ? Map.copyOf(pluginOutputs) : Map.of();
    }
}
