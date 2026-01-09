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
 *   <li>The analyzed application model (classification results)</li>
 *   <li>File writing capabilities (for Java sources, resources, docs)</li>
 *   <li>Diagnostic reporting (for warnings, errors, info messages)</li>
 *   <li>Plugin-specific configuration</li>
 * </ul>
 *
 * <p>Generator plugins receive this context when their {@link GeneratorPlugin#generate(GeneratorContext)}
 * method is invoked by the HexaGlue engine.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class JpaGeneratorPlugin implements GeneratorPlugin {
 *     @Override
 *     public void generate(GeneratorContext context) {
 *         ClassificationSnapshot snapshot = context.snapshot();
 *         ArtifactWriter writer = context.writer();
 *         DiagnosticReporter diagnostics = context.diagnostics();
 *
 *         snapshot.domain().aggregateRoots().forEach(aggregateRoot -> {
 *             String entityCode = generateEntityCode(aggregateRoot);
 *             try {
 *                 writer.writeJavaSource(
 *                     aggregateRoot.packageName() + ".infra",
 *                     aggregateRoot.simpleName() + "Entity",
 *                     entityCode
 *                 );
 *                 diagnostics.info("Generated JPA entity for " + aggregateRoot.simpleName());
 *             } catch (IOException e) {
 *                 diagnostics.error("Failed to write entity", e);
 *             }
 *         });
 *     }
 * }
 * }</pre>
 *
 * @param snapshot    the analyzed application model with classification results
 * @param writer      the artifact writer for generating files
 * @param diagnostics the diagnostic reporter for messages
 * @param config      the plugin-specific configuration
 * @since 3.0.0
 */
public record GeneratorContext(
        ClassificationSnapshot snapshot, ArtifactWriter writer, DiagnosticReporter diagnostics, PluginConfig config) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any parameter is null
     */
    public GeneratorContext {
        Objects.requireNonNull(snapshot, "snapshot required");
        Objects.requireNonNull(writer, "writer required");
        Objects.requireNonNull(diagnostics, "diagnostics required");
        Objects.requireNonNull(config, "config required");
    }

    /**
     * Returns the underlying IR snapshot for advanced use cases.
     *
     * <p>Most plugins should use {@link #snapshot()} instead, but this method
     * provides direct access to the IR snapshot if needed.
     *
     * @return the IR snapshot
     */
    public io.hexaglue.spi.ir.IrSnapshot ir() {
        return snapshot.snapshot();
    }
}
