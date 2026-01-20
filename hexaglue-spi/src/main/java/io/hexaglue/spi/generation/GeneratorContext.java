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

package io.hexaglue.spi.generation;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import java.util.Objects;
import java.util.Optional;

/**
 * Execution context provided to generator plugins.
 *
 * <p>This record encapsulates all the resources and information a generator
 * plugin needs to perform code generation. It provides access to:
 * <ul>
 *   <li>File writing capabilities (for Java sources, resources, docs)</li>
 *   <li>Diagnostic reporting (for warnings, errors, info messages)</li>
 *   <li>Plugin-specific configuration</li>
 *   <li>The architectural model (v4.1.0+)</li>
 * </ul>
 *
 * <p>Generator plugins receive this context when their {@link GeneratorPlugin#generate(GeneratorContext)}
 * method is invoked by the HexaGlue engine.
 *
 * <h2>v4.1 API</h2>
 *
 * <p>Since HexaGlue 4.1.0, the GeneratorContext provides direct access to the
 * {@link ArchitecturalModel} via {@link #model()}, as well as convenience methods
 * for accessing the new domain and port indices.
 *
 * <p>Example v4.1 usage:
 * <pre>{@code
 * public class MyPlugin implements GeneratorPlugin {
 *
 *     @Override
 *     public void generate(GeneratorContext context) {
 *         // Access model directly from context (v4.1.0+)
 *         context.model().ifPresent(model -> {
 *             model.domainIndex().ifPresent(domain -> {
 *                 domain.aggregateRoots().forEach(agg -> {
 *                     Field identity = agg.identityField();
 *                     context.writer().writeJavaSource(
 *                         agg.packageName() + ".infra",
 *                         agg.simpleName() + "Entity",
 *                         generateEntityCode(agg)
 *                     );
 *                 });
 *             });
 *         });
 *     }
 * }
 * }</pre>
 *
 * <h2>v4.0 Migration (Deprecated)</h2>
 *
 * <p>Since HexaGlue 4.0, plugins should use {@code PluginContext.model()} to access the
 * {@link ArchitecturalModel} instead of {@code GeneratorContext.snapshot()}.
 * The snapshot field is deprecated and may be null.
 *
 * @param snapshot       the classification snapshot (DEPRECATED in v4, may be null)
 * @param writer         the artifact writer for generating files
 * @param diagnostics    the diagnostic reporter for messages
 * @param config         the plugin-specific configuration
 * @param pluginContext  the parent plugin context for model access (v4.1.0, may be null)
 * @since 3.0.0
 * @since 4.1.0 - Added pluginContext field and model access methods
 */
public record GeneratorContext(
        @Deprecated(forRemoval = true, since = "4.0.0") ClassificationSnapshot snapshot,
        ArtifactWriter writer,
        DiagnosticReporter diagnostics,
        PluginConfig config,
        PluginContext pluginContext) {

    /**
     * Compact constructor with validation.
     *
     * <p>Note: snapshot and pluginContext may be null in v4+.
     * Use {@link #model()} for model access.
     *
     * @throws NullPointerException if writer, diagnostics, or config is null
     */
    public GeneratorContext {
        // NOTE: snapshot CAN be null in v4 - use model() instead
        // NOTE: pluginContext CAN be null for backward compatibility
        Objects.requireNonNull(writer, "writer required");
        Objects.requireNonNull(diagnostics, "diagnostics required");
        Objects.requireNonNull(config, "config required");
    }

    /**
     * Creates a GeneratorContext without the pluginContext field.
     *
     * <p>This factory method provides backward compatibility for existing code
     * that doesn't provide a PluginContext.
     *
     * @param snapshot    the classification snapshot (may be null)
     * @param writer      the artifact writer
     * @param diagnostics the diagnostic reporter
     * @param config      the plugin configuration
     * @return a new GeneratorContext
     * @deprecated Use the canonical constructor with pluginContext for v4.1.0+
     */
    @Deprecated(since = "4.1.0")
    public static GeneratorContext of(
            ClassificationSnapshot snapshot,
            ArtifactWriter writer,
            DiagnosticReporter diagnostics,
            PluginConfig config) {
        return new GeneratorContext(snapshot, writer, diagnostics, config, null);
    }

    /**
     * Creates a GeneratorContext with full v4.1.0 support.
     *
     * @param writer        the artifact writer
     * @param diagnostics   the diagnostic reporter
     * @param config        the plugin configuration
     * @param pluginContext the parent plugin context
     * @return a new GeneratorContext
     * @since 4.1.0
     */
    public static GeneratorContext withPluginContext(
            ArtifactWriter writer, DiagnosticReporter diagnostics, PluginConfig config, PluginContext pluginContext) {
        return new GeneratorContext(null, writer, diagnostics, config, pluginContext);
    }

    // === v4.1.0 Model Access API ===

    /**
     * Returns the architectural model.
     *
     * <p>Prefer using this over the deprecated snapshot().
     *
     * @return an optional containing the model, or empty if no pluginContext
     * @since 4.1.0
     */
    public Optional<ArchitecturalModel> model() {
        return Optional.ofNullable(pluginContext).map(PluginContext::model);
    }

    /**
     * Returns the domain index for accessing v4.1.0 domain types.
     *
     * @return an optional containing the domain index, or empty if not available
     * @since 4.1.0
     */
    public Optional<DomainIndex> domainIndex() {
        return model().flatMap(ArchitecturalModel::domainIndex);
    }

    /**
     * Returns the port index for accessing v4.1.0 port types.
     *
     * @return an optional containing the port index, or empty if not available
     * @since 4.1.0
     */
    public Optional<PortIndex> portIndex() {
        return model().flatMap(ArchitecturalModel::portIndex);
    }

    /**
     * Returns the classification report.
     *
     * @return an optional containing the classification report, or empty if not available
     * @since 4.1.0
     */
    public Optional<ClassificationReport> classificationReport() {
        return model().flatMap(ArchitecturalModel::classificationReport);
    }

    // === Deprecated API ===

    /**
     * Returns the underlying IR snapshot for advanced use cases.
     *
     * @return the IR snapshot, or null if using v4 ArchitecturalModel
     * @deprecated Since 4.0.0. Use {@link #model()} instead.
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public io.hexaglue.spi.ir.IrSnapshot ir() {
        return snapshot != null ? snapshot.snapshot() : null;
    }
}
