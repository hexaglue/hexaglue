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

import io.hexaglue.core.audit.report.AuditReportGenerator;
import io.hexaglue.core.engine.Diagnostic;
import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.HexaGlueEngine;
import io.hexaglue.spi.audit.AuditSnapshot;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Executes HexaGlue architecture audit and quality analysis.
 *
 * <p>This mojo runs audit plugins to analyze code quality, architecture compliance,
 * and generates reports in various formats (console, HTML, JSON, Markdown).
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
 *                 <goal>audit</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 *     <configuration>
 *         <basePackage>com.example</basePackage>
 *         <failOnError>true</failOnError>
 *         <failOnWarning>false</failOnWarning>
 *     </configuration>
 * </plugin>
 * }</pre>
 */
@Mojo(
        name = "audit",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class AuditMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The base package to analyze. Types outside this package are ignored.
     */
    @Parameter(property = "hexaglue.basePackage", required = true)
    private String basePackage;

    /**
     * Skip HexaGlue audit execution.
     */
    @Parameter(property = "hexaglue.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Fail the build if ERROR-level violations are found.
     */
    @Parameter(property = "hexaglue.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Fail the build if WARNING-level violations are found.
     */
    @Parameter(property = "hexaglue.failOnWarning", defaultValue = "false")
    private boolean failOnWarning;

    /**
     * Generate console report (plain text output to logs).
     */
    @Parameter(property = "hexaglue.consoleReport", defaultValue = "true")
    private boolean consoleReport;

    /**
     * Generate HTML report.
     */
    @Parameter(property = "hexaglue.htmlReport", defaultValue = "true")
    private boolean htmlReport;

    /**
     * Generate JSON report (machine-readable).
     */
    @Parameter(property = "hexaglue.jsonReport", defaultValue = "false")
    private boolean jsonReport;

    /**
     * Generate Markdown report.
     */
    @Parameter(property = "hexaglue.markdownReport", defaultValue = "false")
    private boolean markdownReport;

    /**
     * Output directory for audit reports.
     */
    @Parameter(property = "hexaglue.reportDirectory", defaultValue = "${project.build.directory}/hexaglue-reports")
    private File reportDirectory;

    /**
     * Audit configuration (rules, thresholds, etc.).
     *
     * <p>This can be configured inline or reference an external configuration file.
     */
    @Parameter
    private AuditConfig auditConfig;

    /**
     * Classification profile to use.
     */
    @Parameter(property = "hexaglue.classificationProfile")
    private String classificationProfile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("HexaGlue audit skipped");
            return;
        }

        getLog().info("HexaGlue auditing: " + basePackage);

        // Build engine configuration
        EngineConfig config = buildConfig();
        HexaGlueEngine engine = HexaGlueEngine.create();

        // Run analysis
        EngineResult result = engine.analyze(config);

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

        // Extract audit snapshot from plugin results
        AuditSnapshot auditSnapshot = extractAuditSnapshot(result);
        if (auditSnapshot == null) {
            getLog().warn("No audit plugin executed - ensure an audit plugin is on the classpath");
            return;
        }

        // Generate reports
        try {
            generateReports(auditSnapshot);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate audit reports", e);
        }

        // Log summary
        logAuditSummary(auditSnapshot);

        // Check failure conditions
        if (failOnError && auditSnapshot.errorCount() > 0) {
            throw new MojoFailureException("Audit failed with " + auditSnapshot.errorCount() + " ERROR violations");
        }

        if (failOnWarning && auditSnapshot.warningCount() > 0) {
            throw new MojoFailureException("Audit failed with " + auditSnapshot.warningCount() + " WARNING violations");
        }

        getLog().info("Audit completed: " + (auditSnapshot.passed() ? "PASSED" : "FAILED"));
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

        // Build plugin configs from audit config
        Map<String, Map<String, Object>> pluginConfigs = auditConfig != null ? auditConfig.toPluginConfigs() : Map.of();

        return new EngineConfig(
                sourceRoots,
                classpath,
                javaVersion,
                basePackage,
                null, // No code generation in audit mode
                pluginConfigs,
                Map.of(),
                classificationProfile);
    }

    private AuditSnapshot extractAuditSnapshot(EngineResult result) {
        // In a real implementation, the audit snapshot would be obtained from
        // the plugin execution result. For now, this is a placeholder.
        // The actual integration depends on how audit plugins expose their results.

        // This method should extract the AuditSnapshot from the PluginExecutionResult
        // when audit plugins are properly integrated with the engine.

        getLog().warn("AuditSnapshot extraction not fully implemented - requires engine integration");
        return null;
    }

    private void generateReports(AuditSnapshot snapshot) throws IOException {
        if (!reportDirectory.exists() && !reportDirectory.mkdirs()) {
            throw new IOException("Failed to create report directory: " + reportDirectory);
        }

        AuditReportGenerator generator = new AuditReportGenerator();

        if (consoleReport) {
            String consoleOutput = generator.generateConsole(snapshot);
            getLog().info("\n" + consoleOutput);
        }

        if (htmlReport) {
            Path htmlFile = reportDirectory.toPath().resolve("hexaglue-audit.html");
            String htmlContent = generator.generateHtml(snapshot);
            Files.writeString(htmlFile, htmlContent);
            getLog().info("HTML report: " + htmlFile);
        }

        if (jsonReport) {
            Path jsonFile = reportDirectory.toPath().resolve("hexaglue-audit.json");
            String jsonContent = generator.generateJson(snapshot);
            Files.writeString(jsonFile, jsonContent);
            getLog().info("JSON report: " + jsonFile);
        }

        if (markdownReport) {
            Path mdFile = reportDirectory.toPath().resolve("hexaglue-audit.md");
            String mdContent = generator.generateMarkdown(snapshot);
            Files.writeString(mdFile, mdContent);
            getLog().info("Markdown report: " + mdFile);
        }
    }

    private void logAuditSummary(AuditSnapshot snapshot) {
        getLog().info("─".repeat(80));
        getLog().info("AUDIT SUMMARY");
        getLog().info("─".repeat(80));
        getLog().info("Total Violations: " + snapshot.violations().size());
        getLog().info("  Errors:   " + snapshot.errorCount());
        getLog().info("  Warnings: " + snapshot.warningCount());
        getLog().info("  Info:     " + snapshot.infos().size());
        getLog().info("Status: " + (snapshot.passed() ? "PASSED" : "FAILED"));
        getLog().info("─".repeat(80));
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
