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

package io.hexaglue.plugin.audit.domain.service;

import io.hexaglue.plugin.audit.adapter.report.model.ArchitectureAnalysis;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.Recommendation;
import io.hexaglue.plugin.audit.domain.model.RecommendationPriority;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates actionable recommendations from violations and architecture analysis.
 *
 * <p>Recommendations are grouped by pattern and prioritized based on:
 * <ul>
 *   <li>Severity of violations (BLOCKER &gt; CRITICAL &gt; MAJOR &gt; MINOR)</li>
 *   <li>Number of affected types</li>
 *   <li>Presence of cycles or layer violations</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class RecommendationGenerator {

    /** Effort in person-days per BLOCKER violation. */
    private static final double BLOCKER_EFFORT = 2.0;

    /** Effort in person-days per CRITICAL violation. */
    private static final double CRITICAL_EFFORT = 1.5;

    /** Effort in person-days per MAJOR violation. */
    private static final double MAJOR_EFFORT = 0.5;

    /** Effort in person-days per MINOR violation. */
    private static final double MINOR_EFFORT = 0.25;

    /** Effort in person-days per dependency cycle. */
    private static final double CYCLE_EFFORT = 3.0;

    /** Effort in person-days per layer violation. */
    private static final double LAYER_VIOLATION_EFFORT = 1.0;

    /**
     * Generates recommendations from violations and architecture analysis.
     *
     * @param violations           the list of violations
     * @param architectureAnalysis the architecture analysis (may be null)
     * @param metrics              the collected metrics (may be null)
     * @return list of recommendations sorted by priority
     */
    public List<Recommendation> generate(
            List<Violation> violations, ArchitectureAnalysis architectureAnalysis, Map<String, Metric> metrics) {
        Objects.requireNonNull(violations, "violations required");

        List<Recommendation> recommendations = new ArrayList<>();

        // Group violations by constraint and generate recommendations
        recommendations.addAll(generateFromViolations(violations));

        // Add recommendations from architecture analysis
        if (architectureAnalysis != null) {
            recommendations.addAll(generateFromArchitectureAnalysis(architectureAnalysis));
        }

        // Sort by priority (IMMEDIATE first, then SHORT_TERM, SHORT_TERM, MEDIUM_TERM)
        recommendations.sort(Comparator.comparing(Recommendation::priority));

        return recommendations;
    }

    /**
     * Generates recommendations by grouping violations by constraint.
     */
    private List<Recommendation> generateFromViolations(List<Violation> violations) {
        // Group violations by constraint ID
        Map<ConstraintId, List<Violation>> byConstraint =
                violations.stream().collect(Collectors.groupingBy(Violation::constraintId));

        List<Recommendation> recommendations = new ArrayList<>();

        for (Map.Entry<ConstraintId, List<Violation>> entry : byConstraint.entrySet()) {
            ConstraintId constraintId = entry.getKey();
            List<Violation> group = entry.getValue();

            Recommendation recommendation = createRecommendationForGroup(constraintId, group);
            if (recommendation != null) {
                recommendations.add(recommendation);
            }
        }

        return recommendations;
    }

    /**
     * Creates a recommendation for a group of violations with the same constraint.
     */
    private Recommendation createRecommendationForGroup(ConstraintId constraintId, List<Violation> violations) {
        if (violations.isEmpty()) {
            return null;
        }

        Severity maxSeverity = violations.stream()
                .map(Violation::severity)
                .max(Comparator.naturalOrder())
                .orElse(Severity.MINOR);

        List<String> affectedTypes = violations.stream()
                .flatMap(v -> v.affectedTypes().stream())
                .distinct()
                .toList();

        double effort = calculateEffort(violations);
        RecommendationPriority priority = determinePriority(maxSeverity, violations.size());

        String title = createTitle(constraintId, violations.size());
        String description = createDescription(constraintId, violations);
        String impact = createImpact(constraintId, maxSeverity);

        return Recommendation.builder()
                .priority(priority)
                .title(title)
                .description(description)
                .affectedTypes(affectedTypes)
                .estimatedEffort(effort)
                .expectedImpact(impact)
                .relatedViolations(List.of(constraintId))
                .build();
    }

    /**
     * Generates recommendations from architecture analysis findings.
     */
    private List<Recommendation> generateFromArchitectureAnalysis(ArchitectureAnalysis analysis) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Recommendation for dependency cycles
        int cycleCount = analysis.totalCycles();
        if (cycleCount > 0) {
            recommendations.add(Recommendation.builder()
                    .priority(RecommendationPriority.IMMEDIATE)
                    .title("Resolve " + cycleCount + " dependency cycle(s)")
                    .description("Dependency cycles make the codebase harder to understand, test, and modify. "
                            + "Consider extracting shared interfaces, applying dependency inversion, "
                            + "or reorganizing packages to break the cycles.")
                    .estimatedEffort(cycleCount * CYCLE_EFFORT)
                    .expectedImpact("Improved testability, maintainability, and deployment flexibility")
                    .build());
        }

        // Recommendation for layer violations
        if (analysis.layerViolations() != null && !analysis.layerViolations().isEmpty()) {
            int violationCount = analysis.layerViolations().size();
            recommendations.add(Recommendation.builder()
                    .priority(RecommendationPriority.SHORT_TERM)
                    .title("Fix " + violationCount + " layer violation(s)")
                    .description("Layer violations compromise the architectural integrity of the application. "
                            + "Ensure domain types don't depend on infrastructure and that dependencies "
                            + "flow inward toward the domain.")
                    .estimatedEffort(violationCount * LAYER_VIOLATION_EFFORT)
                    .expectedImpact("Better separation of concerns and adherence to hexagonal architecture")
                    .build());
        }

        return recommendations;
    }

    private double calculateEffort(List<Violation> violations) {
        return violations.stream()
                .mapToDouble(v -> switch (v.severity()) {
                    case BLOCKER -> BLOCKER_EFFORT;
                    case CRITICAL -> CRITICAL_EFFORT;
                    case MAJOR -> MAJOR_EFFORT;
                    case MINOR -> MINOR_EFFORT;
                    case INFO -> 0.1;
                })
                .sum();
    }

    private RecommendationPriority determinePriority(Severity maxSeverity, int violationCount) {
        return switch (maxSeverity) {
            case BLOCKER -> RecommendationPriority.IMMEDIATE;
            case CRITICAL -> violationCount > 3 ? RecommendationPriority.IMMEDIATE : RecommendationPriority.SHORT_TERM;
            case MAJOR -> violationCount > 5 ? RecommendationPriority.SHORT_TERM : RecommendationPriority.SHORT_TERM;
            case MINOR, INFO -> RecommendationPriority.MEDIUM_TERM;
        };
    }

    private String createTitle(ConstraintId constraintId, int count) {
        String constraintName = constraintId.name().replace("-", " ").replace("_", " ");
        return String.format("Address %d %s violation(s)", count, constraintName);
    }

    private String createDescription(ConstraintId constraintId, List<Violation> violations) {
        String category = constraintId.category();

        StringBuilder sb = new StringBuilder();
        sb.append("The constraint '").append(constraintId.value()).append("' was violated ");
        sb.append(violations.size()).append(" time(s). ");

        // Add category-specific guidance
        if ("ddd".equals(category)) {
            sb.append("This relates to Domain-Driven Design patterns. ");
            sb.append("Review the affected domain types to ensure they follow DDD tactical patterns correctly.");
        } else if ("hexagonal".equals(category)) {
            sb.append("This relates to Hexagonal Architecture principles. ");
            sb.append("Ensure proper separation between ports, adapters, and domain logic.");
        }

        return sb.toString();
    }

    private String createImpact(ConstraintId constraintId, Severity maxSeverity) {
        String category = constraintId.category();

        if ("ddd".equals(category)) {
            return switch (maxSeverity) {
                case BLOCKER, CRITICAL ->
                    "Critical improvement in domain model clarity and business logic encapsulation";
                case MAJOR -> "Improved domain model consistency and maintainability";
                case MINOR, INFO -> "Minor improvement in DDD pattern adherence";
            };
        } else if ("hexagonal".equals(category)) {
            return switch (maxSeverity) {
                case BLOCKER, CRITICAL -> "Critical improvement in architectural boundary enforcement";
                case MAJOR -> "Better separation of concerns and testability";
                case MINOR, INFO -> "Minor improvement in hexagonal architecture compliance";
            };
        }

        return "Improved code quality and architectural compliance";
    }
}
