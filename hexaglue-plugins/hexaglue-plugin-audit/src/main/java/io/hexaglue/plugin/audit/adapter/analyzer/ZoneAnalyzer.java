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

import io.hexaglue.plugin.audit.domain.model.PackageZoneMetrics;
import io.hexaglue.plugin.audit.domain.model.ZoneCategory;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Analyzes packages according to Robert C. Martin's metrics.
 *
 * <p>This analyzer implements the zone analysis framework described in
 * "Agile Software Development: Principles, Patterns, and Practices" by
 * Robert C. Martin. It evaluates packages based on their position in the
 * Abstractness-Instability plane to identify architectural health issues.
 *
 * <p><strong>Metrics calculated:</strong>
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
 * <p><strong>Abstractness calculation:</strong>
 * <pre>
 * A = (Number of Abstract Classes + Number of Interfaces) / Total Classes
 * </pre>
 *
 * <p><strong>Instability calculation:</strong>
 * <pre>
 * I = Ce / (Ca + Ce)
 * where:
 *   Ce = Efferent Coupling (outgoing dependencies)
 *   Ca = Afferent Coupling (incoming dependencies)
 * </pre>
 *
 * <p><strong>Distance calculation:</strong>
 * <pre>
 * D = |A + I - 1|
 * </pre>
 *
 * <p><strong>Edge cases:</strong>
 * <ul>
 *   <li><strong>Empty packages:</strong> Skipped (no metrics calculated).</li>
 *   <li><strong>Packages with no dependencies:</strong> Instability = 1.0 (maximally unstable).</li>
 *   <li><strong>Packages with only interfaces:</strong> Abstractness = 1.0.</li>
 *   <li><strong>Packages with only concrete classes:</strong> Abstractness = 0.0.</li>
 * </ul>
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * ZoneAnalyzer analyzer = new ZoneAnalyzer();
 * List<PackageZoneMetrics> metrics = analyzer.analyze(codebase);
 *
 * metrics.stream()
 *     .filter(PackageZoneMetrics::isProblematic)
 *     .forEach(m -> System.out.println(m.description()));
 * }</pre>
 *
 * @since 1.0.0
 */
public class ZoneAnalyzer {

    /**
     * Analyzes all packages in the codebase and calculates zone metrics.
     *
     * <p>This method:
     * <ol>
     *   <li>Groups code units by package</li>
     *   <li>Calculates abstractness for each package</li>
     *   <li>Calculates instability based on dependency analysis</li>
     *   <li>Computes distance from the main sequence</li>
     *   <li>Categorizes each package into its appropriate zone</li>
     * </ol>
     *
     * <p>Empty packages (containing no code units) are excluded from the analysis.
     *
     * @param codebase the codebase to analyze
     * @return list of package metrics, one per non-empty package
     * @throws NullPointerException if codebase is null
     */
    public List<PackageZoneMetrics> analyze(Codebase codebase) {
        Objects.requireNonNull(codebase, "codebase required");

        // Group units by package
        Map<String, List<CodeUnit>> packageToUnits = groupByPackage(codebase.units());

        if (packageToUnits.isEmpty()) {
            return List.of();
        }

        // Build package-level dependency graph
        Map<String, Set<String>> packageDependencies = buildPackageDependencies(codebase);

        // Calculate metrics for each package
        List<PackageZoneMetrics> results = new ArrayList<>();

        for (Map.Entry<String, List<CodeUnit>> entry : packageToUnits.entrySet()) {
            String packageName = entry.getKey();
            List<CodeUnit> units = entry.getValue();

            double abstractness = calculateAbstractness(units);
            double instability = calculateInstability(packageName, packageDependencies);
            double distance = calculateDistance(abstractness, instability);
            ZoneCategory zone = ZoneCategory.categorize(distance, instability);

            results.add(new PackageZoneMetrics(packageName, abstractness, instability, distance, zone));
        }

        return results;
    }

    /**
     * Groups code units by their package name.
     *
     * @param units the code units to group
     * @return map of package name to list of units in that package
     */
    private Map<String, List<CodeUnit>> groupByPackage(List<CodeUnit> units) {
        Map<String, List<CodeUnit>> packageMap = new HashMap<>();

        for (CodeUnit unit : units) {
            String packageName = unit.packageName();
            if (!packageName.isEmpty()) {
                packageMap.computeIfAbsent(packageName, k -> new ArrayList<>()).add(unit);
            }
        }

        return packageMap;
    }

