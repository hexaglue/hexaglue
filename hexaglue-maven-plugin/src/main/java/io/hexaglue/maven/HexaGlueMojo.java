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

import io.hexaglue.core.engine.Diagnostic;
import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.HexaGlueEngine;
import io.hexaglue.core.plugin.PluginCyclicDependencyException;
import io.hexaglue.core.plugin.PluginDependencyException;
import io.hexaglue.spi.core.ClassificationConfig;
import io.hexaglue.spi.generation.PluginCategory;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.yaml.snakeyaml.Yaml;

/**
 * Executes HexaGlue analysis and code generation.
 *
 * <p>Usage in pom.xml:
 * <pre>{@code
 * <plugin>
 *     <groupId>io.hexaglue</groupId>
 *     <artifactId>hexaglue-maven-plugin</artifactId>
 *     <version>${hexaglue.version}</version>
 *     <executions>
 *         <execution>
 *             <goals>
 *                 <goal>generate</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 *     <configuration>
 *         <basePackage>com.example</basePackage>
 *     </configuration>
 * </plugin>
 * }</pre>
 */
@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class HexaGlueMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The base package to analyze. Types outside this package are ignored.
     */
    @Parameter(property = "hexaglue.basePackage", required = true)
    private String basePackage;

    /**
     * Skip HexaGlue execution.
     */
    @Parameter(property = "hexaglue.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Output directory for generated sources.
     */
    @Parameter(
            property = "hexaglue.outputDirectory",
            defaultValue = "${project.build.directory}/hexaglue/generated-sources")
    private File outputDirectory;

    /**
     * Whether to fail the build if unclassified types remain.
     *
     * <p>When enabled, the build will fail if any domain types cannot be
     * classified with sufficient confidence. This encourages explicit
     * jMolecules annotations for ambiguous types.
     *
     * @since 3.0.0
     */
    @Parameter(property = "hexaglue.failOnUnclassified", defaultValue = "false")
    private boolean failOnUnclassified;

    /**
     * Skip validation step before generation.
     *
     * <p>When true, generation will proceed even if there are unclassified types.
     * This is not recommended for production builds.
     *
     * @since 3.0.0
     */
    @Parameter(property = "hexaglue.skipValidation", defaultValue = "false")
    private boolean skipValidation;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("HexaGlue generation skipped");
            return;
        }

        getLog().info("HexaGlue analyzing: " + basePackage);

        EngineConfig config = buildConfig();
        HexaGlueEngine engine = HexaGlueEngine.create();

        EngineResult result;
        try {
            result = engine.analyze(config);
        } catch (PluginDependencyException e) {
            throw new MojoExecutionException("Plugin dependency error: " + e.getMessage(), e);
        } catch (PluginCyclicDependencyException e) {
            throw new MojoExecutionException("Cyclic plugin dependency detected: " + e.getMessage(), e);
        }

        // Log diagnostics
        for (Diagnostic diag : result.diagnostics()) {
            switch (diag.severity()) {
                case INFO -> getLog().info(formatDiagnostic(diag));
                case WARNING -> getLog().warn(formatDiagnostic(diag));
                case ERROR -> getLog().error(formatDiagnostic(diag));
            }
        }

        // Report metrics
        getLog().info(String.format(
                "Analysis complete: %d types, %d classified, %d ports in %dms",
                result.metrics().totalTypes(),
                result.metrics().classifiedTypes(),
                result.metrics().portsDetected(),
                result.metrics().analysisTime().toMillis()));

        if (!result.isSuccess()) {
            throw new MojoExecutionException("HexaGlue analysis failed with errors");
        }

        // Validation check: fail if unclassified types exist and failOnUnclassified is true
        if (!skipValidation && failOnUnclassified && result.unclassifiedCount() > 0) {
            getLog().error("Validation failed: " + result.unclassifiedCount() + " unclassified types");
            for (var unclassified : result.unclassifiedTypes()) {
                getLog().error("  - " + unclassified.typeName() + ": " + unclassified.reasoning());
            }
            throw new MojoFailureException("Generation blocked: " + result.unclassifiedCount() + " unclassified types. "
                    + "Add jMolecules annotations or configure explicit classifications in hexaglue.yaml. "
                    + "Use -Dhexaglue.skipValidation=true to bypass (not recommended).");
        } else if (result.unclassifiedCount() > 0) {
            getLog().warn("Warning: " + result.unclassifiedCount() + " unclassified types detected");
        }

        // Log plugin results (plugins are executed by the engine when outputDirectory is set)
        if (result.generatedFileCount() > 0) {
            getLog().info("Generated " + result.generatedFileCount() + " files");
        }

        // Add generated sources to compilation
        if (outputDirectory.exists() || outputDirectory.mkdirs()) {
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        }
    }

    private EngineConfig buildConfig() {
        List<Path> sourceRoots =
                project.getCompileSourceRoots().stream().map(Path::of).toList();

        List<Path> classpath = project.getArtifacts().stream()
                .map(Artifact::getFile)
                .map(File::toPath)
                .toList();

        String javaVersionStr = project.getProperties().getProperty("maven.compiler.release", "21");
        int javaVersion;
        try {
            javaVersion = Integer.parseInt(javaVersionStr);
        } catch (NumberFormatException e) {
            javaVersion = 21;
        }

        Map<String, Map<String, Object>> pluginConfigs = loadPluginConfigs();
        ClassificationConfig classificationConfig = loadClassificationConfig();

        return new EngineConfig(
                sourceRoots,
                classpath,
                javaVersion,
                basePackage,
                project.getName(),
                project.getVersion(),
                outputDirectory.toPath(),
                pluginConfigs,
                Map.of(), // options
                classificationConfig,
                Set.of(PluginCategory.GENERATOR)); // Only run generator plugins
    }

    /**
     * Loads plugin configurations from hexaglue.yaml or hexaglue.yml in the project root.
     *
     * <p>Expected YAML structure:
     * <pre>{@code
     * plugins:
     *   jpa:
     *     enabled: true
     *     entitySuffix: "Entity"
     *     repositoryPackage: "infra.persistence"
     *   living-doc:
     *     enabled: true
     *     outputFormat: "html"
     * }</pre>
     *
     * @return a map of plugin IDs to their configuration maps, or an empty map if no config file exists
     */
    private Map<String, Map<String, Object>> loadPluginConfigs() {
        Path baseDir = project.getBasedir().toPath();

        // Try hexaglue.yaml first, then hexaglue.yml
        Path configPath = baseDir.resolve("hexaglue.yaml");
        if (!Files.exists(configPath)) {
            configPath = baseDir.resolve("hexaglue.yml");
        }

        if (!Files.exists(configPath)) {
            getLog().debug(
                            "No hexaglue.yaml or hexaglue.yml found in project root - using default plugin configurations");
            return Map.of();
        }

        getLog().info("Loading plugin configurations from: " + configPath.getFileName());

        try (Reader reader = Files.newBufferedReader(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(reader);

            if (root == null || !root.containsKey("plugins")) {
                getLog().warn("Configuration file exists but contains no 'plugins' section");
                return Map.of();
            }

            Object pluginsObj = root.get("plugins");
            if (!(pluginsObj instanceof Map)) {
                getLog().warn("Invalid configuration: 'plugins' must be a map");
                return Map.of();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> pluginsMap = (Map<String, Object>) pluginsObj;

            Map<String, Map<String, Object>> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : pluginsMap.entrySet()) {
                String pluginId = entry.getKey();
                Object configObj = entry.getValue();

                if (configObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> configMap = (Map<String, Object>) configObj;
                    result.put(pluginId, configMap);
                    getLog().debug(String.format(
                            "Loaded configuration for plugin '%s' with %d settings", pluginId, configMap.size()));
                } else {
                    getLog().warn(String.format(
                            "Skipping plugin '%s': configuration must be a map, got %s",
                            pluginId, configObj != null ? configObj.getClass().getSimpleName() : "null"));
                }
            }

            getLog().info(String.format("Loaded configurations for %d plugin(s)", result.size()));
            return Collections.unmodifiableMap(result);

        } catch (IOException e) {
            getLog().warn("Failed to read configuration file: " + configPath, e);
            return Map.of();
        } catch (Exception e) {
            getLog().warn("Failed to parse configuration file: " + configPath, e);
            return Map.of();
        }
    }

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
     *     com.example.payment.domain.PaymentStatus: VALUE_OBJECT
     *   validation:
     *     failOnUnclassified: true
     *     allowInferred: true
     * }</pre>
     *
     * <p>The Maven parameter {@code failOnUnclassified} can also be used to override
     * the YAML configuration.
     *
     * @return the classification configuration, or defaults if not configured
     * @since 3.0.0
     */
    @SuppressWarnings("unchecked")
    private ClassificationConfig loadClassificationConfig() {
        Path baseDir = project.getBasedir().toPath();

        // Try hexaglue.yaml first, then hexaglue.yml
        Path configPath = baseDir.resolve("hexaglue.yaml");
        if (!Files.exists(configPath)) {
            configPath = baseDir.resolve("hexaglue.yml");
        }

        ClassificationConfig.Builder builder = ClassificationConfig.builder();

        // Apply Maven parameter override
        if (failOnUnclassified) {
            builder.failOnUnclassified();
        }

        if (!Files.exists(configPath)) {
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
                getLog().warn("Invalid configuration: 'classification' must be a map");
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
                    getLog().debug("Loaded " + excludePatterns.size() + " exclude patterns");
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
                    getLog().debug("Loaded " + explicitClassifications.size() + " explicit classifications");
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
                getLog().info("Loaded classification configuration from: " + configPath.getFileName());
            }
            return config;

        } catch (IOException e) {
            getLog().warn("Failed to read configuration file: " + configPath, e);
            return builder.build();
        } catch (Exception e) {
            getLog().warn("Failed to parse classification configuration: " + e.getMessage());
            return builder.build();
        }
    }

    private String formatDiagnostic(Diagnostic diag) {
        if (diag.location() != null) {
            return String.format(
                    "[%s] %s (%s:%d)",
                    diag.code(),
                    diag.message(),
                    diag.location().filePath(),
                    diag.location().lineStart());
        }
        return String.format("[%s] %s", diag.code(), diag.message());
    }
}
