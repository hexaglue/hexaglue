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

package io.hexaglue.plugin.audit.adapter.analyzer;

import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.CouplingMetrics;
import io.hexaglue.arch.model.audit.ZoneClassification;
import io.hexaglue.plugin.audit.domain.model.PackageZoneMetrics;
import io.hexaglue.plugin.audit.domain.model.ZoneCategory;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.List;
import java.util.Objects;

/**
 * Analyzes packages according to Robert C. Martin's metrics.
 *
 * <p><b>REFACTORED (v3):</b> This analyzer now delegates calculations to the Core
 * via {@link ArchitectureQuery}. It only performs judgment and categorization.
 *
 * <p>Principle: "Le Core produit des faits, les plugins les exploitent."
 *
 * <p>This analyzer implements the zone analysis framework described in
 * "Agile Software Development: Principles, Patterns, and Practices" by
 * Robert C. Martin. It evaluates packages based on their position in the
 * Abstractness-Instability plane to identify architectural health issues.
 *
 * <p><strong>Metrics calculated by Core:</strong>
 * <ul>
 *   <li><strong>Abstractness (A):</strong> Ratio of abstract types (interfaces,
 *       abstract classes) to total types in the package.</li>
 *   <li><strong>Instability (I):</strong> Ratio of outgoing dependencies to total
 *       dependencies (outgoing + incoming).</li>
 *   <li><strong>Distance (D):</strong> Normalized distance from the Main Sequence,
 *       calculated as |A + I - 1|.</li>
 * </ul>
 *
 * <p><strong>Zone categorization:</strong>
 * <ul>
 *   <li><strong>Zone of Pain:</strong> Concrete and stable (A ≈ 0, I ≈ 0, D &gt; 0.3).
 *       Hard to change due to many dependents.</li>
 *   <li><strong>Zone of Uselessness:</strong> Abstract and unstable (A ≈ 1, I ≈ 1, D &gt; 0.3).
 *       Unused abstractions that add complexity.</li>
 *   <li><strong>Main Sequence:</strong> Balanced (0 &lt; D ≤ 0.3). Healthy packages.</li>
 *   <li><strong>Ideal:</strong> Perfect balance (D = 0). Optimal design.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ZoneAnalyzer {

    /**
     * Analyzes all packages using ArchitectureQuery from Core.
     *
     * <p>Algorithm:
     * <pre>
     * 1. Call architectureQuery.analyzeAllPackageCoupling()
     * 2. For each CouplingMetrics:
     *    a. Extract zone from CouplingMetrics.zone()
     *    b. Convert ZoneClassification -&gt; ZoneCategory (plugin domain)
     *    c. Build PackageZoneMetrics
     * 3. Return list of metrics
     * </pre>
     *
     * @param codebase the codebase (for package discovery, if needed)
     * @param architectureQuery the query interface from Core (required)
     * @return list of package metrics, one per non-empty package
     * @throws NullPointerException if architectureQuery is null
     */
    public List<PackageZoneMetrics> analyze(Codebase codebase, ArchitectureQuery architectureQuery) {
        Objects.requireNonNull(architectureQuery, "architectureQuery is required - Core must provide it");

        // Delegate ALL calculations to Core
        List<CouplingMetrics> couplingMetrics = architectureQuery.analyzeAllPackageCoupling();

        // Transform Core facts into plugin domain objects
        return couplingMetrics.stream().map(this::toPackageZoneMetrics).toList();
    }

    /**
     * Converts a CouplingMetrics (SPI) to PackageZoneMetrics (plugin domain).
     *
     * <p>This is a JUDGMENT operation - converting Core facts to plugin decisions.
     *
     * @param metrics the coupling metrics from Core
     * @return the package zone metrics for the plugin
     */
    private PackageZoneMetrics toPackageZoneMetrics(CouplingMetrics metrics) {
        // All calculations are already done in CouplingMetrics (SPI record)
        double abstractness = metrics.abstractness();
        double instability = metrics.instability();
        double distance = metrics.distanceFromMainSequence();

        // Get zone classification from SPI
        ZoneClassification spiZone = metrics.zone();

        // Convert to plugin domain enum
        ZoneCategory pluginZone = convertZone(spiZone);

        return new PackageZoneMetrics(metrics.packageName(), abstractness, instability, distance, pluginZone);
    }

    /**
     * Converts SPI ZoneClassification to plugin domain ZoneCategory.
     *
     * @param spiZone the SPI zone
     * @return the plugin domain zone
     */
    private ZoneCategory convertZone(ZoneClassification spiZone) {
        return switch (spiZone) {
            case IDEAL -> ZoneCategory.IDEAL;
            case MAIN_SEQUENCE -> ZoneCategory.MAIN_SEQUENCE;
            case ZONE_OF_PAIN -> ZoneCategory.ZONE_OF_PAIN;
            case ZONE_OF_USELESSNESS -> ZoneCategory.ZONE_OF_USELESSNESS;
        };
    }

    // ========================================================================
    // REMOVED: The following methods have been removed because they were
    // duplicating calculations already done in Core. Use ArchitectureQuery
    // instead.
    //
    // - calculateAbstractness(List<CodeUnit>)
    //   -> Use CouplingMetrics.abstractness()
    //
    // - calculateInstability(String, Map<String, Set<String>>)
    //   -> Use CouplingMetrics.instability()
    //
    // - calculateDistance(double, double)
    //   -> Use CouplingMetrics.distanceFromMainSequence()
    //
    // - buildPackageDependencies(Codebase)
    //   -> Core already does this calculation via the graph
    //
    // - isAbstractType(CodeUnit)
    //   -> Core uses TypeNode.isAbstract() with real modifiers
    //
    // - groupByPackage(List<CodeUnit>)
    //   -> Core handles package grouping internally
    //
    // - extractPackageName(String)
    //   -> Core handles this internally
    // ========================================================================
}
