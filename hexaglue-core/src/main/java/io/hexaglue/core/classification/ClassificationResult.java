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

package io.hexaglue.core.classification;

import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.graph.model.NodeId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of classifying a type.
 *
 * <p>Contains the classification outcome, including the winning kind,
 * confidence, criteria that matched, evidence, and any conflicts.
 *
 * @param subjectId the node that was classified
 * @param target domain or port classification
 * @param kind the classification kind (e.g., "AGGREGATE_ROOT", "REPOSITORY")
 * @param confidence confidence level of the classification
 * @param matchedCriteria name of the criteria that won
 * @param matchedPriority priority of the winning criteria
 * @param justification human-readable justification
 * @param evidence evidence supporting the classification
 * @param conflicts any conflicting criteria that also matched
 * @param portDirection the port direction (null if target != PORT)
 * @param status the classification status
 * @param reasonTrace detailed trace of the classification decision (for debugging)
 */
public record ClassificationResult(
        NodeId subjectId,
        ClassificationTarget target,
        String kind,
        ConfidenceLevel confidence,
        String matchedCriteria,
        int matchedPriority,
        String justification,
        List<Evidence> evidence,
        List<Conflict> conflicts,
        PortDirection portDirection,
        ClassificationStatus status,
        ReasonTrace reasonTrace) {

    public ClassificationResult {
        Objects.requireNonNull(subjectId, "subjectId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
    }

    /**
     * Creates a successful domain classification result.
     */
    public static ClassificationResult classified(
            NodeId subjectId,
            ClassificationTarget target,
            String kind,
            ConfidenceLevel confidence,
            String matchedCriteria,
            int matchedPriority,
            String justification,
            List<Evidence> evidence,
            List<Conflict> conflicts) {
        return classified(
                subjectId,
                target,
                kind,
                confidence,
                matchedCriteria,
                matchedPriority,
                justification,
                evidence,
                conflicts,
                null);
    }

    /**
     * Creates a successful domain classification result with reason trace.
     */
    public static ClassificationResult classified(
            NodeId subjectId,
            ClassificationTarget target,
            String kind,
            ConfidenceLevel confidence,
            String matchedCriteria,
            int matchedPriority,
            String justification,
            List<Evidence> evidence,
            List<Conflict> conflicts,
            ReasonTrace reasonTrace) {
        return new ClassificationResult(
                subjectId,
                target,
                kind,
                confidence,
                matchedCriteria,
                matchedPriority,
                justification,
                evidence,
                conflicts,
                null,
                ClassificationStatus.CLASSIFIED,
                reasonTrace);
    }

    /**
     * Creates a successful port classification result with direction.
     */
    public static ClassificationResult classifiedPort(
            NodeId subjectId,
            String kind,
            ConfidenceLevel confidence,
            String matchedCriteria,
            int matchedPriority,
            String justification,
            List<Evidence> evidence,
            List<Conflict> conflicts,
            PortDirection direction) {
        return classifiedPort(
                subjectId,
                kind,
                confidence,
                matchedCriteria,
                matchedPriority,
                justification,
                evidence,
                conflicts,
                direction,
                null);
    }

    /**
     * Creates a successful port classification result with direction and reason trace.
     */
    public static ClassificationResult classifiedPort(
            NodeId subjectId,
            String kind,
            ConfidenceLevel confidence,
            String matchedCriteria,
            int matchedPriority,
            String justification,
            List<Evidence> evidence,
            List<Conflict> conflicts,
            PortDirection direction,
            ReasonTrace reasonTrace) {
        return new ClassificationResult(
                subjectId,
                ClassificationTarget.PORT,
                kind,
                confidence,
                matchedCriteria,
                matchedPriority,
                justification,
                evidence,
                conflicts,
                direction,
                ClassificationStatus.CLASSIFIED,
                reasonTrace);
    }

    /**
     * Creates an unclassified result (no criteria matched).
     *
     * @deprecated Use {@link #unclassifiedDomain} or {@link #unclassifiedPort} for explicit target.
     */
    @Deprecated(since = "3.0.0")
    public static ClassificationResult unclassified(NodeId subjectId) {
        return unclassified(subjectId, null);
    }

    /**
     * Creates an unclassified result with reason trace.
     *
     * @deprecated Use {@link #unclassifiedDomain} or {@link #unclassifiedPort} for explicit target.
     */
    @Deprecated(since = "3.0.0")
    public static ClassificationResult unclassified(NodeId subjectId, ReasonTrace reasonTrace) {
        return new ClassificationResult(
                subjectId,
                null,
                null,
                null,
                null,
                0,
                null,
                List.of(),
                List.of(),
                null,
                ClassificationStatus.UNCLASSIFIED,
                reasonTrace);
    }

    /**
     * Creates an unclassified domain result.
     *
     * <p>This indicates that no classification criteria matched with sufficient
     * confidence for this domain type. Users should add explicit jMolecules
     * annotations to resolve the classification.
     *
     * @param subjectId the node that could not be classified
     * @param reasonTrace detailed trace explaining why classification failed
     * @return an UNCLASSIFIED result for the domain target
     * @since 3.0.0
     */
    public static ClassificationResult unclassifiedDomain(NodeId subjectId, ReasonTrace reasonTrace) {
        return new ClassificationResult(
                subjectId,
                ClassificationTarget.DOMAIN,
                "UNCLASSIFIED",
                null,
                null,
                0,
                "No classification criteria matched with sufficient confidence",
                List.of(),
                List.of(),
                null,
                ClassificationStatus.UNCLASSIFIED,
                reasonTrace);
    }

    /**
     * Creates an unclassified port result.
     *
     * <p>This indicates that no classification criteria matched for this port.
     * The interface may not be a port, or it needs explicit jMolecules annotations.
     *
     * @param subjectId the node that could not be classified
     * @param reasonTrace detailed trace explaining why classification failed
     * @return an UNCLASSIFIED result for the port target
     * @since 3.0.0
     */
    public static ClassificationResult unclassifiedPort(NodeId subjectId, ReasonTrace reasonTrace) {
        return new ClassificationResult(
                subjectId,
                ClassificationTarget.PORT,
                null, // Ports don't use UNCLASSIFIED kind, they simply aren't ports
                null,
                null,
                0,
                "No port classification criteria matched",
                List.of(),
                List.of(),
                null,
                ClassificationStatus.UNCLASSIFIED,
                reasonTrace);
    }

    /**
     * Creates a conflict result (multiple incompatible criteria matched).
     *
     * @deprecated Use {@link #conflictDomain} or {@link #conflictPort} instead for explicit target.
     */
    @Deprecated(since = "0.5.0")
    public static ClassificationResult conflict(NodeId subjectId, List<Conflict> conflicts) {
        return conflict(subjectId, conflicts, null);
    }

    /**
     * Creates a conflict result with reason trace.
     *
     * @deprecated Use {@link #conflictDomain} or {@link #conflictPort} instead for explicit target.
     */
    @Deprecated(since = "0.5.0")
    public static ClassificationResult conflict(NodeId subjectId, List<Conflict> conflicts, ReasonTrace reasonTrace) {
        return new ClassificationResult(
                subjectId,
                null,
                null,
                null,
                null,
                0,
                "Multiple conflicting criteria matched",
                List.of(),
                conflicts,
                null,
                ClassificationStatus.CONFLICT,
                reasonTrace);
    }

    /**
     * Creates a domain conflict result (multiple incompatible domain criteria matched).
     */
    public static ClassificationResult conflictDomain(NodeId subjectId, List<Conflict> conflicts) {
        return conflictDomain(subjectId, conflicts, null);
    }

    /**
     * Creates a domain conflict result with reason trace.
     */
    public static ClassificationResult conflictDomain(
            NodeId subjectId, List<Conflict> conflicts, ReasonTrace reasonTrace) {
        return new ClassificationResult(
                subjectId,
                ClassificationTarget.DOMAIN,
                null,
                null,
                null,
                0,
                "Multiple conflicting domain criteria matched",
                List.of(),
                conflicts,
                null,
                ClassificationStatus.CONFLICT,
                reasonTrace);
    }

    /**
     * Creates a port conflict result (multiple incompatible port criteria matched).
     */
    public static ClassificationResult conflictPort(NodeId subjectId, List<Conflict> conflicts) {
        return conflictPort(subjectId, conflicts, null);
    }

    /**
     * Creates a port conflict result with reason trace.
     */
    public static ClassificationResult conflictPort(
            NodeId subjectId, List<Conflict> conflicts, ReasonTrace reasonTrace) {
        return new ClassificationResult(
                subjectId,
                ClassificationTarget.PORT,
                null,
                null,
                null,
                0,
                "Multiple conflicting port criteria matched",
                List.of(),
                conflicts,
                null,
                ClassificationStatus.CONFLICT,
                reasonTrace);
    }

    /**
     * Returns true if this is a successful classification.
     */
    public boolean isClassified() {
        return status == ClassificationStatus.CLASSIFIED;
    }

    /**
     * Returns true if no criteria matched.
     */
    public boolean isUnclassified() {
        return status == ClassificationStatus.UNCLASSIFIED;
    }

    /**
     * Returns true if there were conflicts.
     */
    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    /**
     * Returns the kind as an Optional.
     */
    public Optional<String> kindOpt() {
        return Optional.ofNullable(kind);
    }

    /**
     * Returns the confidence as an Optional.
     */
    public Optional<ConfidenceLevel> confidenceOpt() {
        return Optional.ofNullable(confidence);
    }

    /**
     * Returns the port direction as an Optional.
     */
    public Optional<PortDirection> portDirectionOpt() {
        return Optional.ofNullable(portDirection);
    }

    /**
     * Returns the reason trace as an Optional.
     */
    public Optional<ReasonTrace> reasonTraceOpt() {
        return Optional.ofNullable(reasonTrace);
    }
}
