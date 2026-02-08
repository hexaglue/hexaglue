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

package io.hexaglue.maven;

import io.hexaglue.arch.model.index.ModuleRole;
import io.hexaglue.spi.core.ClassificationConfig;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads configuration from hexaglue.yaml or hexaglue.yml.
 *
 * <p>This utility is shared by all HexaGlue Maven mojos to ensure consistent
 * configuration loading, especially classification exclusion patterns and
 * plugin configurations.
 *
 * @since 5.0.0
 */
final class MojoConfigLoader {

    private MojoConfigLoader() {}

    /**
     * Loads classification configuration from hexaglue.yaml or hexaglue.yml.
     *
     * <p>Expected YAML structure:
     * <pre>{@code
     * classification:
     *   exclude:
     *     - "*.shared.DomainEvent"
     *     - "**.*Exception"
     *   explicit:
     *     com.example.order.domain.OrderDetails: ENTITY
     *   validation:
     *     failOnUnclassified: true
     *     allowInferred: true
     * }</pre>
     *
     * @param projectBaseDir the project base directory
     * @param failOnUnclassified Maven parameter override for failOnUnclassified
     * @param log the Maven logger
     * @return the classification configuration, or defaults if not configured
     */
    // Suppressed: SnakeYAML returns untyped Map from yaml.load(), safe because we validate instanceof before cast
    @SuppressWarnings("unchecked")
    static ClassificationConfig loadClassificationConfig(Path projectBaseDir, boolean failOnUnclassified, Log log) {
        Path configPath = resolveConfigPath(projectBaseDir);

        ClassificationConfig.Builder builder = ClassificationConfig.builder();

        // Apply Maven parameter override
        if (failOnUnclassified) {
            builder.failOnUnclassified();
        }

        if (configPath == null) {
            return builder.build();
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(reader);

            if (root == null || !root.containsKey("classification")) {
                return builder.build();
            }

            Object classificationObj = root.get("classification");
            if (!(classificationObj instanceof Map)) {
                log.warn("Invalid configuration: 'classification' must be a map");
                return builder.build();
            }

            Map<String, Object> classificationMap = (Map<String, Object>) classificationObj;

            // Parse exclude patterns
            if (classificationMap.containsKey("exclude")) {
                Object excludeObj = classificationMap.get("exclude");
                if (excludeObj instanceof List) {
                    List<String> excludePatterns = ((List<?>) excludeObj)
                            .stream()
                                    .filter(String.class::isInstance)
                                    .map(String.class::cast)
                                    .toList();
                    builder.excludePatterns(excludePatterns);
                    log.debug("Loaded " + excludePatterns.size() + " exclude patterns");
                }
            }

            // Parse explicit classifications
            if (classificationMap.containsKey("explicit")) {
                Object explicitObj = classificationMap.get("explicit");
                if (explicitObj instanceof Map) {
                    Map<String, String> explicitClassifications = new HashMap<>();
                    Map<?, ?> explicitMap = (Map<?, ?>) explicitObj;
                    for (Map.Entry<?, ?> entry : explicitMap.entrySet()) {
                        if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                            explicitClassifications.put((String) entry.getKey(), (String) entry.getValue());
                        }
                    }
                    builder.explicitClassifications(explicitClassifications);
                    log.debug("Loaded " + explicitClassifications.size() + " explicit classifications");
                }
            }

            // Parse validation settings
            if (classificationMap.containsKey("validation")) {
                Object validationObj = classificationMap.get("validation");
                if (validationObj instanceof Map) {
                    Map<?, ?> validationMap = (Map<?, ?>) validationObj;

                    boolean yamlFailOnUnclassified = failOnUnclassified; // Maven param takes precedence
                    if (!failOnUnclassified && validationMap.containsKey("failOnUnclassified")) {
                        Object val = validationMap.get("failOnUnclassified");
                        if (val instanceof Boolean) {
                            yamlFailOnUnclassified = (Boolean) val;
                        }
                    }

                    boolean allowInferred = true;
                    if (validationMap.containsKey("allowInferred")) {
                        Object val = validationMap.get("allowInferred");
                        if (val instanceof Boolean) {
                            allowInferred = (Boolean) val;
                        }
                    }

                    builder.validationConfig(
                            new ClassificationConfig.ValidationConfig(yamlFailOnUnclassified, allowInferred));
                }
            }

            ClassificationConfig config = builder.build();
            if (config.hasExclusions() || config.hasExplicitClassifications()) {
                log.info("Loaded classification configuration from: " + configPath.getFileName());
            }
            return config;

        } catch (IOException e) {
            log.warn("Failed to read configuration file: " + configPath);
            return builder.build();
        } catch (Exception e) {
            log.warn("Failed to parse classification configuration: " + e.getMessage());
            return builder.build();
        }
    }

    /**
     * Loads plugin configurations from hexaglue.yaml or hexaglue.yml.
     *
     * @param projectBaseDir the project base directory
     * @param log the Maven logger
     * @return a map of plugin IDs to their configuration maps, or an empty map if not configured
     */
    @SuppressWarnings("unchecked")
    static Map<String, Map<String, Object>> loadPluginConfigs(Path projectBaseDir, Log log) {
        Path configPath = resolveConfigPath(projectBaseDir);

        if (configPath == null) {
            log.debug("No hexaglue.yaml or hexaglue.yml found in project root - using default plugin configurations");
            return Map.of();
        }

        log.info("Loading plugin configurations from: " + configPath.getFileName());

        try (Reader reader = Files.newBufferedReader(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(reader);

            if (root == null || !root.containsKey("plugins")) {
                log.warn("Configuration file exists but contains no 'plugins' section");
                return Map.of();
            }

            Object pluginsObj = root.get("plugins");
            if (!(pluginsObj instanceof Map)) {
                log.warn("Invalid configuration: 'plugins' must be a map");
                return Map.of();
            }

            Map<String, Object> pluginsMap = (Map<String, Object>) pluginsObj;

            Map<String, Map<String, Object>> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : pluginsMap.entrySet()) {
                String pluginId = entry.getKey();
                Object configObj = entry.getValue();

                if (configObj instanceof Map) {
                    Map<String, Object> configMap = (Map<String, Object>) configObj;
                    result.put(pluginId, configMap);
                    log.debug(String.format(
                            "Loaded configuration for plugin '%s' with %d settings", pluginId, configMap.size()));
                } else {
                    log.warn(String.format(
                            "Skipping plugin '%s': configuration must be a map, got %s",
                            pluginId, configObj != null ? configObj.getClass().getSimpleName() : "null"));
                }
            }

            log.info(String.format("Loaded configurations for %d plugin(s)", result.size()));
            return Collections.unmodifiableMap(result);

        } catch (IOException e) {
            log.warn("Failed to read configuration file: " + configPath);
            return Map.of();
        } catch (Exception e) {
            log.warn("Failed to parse configuration file: " + e.getMessage());
            return Map.of();
        }
    }

    /**
     * Loads module configurations from the {@code modules:} section of hexaglue.yaml.
     *
     * <p>Expected YAML structure:
     * <pre>{@code
     * modules:
     *   banking-core:
     *     role: DOMAIN
     *   banking-persistence:
     *     role: INFRASTRUCTURE
     * }</pre>
     *
     * <p>If a module declares an invalid role, it defaults to {@link ModuleRole#SHARED}
     * and a warning is logged.
     *
     * @param projectBaseDir the project base directory (typically the reactor root)
     * @param log the Maven logger
     * @return a map of module IDs to their configured roles, or empty if not configured
     * @since 5.0.0
     */
    // Suppressed: SnakeYAML returns untyped Map from yaml.load(), safe because we validate instanceof before cast
    @SuppressWarnings("unchecked")
    static Map<String, ModuleRole> loadModuleConfigs(Path projectBaseDir, Log log) {
        Path configPath = resolveConfigPath(projectBaseDir);

        if (configPath == null) {
            return Map.of();
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(reader);

            if (root == null || !root.containsKey("modules")) {
                return Map.of();
            }

            Object modulesObj = root.get("modules");
            if (!(modulesObj instanceof Map)) {
                log.warn("Invalid configuration: 'modules' must be a map");
                return Map.of();
            }

            Map<String, Object> modulesMap = (Map<String, Object>) modulesObj;
            Map<String, ModuleRole> result = new HashMap<>();

            for (Map.Entry<String, Object> entry : modulesMap.entrySet()) {
                String moduleId = entry.getKey();
                Object moduleConfig = entry.getValue();

                ModuleRole role = ModuleRole.SHARED;
                if (moduleConfig instanceof Map) {
                    Map<String, Object> configMap = (Map<String, Object>) moduleConfig;
                    Object roleObj = configMap.get("role");
                    if (roleObj instanceof String roleStr) {
                        try {
                            role = ModuleRole.valueOf(roleStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            log.warn(String.format(
                                    "Invalid role '%s' for module '%s', defaulting to SHARED", roleStr, moduleId));
                        }
                    }
                }

                result.put(moduleId, role);
            }

            if (!result.isEmpty()) {
                log.info(String.format("Loaded module configurations for %d module(s)", result.size()));
            }
            return Collections.unmodifiableMap(result);

        } catch (IOException e) {
            log.warn("Failed to read configuration file: " + configPath);
            return Map.of();
        } catch (Exception e) {
            log.warn("Failed to parse module configurations: " + e.getMessage());
            return Map.of();
        }
    }

    /**
     * Loads output directory configuration from the {@code output:} section of hexaglue.yaml.
     *
     * <p>Expected YAML structure:
     * <pre>{@code
     * output:
     *   sources: "target/generated-sources/hexaglue"
     *   reports: "target/hexaglue/reports"
     * }</pre>
     *
     * <p>Missing keys default to the standard HexaGlue convention paths.</p>
     *
     * @param projectBaseDir the project base directory (typically the reactor root)
     * @param log the Maven logger
     * @return the output configuration, never null
     * @since 5.0.0
     */
    // Suppressed: SnakeYAML returns untyped Map from yaml.load(), safe because we validate instanceof before cast
    @SuppressWarnings("unchecked")
    static OutputConfig loadOutputConfig(Path projectBaseDir, Log log) {
        Path configPath = resolveConfigPath(projectBaseDir);

        if (configPath == null) {
            return OutputConfig.defaults();
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(reader);

            if (root == null || !root.containsKey("output")) {
                return OutputConfig.defaults();
            }

            Object outputObj = root.get("output");
            if (!(outputObj instanceof Map)) {
                log.warn("Invalid configuration: 'output' must be a map");
                return OutputConfig.defaults();
            }

            Map<String, Object> outputMap = (Map<String, Object>) outputObj;

            String sourcesBase = OutputConfig.DEFAULT_SOURCES_BASE;
            if (outputMap.get("sources") instanceof String s) {
                sourcesBase = s;
            }

            String reportsBase = OutputConfig.DEFAULT_REPORTS_BASE;
            if (outputMap.get("reports") instanceof String s) {
                reportsBase = s;
            }

            OutputConfig config = new OutputConfig(sourcesBase, reportsBase);
            if (!config.equals(OutputConfig.defaults())) {
                log.info("Loaded output configuration from: " + configPath.getFileName());
            }
            return config;

        } catch (IOException e) {
            log.warn("Failed to read configuration file: " + configPath);
            return OutputConfig.defaults();
        } catch (Exception e) {
            log.warn("Failed to parse output configuration: " + e.getMessage());
            return OutputConfig.defaults();
        }
    }

    /**
     * Output directory configuration parsed from the {@code output:} section of hexaglue.yaml.
     *
     * @param sourcesBase relative path for generated sources (e.g., {@code "target/generated-sources/hexaglue"})
     * @param reportsBase relative path for reports (e.g., {@code "target/hexaglue/reports"})
     * @since 5.0.0
     */
    record OutputConfig(String sourcesBase, String reportsBase) {

        /** Default base path for generated sources, relative to the project root. */
        static final String DEFAULT_SOURCES_BASE = "target/generated-sources/hexaglue";

        /** Default base path for reports, relative to the project root. */
        static final String DEFAULT_REPORTS_BASE = "target/hexaglue/reports";

        /** Returns the default output configuration. */
        static OutputConfig defaults() {
            return new OutputConfig(DEFAULT_SOURCES_BASE, DEFAULT_REPORTS_BASE);
        }
    }

    /**
     * Resolves the path to hexaglue.yaml or hexaglue.yml in the project base directory.
     *
     * @param projectBaseDir the project base directory
     * @return the config path, or null if neither file exists
     */
    private static Path resolveConfigPath(Path projectBaseDir) {
        return resolveLocal(projectBaseDir);
    }

    /**
     * Resolves hexaglue.yaml or hexaglue.yml in the given directory.
     *
     * @param dir the directory to search in
     * @return the config path, or null if neither file exists
     */
    private static Path resolveLocal(Path dir) {
        Path configPath = dir.resolve("hexaglue.yaml");
        if (Files.exists(configPath)) {
            return configPath;
        }
        configPath = dir.resolve("hexaglue.yml");
        if (Files.exists(configPath)) {
            return configPath;
        }
        return null;
    }

    /**
     * Resolves configuration path with hierarchical lookup: local directory first, then parent.
     *
     * <p>This is used in multi-module projects where a child module may override
     * the parent configuration, or fall back to the reactor root configuration.
     *
     * @param localDir the local module directory (checked first)
     * @param parentDir the parent/reactor root directory (fallback)
     * @return the config path, or null if neither location has a config file
     * @since 5.0.0
     */
    static Path resolveConfigPathHierarchical(Path localDir, Path parentDir) {
        Path local = resolveLocal(localDir);
        if (local != null) {
            return local;
        }
        return resolveLocal(parentDir);
    }
}
