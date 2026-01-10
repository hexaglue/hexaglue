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

import io.hexaglue.spi.generation.PluginCategory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * @param classificationProfile the classification profile name (e.g., "default", "strict",
 *     "repository-aware") or null for legacy behavior
 * @param enabledCategories plugin categories to execute (null or empty for all categories)
 */
public record EngineConfig(
        List<Path> sourceRoots,
        List<Path> classpathEntries,
        int javaVersion,
        String basePackage,
        Path outputDirectory,
        Map<String, Map<String, Object>> pluginConfigs,
        Map<String, Object> options,
        String classificationProfile,
        Set<PluginCategory> enabledCategories) {

    /**
     * Compact constructor with validation.
     *
     * <p>Validates all required fields and makes defensive copies of mutable collections.
     */
    public EngineConfig {
        // Null checks
        Objects.requireNonNull(sourceRoots, "sourceRoots cannot be null");
        Objects.requireNonNull(classpathEntries, "classpathEntries cannot be null");
        Objects.requireNonNull(basePackage, "basePackage cannot be null");
        Objects.requireNonNull(pluginConfigs, "pluginConfigs cannot be null");
        Objects.requireNonNull(options, "options cannot be null");

        // Validate basePackage
        if (basePackage.isBlank()) {
            throw new IllegalArgumentException("basePackage cannot be blank");
        }

        // Validate javaVersion
        if (javaVersion < 8 || javaVersion > 24) {
            throw new IllegalArgumentException("javaVersion must be between 8 and 24, got: " + javaVersion);
        }

        // Validate sourceRoots exist and are directories
        for (Path root : sourceRoots) {
            if (!Files.exists(root)) {
                throw new IllegalArgumentException("Source root does not exist: " + root);
            }
            if (!Files.isDirectory(root)) {
                throw new IllegalArgumentException("Source root is not a directory: " + root);
            }
        }

        // Defensive copies
        sourceRoots = List.copyOf(sourceRoots);
        classpathEntries = List.copyOf(classpathEntries);
        pluginConfigs = Map.copyOf(pluginConfigs);
        options = Map.copyOf(options);
        enabledCategories = enabledCategories != null ? Set.copyOf(enabledCategories) : null;
    }

    /**
     * Creates a minimal configuration for testing (no plugin execution).
     */
    public static EngineConfig minimal(Path sourceRoot, String basePackage) {
        return new EngineConfig(List.of(sourceRoot), List.of(), 21, basePackage, null, Map.of(), Map.of(), null, null);
    }

    /**
     * Creates a configuration with plugin execution enabled.
     */
    public static EngineConfig withPlugins(
            Path sourceRoot, String basePackage, Path outputDirectory, Map<String, Map<String, Object>> pluginConfigs) {
        return new EngineConfig(
                List.of(sourceRoot), List.of(), 21, basePackage, outputDirectory, pluginConfigs, Map.of(), null, null);
    }

    /**
     * Creates a configuration with a specific classification profile.
     *
     * @param sourceRoot the source root directory
     * @param basePackage the base package to analyze
     * @param classificationProfile the profile name (e.g., "repository-aware", "strict")
     * @return the configuration
     */
    public static EngineConfig withProfile(Path sourceRoot, String basePackage, String classificationProfile) {
        return new EngineConfig(
                List.of(sourceRoot), List.of(), 21, basePackage, null, Map.of(), Map.of(), classificationProfile, null);
    }

    /**
     * Returns a new config with only GENERATOR plugins enabled.
     *
     * @return a config restricted to generator plugins
     */
    public EngineConfig onlyGenerators() {
        return new EngineConfig(
                sourceRoots,
                classpathEntries,
                javaVersion,
                basePackage,
                outputDirectory,
                pluginConfigs,
                options,
                classificationProfile,
                Set.of(PluginCategory.GENERATOR));
    }

    /**
     * Returns a new config with only AUDIT plugins enabled.
     *
     * @return a config restricted to audit plugins
     */
    public EngineConfig onlyAuditors() {
        return new EngineConfig(
                sourceRoots,
                classpathEntries,
                javaVersion,
                basePackage,
                outputDirectory,
                pluginConfigs,
                options,
                classificationProfile,
                Set.of(PluginCategory.AUDIT));
    }

    /**
     * Returns a new config with all plugin categories enabled.
     *
     * @return a config with all categories enabled
     */
    public EngineConfig allCategories() {
        return new EngineConfig(
                sourceRoots,
                classpathEntries,
                javaVersion,
                basePackage,
                outputDirectory,
                pluginConfigs,
                options,
                classificationProfile,
                null);
    }

    /**
     * Returns true if plugin execution is enabled.
     */
    public boolean pluginsEnabled() {
        return outputDirectory != null;
    }
}
