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

package io.hexaglue.core.engine;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.core.plugin.PluginExecutionResult;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Result of the HexaGlue analysis.
 *
 * @param model the architectural model (never null)
 * @param diagnostics any warnings or errors encountered during analysis
 * @param metrics analysis metrics
 * @param pluginResult results from plugin execution (null if plugins not enabled)
 * @param primaryClassifications the primary classification results for validation (since 3.0.0)
 * @since 4.0.0 - Changed from IrSnapshot to ArchitecturalModel
 */
public record EngineResult(
        ArchitecturalModel model,
        List<Diagnostic> diagnostics,
        EngineMetrics metrics,
        PluginExecutionResult pluginResult,
        List<PrimaryClassificationResult> primaryClassifications) {

    /**
     * Creates a result without plugin execution.
     */
    public static EngineResult withoutPlugins(
            ArchitecturalModel model,
            List<Diagnostic> diagnostics,
            EngineMetrics metrics,
            List<PrimaryClassificationResult> primaryClassifications) {
        return new EngineResult(model, diagnostics, metrics, null, primaryClassifications);
    }

    /**
     * Returns true if the analysis completed without errors.
     */
    public boolean isSuccess() {
        boolean analysisSuccess = diagnostics.stream().noneMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
        boolean pluginSuccess = pluginResult == null || pluginResult.isSuccess();
        return analysisSuccess && pluginSuccess;
    }

    /**
     * Returns only error diagnostics.
     */
    public List<Diagnostic> errors() {
        return diagnostics.stream()
                .filter(d -> d.severity() == Diagnostic.Severity.ERROR)
                .toList();
    }

    /**
     * Returns only warning diagnostics.
     */
    public List<Diagnostic> warnings() {
        return diagnostics.stream()
                .filter(d -> d.severity() == Diagnostic.Severity.WARNING)
                .toList();
    }

    /**
     * Returns all generated files from plugins.
     */
    public List<Path> generatedFiles() {
        return pluginResult != null ? pluginResult.allGeneratedFiles() : List.of();
    }

    /**
     * Returns the total number of generated files.
     */
    public int generatedFileCount() {
        return pluginResult != null ? pluginResult.totalGeneratedFiles() : 0;
    }

    /**
     * Returns the count of unclassified types.
     *
     * @return number of types that could not be classified
     * @since 3.0.0
     */
    public int unclassifiedCount() {
        if (primaryClassifications == null) {
            return 0;
        }
        return (int)
                primaryClassifications.stream().filter(r -> !r.isClassified()).count();
    }

    /**
     * Returns the list of unclassified type results.
     *
     * @return unclassified types
     * @since 3.0.0
     */
    public List<PrimaryClassificationResult> unclassifiedTypes() {
        if (primaryClassifications == null) {
            return List.of();
        }
        return primaryClassifications.stream().filter(r -> !r.isClassified()).toList();
    }

    /**
     * Returns true if there are no unclassified types.
     *
     * @return true if validation passes (no unclassified types)
     * @since 3.0.0
     */
    public boolean validationPassed() {
        return unclassifiedCount() == 0;
    }
}
