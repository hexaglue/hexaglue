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

package io.hexaglue.spi.audit;

import io.hexaglue.spi.generation.PluginCategory;
import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginContext;

/**
 * Service Provider Interface for code audit plugins.
 *
 * <p>Audit plugins analyze the codebase for quality, compliance, and
 * architectural issues. They apply a set of rules to code units and produce
 * an audit snapshot with violations and metrics.
 *
 * <p>Audit plugins are discovered via {@link java.util.ServiceLoader} and
 * must be registered in {@code META-INF/services/io.hexaglue.spi.plugin.HexaGluePlugin}.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class HexagonalArchitectureAuditPlugin implements AuditPlugin {
 *
 *     @Override
 *     public String id() {
 *         return "io.hexaglue.audit.hexagonal";
 *     }
 *
 *     @Override
 *     public AuditSnapshot audit(AuditContext context) {
 *         Codebase codebase = context.codebase();
 *         List<RuleViolation> violations = new ArrayList<>();
 *
 *         // Apply all rules to all code units
 *         for (CodeUnit unit : codebase.units()) {
 *             for (AuditRule rule : context.rules()) {
 *                 violations.addAll(rule.check(unit));
 *             }
 *         }
 *
 *         // Compute metrics
 *         QualityMetrics qualityMetrics = computeQualityMetrics(codebase);
 *         ArchitectureMetrics archMetrics = computeArchitectureMetrics(codebase);
 *
 *         return new AuditSnapshot(
 *             codebase,
 *             DetectedArchitectureStyle.HEXAGONAL,
 *             violations,
 *             qualityMetrics,
 *             archMetrics,
 *             new AuditMetadata(Instant.now(), "3.0.0", Duration.ofSeconds(5))
 *         );
 *     }
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
public interface AuditPlugin extends HexaGluePlugin {

    /**
     * Performs the audit and returns the complete snapshot.
     *
     * <p>This method is called by the HexaGlue engine after the analysis phase
     * completes. Implementations should apply all configured rules to the
     * codebase and collect violations and metrics.
     *
     * @param context the audit context (never null)
     * @return the complete audit snapshot
     * @throws Exception if audit execution fails
     */
    AuditSnapshot audit(AuditContext context) throws Exception;

    /**
     * Returns the plugin category (always AUDIT for audit plugins).
     *
     * @return {@link PluginCategory#AUDIT}
     */
    @Override
    default PluginCategory category() {
        return PluginCategory.AUDIT;
    }

    /**
     * Executes the plugin (delegates to audit with context adaptation).
     *
     * <p>Since v4, this default implementation is no longer supported.
     * Plugins must override this method and use {@code context.model()} to
     * build the codebase from the ArchitecturalModel.
     *
     * <p>The resulting {@link AuditSnapshot} should be stored in the plugin output store
     * under the key {@code "audit-snapshot"} for retrieval by the engine/mojos.
     *
     * @param context the generic plugin context
     * @throws UnsupportedOperationException always, since v4 requires plugins to override this method
     * @since 4.0.0 - This method must be overridden by v4 plugins
     */
    @Override
    default void execute(PluginContext context) {
        throw new UnsupportedOperationException(
                "v4 AuditPlugin implementations must override execute() and use context.model() "
                        + "to build the Codebase from ArchitecturalModel. "
                        + "See DddAuditPlugin for an example implementation.");
    }
}
