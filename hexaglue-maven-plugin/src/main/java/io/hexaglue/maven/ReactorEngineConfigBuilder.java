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
import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.core.engine.ModuleSourceSet;
import io.hexaglue.core.engine.MultiModuleOutputResolver;
import io.hexaglue.spi.core.ClassificationConfig;
import io.hexaglue.spi.generation.PluginCategory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Builds a unified {@link EngineConfig} from a multi-module Maven reactor session.
 *
 * <p>This utility collects source roots and classpath entries from all {@code jar}-packaged
 * modules in the reactor, constructs {@link ModuleSourceSet} descriptors for each, and produces
 * a single {@link EngineConfig} suitable for whole-reactor analysis.</p>
 *
 * <p>Module roles are resolved using a priority chain: explicit YAML configuration &gt;
 * convention-based detection from the Maven artifactId suffix &gt;
 * default {@link ModuleRole#SHARED}.</p>
 *
 * @since 5.0.0
 */
final class ReactorEngineConfigBuilder {

    private ReactorEngineConfigBuilder() {}

    /**
     * Builds a unified {@link EngineConfig} from the Maven reactor session.
     *
     * <p>Only modules with {@code jar} packaging are included. Source roots that do not
     * exist on disk are filtered out. Classpath entries are deduplicated.</p>
     *
     * @param session the Maven session containing all reactor projects
     * @param basePackage the base package to analyze
     * @param sourcesOutputDirectory directory for generated sources (null for audit-only)
     * @param reportsOutputDirectory directory for reports (null for generate-only)
     * @param pluginConfigs plugin configurations from hexaglue.yaml
     * @param classificationConfig classification configuration (exclusions, explicit mappings)
     * @param enabledCategories plugin categories to execute
     * @param includeGenerated whether to include {@code @Generated}-annotated types
     * @param log the Maven logger
     * @return the unified engine configuration
     * @since 5.0.0 added separate sourcesOutputDirectory and reportsOutputDirectory
     */
    static EngineConfig build(
            MavenSession session,
            String basePackage,
            Path sourcesOutputDirectory,
            Path reportsOutputDirectory,
            Map<String, Map<String, Object>> pluginConfigs,
            ClassificationConfig classificationConfig,
            Set<PluginCategory> enabledCategories,
            boolean includeGenerated,
            Log log) {
        MavenProject topLevel = findTopLevelProject(session);
        return build(
                session,
                basePackage,
                sourcesOutputDirectory,
                reportsOutputDirectory,
                pluginConfigs,
                classificationConfig,
                enabledCategories,
                includeGenerated,
                Map.of("hexaglue.projectRoot", topLevel.getBasedir().toPath()),
                log);
    }

    /**
     * Builds a unified {@link EngineConfig} from the Maven reactor session with custom options.
     *
     * @param session the Maven session containing all reactor projects
     * @param basePackage the base package to analyze
     * @param sourcesOutputDirectory directory for generated sources (null for audit-only)
     * @param reportsOutputDirectory directory for reports (null for generate-only)
     * @param pluginConfigs plugin configurations from hexaglue.yaml
     * @param classificationConfig classification configuration (exclusions, explicit mappings)
     * @param enabledCategories plugin categories to execute
     * @param includeGenerated whether to include {@code @Generated}-annotated types
     * @param options additional engine options (e.g. projectRoot)
     * @param log the Maven logger
     * @return the unified engine configuration
     * @since 5.0.0
     */
    static EngineConfig build(
            MavenSession session,
            String basePackage,
            Path sourcesOutputDirectory,
            Path reportsOutputDirectory,
            Map<String, Map<String, Object>> pluginConfigs,
            ClassificationConfig classificationConfig,
            Set<PluginCategory> enabledCategories,
            boolean includeGenerated,
            Map<String, Object> options,
            Log log) {

        MavenProject topLevel = findTopLevelProject(session);
        Path rootBaseDir = topLevel.getBasedir().toPath();

        // Load output config from YAML for custom base paths in MultiModuleOutputResolver
        MojoConfigLoader.OutputConfig outputConfig = MojoConfigLoader.loadOutputConfig(rootBaseDir, log);
        MultiModuleOutputResolver outputResolver = new MultiModuleOutputResolver(
                rootBaseDir,
                outputConfig.sourcesBase(),
                outputConfig.sourcesBase().replace("generated-sources", "generated-resources"),
                outputConfig.reportsBase());

        // Load module role configurations from hexaglue.yaml
        Map<String, ModuleRole> moduleRoles = MojoConfigLoader.loadModuleConfigs(rootBaseDir, log);

        // Collect source roots, classpath, and module source sets from jar modules
        List<Path> allSourceRoots = new ArrayList<>();
        Set<Path> classpathSet = new LinkedHashSet<>();
        List<ModuleSourceSet> moduleSourceSets = new ArrayList<>();

        for (MavenProject project : session.getProjects()) {
            // Skip non-jar modules (POM aggregators, etc.)
            if (!"jar".equals(project.getPackaging())) {
                log.debug("Skipping non-jar module: " + project.getArtifactId());
                continue;
            }

            String moduleId = project.getArtifactId();
            ModuleRole role = resolveModuleRole(moduleId, moduleRoles, log);
            Path moduleBaseDir = project.getBasedir().toPath();

            // Collect source roots (with delombok substitution when applicable)
            List<Path> moduleSourceRoots = MojoSourceRootsResolver.resolveSourceRoots(project).stream()
                    .filter(Files::isDirectory)
                    .toList();

            allSourceRoots.addAll(moduleSourceRoots);

            // Collect classpath entries (includes target/classes when available)
            classpathSet.addAll(MojoClasspathBuilder.buildClasspath(project));

            // Module-specific output directory — placed under the parent's target/ so that
            // child module clean phases do not erase generated sources (see reactor-lifecycle-fix.md)
            Path moduleOutputDir = outputResolver.resolveSourcesDirectory(moduleId);

            moduleSourceSets.add(new ModuleSourceSet(
                    moduleId, role, moduleSourceRoots, List.copyOf(classpathSet), moduleOutputDir, moduleBaseDir));

            log.debug(String.format("Module '%s' [%s]: %d source root(s)", moduleId, role, moduleSourceRoots.size()));
        }

        // Resolve Java version from top-level project
        int javaVersion = resolveJavaVersion(topLevel);

        log.info(String.format(
                "Reactor config: %d module(s), %d source root(s), %d classpath entries",
                moduleSourceSets.size(), allSourceRoots.size(), classpathSet.size()));

        return new EngineConfig(
                allSourceRoots,
                List.copyOf(classpathSet),
                javaVersion,
                basePackage,
                topLevel.getName(),
                topLevel.getVersion(),
                sourcesOutputDirectory,
                reportsOutputDirectory,
                pluginConfigs,
                options != null ? options : Map.of(),
                classificationConfig,
                enabledCategories,
                includeGenerated,
                moduleSourceSets,
                false);
    }

    /**
     * Resolves the {@link ModuleRole} for a module using a priority chain:
     * YAML explicit &gt; convention detection &gt; SHARED default.
     */
    private static ModuleRole resolveModuleRole(String moduleId, Map<String, ModuleRole> yamlRoles, Log log) {
        // Priority 1: explicit YAML configuration
        ModuleRole yamlRole = yamlRoles.get(moduleId);
        if (yamlRole != null) {
            log.debug(String.format("Module '%s': role %s (from hexaglue.yaml)", moduleId, yamlRole));
            return yamlRole;
        }

        // Priority 2: convention-based detection from artifactId suffix
        return ModuleRoleDetector.detect(moduleId)
                .map(detected -> {
                    log.info(String.format("Module '%s': role %s (detected by convention)", moduleId, detected));
                    return detected;
                })
                .orElseGet(() -> {
                    log.debug(String.format("Module '%s': role SHARED (default)", moduleId));
                    return ModuleRole.SHARED;
                });
    }

    /**
     * Finds the top-level project in the session.
     *
     * <p>Prefers {@link MavenSession#getTopLevelProject()} when available,
     * otherwise falls back to the first project in the reactor.</p>
     */
    private static MavenProject findTopLevelProject(MavenSession session) {
        try {
            MavenProject topLevel = session.getTopLevelProject();
            if (topLevel != null) {
                return topLevel;
            }
        } catch (Exception ignored) {
            // Fallback below
        }
        return session.getProjects().get(0);
    }

    private static int resolveJavaVersion(MavenProject project) {
        String javaVersionStr = project.getProperties().getProperty("maven.compiler.release", "21");
        try {
            return Integer.parseInt(javaVersionStr);
        } catch (NumberFormatException e) {
            return 21;
        }
    }
}
