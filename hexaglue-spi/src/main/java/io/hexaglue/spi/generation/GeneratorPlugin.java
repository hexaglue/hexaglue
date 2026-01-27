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

import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginContext;

/**
 * Service Provider Interface for code generation plugins.
 *
 * <p>Generator plugins analyze the classified domain model and produce code
 * artifacts such as JPA entities, REST controllers, GraphQL schemas, database
 * migration scripts, and more.
 *
 * <p>Generator plugins are discovered via {@link java.util.ServiceLoader} and
 * must be registered in {@code META-INF/services/io.hexaglue.spi.plugin.HexaGluePlugin}.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Plugin is discovered and instantiated by ServiceLoader</li>
 *   <li>HexaGlue analyzes the codebase and produces a classification snapshot</li>
 *   <li>Plugin dependencies are resolved and executed first</li>
 *   <li>{@link #generate(GeneratorContext)} is called with the complete context</li>
 *   <li>Plugin generates code using the provided writer</li>
 *   <li>Diagnostics are collected and reported to the user</li>
 * </ol>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class JpaGeneratorPlugin implements GeneratorPlugin {
 *
 *     @Override
 *     public String id() {
 *         return "io.hexaglue.plugin.jpa";
 *     }
 *
 *     @Override
 *     public void generate(GeneratorContext context) {
 *         ClassificationSnapshot snapshot = context.snapshot();
 *         ArtifactWriter writer = context.writer();
 *
 *         // Generate JPA entities for aggregate roots
 *         snapshot.domain().aggregateRoots().forEach(aggregateRoot -> {
 *             if (aggregateRoot.certainty().isReliable()) {
 *                 String entityCode = generateEntityCode(aggregateRoot);
 *                 writer.writeJavaSource(
 *                     aggregateRoot.packageName() + ".infra.persistence",
 *                     aggregateRoot.simpleName() + "Entity",
 *                     entityCode
 *                 );
 *             } else {
 *                 context.diagnostics().warn(
 *                     "Skipping uncertain classification: " + aggregateRoot.qualifiedName()
 *                 );
 *             }
 *         });
 *     }
 *
 *     private String generateEntityCode(DomainType aggregateRoot) {
 *         // Code generation logic using templates or builders
 *         return "...";
 *     }
 * }
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Check {@link io.hexaglue.arch.model.classification.CertaintyLevel} before generating code</li>
 *   <li>Report warnings for uncertain classifications instead of failing</li>
 *   <li>Use {@link io.hexaglue.spi.plugin.DiagnosticReporter} for progress and error messages</li>
 *   <li>Handle {@link java.io.IOException} gracefully and report via diagnostics</li>
 *   <li>Respect plugin configuration from {@link GeneratorContext#config()}</li>
 *   <li>Use {@link io.hexaglue.spi.plugin.TemplateEngine} for simple templating needs</li>
 * </ul>
 *
 * @since 3.0.0
 */
public interface GeneratorPlugin extends HexaGluePlugin {

    /**
     * Generates code artifacts based on the classified domain model.
     *
     * <p>This method is called by the HexaGlue engine after the analysis phase
     * completes and all plugin dependencies have been executed.
     *
     * <p>Implementations should:
     * <ol>
     *   <li>Read the classification snapshot from the context</li>
     *   <li>Filter relevant domain types based on certainty level</li>
     *   <li>Generate code using templates or builders</li>
     *   <li>Write files using the artifact writer</li>
     *   <li>Report progress and errors via diagnostics</li>
     * </ol>
     *
     * @param context the generation context (never null)
     * @throws Exception if generation fails (exceptions are caught and reported)
     */
    void generate(GeneratorContext context) throws Exception;

    /**
     * Returns the plugin category (always GENERATOR for generator plugins).
     *
     * @return {@link PluginCategory#GENERATOR}
     */
    @Override
    default PluginCategory category() {
        return PluginCategory.GENERATOR;
    }

    /**
     * Executes the plugin (delegates to generate with context adaptation).
     *
     * <p>This method adapts the generic {@link PluginContext} to the specialized
     * {@link GeneratorContext} required by generator plugins.
     *
     * <p>Since v4.1.0, the GeneratorContext includes a reference to the PluginContext,
     * allowing direct access to the model via {@link GeneratorContext#model()}.
     *
     * @param context the generic plugin context
     * @since 4.0.0 - Updated to use ArchitecturalModel instead of IrSnapshot
     * @since 4.1.0 - GeneratorContext now includes PluginContext for model access
     */
    @Override
    default void execute(PluginContext context) {
        try {
            GeneratorContext generatorContext = new GeneratorContext(
                    ArtifactWriter.of(context.writer()), context.diagnostics(), context.config(), context);
            generate(generatorContext);
        } catch (Exception e) {
            context.diagnostics().error("Generator plugin execution failed: " + id(), e);
        }
    }
}
