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

import io.hexaglue.arch.model.classification.CertaintyLevel;
import io.hexaglue.core.engine.Diagnostic;
import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.HexaGlueEngine;
import io.hexaglue.core.plugin.PluginCyclicDependencyException;
import io.hexaglue.core.plugin.PluginDependencyException;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.core.ClassificationConfig;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * Validates domain type classifications before code generation.
 *
 * <p>This mojo analyzes the codebase and generates a validation report showing:
 * <ul>
 *   <li><b>EXPLICIT</b> - Types with jMolecules annotations or explicit configuration</li>
 *   <li><b>INFERRED</b> - Types classified by structural analysis with high confidence</li>
 *   <li><b>UNCLASSIFIED</b> - Ambiguous types requiring user intervention</li>
 * </ul>
 *
 * <p>When {@code failOnUnclassified} is true, the build fails if any types remain unclassified,
 * encouraging explicit classification via jMolecules annotations or hexaglue.yaml configuration.
 *
 * <p>Usage in pom.xml:
 * <pre>{@code
 * <plugin>
 *     <groupId>io.hexaglue</groupId>
 *     <artifactId>hexaglue-maven-plugin</artifactId>
 *     <executions>
 *         <execution>
 *             <goals>
 *                 <goal>validate</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 *     <configuration>
 *         <basePackage>com.example</basePackage>
 *         <failOnUnclassified>true</failOnUnclassified>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * @since 3.0.0
 */
@Mojo(
        name = "validate",
        defaultPhase = LifecyclePhase.VALIDATE,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class ValidateMojo extends AbstractMojo {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The base package to analyze. Types outside this package are ignored.
     */
    @Parameter(property = "hexaglue.basePackage", required = true)
    private String basePackage;

    /**
     * Skip HexaGlue validation.
     */
    @Parameter(property = "hexaglue.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Whether to fail the build if unclassified types remain.
     *
     * <p>When enabled, the build will fail if any domain types cannot be
     * classified with sufficient confidence. This encourages explicit
     * jMolecules annotations for ambiguous types.
     */
    @Parameter(property = "hexaglue.failOnUnclassified", defaultValue = "false")
    private boolean failOnUnclassified;

    /**
     * Output path for the validation report file.
     *
     * <p>If specified, a Markdown report will be written to this location
     * in addition to the console output.
     */
    @Parameter(
            property = "hexaglue.validationReportPath",
            defaultValue = "${project.build.directory}/hexaglue/reports/validation/validation-report.md")
    private File validationReportPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("HexaGlue validation skipped");
            return;
        }

        getLog().info("HexaGlue validating classifications for: " + basePackage);

        // Build engine configuration (no plugins needed for validation)
        EngineConfig config = buildConfig();
        HexaGlueEngine engine = HexaGlueEngine.create();

        // Run analysis
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

        if (!result.isSuccess()) {
            throw new MojoExecutionException("HexaGlue analysis failed with errors");
        }

        // Generate and display validation report
        String consoleReport = generateConsoleReport(result);
        for (String line : consoleReport.split("\n")) {
            getLog().info(line);
        }

        // Write markdown report to file if path specified
        if (validationReportPath != null) {
            writeMarkdownReport(result);
        }

        // Check failure condition
        if (failOnUnclassified && result.unclassifiedCount() > 0) {
            throw new MojoFailureException("Validation failed: " + result.unclassifiedCount() + " unclassified types. "
                    + "Add jMolecules annotations or configure explicit classifications in hexaglue.yaml");
        }

        if (result.validationPassed()) {
            getLog().info("Validation PASSED: All types are classified");
        } else {
            getLog().warn("Validation completed with " + result.unclassifiedCount() + " unclassified types");
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

        ClassificationConfig classificationConfig = loadClassificationConfig();

        // No plugins needed for validation - just analysis
        return new EngineConfig(
                sourceRoots,
                classpath,
                javaVersion,
                basePackage,
                project.getName(),
                project.getVersion(),
                null, // No output directory needed
                Map.of(),
                Map.of(),
                classificationConfig,
                Set.of(), // No plugins
                false); // Do not include @Generated types during validation
    }

    // Suppressed: SnakeYAML returns untyped Map from yaml.load(), safe because we validate instanceof before cast
    @SuppressWarnings("unchecked")
    private ClassificationConfig loadClassificationConfig() {
        Path baseDir = project.getBasedir().toPath();
        Path configPath = baseDir.resolve("hexaglue.yaml");
        if (!Files.exists(configPath)) {
            configPath = baseDir.resolve("hexaglue.yml");
        }

        ClassificationConfig.Builder builder = ClassificationConfig.builder();
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
                }
            }

            return builder.build();
        } catch (IOException e) {
            getLog().warn("Failed to read configuration file: " + configPath, e);
            return builder.build();
        }
    }

    private String generateConsoleReport(EngineResult result) {
        StringBuilder report = new StringBuilder();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        List<PrimaryClassificationResult> classifications = result.primaryClassifications();

        // Categorize
        List<PrimaryClassificationResult> explicit = classifications.stream()
                .filter(r -> r.certainty() == CertaintyLevel.EXPLICIT)
                .toList();
        List<PrimaryClassificationResult> inferred = classifications.stream()
                .filter(r -> r.isClassified() && r.certainty() != CertaintyLevel.EXPLICIT)
                .toList();
        List<PrimaryClassificationResult> unclassified =
                classifications.stream().filter(r -> !r.isClassified()).toList();

        int total = explicit.size() + inferred.size() + unclassified.size();
        boolean passed = unclassified.isEmpty();

        // Header
        report.append("=".repeat(80)).append("\n");
        report.append("HEXAGLUE VALIDATION REPORT\n");
        report.append("=".repeat(80)).append("\n");
        report.append("Generated: ").append(timestamp).append("\n");
        report.append("Project: ").append(project.getName()).append("\n");
        report.append("\n");

        // Summary
        report.append("CLASSIFICATION SUMMARY\n");
        report.append("-".repeat(80)).append("\n");
        report.append(String.format(
                "%-20s %5d (%5.1f%%)%n", "EXPLICIT:", explicit.size(), percentage(explicit.size(), total)));
        report.append(String.format(
                "%-20s %5d (%5.1f%%)%n", "INFERRED:", inferred.size(), percentage(inferred.size(), total)));
        report.append(String.format(
                "%-20s %5d (%5.1f%%)%n", "UNCLASSIFIED:", unclassified.size(), percentage(unclassified.size(), total)));
        report.append(String.format("%-20s %5d%n", "TOTAL:", total));
        report.append("\n");
        report.append("Status: ").append(passed ? "PASSED" : "FAILED").append("\n");
        report.append("\n");

        // Unclassified Types (most important)
        if (!unclassified.isEmpty()) {
            report.append("UNCLASSIFIED TYPES (").append(unclassified.size()).append(" types) - ACTION REQUIRED\n");
            report.append("-".repeat(80)).append("\n");
            for (PrimaryClassificationResult r : unclassified) {
                report.append("\n  ").append(r.typeName()).append("\n");
                report.append("    Reason: ").append(r.reasoning()).append("\n");
                report.append("    Suggested Actions:\n");
                report.append(
                        "      - Add appropriate jMolecules annotation (@AggregateRoot, @Entity, @ValueObject)\n");
                report.append("      - Configure in hexaglue.yaml: explicit: { ")
                        .append(r.typeName())
                        .append(": KIND }\n");
                report.append("      - Exclude from classification if not a domain type\n");
            }
            report.append("\n");
        }

        report.append("=".repeat(80)).append("\n");
        if (!passed) {
            report.append(unclassified.size()).append(" unclassified types must be resolved before generation.\n");
        }

        return report.toString();
    }

    private void writeMarkdownReport(EngineResult result) {
        try {
            Files.createDirectories(validationReportPath.toPath().getParent());

            StringBuilder md = new StringBuilder();
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

            List<PrimaryClassificationResult> classifications = result.primaryClassifications();

            // Categorize
            List<PrimaryClassificationResult> explicit = classifications.stream()
                    .filter(r -> r.certainty() == CertaintyLevel.EXPLICIT)
                    .toList();
            List<PrimaryClassificationResult> inferred = classifications.stream()
                    .filter(r -> r.isClassified() && r.certainty() != CertaintyLevel.EXPLICIT)
                    .toList();
            List<PrimaryClassificationResult> unclassified =
                    classifications.stream().filter(r -> !r.isClassified()).toList();

            int total = explicit.size() + inferred.size() + unclassified.size();
            boolean passed = unclassified.isEmpty();

            // Header
            md.append("# HexaGlue Validation Report\n\n");
            md.append("**Generated:** ").append(timestamp).append("  \n");
            md.append("**Project:** ").append(project.getName()).append("  \n");
            md.append("**Status:** ").append(passed ? "✅ PASSED" : "❌ FAILED").append("\n\n");

            // Summary
            md.append("## Classification Summary\n\n");
            md.append("| Category | Count | Percentage |\n");
            md.append("|----------|-------|------------|\n");
            md.append(
                    String.format("| EXPLICIT | %d | %.1f%% |%n", explicit.size(), percentage(explicit.size(), total)));
            md.append(
                    String.format("| INFERRED | %d | %.1f%% |%n", inferred.size(), percentage(inferred.size(), total)));
            md.append(String.format(
                    "| UNCLASSIFIED | %d | %.1f%% |%n", unclassified.size(), percentage(unclassified.size(), total)));
            md.append(String.format("| **Total** | **%d** | 100%% |%n%n", total));

            // Explicit
            if (!explicit.isEmpty()) {
                md.append("## Explicit Classifications (")
                        .append(explicit.size())
                        .append(" types)\n\n");
                md.append("| Type | Kind | Certainty |\n");
                md.append("|------|------|----------|\n");
                for (PrimaryClassificationResult r : explicit) {
                    String simpleName = extractSimpleName(r.typeName());
                    md.append(String.format(
                            "| `%s` | %s | %s |%n", simpleName, r.kind().orElse(null), r.certainty()));
                }
                md.append("\n");
            }

            // Inferred
            if (!inferred.isEmpty()) {
                md.append("## Inferred Classifications (")
                        .append(inferred.size())
                        .append(" types)\n\n");
                md.append("| Type | Kind | Certainty | Reasoning |\n");
                md.append("|------|------|-----------|----------|\n");
                for (PrimaryClassificationResult r : inferred) {
                    String simpleName = extractSimpleName(r.typeName());
                    md.append(String.format(
                            "| `%s` | %s | %s | %s |%n",
                            simpleName, r.kind().orElse(null), r.certainty(), r.reasoning()));
                }
                md.append("\n");
            }

            // Unclassified
            if (!unclassified.isEmpty()) {
                md.append("## Unclassified Types (")
                        .append(unclassified.size())
                        .append(" types) - ACTION REQUIRED\n\n");
                int index = 1;
                for (PrimaryClassificationResult r : unclassified) {
                    md.append("### ")
                            .append(index++)
                            .append(". `")
                            .append(r.typeName())
                            .append("`\n\n");
                    md.append("**Reason:** ").append(r.reasoning()).append("\n\n");
                    md.append("**Suggested Actions:**\n");
                    md.append(
                            "- Add appropriate jMolecules annotation (`@AggregateRoot`, `@Entity`, `@ValueObject`)\n");
                    md.append("- Configure in hexaglue.yaml: `explicit: { ")
                            .append(r.typeName())
                            .append(": KIND }`\n");
                    md.append("- Exclude from classification if not a domain type\n\n");
                }
            }

            // Footer
            md.append("---\n\n");
            if (!passed) {
                md.append("## Validation Status: FAILED\n\n");
                md.append(unclassified.size()).append(" unclassified types must be resolved before generation.\n");
            } else {
                md.append("## Validation Status: PASSED\n\n");
                md.append("All types are classified. Generation can proceed.\n");
            }

            Files.writeString(validationReportPath.toPath(), md.toString());
            getLog().info("Validation report written to: " + validationReportPath);

        } catch (IOException e) {
            getLog().warn("Failed to write validation report: " + e.getMessage());
        }
    }

    private String extractSimpleName(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        return lastDot > 0 ? fqn.substring(lastDot + 1) : fqn;
    }

    private double percentage(int count, int total) {
        return total == 0 ? 0.0 : (count * 100.0 / total);
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
