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
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
            defaultValue = "${project.build.directory}/generated-sources/hexaglue")
    private File outputDirectory;

    /**
     * Classification profile to use.
     *
     * <p>Available profiles:
     * <ul>
     *   <li>{@code default} - Standard priorities for all criteria</li>
     *   <li>{@code strict} - Favors explicit annotations over heuristics</li>
     *   <li>{@code annotation-only} - Only trusts explicit annotations</li>
     *   <li>{@code repository-aware} - Better detection of repository ports with plural names</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * <configuration>
     *     <classificationProfile>repository-aware</classificationProfile>
     * </configuration>
     * }</pre>
     */
    @Parameter(property = "hexaglue.classificationProfile")
    private String classificationProfile;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("HexaGlue generation skipped");
            return;
        }

        getLog().info("HexaGlue analyzing: " + basePackage);

        EngineConfig config = buildConfig();
        HexaGlueEngine engine = HexaGlueEngine.create();

        EngineResult result = engine.analyze(config);

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

        return new EngineConfig(
                sourceRoots,
                classpath,
                javaVersion,
                basePackage,
                outputDirectory.toPath(),
                Map.of(), // pluginConfigs - TODO: load from hexaglue.yaml
                Map.of(), // options
                classificationProfile);
    }

    private String formatDiagnostic(Diagnostic diag) {
        if (diag.sourceRef() != null) {
            return String.format(
                    "[%s] %s (%s:%d)",
                    diag.code(),
                    diag.message(),
                    diag.sourceRef().filePath(),
                    diag.sourceRef().lineStart());
        }
        return String.format("[%s] %s", diag.code(), diag.message());
    }
}
