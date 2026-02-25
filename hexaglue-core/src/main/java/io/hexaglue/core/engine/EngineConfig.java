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

import io.hexaglue.spi.core.ClassificationConfig;
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
 * @param projectName the name of the project being analyzed (e.g., from Maven pom.xml)
 * @param projectVersion the version of the project (e.g., "1.0.0")
 * @param outputDirectory directory for generated sources (null to skip plugin execution)
 * @param reportsOutputDirectory directory for generated reports (null to use outputDirectory parent + /reports)
 * @param pluginConfigs plugin configurations keyed by plugin ID
 * @param options additional options (key-value pairs)
 * @param classificationConfig configuration for classification (exclusions, explicit mappings, validation)
 * @param enabledCategories plugin categories to execute (null or empty for all categories)
 * @param includeGenerated whether to include {@code @Generated}-annotated types in the semantic model
 * @param moduleSourceSets module source sets for multi-module projects (empty list for mono-module)
 * @param tolerantResolution whether to use tolerant (noClasspath) mode for Spoon type resolution
 * @since 5.0.0 added includeGenerated parameter
 * @since 5.0.0 added moduleSourceSets for multi-module support
 * @since 5.0.0 added reportsOutputDirectory to separate sources and reports output
 * @since 6.0.0 added tolerantResolution for annotation processor compatibility
 */
public record EngineConfig(
        List<Path> sourceRoots,
        List<Path> classpathEntries,
        int javaVersion,
        String basePackage,
        String projectName,
        String projectVersion,
        Path outputDirectory,
        Path reportsOutputDirectory,
        Map<String, Map<String, Object>> pluginConfigs,
        Map<String, Object> options,
        ClassificationConfig classificationConfig,
        Set<PluginCategory> enabledCategories,
        boolean includeGenerated,
        List<ModuleSourceSet> moduleSourceSets,
        boolean tolerantResolution) {

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
        classificationConfig = classificationConfig != null ? classificationConfig : ClassificationConfig.defaults();
        enabledCategories = enabledCategories != null ? Set.copyOf(enabledCategories) : null;
        moduleSourceSets = moduleSourceSets != null ? List.copyOf(moduleSourceSets) : List.of();
    }

    /**
     * Creates a minimal configuration for testing (no plugin execution).
     */
    public static EngineConfig minimal(Path sourceRoot, String basePackage) {
        return new EngineConfig(
                List.of(sourceRoot),
                List.of(),
                21,
                basePackage,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of(),
                null,
                null,
                false,
                List.of(),
                false);
    }

    /**
     * Creates a configuration with plugin execution enabled.
     */
    public static EngineConfig withPlugins(
            Path sourceRoot, String basePackage, Path outputDirectory, Map<String, Map<String, Object>> pluginConfigs) {
        return new EngineConfig(
                List.of(sourceRoot),
                List.of(),
                21,
                basePackage,
                null,
                null,
                outputDirectory,
                null,
                pluginConfigs,
                Map.of(),
                null,
                null,
                false,
                List.of(),
                false);
    }

    /**
     * Creates a configuration with a specific classification config.
     *
     * @param sourceRoot the source root directory
     * @param basePackage the base package to analyze
     * @param classificationConfig the classification configuration
     * @return the configuration
     * @since 3.0.0
     */
    public static EngineConfig withClassificationConfig(
            Path sourceRoot, String basePackage, ClassificationConfig classificationConfig) {
        return new EngineConfig(
                List.of(sourceRoot),
                List.of(),
                21,
                basePackage,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of(),
                classificationConfig,
                null,
                false,
                List.of(),
                false);
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
                projectName,
                projectVersion,
                outputDirectory,
                reportsOutputDirectory,
                pluginConfigs,
                options,
                classificationConfig,
                Set.of(PluginCategory.GENERATOR),
                includeGenerated,
                moduleSourceSets,
                tolerantResolution);
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
                projectName,
                projectVersion,
                outputDirectory,
                reportsOutputDirectory,
                pluginConfigs,
                options,
                classificationConfig,
                Set.of(PluginCategory.AUDIT),
                includeGenerated,
                moduleSourceSets,
                tolerantResolution);
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
                projectName,
                projectVersion,
                outputDirectory,
                reportsOutputDirectory,
                pluginConfigs,
                options,
                classificationConfig,
                null,
                includeGenerated,
                moduleSourceSets,
                tolerantResolution);
    }

    /**
     * Returns a new config with {@code @Generated}-annotated types included in the semantic model.
     *
     * <p>This is used in audit mode so that generated adapters (JPA, MapStruct, etc.)
     * are visible for port coverage validation.
     *
     * @return a config with includeGenerated set to true
     * @since 5.0.0
     */
    public EngineConfig withGeneratedTypes() {
        return new EngineConfig(
                sourceRoots,
                classpathEntries,
                javaVersion,
                basePackage,
                projectName,
                projectVersion,
                outputDirectory,
                reportsOutputDirectory,
                pluginConfigs,
                options,
                classificationConfig,
                enabledCategories,
                true,
                moduleSourceSets,
                tolerantResolution);
    }

    /**
     * Returns true if plugin execution is enabled.
     *
     * <p>Plugins are enabled when at least one output directory is configured:
     * either for generated sources or for reports (audit-only mode).</p>
     */
    public boolean pluginsEnabled() {
        return outputDirectory != null || reportsOutputDirectory != null;
    }

    /**
     * Returns whether this configuration targets a multi-module project.
     *
     * @return true if module source sets are defined
     * @since 5.0.0
     */
    public boolean isMultiModule() {
        return !moduleSourceSets.isEmpty();
    }
}
