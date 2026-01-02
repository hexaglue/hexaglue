package io.hexaglue.core.plugin;

import io.hexaglue.spi.plugin.PluginConfig;
import java.util.Map;
import java.util.Optional;

/**
 * {@link PluginConfig} implementation backed by a Map.
 */
final class MapPluginConfig implements PluginConfig {

    private final Map<String, Object> values;

    MapPluginConfig(Map<String, Object> values) {
        this.values = values;
    }

    @Override
    public Optional<String> getString(String key) {
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(value));
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Boolean b) {
            return Optional.of(b);
        }
        return Optional.of(Boolean.parseBoolean(String.valueOf(value)));
    }

    @Override
    public Optional<Integer> getInteger(String key) {
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Number n) {
            return Optional.of(n.intValue());
        }
        try {
            return Optional.of(Integer.parseInt(String.valueOf(value)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
