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

package io.hexaglue.core.audit.metrics;

import io.hexaglue.spi.audit.CodeUnit;
import java.util.List;

/**
 * Calculator for codebase-level metrics.
 *
 * <p>This class aggregates metrics from individual code units to produce
 * codebase-wide statistics about size, complexity, and documentation.
 *
 * @since 3.0.0
 */
public final class MetricsCalculator {

    /**
     * Calculates code size metrics for a collection of code units.
     *
     * @param units the code units to analyze
     * @return code size metrics
     */
    public CodeSizeMetrics calculateCodeSize(List<CodeUnit> units) {
        if (units == null || units.isEmpty()) {
            return new CodeSizeMetrics(0, 0, 0, 0);
        }

        int typeCount = units.size();
        int methodCount = units.stream()
                .mapToInt(unit -> unit.metrics().numberOfMethods())
                .sum();
        int fieldCount =
                units.stream().mapToInt(unit -> unit.metrics().numberOfFields()).sum();
        int locTotal =
                units.stream().mapToInt(unit -> unit.metrics().linesOfCode()).sum();

        return new CodeSizeMetrics(typeCount, methodCount, fieldCount, locTotal);
    }

    /**
     * Calculates complexity metrics for a collection of code units.
     *
     * @param units the code units to analyze
     * @return complexity metrics
     */
    public ComplexityMetrics calculateComplexity(List<CodeUnit> units) {
        if (units == null || units.isEmpty()) {
            return new ComplexityMetrics(0, 0.0, 0);
        }

        int maxComplexity = units.stream()
                .mapToInt(unit -> unit.metrics().cyclomaticComplexity())
                .max()
                .orElse(0);

        int totalComplexity = units.stream()
                .mapToInt(unit -> unit.metrics().cyclomaticComplexity())
                .sum();

        int totalMethods = units.stream()
                .mapToInt(unit -> unit.metrics().numberOfMethods())
                .sum();

        double avgComplexity = totalMethods > 0 ? (double) totalComplexity / totalMethods : 0.0;

        // Count methods above threshold (complexity > 10)
        int methodsAboveThreshold = (int) units.stream()
                .flatMap(unit -> unit.methods().stream())
                .filter(method -> method.complexity() > 10)
                .count();

        return new ComplexityMetrics(maxComplexity, avgComplexity, methodsAboveThreshold);
    }

    /**
     * Calculates documentation metrics for a collection of code units.
     *
     * @param units the code units to analyze
     * @return documentation metrics
     */
    public DocumentationMetrics calculateDocumentation(List<CodeUnit> units) {
        if (units == null || units.isEmpty()) {
            return new DocumentationMetrics(0.0, 0.0);
        }

        long documentedTypes =
                units.stream().filter(unit -> unit.documentation().hasJavadoc()).count();

        double typesRatio = (double) documentedTypes / units.size();

        // Calculate public method documentation ratio
        // We approximate by averaging the javadoc coverage from each unit
        double avgPublicMethodCoverage = units.stream()
                .mapToInt(unit -> unit.documentation().javadocCoverage())
                .average()
                .orElse(0.0);

        double publicMethodsRatio = avgPublicMethodCoverage / 100.0;

        return new DocumentationMetrics(typesRatio, publicMethodsRatio);
    }

    /**
     * Calculates all metrics at once.
     *
     * @param units the code units to analyze
     * @return a record containing all metric types
     */
    public AllMetrics calculateAll(List<CodeUnit> units) {
        return new AllMetrics(calculateCodeSize(units), calculateComplexity(units), calculateDocumentation(units));
    }

    /**
     * Aggregate record containing all metric types.
     *
     * @param codeSize code size metrics
     * @param complexity complexity metrics
     * @param documentation documentation metrics
     */
    public record AllMetrics(
            CodeSizeMetrics codeSize, ComplexityMetrics complexity, DocumentationMetrics documentation) {}
}
