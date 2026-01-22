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
 *   <li>The architectural model</li>
 * </ul>
 *
 * <p>Generator plugins receive this context when their {@link GeneratorPlugin#generate(GeneratorContext)}
 * method is invoked by the HexaGlue engine.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MyPlugin implements GeneratorPlugin {
 *
 *     @Override
 *     public void generate(GeneratorContext context) {
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
 * @param writer         the artifact writer for generating files
 * @param diagnostics    the diagnostic reporter for messages
 * @param config         the plugin-specific configuration
 * @param pluginContext  the parent plugin context for model access
 * @since 3.0.0
 * @since 5.0.0 - Removed deprecated snapshot field
 */
public record GeneratorContext(
        ArtifactWriter writer,
        DiagnosticReporter diagnostics,
        PluginConfig config,
        PluginContext pluginContext) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if writer, diagnostics, or config is null
     */
    public GeneratorContext {
        Objects.requireNonNull(writer, "writer required");
        Objects.requireNonNull(diagnostics, "diagnostics required");
        Objects.requireNonNull(config, "config required");
        // NOTE: pluginContext CAN be null for backward compatibility
    }

    /**
     * Creates a GeneratorContext with all required parameters.
     *
     * @param writer        the artifact writer
     * @param diagnostics   the diagnostic reporter
     * @param config        the plugin configuration
     * @param pluginContext the parent plugin context
     * @return a new GeneratorContext
     * @since 5.0.0
     */
    public static GeneratorContext of(
            ArtifactWriter writer, DiagnosticReporter diagnostics, PluginConfig config, PluginContext pluginContext) {
        return new GeneratorContext(writer, diagnostics, config, pluginContext);
    }

    // === Model Access API ===

    /**
     * Returns the architectural model.
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
}
