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

package io.hexaglue.core.graph.style;

import static java.util.stream.Collectors.toSet;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.*;

/**
 * Detects the package organization style of a codebase by analyzing package patterns.
 *
 * <p>The detection works by:
 * <ol>
 *   <li>Collecting all unique package names from the analyzed types</li>
 *   <li>Matching packages against characteristic patterns for each style</li>
 *   <li>Scoring each style based on pattern matches and weights</li>
 *   <li>Selecting the highest scoring style with appropriate confidence</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * StyleDetector detector = new StyleDetector();
 * StyleDetectionResult result = detector.detect(graph.typeNodes(), "com.example");
 *
 * if (result.isConfident()) {
 *     // Use the detected style for classification
 *     PackageOrganizationStyle style = result.style();
 * }
 * }</pre>
 */
public final class StyleDetector {

    private static final Map<PackageOrganizationStyle, List<PatternRule>> STYLE_PATTERNS;

    static {
        Map<PackageOrganizationStyle, List<PatternRule>> patterns = new EnumMap<>(PackageOrganizationStyle.class);

        patterns.put(
                PackageOrganizationStyle.HEXAGONAL,
                List.of(
                        new PatternRule(".ports.in", 3),
                        new PatternRule(".ports.out", 3),
                        new PatternRule(".port.in", 3),
                        new PatternRule(".port.out", 3),
                        new PatternRule(".adapters", 2),
                        new PatternRule(".adapter", 2),
                        new PatternRule(".inbound", 2),
                        new PatternRule(".outbound", 2),
                        new PatternRule(".primary", 1),
                        new PatternRule(".secondary", 1)));

        patterns.put(
                PackageOrganizationStyle.BY_LAYER,
                List.of(
                        new PatternRule(".domain", 2), // matches .domain or .domain.x
                        new PatternRule(".application", 2),
                        new PatternRule(".infrastructure", 2),
                        new PatternRule(".presentation", 2),
                        new PatternRule(".persistence", 1),
                        new PatternRule(".web", 1),
                        new PatternRule(".api", 1)));

        patterns.put(
                PackageOrganizationStyle.CLEAN_ARCHITECTURE,
                List.of(
                        new PatternRule(".entities", 2),
                        new PatternRule(".usecases", 3),
                        new PatternRule(".usecase", 3),
                        new PatternRule(".gateways", 2),
                        new PatternRule(".gateway", 2),
                        new PatternRule(".controllers", 2),
                        new PatternRule(".presenters", 2)));

        patterns.put(
                PackageOrganizationStyle.ONION,
                List.of(
                        new PatternRule(".core", 2),
                        new PatternRule(".domain.services", 3),
                        new PatternRule(".domain.model", 2),
                        new PatternRule(".infrastructure.persistence", 2)));

        // BY_FEATURE is harder to detect automatically as it depends on feature names
        // We look for patterns like feature.domain, feature.ports, etc.
        patterns.put(
                PackageOrganizationStyle.BY_FEATURE,
                List.of(
                        // These patterns suggest feature-based: domain/ports per feature
                        new PatternRule(".order.domain", 2),
                        new PatternRule(".order.ports", 2),
                        new PatternRule(".customer.domain", 2),
                        new PatternRule(".payment.domain", 2),
                        new PatternRule(".product.domain", 2)));

        STYLE_PATTERNS = Collections.unmodifiableMap(patterns);
    }

    private record PatternRule(String pattern, int weight) {}

    /**
     * Detects the package organization style from the given types.
     *
     * @param types the types to analyze
     * @param basePackage the base package of the project
     * @return the detection result with style, confidence, and evidence
     */
    public StyleDetectionResult detect(Collection<TypeNode> types, String basePackage) {
        if (types == null || types.isEmpty()) {
            return StyleDetectionResult.unknown(basePackage != null ? basePackage : "");
        }

        // Collect all unique package names
        Set<String> packages = types.stream().map(TypeNode::packageName).collect(toSet());

        // Score each style
        Map<PackageOrganizationStyle, Integer> scores = new EnumMap<>(PackageOrganizationStyle.class);
        Map<String, Integer> detectedPatterns = new LinkedHashMap<>();

        for (var entry : STYLE_PATTERNS.entrySet()) {
            PackageOrganizationStyle style = entry.getKey();
            int score = 0;

            for (PatternRule rule : entry.getValue()) {
                long matches = packages.stream()
                        .filter(pkg -> pkg.contains(rule.pattern()))
                        .count();

                if (matches > 0) {
                    score += (int) matches * rule.weight();
                    detectedPatterns.merge(rule.pattern(), (int) matches, Integer::sum);
                }
            }

            if (score > 0) {
                scores.put(style, score);
            }
        }

        if (scores.isEmpty()) {
            return StyleDetectionResult.unknown(basePackage != null ? basePackage : "");
        }

        // Find best match
        var winner = scores.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .orElseThrow();

        // Determine confidence based on score and distinctiveness
        ConfidenceLevel confidence = determineConfidence(winner.getValue(), scores, types.size());

        return new StyleDetectionResult(
                winner.getKey(), confidence, detectedPatterns, basePackage != null ? basePackage : "");
    }

    private ConfidenceLevel determineConfidence(
            int winnerScore, Map<PackageOrganizationStyle, Integer> scores, int typeCount) {
        // Rule 1: Very few types -> LOW confidence (insufficient evidence)
        if (typeCount < 5) {
            return ConfidenceLevel.LOW;
        }

        // Rule 2: Multiple styles with similar scores -> MEDIUM (ambiguous)
        long competingStyles =
                scores.values().stream().filter(s -> s >= winnerScore * 0.6).count();
        if (competingStyles > 1) {
            return ConfidenceLevel.MEDIUM;
        }

        // Rule 3: Winner has very low score relative to type count -> LOW
        if (winnerScore < 3) {
            return ConfidenceLevel.LOW;
        }

        // Rule 4: Strong score and clear winner -> HIGH
        if (winnerScore >= typeCount * 0.3 && competingStyles == 1) {
            return ConfidenceLevel.HIGH;
        }

        return ConfidenceLevel.MEDIUM;
    }
}
