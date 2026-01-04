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