    /**
     * Builds package-level dependencies from unit-level dependencies.
     *
     * <p>This method transforms unit-level dependencies (class A depends on class B)
     * into package-level dependencies (package P1 depends on package P2).
     *
     * @param codebase the codebase containing dependency information
     * @return map of package name to set of packages it depends on
     */
    private Map<String, Set<String>> buildPackageDependencies(Codebase codebase) {
        Map<String, Set<String>> packageDeps = new HashMap<>();
        Map<String, Set<String>> unitDeps = codebase.dependencies();

        for (Map.Entry<String, Set<String>> entry : unitDeps.entrySet()) {
            String fromUnit = entry.getKey();
            String fromPackage = extractPackageName(fromUnit);

            if (fromPackage.isEmpty()) {
                continue;
            }

            Set<String> toDependencies = packageDeps.computeIfAbsent(fromPackage, k -> new java.util.HashSet<>());

            for (String toUnit : entry.getValue()) {
                String toPackage = extractPackageName(toUnit);

                // Only track inter-package dependencies (not intra-package)
                if (!toPackage.isEmpty() && !toPackage.equals(fromPackage)) {
                    toDependencies.add(toPackage);
                }
            }
        }

        return packageDeps;
    }

    /**
     * Extracts the package name from a fully qualified class name.
     *
     * @param qualifiedName the fully qualified name
     * @return the package name, or empty string if none
     */
    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";
    }

    /**
     * Calculates the abstractness metric for a package.
     *
     * <p>Abstractness is the ratio of abstract types to total types:
     * <pre>
     * A = (Abstract Classes + Interfaces) / Total Classes
     * </pre>
     *
     * <p>Abstract classes are those explicitly marked as abstract.
     * Interfaces are counted as abstract types.
     * Enums and records are counted as concrete types.
     *
     * @param units the code units in the package
     * @return the abstractness metric [0.0, 1.0]
     */
    private double calculateAbstractness(List<CodeUnit> units) {
        if (units.isEmpty()) {
            return 0.0;
        }

        long abstractCount = units.stream().filter(this::isAbstractType).count();

        return (double) abstractCount / units.size();
    }

    /**
     * Determines if a code unit represents an abstract type.
     *
     * <p>A type is considered abstract if it's:
     * <ul>
     *   <li>An interface</li>
     *   <li>An abstract class (determined by naming convention or structure)</li>
     * </ul>
     *
     * <p>Note: Since we don't have access to modifiers in CodeUnit, we use
     * a heuristic: INTERFACE kind is abstract, CLASS kind is concrete unless
     * it has "Abstract" in its name.
     *
     * @param unit the code unit to check
     * @return true if the unit is abstract
     */
    private boolean isAbstractType(CodeUnit unit) {
        if (unit.kind() == CodeUnitKind.INTERFACE) {
            return true;
        }

        // Heuristic: Classes starting with "Abstract" are likely abstract
        // This is imperfect but works for many codebases
        if (unit.kind() == CodeUnitKind.CLASS) {
            String simpleName = unit.simpleName();
            return simpleName.startsWith("Abstract") || simpleName.endsWith("Base");
        }

        return false;
    }

    /**
     * Calculates the instability metric for a package.
     *
     * <p>Instability measures how resistant a package is to change:
     * <pre>
     * I = Ce / (Ca + Ce)
     * where:
     *   Ce = Efferent Coupling (outgoing dependencies)
     *   Ca = Afferent Coupling (incoming dependencies)
     * </pre>
     *
     * <p>Special cases:
     * <ul>
     *   <li>If Ca + Ce = 0 (no dependencies), returns 1.0 (maximally unstable)</li>
     *   <li>If only incoming deps (Ce = 0), returns 0.0 (maximally stable)</li>
     *   <li>If only outgoing deps (Ca = 0), returns 1.0 (maximally unstable)</li>
     * </ul>
     *
     * @param packageName         the package to analyze
     * @param packageDependencies the package-level dependency graph
     * @return the instability metric [0.0, 1.0]
     */
    private double calculateInstability(String packageName, Map<String, Set<String>> packageDependencies) {
        // Ce: Efferent coupling (outgoing dependencies)
        Set<String> outgoing = packageDependencies.getOrDefault(packageName, Set.of());
        int ce = outgoing.size();

        // Ca: Afferent coupling (incoming dependencies)
        int ca = 0;
        for (Map.Entry<String, Set<String>> entry : packageDependencies.entrySet()) {
            if (!entry.getKey().equals(packageName) && entry.getValue().contains(packageName)) {
                ca++;
            }
        }

        // If no dependencies, the package is maximally unstable
        if (ca + ce == 0) {
            return 1.0;
        }

        return (double) ce / (ca + ce);
    }

    /**
     * Calculates the normalized distance from the main sequence.
     *
     * <p>The main sequence is the ideal balance line where A + I = 1.
     * Distance measures how far a package deviates from this ideal:
     * <pre>
     * D = |A + I - 1|
     * </pre>
     *
     * <p>A distance of 0.0 means the package is perfectly balanced.
     * A distance approaching 1.0 indicates the package is in a problematic zone.
     *
     * @param abstractness the abstractness metric [0.0, 1.0]
     * @param instability  the instability metric [0.0, 1.0]
     * @return the distance metric [0.0, 1.0]
     */
    private double calculateDistance(double abstractness, double instability) {
        return Math.abs(abstractness + instability - 1.0);
    }
}
