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
}
