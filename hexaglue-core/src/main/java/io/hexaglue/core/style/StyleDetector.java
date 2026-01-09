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

package io.hexaglue.core.style;

import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects the architecture style of a codebase.
 *
 * <p>The detector analyzes package structures, naming conventions, and
 * architectural patterns to determine the most likely architecture style.
 *
 * <p>Detection strategy:
 * <ol>
 *   <li>Analyze package structure (domain/, ports/, adapters/, etc.)</li>
 *   <li>Count characteristic markers for each style</li>
 *   <li>Score each style based on evidence strength</li>
 *   <li>Return the highest-scoring style with confidence</li>
 * </ol>
 */
public class StyleDetector {

    private final ApplicationGraph graph;

    public StyleDetector(ApplicationGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
    }

    /**
     * Detects the architecture style of the codebase.
     *
     * @return detected style with confidence score
     */
    public DetectedStyle detect() {
        Map<String, Integer> packageCounts = analyzePackageStructure();
        Map<ArchitectureStyle, StyleScore> scores = scoreAllStyles(packageCounts);

        // Find the highest-scoring style
        Optional<Map.Entry<ArchitectureStyle, StyleScore>> best = scores.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().totalScore()));

        if (best.isEmpty() || best.get().getValue().totalScore() < 0.3) {
            return DetectedStyle.unknown();
        }

        ArchitectureStyle style = best.get().getKey();
        StyleScore score = best.get().getValue();

        return new DetectedStyle(
                style,
                score.confidence(),
                score.description(),
                Map.of(
                        "packageCounts", packageCounts,
                        "markers", score.markers(),
                        "score", score.totalScore()));
    }

    /**
     * Analyzes the package structure and counts characteristic packages.
     */
    private Map<String, Integer> analyzePackageStructure() {
        Map<String, Integer> counts = new HashMap<>();

        for (TypeNode type : graph.typeNodes()) {
            String pkg = type.packageName().toLowerCase();

            // Count hexagonal markers
            if (pkg.contains("domain")) counts.merge("domain", 1, Integer::sum);
            if (pkg.contains("ports") || pkg.contains("port")) counts.merge("ports", 1, Integer::sum);
            if (pkg.contains("adapters") || pkg.contains("adapter")) counts.merge("adapters", 1, Integer::sum);

            // Count onion markers
            if (pkg.contains("core")) counts.merge("core", 1, Integer::sum);
            if (pkg.contains("application")) counts.merge("application", 1, Integer::sum);
            if (pkg.contains("infrastructure")) counts.merge("infrastructure", 1, Integer::sum);

            // Count layered markers
            if (pkg.contains("controller")) counts.merge("controller", 1, Integer::sum);
            if (pkg.contains("service")) counts.merge("service", 1, Integer::sum);
            if (pkg.contains("repository")) counts.merge("repository", 1, Integer::sum);
            if (pkg.contains("model")) counts.merge("model", 1, Integer::sum);

            // Count clean architecture markers
            if (pkg.contains("entities") || pkg.contains("entity")) counts.merge("entities", 1, Integer::sum);
            if (pkg.contains("usecases") || pkg.contains("usecase")) counts.merge("usecases", 1, Integer::sum);
            if (pkg.contains("gateways") || pkg.contains("gateway")) counts.merge("gateways", 1, Integer::sum);
            if (pkg.contains("frameworks") || pkg.contains("framework")) counts.merge("frameworks", 1, Integer::sum);
        }

        return counts;
    }

    /**
     * Scores all architecture styles based on evidence.
     */
    private Map<ArchitectureStyle, StyleScore> scoreAllStyles(Map<String, Integer> packageCounts) {
        Map<ArchitectureStyle, StyleScore> scores = new HashMap<>();

        scores.put(ArchitectureStyle.DDD_HEXAGONAL, scoreHexagonal(packageCounts));
        scores.put(ArchitectureStyle.DDD_ONION, scoreOnion(packageCounts));
        scores.put(ArchitectureStyle.LAYERED_TRADITIONAL, scoreLayered(packageCounts));
        scores.put(ArchitectureStyle.CLEAN_ARCHITECTURE, scoreClean(packageCounts));
        scores.put(ArchitectureStyle.MODULAR_MONOLITH, scoreModular(packageCounts));

        return scores;
    }

    private StyleScore scoreHexagonal(Map<String, Integer> counts) {
        List<String> markers = new ArrayList<>();
        double score = 0.0;

        if (counts.getOrDefault("domain", 0) > 0) {
            score += 0.4;
            markers.add("domain package");
        }
        if (counts.getOrDefault("ports", 0) > 0) {
            score += 0.4;
            markers.add("ports package");
        }
        if (counts.getOrDefault("adapters", 0) > 0) {
            score += 0.3;
            markers.add("adapters package");
        }

        String description = markers.isEmpty()
                ? "No hexagonal architecture markers found"
                : "Found hexagonal markers: " + String.join(", ", markers);

        return new StyleScore(score, description, markers);
    }

    private StyleScore scoreOnion(Map<String, Integer> counts) {
        List<String> markers = new ArrayList<>();
        double score = 0.0;

        if (counts.getOrDefault("core", 0) > 0) {
            score += 0.4;
            markers.add("core package");
        }
        if (counts.getOrDefault("application", 0) > 0) {
            score += 0.3;
            markers.add("application package");
        }
        if (counts.getOrDefault("infrastructure", 0) > 0) {
            score += 0.3;
            markers.add("infrastructure package");
        }

        String description = markers.isEmpty()
                ? "No onion architecture markers found"
                : "Found onion markers: " + String.join(", ", markers);

        return new StyleScore(score, description, markers);
    }

    private StyleScore scoreLayered(Map<String, Integer> counts) {
        List<String> markers = new ArrayList<>();
        double score = 0.0;

        if (counts.getOrDefault("controller", 0) > 0) {
            score += 0.3;
            markers.add("controller package");
        }
        if (counts.getOrDefault("service", 0) > 0) {
            score += 0.3;
            markers.add("service package");
        }
        if (counts.getOrDefault("repository", 0) > 0) {
            score += 0.2;
            markers.add("repository package");
        }
        if (counts.getOrDefault("model", 0) > 0) {
            score += 0.2;
            markers.add("model package");
        }

        String description = markers.isEmpty()
                ? "No traditional layered markers found"
                : "Found layered markers: " + String.join(", ", markers);

        return new StyleScore(score, description, markers);
    }

    private StyleScore scoreClean(Map<String, Integer> counts) {
        List<String> markers = new ArrayList<>();
        double score = 0.0;

        if (counts.getOrDefault("entities", 0) > 0) {
            score += 0.35;
            markers.add("entities package");
        }
        if (counts.getOrDefault("usecases", 0) > 0) {
            score += 0.35;
            markers.add("usecases package");
        }
        if (counts.getOrDefault("gateways", 0) > 0) {
            score += 0.2;
            markers.add("gateways package");
        }
        if (counts.getOrDefault("frameworks", 0) > 0) {
            score += 0.2;
            markers.add("frameworks package");
        }

        String description = markers.isEmpty()
                ? "No clean architecture markers found"
                : "Found clean architecture markers: " + String.join(", ", markers);

        return new StyleScore(score, description, markers);
    }

    private StyleScore scoreModular(Map<String, Integer> counts) {
        // Modular monolith detection is more complex - look for multiple top-level packages
        Set<String> topLevelPackages = graph.typeNodes().stream()
                .map(TypeNode::packageName)
                .map(this::extractTopLevelPackage)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> markers = new ArrayList<>();
        double score = 0.0;

        if (topLevelPackages.size() >= 3) {
            score += 0.4;
            markers.add(topLevelPackages.size() + " top-level packages");
        }

        String description = markers.isEmpty()
                ? "No modular monolith markers found"
                : "Found modular markers: " + String.join(", ", markers);

        return new StyleScore(score, description, markers);
    }

    private String extractTopLevelPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }

        String[] parts = packageName.split("\\.");
        // Skip common prefixes (com, org, io, etc.)
        int startIndex = 0;
        if (parts.length > 0 && isCommonPrefix(parts[0])) {
            startIndex = 1;
        }
        if (parts.length > startIndex + 1 && isCommonPrefix(parts[startIndex])) {
            startIndex++;
        }

        return startIndex < parts.length ? parts[startIndex] : null;
    }

    private boolean isCommonPrefix(String part) {
        return part.equals("com") || part.equals("org") || part.equals("io") || part.equals("net");
    }

    /**
     * Scoring result for a specific architecture style.
     */
    private record StyleScore(double totalScore, String description, List<String> markers) {

        /**
         * Converts the raw score to a confidence level (0.0 to 1.0).
         */
        double confidence() {
            // Normalize to 0-1 range
            return Math.min(1.0, totalScore);
        }
    }
}
