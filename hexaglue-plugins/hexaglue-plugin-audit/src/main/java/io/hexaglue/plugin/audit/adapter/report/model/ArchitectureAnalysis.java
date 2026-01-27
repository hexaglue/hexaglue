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

package io.hexaglue.plugin.audit.adapter.report.model;

import io.hexaglue.arch.model.audit.CouplingMetrics;
import io.hexaglue.arch.model.audit.DependencyCycle;
import io.hexaglue.arch.model.audit.LayerViolation;
import io.hexaglue.arch.model.audit.StabilityViolation;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.List;
import java.util.Objects;

/**
 * Architecture analysis data for the audit report.
 *
 * <p>This record contains the results of advanced architecture analysis
 * including cycle detection, layer violations, stability violations,
 * and coupling metrics.
 *
 * @param typeCycles            type-level dependency cycles
 * @param packageCycles         package-level dependency cycles
 * @param boundedContextCycles  bounded context level cycles
 * @param layerViolations       layer dependency violations
 * @param stabilityViolations   stability principle violations
 * @param couplingMetrics       coupling metrics per package
 * @since 1.0.0
 */
public record ArchitectureAnalysis(
        List<CycleEntry> typeCycles,
        List<CycleEntry> packageCycles,
        List<CycleEntry> boundedContextCycles,
        List<LayerViolationEntry> layerViolations,
        List<StabilityViolationEntry> stabilityViolations,
        List<PackageCouplingEntry> couplingMetrics) {

    public ArchitectureAnalysis {
        typeCycles = typeCycles != null ? List.copyOf(typeCycles) : List.of();
        packageCycles = packageCycles != null ? List.copyOf(packageCycles) : List.of();
        boundedContextCycles = boundedContextCycles != null ? List.copyOf(boundedContextCycles) : List.of();
        layerViolations = layerViolations != null ? List.copyOf(layerViolations) : List.of();
        stabilityViolations = stabilityViolations != null ? List.copyOf(stabilityViolations) : List.of();
        couplingMetrics = couplingMetrics != null ? List.copyOf(couplingMetrics) : List.of();
    }

    /**
     * Creates an empty architecture analysis.
     */
    public static ArchitectureAnalysis empty() {
        return new ArchitectureAnalysis(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Creates architecture analysis from an ArchitectureQuery.
     *
     * @param query the architecture query (may be null)
     * @return the architecture analysis
     */
    public static ArchitectureAnalysis from(ArchitectureQuery query) {
        if (query == null) {
            return empty();
        }

        // Convert type-level cycles
        List<CycleEntry> typeCycles =
                query.findDependencyCycles().stream().map(CycleEntry::from).toList();

        // Convert package-level cycles
        List<CycleEntry> packageCycles =
                query.findPackageCycles().stream().map(CycleEntry::from).toList();

        // Convert bounded context cycles
        List<CycleEntry> boundedContextCycles =
                query.findBoundedContextCycles().stream().map(CycleEntry::from).toList();

        // Convert layer violations
        List<LayerViolationEntry> layerViolations = query.findLayerViolations().stream()
                .map(LayerViolationEntry::from)
                .toList();

        // Convert stability violations
        List<StabilityViolationEntry> stabilityViolations = query.findStabilityViolations().stream()
                .map(StabilityViolationEntry::from)
                .toList();

        // Convert coupling metrics
        List<PackageCouplingEntry> couplingMetrics = query.analyzeAllPackageCoupling().stream()
                .map(PackageCouplingEntry::from)
                .toList();

        return new ArchitectureAnalysis(
                typeCycles, packageCycles, boundedContextCycles, layerViolations, stabilityViolations, couplingMetrics);
    }

    /**
     * Returns the total number of cycles detected.
     */
    public int totalCycles() {
        return typeCycles.size() + packageCycles.size() + boundedContextCycles.size();
    }

    /**
     * Returns the total number of violations detected.
     */
    public int totalViolations() {
        return layerViolations.size() + stabilityViolations.size();
    }

    /**
     * Returns true if no architecture issues were detected.
     */
    public boolean isClean() {
        return totalCycles() == 0 && totalViolations() == 0;
    }

    // === Nested records for report entries ===

    /**
     * A cycle entry in the report.
     *
     * @param kind       the cycle kind (TYPE_LEVEL, PACKAGE_LEVEL, BOUNDED_CONTEXT_LEVEL)
     * @param path       the cycle path (list of qualified names)
     * @param length     the cycle length
     */
    public record CycleEntry(String kind, List<String> path, int length) {

        public CycleEntry {
            Objects.requireNonNull(kind, "kind required");
            path = path != null ? List.copyOf(path) : List.of();
        }

        public static CycleEntry from(DependencyCycle cycle) {
            return new CycleEntry(
                    cycle.kind().name(), List.copyOf(cycle.path()), cycle.path().size());
        }
    }

    /**
     * A layer violation entry in the report.
     *
     * @param sourceType      the source type that has the violation
     * @param targetType      the target type being incorrectly depended on
     * @param sourceLayer     the layer of the source type
     * @param targetLayer     the layer of the target type
     * @param description     human-readable description of the violation
     */
    public record LayerViolationEntry(
            String sourceType, String targetType, String sourceLayer, String targetLayer, String description) {

        public LayerViolationEntry {
            Objects.requireNonNull(sourceType, "sourceType required");
            Objects.requireNonNull(targetType, "targetType required");
            Objects.requireNonNull(sourceLayer, "sourceLayer required");
            Objects.requireNonNull(targetLayer, "targetLayer required");
        }

        public static LayerViolationEntry from(LayerViolation violation) {
            String description = violation.description() != null
                    ? violation.description()
                    : "%s → %s: %s should not depend on %s"
                            .formatted(
                                    violation.fromType(),
                                    violation.toType(),
                                    violation.fromLayer(),
                                    violation.toLayer());
            return new LayerViolationEntry(
                    violation.fromType(), violation.toType(), violation.fromLayer(), violation.toLayer(), description);
        }
    }

    /**
     * A stability violation entry in the report.
     *
     * @param sourceType       the source type that has the violation
     * @param targetType       the target type being depended on
     * @param sourceStability  the stability metric of the source
     * @param targetStability  the stability metric of the target
     * @param description      human-readable description
     */
    public record StabilityViolationEntry(
            String sourceType, String targetType, double sourceStability, double targetStability, String description) {

        public StabilityViolationEntry {
            Objects.requireNonNull(sourceType, "sourceType required");
            Objects.requireNonNull(targetType, "targetType required");
        }

        public static StabilityViolationEntry from(StabilityViolation violation) {
            String description = "Unstable component %s (I=%.2f) depends on more stable component %s (I=%.2f)"
                    .formatted(
                            violation.fromType(),
                            violation.fromStability(),
                            violation.toType(),
                            violation.toStability());
            return new StabilityViolationEntry(
                    violation.fromType(),
                    violation.toType(),
                    violation.fromStability(),
                    violation.toStability(),
                    description);
        }
    }

    /**
     * Package coupling metrics entry in the report.
     *
     * @param packageName     the package name
     * @param afferentCoupling  Ca: incoming dependencies from other packages
     * @param efferentCoupling  Ce: outgoing dependencies to other packages
     * @param instability       I = Ce / (Ca + Ce), 0.0 = stable, 1.0 = unstable
     * @param abstractness      A = abstract types / total types
     * @param distance          D = |A + I - 1|, distance from main sequence
     */
    public record PackageCouplingEntry(
            String packageName,
            int afferentCoupling,
            int efferentCoupling,
            double instability,
            double abstractness,
            double distance) {

        public PackageCouplingEntry {
            Objects.requireNonNull(packageName, "packageName required");
        }

        public static PackageCouplingEntry from(CouplingMetrics metrics) {
            return new PackageCouplingEntry(
                    metrics.packageName(),
                    metrics.afferentCoupling(),
                    metrics.efferentCoupling(),
                    metrics.instability(),
                    metrics.abstractness(),
                    metrics.distanceFromMainSequence());
        }

        /**
         * Returns true if this package is in the "zone of pain" (stable but concrete).
         * These packages are hard to change but have many dependents.
         */
        public boolean isInZoneOfPain() {
            // Zone of pain: low instability (stable) and low abstractness (concrete)
            return instability < 0.3 && abstractness < 0.3;
        }

        /**
         * Returns true if this package is in the "zone of uselessness" (abstract but unstable).
         * These packages have abstractions that nobody uses.
         */
        public boolean isInZoneOfUselessness() {
            // Zone of uselessness: high instability (unstable) and high abstractness
            return instability > 0.7 && abstractness > 0.7;
        }

        /**
         * Returns true if this package is a domain/core package where stability is expected.
         *
         * <p>Domain packages in DDD/Hexagonal architecture are expected to be stable
         * and concrete. Being in the "Zone of Pain" is normal for these packages.
         *
         * @return true if package name suggests it's a domain package
         * @since 5.0.0
         */
        public boolean isDomainPackage() {
            String lower = packageName.toLowerCase();
            return lower.contains(".domain")
                    || lower.contains(".core")
                    || lower.contains(".model")
                    || lower.endsWith("domain")
                    || lower.endsWith("core");
        }

        /**
         * Returns the zone label with context for domain packages.
         *
         * <p>For domain packages in Zone of Pain, returns "Stable Core" instead
         * of "Pain" to indicate this is expected behavior.
         *
         * @return the zone label with appropriate context
         * @since 5.0.0
         */
        public String getZoneLabel() {
            if (isInZoneOfPain()) {
                return isDomainPackage() ? "Stable Core" : "Pain";
            }
            if (isInZoneOfUselessness()) {
                return "Useless";
            }
            return "";
        }

        /**
         * Returns the zone emoji with context for domain packages.
         *
         * @return emoji representation of the zone
         * @since 5.0.0
         */
        public String getZoneEmoji() {
            if (isInZoneOfPain()) {
                return isDomainPackage() ? "✅" : "🔥";
            }
            if (isInZoneOfUselessness()) {
                return "⚠️";
            }
            return "";
        }
    }
}
