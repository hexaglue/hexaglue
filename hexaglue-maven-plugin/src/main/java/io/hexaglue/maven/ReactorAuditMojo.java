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

import io.hexaglue.arch.model.audit.AuditSnapshot;
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
import java.util.Optional;
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

/**
 * Executes HexaGlue architecture audit across all modules in a multi-module reactor.
 *
 * <p>This aggregator mojo runs once at the reactor root, analyzes sources from all
 * {@code jar}-packaged modules, and produces a single aggregated audit report.</p>
 *
 * <p>The mojo is automatically injected by {@link HexaGlueLifecycleParticipant} when
 * a multi-module project is detected.</p>
 *
 * @since 5.0.0
 */
@Mojo(
        name = "reactor-audit",
        defaultPhase = LifecyclePhase.VERIFY,
        aggregator = true,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class ReactorAuditMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

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
     * Output directory for audit reports.
     */
    @Parameter(property = "hexaglue.reportDirectory", defaultValue = "${project.build.directory}/hexaglue/reports")
    private File reportDirectory;

    /**
     * Audit configuration (rules, thresholds, etc.).
     */
    @Parameter
    private AuditConfig auditConfig;

    /**
     * Whether to fail the build if unclassified types remain.
     *
     * @since 5.0.0
     */
    @Parameter(property = "hexaglue.failOnUnclassified", defaultValue = "false")
    private boolean failOnUnclassified;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("HexaGlue reactor-audit skipped");
            return;
        }

        getLog().info("HexaGlue reactor-audit: auditing all modules for base package: " + basePackage);

        // Load configurations from root hexaglue.yaml
        Path rootBaseDir = session.getTopLevelProject() != null
                ? session.getTopLevelProject().getBasedir().toPath()
                : session.getProjects().get(0).getBasedir().toPath();

        // Build plugin configs: audit config override if present, otherwise YAML
        Map<String, Map<String, Object>> pluginConfigs = auditConfig != null
                ? auditConfig.toPluginConfigs()
                : MojoConfigLoader.loadPluginConfigs(rootBaseDir, getLog());

        ClassificationConfig classificationConfig =
                MojoConfigLoader.loadClassificationConfig(rootBaseDir, failOnUnclassified, getLog());

        // Build unified engine config from reactor
        EngineConfig config = ReactorEngineConfigBuilder.build(
                session,
                basePackage,
                reportDirectory.toPath(),
                pluginConfigs,
                classificationConfig,
                Set.of(PluginCategory.AUDIT),
                true, // Include @Generated types so audit can see generated adapters
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

        if (!result.isSuccess()) {
            throw new MojoExecutionException("HexaGlue reactor analysis failed with errors");
        }

        // Extract audit snapshot from plugin results
        Optional<AuditSnapshot> auditSnapshot = extractAuditSnapshot(result);
        if (auditSnapshot.isEmpty()) {
            getLog().warn("No audit plugin executed - ensure an audit plugin is on the classpath");
            return;
        }

        AuditSnapshot snapshot = auditSnapshot.get();

        // Log summary
        logAuditSummary(snapshot);

        // Check failure conditions
        if (failOnError && snapshot.errorCount() > 0) {
            throw new MojoFailureException("Reactor audit failed with " + snapshot.errorCount() + " ERROR violations");
        }

        if (failOnWarning && snapshot.warningCount() > 0) {
            throw new MojoFailureException(
                    "Reactor audit failed with " + snapshot.warningCount() + " WARNING violations");
        }

        getLog().info("Reactor audit completed: " + (snapshot.passed() ? "PASSED" : "FAILED"));
    }

    private Optional<AuditSnapshot> extractAuditSnapshot(EngineResult result) {
        if (result.pluginResult() == null) {
            getLog().debug("No plugin execution result available");
            return Optional.empty();
        }

        Optional<AuditSnapshot> snapshot = result.pluginResult().findOutput("audit-snapshot", AuditSnapshot.class);

        if (snapshot.isEmpty()) {
            getLog().warn("No audit plugin produced an AuditSnapshot - "
                    + "ensure an audit plugin is on the classpath");
        }

        return snapshot;
    }

    private void logAuditSummary(AuditSnapshot snapshot) {
        getLog().info("─".repeat(80));
        getLog().info("REACTOR AUDIT SUMMARY");
        getLog().info("─".repeat(80));
        getLog().info("Total Violations: " + snapshot.violations().size());
        getLog().info("  Errors:   " + snapshot.errorCount());
        getLog().info("  Warnings: " + snapshot.warningCount());
        getLog().info("  Info:     " + snapshot.infos().size());
        getLog().info("Status: " + (snapshot.passed() ? "PASSED" : "FAILED"));
        getLog().info("─".repeat(80));
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
