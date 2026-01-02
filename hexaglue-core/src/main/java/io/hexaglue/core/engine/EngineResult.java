package io.hexaglue.core.engine;

import io.hexaglue.core.plugin.PluginExecutionResult;
import io.hexaglue.spi.ir.IrSnapshot;
import java.nio.file.Path;
import java.util.List;

/**
 * Result of the HexaGlue analysis.
 *
 * @param ir the intermediate representation (never null)
 * @param diagnostics any warnings or errors encountered during analysis
 * @param metrics analysis metrics
 * @param pluginResult results from plugin execution (null if plugins not enabled)
 */
public record EngineResult(
        IrSnapshot ir, List<Diagnostic> diagnostics, EngineMetrics metrics, PluginExecutionResult pluginResult) {

    /**
     * Creates a result without plugin execution.
     */
    public static EngineResult withoutPlugins(IrSnapshot ir, List<Diagnostic> diagnostics, EngineMetrics metrics) {
        return new EngineResult(ir, diagnostics, metrics, null);
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
}
