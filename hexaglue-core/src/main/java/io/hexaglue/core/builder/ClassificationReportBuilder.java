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

package io.hexaglue.core.builder;

import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.arch.model.report.ClassificationConflict;
import io.hexaglue.arch.model.report.ClassificationConflict.ConflictingContribution;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.arch.model.report.ClassificationStats;
import io.hexaglue.arch.model.report.PrioritizedRemediation;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationStatus;
import io.hexaglue.core.classification.Conflict;
import io.hexaglue.core.graph.model.NodeId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@link ClassificationReport} from classification results.
 *
 * <p>This builder transforms the raw classification data into a structured
 * report with statistics, conflicts, and prioritized remediation suggestions.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ClassificationReportBuilder builder = new ClassificationReportBuilder();
 * ClassificationReport report = builder.build(allTypes, unclassified, results);
 *
 * // Use the report
 * if (report.hasIssues()) {
 *     System.out.println("Issues found!");
 *     report.actionRequired().forEach(u ->
 *         System.out.println("Action needed: " + u.simpleName()));
 * }
 * }</pre>
 *
 * @since 4.1.0
 */
public final class ClassificationReportBuilder {

    /**
     * Builds a classification report from the given data.
     *
     * @param allTypes all architectural types (classified and unclassified)
     * @param unclassified the unclassified types
     * @param results the classification results from the classifier
     * @return a new ClassificationReport
     */
    public ClassificationReport build(
            Collection<ArchType> allTypes, List<UnclassifiedType> unclassified, ClassificationResults results) {

        ClassificationStats stats = computeStats(allTypes, results);
        Map<UnclassifiedCategory, List<UnclassifiedType>> unclassifiedByCategory = groupByCategory(unclassified);
        List<ClassificationConflict> conflicts = extractConflicts(results);
        List<PrioritizedRemediation> remediations = buildRemediations(unclassified);

        return new ClassificationReport(stats, unclassifiedByCategory, conflicts, remediations, Instant.now());
    }

    private ClassificationStats computeStats(Collection<ArchType> allTypes, ClassificationResults results) {
        int totalTypes = allTypes.size();
        int classifiedTypes = 0;
        int unclassifiedTypes = 0;
        int conflictCount = 0;
        int outOfScopeTypes = 0;

        Map<ArchKind, Integer> countByKind = new EnumMap<>(ArchKind.class);
        Map<ConfidenceLevel, Integer> countByConfidence = new EnumMap<>(ConfidenceLevel.class);

        for (ArchType type : allTypes) {
            if (type instanceof UnclassifiedType unclassified) {
                unclassifiedTypes++;
                if (unclassified.category() == UnclassifiedCategory.OUT_OF_SCOPE) {
                    outOfScopeTypes++;
                }
            } else {
                classifiedTypes++;
            }

            // Count by kind
            countByKind.merge(type.kind(), 1, Integer::sum);

            // Count by confidence from classification trace
            ConfidenceLevel confidence = type.classification().confidence();
            if (confidence != null) {
                countByConfidence.merge(confidence, 1, Integer::sum);
            }
        }

        // Count conflicts from results
        for (ClassificationResult result : results.toList()) {
            if (result.status() == ClassificationStatus.CONFLICT || result.hasConflicts()) {
                conflictCount++;
            }
        }

        return ClassificationStats.of(
                totalTypes,
                classifiedTypes,
                unclassifiedTypes,
                countByKind,
                countByConfidence,
                conflictCount,
                outOfScopeTypes);
    }

    private Map<UnclassifiedCategory, List<UnclassifiedType>> groupByCategory(List<UnclassifiedType> unclassified) {
        Map<UnclassifiedCategory, List<UnclassifiedType>> result = new EnumMap<>(UnclassifiedCategory.class);
        for (UnclassifiedType type : unclassified) {
            result.computeIfAbsent(type.category(), k -> new ArrayList<>()).add(type);
        }
        return result;
    }

    private List<ClassificationConflict> extractConflicts(ClassificationResults results) {
        List<ClassificationConflict> conflicts = new ArrayList<>();
        Map<String, String> nodeIdToSimpleName = new HashMap<>();

        for (ClassificationResult result : results.toList()) {
            if (result.hasConflicts()) {
                String qualifiedName = extractQualifiedName(result.subjectId());
                TypeId typeId = TypeId.of(qualifiedName);
                String simpleName = typeId.simpleName();
                nodeIdToSimpleName.put(result.subjectId().value(), simpleName);

                List<ConflictingContribution> contributions =
                        result.conflicts().stream().map(this::toContribution).toList();

                String resolution =
                        result.isClassified() ? "Resolved to " + result.kind() : "Could not resolve conflict";

                conflicts.add(ClassificationConflict.of(typeId, simpleName, contributions, resolution, Instant.now()));
            }
        }

        return conflicts;
    }

    private ConflictingContribution toContribution(Conflict conflict) {
        return ConflictingContribution.of(
                toArchKind(conflict.competingKind()),
                conflict.competingCriteria(),
                conflict.competingPriority(),
                toArchConfidence(conflict.competingConfidence()));
    }

    private ArchKind toArchKind(String kind) {
        try {
            return ArchKind.valueOf(kind);
        } catch (IllegalArgumentException e) {
            return ArchKind.UNCLASSIFIED;
        }
    }

    private ConfidenceLevel toArchConfidence(io.hexaglue.core.classification.ConfidenceLevel coreConfidence) {
        if (coreConfidence == null) {
            return ConfidenceLevel.LOW;
        }
        return switch (coreConfidence) {
            case EXPLICIT, HIGH -> ConfidenceLevel.HIGH;
            case MEDIUM -> ConfidenceLevel.MEDIUM;
            case LOW -> ConfidenceLevel.LOW;
        };
    }

    private List<PrioritizedRemediation> buildRemediations(List<UnclassifiedType> unclassified) {
        return unclassified.stream()
                .map(PrioritizedRemediation::forUnclassified)
                .toList();
    }

    private String extractQualifiedName(NodeId nodeId) {
        String value = nodeId.value();
        int colonIndex = value.indexOf(':');
        return colonIndex > 0 ? value.substring(colonIndex + 1) : value;
    }
}
