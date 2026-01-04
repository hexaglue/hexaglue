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

package io.hexaglue.core.engine;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the HexaGlue engine.
 *
 * @param sourceRoots directories containing Java source files to analyze
 * @param classpathEntries classpath entries for type resolution
 * @param javaVersion the Java version (e.g., 21)
 * @param basePackage the base package to analyze (types outside are ignored)
 * @param outputDirectory directory for generated sources (null to skip plugin execution)
 * @param pluginConfigs plugin configurations keyed by plugin ID
 * @param options additional options (key-value pairs)
 */
public record EngineConfig(
        List<Path> sourceRoots,
        List<Path> classpathEntries,
        int javaVersion,
        String basePackage,
        Path outputDirectory,
        Map<String, Map<String, Object>> pluginConfigs,
        Map<String, Object> options) {

    /**
     * Creates a minimal configuration for testing (no plugin execution).
     */
    public static EngineConfig minimal(Path sourceRoot, String basePackage) {
        return new EngineConfig(List.of(sourceRoot), List.of(), 21, basePackage, null, Map.of(), Map.of());
    }

    /**
     * Creates a configuration with plugin execution enabled.
     */
    public static EngineConfig withPlugins(
            Path sourceRoot, String basePackage, Path outputDirectory, Map<String, Map<String, Object>> pluginConfigs) {
        return new EngineConfig(
                List.of(sourceRoot), List.of(), 21, basePackage, outputDirectory, pluginConfigs, Map.of());
    }

    /**
     * Returns true if plugin execution is enabled.
     */
    public boolean pluginsEnabled() {
        return outputDirectory != null;
    }
}
