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

import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import java.util.Objects;

/**
 * Execution context provided to generator plugins.
 *
 * <p>This record encapsulates all the resources and information a generator
 * plugin needs to perform code generation. It provides access to:
 * <ul>
 *   <li>File writing capabilities (for Java sources, resources, docs)</li>
 *   <li>Diagnostic reporting (for warnings, errors, info messages)</li>
 *   <li>Plugin-specific configuration</li>
 * </ul>
 *
 * <p>Generator plugins receive this context when their {@link GeneratorPlugin#generate(GeneratorContext)}
 * method is invoked by the HexaGlue engine.
 *
 * <h2>v4 Migration</h2>
 *
 * <p>Since HexaGlue 4.0, plugins should use {@code PluginContext.model()} to access the
 * {@link io.hexaglue.arch.ArchitecturalModel} instead of {@code GeneratorContext.snapshot()}.
 * The snapshot field is deprecated and may be null.
 *
 * <p>Example v4 usage:
 * <pre>{@code
 * public class MyPlugin implements GeneratorPlugin {
 *     private ArchitecturalModel model;
 *
 *     @Override
 *     public void execute(PluginContext context) {
 *         this.model = context.model();  // Capture v4 model
 *         super.execute(context);
 *     }
 *
 *     @Override
 *     public void generate(GeneratorContext context) {
 *         // Use this.model instead of context.snapshot()
 *         model.aggregates().forEach(agg -> {
 *             context.writer().writeJavaSource(
 *                 agg.packageName() + ".infra",
 *                 agg.simpleName() + "Entity",
 *                 generateEntityCode(agg)
 *             );
 *         });
 *     }
 * }
 * }</pre>
 *
 * @param snapshot    the classification snapshot (DEPRECATED in v4, may be null)
 * @param writer      the artifact writer for generating files
 * @param diagnostics the diagnostic reporter for messages
 * @param config      the plugin-specific configuration
 * @since 3.0.0
 */
public record GeneratorContext(
        @Deprecated(forRemoval = true, since = "4.0.0") ClassificationSnapshot snapshot,
        ArtifactWriter writer,
        DiagnosticReporter diagnostics,
        PluginConfig config) {

    /**
     * Compact constructor with validation.
     *
     * <p>Note: snapshot may be null in v4. Use {@code PluginContext.model()} instead.
     *
     * @throws NullPointerException if writer, diagnostics, or config is null
     */
    public GeneratorContext {
        // NOTE: snapshot CAN be null in v4 - use PluginContext.model() instead
        Objects.requireNonNull(writer, "writer required");
        Objects.requireNonNull(diagnostics, "diagnostics required");
        Objects.requireNonNull(config, "config required");
    }

    /**
     * Returns the underlying IR snapshot for advanced use cases.
     *
     * @return the IR snapshot, or null if using v4 ArchitecturalModel
     * @deprecated Since 4.0.0. Use {@code PluginContext.model()} instead.
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public io.hexaglue.spi.ir.IrSnapshot ir() {
        return snapshot != null ? snapshot.snapshot() : null;
    }
}
