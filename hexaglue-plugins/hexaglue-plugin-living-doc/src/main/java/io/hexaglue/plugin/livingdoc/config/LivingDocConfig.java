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

package io.hexaglue.plugin.livingdoc.config;

import io.hexaglue.spi.plugin.PluginConfig;

/**
 * Centralized configuration for the Living Documentation plugin.
 *
 * <p>Encapsulates all plugin configuration options in a single immutable record,
 * avoiding repeated lookups into {@link PluginConfig} throughout the plugin code.</p>
 *
 * @param outputDir the output directory for documentation files
 * @param generateDiagrams whether to generate Mermaid diagrams
 * @param maxPropertiesInDiagram maximum number of properties to show per class in diagrams
 * @param includeDebugSections whether to include debug information sections
 * @since 5.0.0
 */
public record LivingDocConfig(
        String outputDir, boolean generateDiagrams, int maxPropertiesInDiagram, boolean includeDebugSections) {

    /** Default output directory. */
    static final String DEFAULT_OUTPUT_DIR = "living-doc";

    /** Default maximum properties shown in diagrams. */
    static final int DEFAULT_MAX_PROPERTIES_IN_DIAGRAM = 5;

    /**
     * Creates a configuration with default values.
     *
     * @return the default configuration
     */
    public static LivingDocConfig defaults() {
        return new LivingDocConfig(DEFAULT_OUTPUT_DIR, true, DEFAULT_MAX_PROPERTIES_IN_DIAGRAM, true);
    }

    /**
     * Creates a configuration from a {@link PluginConfig}.
     *
     * @param pluginConfig the plugin configuration source
     * @return a configuration populated from the plugin config with defaults for missing values
     */
    public static LivingDocConfig from(PluginConfig pluginConfig) {
        return new LivingDocConfig(
                pluginConfig.getString("outputDir", DEFAULT_OUTPUT_DIR),
                pluginConfig.getBoolean("generateDiagrams", true),
                pluginConfig.getInteger("maxPropertiesInDiagram").orElse(DEFAULT_MAX_PROPERTIES_IN_DIAGRAM),
                pluginConfig.getBoolean("includeDebugSections", true));
    }
}
