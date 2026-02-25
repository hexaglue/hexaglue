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
import io.hexaglue.core.engine.ModuleSourceSet;
import io.hexaglue.core.plugin.PluginCyclicDependencyException;
import io.hexaglue.core.plugin.PluginDependencyException;
import io.hexaglue.spi.core.ClassificationConfig;
import io.hexaglue.spi.generation.PluginCategory;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Executes HexaGlue analysis and code generation across all modules in a multi-module reactor.
 *
 * <p>This aggregator mojo runs once at the reactor root, analyzes sources from all
 * {@code jar}-packaged modules, builds a unified architectural model, and routes
 * generated code to the appropriate module output directories.</p>
 *
 * <p>The mojo is automatically injected by {@link HexaGlueLifecycleParticipant} when
 * a multi-module project is detected.</p>
 *
 * @since 5.0.0
 */
@Mojo(
        name = "reactor-generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        aggregator = true,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class ReactorGenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

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
     * Whether to fail the build if unclassified types remain.
     *
     * @since 5.0.0
     */
    @Parameter(property = "hexaglue.failOnUnclassified", defaultValue = "false")
    private boolean failOnUnclassified;

    /**
     * Skip validation step before generation.
     *
     * @since 5.0.0
     */
    @Parameter(property = "hexaglue.skipValidation", defaultValue = "false")
    private boolean skipValidation;

    /**
     * Enable tolerant type resolution for projects using annotation processors.
     *
     * <p>When enabled, HexaGlue accepts unresolved types during analysis instead of
     * failing. This is useful for projects using annotation processors (MapStruct,
     * Immutables) whose generated types are not yet on the classpath.
     *
     * @since 6.0.0
     */
    @Parameter(property = "hexaglue.tolerantResolution", defaultValue = "false")
    private boolean tolerantResolution;

    /**
     * Default output directory for generated sources (reactor-level).
     */
    @Parameter(
            property = "hexaglue.outputDirectory",
            defaultValue = "${project.build.directory}/hexaglue/generated-sources")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("HexaGlue reactor-generate skipped");
            return;
        }

        getLog().info("HexaGlue reactor-generate: analyzing all modules for base package: " + basePackage);

        // Load configurations from root hexaglue.yaml
        Path rootBaseDir = session.getTopLevelProject() != null
                ? session.getTopLevelProject().getBasedir().toPath()
                : session.getProjects().get(0).getBasedir().toPath();

        Map<String, Map<String, Object>> pluginConfigs = MojoConfigLoader.loadPluginConfigs(rootBaseDir, getLog());
        ClassificationConfig classificationConfig =
                MojoConfigLoader.loadClassificationConfig(rootBaseDir, failOnUnclassified, getLog());

        // Build unified engine config from reactor
        EngineConfig config = ReactorEngineConfigBuilder.build(
                session,
                basePackage,
                outputDirectory.toPath(),
                null, // No reports directory for generate-only
                pluginConfigs,
                classificationConfig,
                Set.of(PluginCategory.GENERATOR),
                false, // Do not include @Generated types during generation
                tolerantResolution,
                getLog());

        // Validate targetModule references in plugin configurations
        if (config.isMultiModule()) {
            Set<String> knownModuleIds = config.moduleSourceSets().stream()
                    .map(ModuleSourceSet::moduleId)
                    .collect(Collectors.toSet());
            TargetModuleValidator.ValidationResult validation =
                    TargetModuleValidator.validate(pluginConfigs, knownModuleIds);
            if (!validation.isValid()) {
                validation.errors().forEach(err -> getLog().error(err));
                throw new MojoExecutionException("Invalid targetModule configuration: "
                        + validation.errors().size() + " error(s)");
            }
        }

        // Run analysis
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
                "Reactor analysis complete: %d types, %d classified, %d ports in %dms",
                result.metrics().totalTypes(),
                result.metrics().classifiedTypes(),
                result.metrics().portsDetected(),
                result.metrics().analysisTime().toMillis()));

        if (!result.isSuccess()) {
            throw new MojoExecutionException("HexaGlue reactor analysis failed with errors");
        }

        // Validation check
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

        // Log plugin results
        if (result.generatedFileCount() > 0) {
            getLog().info("Generated " + result.generatedFileCount() + " files across reactor modules");
        }

        // Register generated sources in each module project (skip if already a source root)
        for (ModuleSourceSet mss : config.moduleSourceSets()) {
            for (MavenProject project : session.getProjects()) {
                if (project.getArtifactId().equals(mss.moduleId())) {
                    String sourceRoot = mss.outputDirectory().toAbsolutePath().toString();
                    if (!project.getCompileSourceRoots().contains(sourceRoot)) {
                        project.addCompileSourceRoot(sourceRoot);
                        getLog().debug("Registered generated-sources for module: " + mss.moduleId());
                    }
                    break;
                }
            }
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
