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
import io.hexaglue.spi.core.ClassificationConfig;
import io.hexaglue.spi.generation.PluginCategory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
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
 * <p>Module roles are resolved from the {@code modules:} section of the root {@code hexaglue.yaml}.
 * Modules without an explicit role default to {@link ModuleRole#SHARED}.</p>
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
     * @param outputDirectory the default output directory (for reactor-level output)
     * @param pluginConfigs plugin configurations from hexaglue.yaml
     * @param classificationConfig classification configuration (exclusions, explicit mappings)
     * @param enabledCategories plugin categories to execute
     * @param includeGenerated whether to include {@code @Generated}-annotated types
     * @param log the Maven logger
     * @return the unified engine configuration
     */
    static EngineConfig build(
            MavenSession session,
            String basePackage,
            Path outputDirectory,
            Map<String, Map<String, Object>> pluginConfigs,
            ClassificationConfig classificationConfig,
            Set<PluginCategory> enabledCategories,
            boolean includeGenerated,
            Log log) {

        MavenProject topLevel = findTopLevelProject(session);
        Path rootBaseDir = topLevel.getBasedir().toPath();

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
            ModuleRole role = moduleRoles.getOrDefault(moduleId, ModuleRole.SHARED);
            Path moduleBaseDir = project.getBasedir().toPath();

            // Collect source roots (filter to existing directories)
            List<Path> moduleSourceRoots = project.getCompileSourceRoots().stream()
                    .map(Path::of)
                    .filter(Files::isDirectory)
                    .toList();

            allSourceRoots.addAll(moduleSourceRoots);

            // Collect classpath entries
            for (Artifact artifact : project.getArtifacts()) {
                File file = artifact.getFile();
                if (file != null) {
                    classpathSet.add(file.toPath());
                }
            }

            // Module-specific output directory
            Path moduleOutputDir = moduleBaseDir.resolve("target/hexaglue/generated-sources");

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
                outputDirectory,
                pluginConfigs,
                Map.of(),
                classificationConfig,
                enabledCategories,
                includeGenerated,
                moduleSourceSets);
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
