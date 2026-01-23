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

package io.hexaglue.spi.plugin;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.Optional;

/**
 * Execution context provided to plugins.
 *
 * <p>This interface provides access to:
 * <ul>
 *   <li>The unified architectural model ({@link ArchitecturalModel})</li>
 *   <li>Plugin-specific configuration</li>
 *   <li>Code generation facilities</li>
 *   <li>Diagnostic reporting</li>
 *   <li>Output sharing between plugins</li>
 * </ul>
 *
 * @since 1.0.0
 * @since 4.0.0 - Removed deprecated ir() method, use model() instead
 */
public interface PluginContext {

    /**
     * Returns the unified architectural model.
     *
     * <p>The model is immutable and represents the complete analysis result,
     * including all classified domain elements, ports, and their relationships.
     *
     * @return the architectural model (never null)
     * @since 4.0.0
     */
    ArchitecturalModel model();

    /**
     * Returns the plugin configuration.
     *
     * @return the configuration (never null, may be empty)
     */
    PluginConfig config();

    /**
     * Returns the code writer for generating files.
     *
     * @return the code writer (never null)
     */
    CodeWriter writer();

    /**
     * Returns the diagnostic reporter.
     *
     * @return the diagnostic reporter (never null)
     */
    DiagnosticReporter diagnostics();

    /**
     * Stores an output value that can be retrieved by other plugins.
     *
     * <p>This enables plugins to share computed data. For example, a JPA plugin
     * might share a map of generated entity classes for use by a Liquibase plugin.
     *
     * <p>Example:
     * <pre>{@code
     * // In JPA plugin
     * Map<String, JpaEntity> entities = generateEntities();
     * context.setOutput("generated-entities", entities);
     *
     * // In Liquibase plugin (which depends on JPA plugin)
     * Optional<Map<String, JpaEntity>> entities = context.getOutput(
     *     "io.hexaglue.plugin.jpa", "generated-entities", Map.class);
     * }</pre>
     *
     * @param key the output key (unique within this plugin)
     * @param value the value to store
     * @param <T> the value type
     */
    <T> void setOutput(String key, T value);

    /**
     * Retrieves an output value stored by another plugin.
     *
     * <p>The plugin must be listed in {@link HexaGluePlugin#dependsOn()} to ensure
     * it has already executed before trying to read its output.
     *
     * @param pluginId the ID of the plugin that stored the output
     * @param key the output key
     * @param type the expected value type
     * @param <T> the value type
     * @return the output value, or empty if not found or wrong type
     */
    <T> Optional<T> getOutput(String pluginId, String key, Class<T> type);

    /**
     * Returns the ID of the currently executing plugin.
     *
     * @return the plugin ID
     */
    String currentPluginId();

    /**
     * Returns the template engine for code generation.
     *
     * <p>The template engine provides simple placeholder-based rendering.
     * For more advanced templating needs, plugins can use external libraries.
     *
     * @return the template engine (never null)
     */
    TemplateEngine templates();

    /**
     * Returns the architecture query interface for advanced analysis.
     *
     * <p>This provides access to cycle detection, Lakos metrics, coupling analysis,
     * layer violation detection, and other architectural insights computed from
     * the full application dependency graph.
     *
     * @return the architecture query, or empty if not available
     * @since 3.0.0
     */
    default Optional<ArchitectureQuery> architectureQuery() {
        return Optional.empty();
    }

    // === New v4.1.0 API ===

    /**
     * Returns the domain index for accessing new v4.1.0 domain types.
     *
     * <p>The domain index provides type-safe access to enriched domain types
     * including {@code AggregateRoot}, {@code Entity}, {@code ValueObject}, etc.
     * with their computed metadata (identity fields, relationships, etc.).
     *
     * <p>Example:
     * <pre>{@code
     * context.domainIndex().ifPresent(domain -> {
     *     domain.aggregateRoots().forEach(agg -> {
     *         Field identity = agg.identityField();
     *         List<TypeRef> entities = agg.entities();
     *         generate(agg);
     *     });
     * });
     * }</pre>
     *
     * @return an optional containing the domain index, or empty if not available
     * @since 4.1.0
     */
    default Optional<DomainIndex> domainIndex() {
        return model().domainIndex();
    }

    /**
     * Returns the port index for accessing new v4.1.0 port types.
     *
     * <p>The port index provides type-safe access to enriched port types
     * including {@code DrivingPort} and {@code DrivenPort} with their
     * port type classification and managed aggregate references.
     *
     * <p>Example:
     * <pre>{@code
     * context.portIndex().ifPresent(ports -> {
     *     ports.repositories().forEach(repo -> {
     *         Optional<TypeRef> aggregate = repo.managedAggregate();
     *         DrivenPortType type = repo.portType();  // REPOSITORY
     *         generate(repo);
     *     });
     * });
     * }</pre>
     *
     * @return an optional containing the port index, or empty if not available
     * @since 4.1.0
     */
    default Optional<PortIndex> portIndex() {
        return model().portIndex();
    }

    /**
     * Returns the classification report with statistics and remediation hints.
     *
     * <p>The classification report provides insights into the classification
     * process, including:
     * <ul>
     *   <li>Classification statistics (rates, counts)</li>
     *   <li>Unclassified types grouped by category</li>
     *   <li>Classification conflicts</li>
     *   <li>Prioritized remediation suggestions</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * context.classificationReport().ifPresent(report -> {
     *     diagnostics().info("Classification rate: " +
     *         (report.stats().classificationRate() * 100) + "%");
     *
     *     report.actionRequired().forEach(unclassified ->
     *         diagnostics().warn("Needs attention: " +
     *             unclassified.id().simpleName()));
     * });
     * }</pre>
     *
     * @return an optional containing the classification report, or empty if not available
     * @since 4.1.0
     */
    default Optional<ClassificationReport> classificationReport() {
        return model().classificationReport();
    }
}
