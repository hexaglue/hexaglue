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
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

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

        // Add generated sources to compilation (skip if already a source root, e.g. src/main/java)
        String outputPath = outputDirectory.getAbsolutePath();
        if (!project.getCompileSourceRoots().contains(outputPath)) {
            if (outputDirectory.exists() || outputDirectory.mkdirs()) {
                project.addCompileSourceRoot(outputPath);
            }
        }
    }

    private EngineConfig buildConfig() {
        List<Path> sourceRoots = MojoSourceRootsResolver.resolveSourceRoots(project);

        List<Path> classpath = MojoClasspathBuilder.buildClasspath(project);

        String javaVersionStr = project.getProperties().getProperty("maven.compiler.release", "21");
        int javaVersion;
        try {
            javaVersion = Integer.parseInt(javaVersionStr);
        } catch (NumberFormatException e) {
            javaVersion = 21;
        }

        Map<String, Map<String, Object>> pluginConfigs =
                MojoConfigLoader.loadPluginConfigs(project.getBasedir().toPath(), getLog());
        ClassificationConfig classificationConfig =
                MojoConfigLoader.loadClassificationConfig(project.getBasedir().toPath(), failOnUnclassified, getLog());

        return new EngineConfig(
                sourceRoots,
                classpath,
                javaVersion,
                basePackage,
                project.getName(),
                project.getVersion(),
                outputDirectory.toPath(),
                null,
                pluginConfigs,
                Map.of("hexaglue.projectRoot", project.getBasedir().toPath()),
                classificationConfig,
                Set.of(PluginCategory.GENERATOR), // Only run generator plugins
                false, // Do not include @Generated types during generation
                List.of(), // Mono-module
                tolerantResolution);
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
