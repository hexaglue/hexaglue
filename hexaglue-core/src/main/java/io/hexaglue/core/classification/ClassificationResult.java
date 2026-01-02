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
        ClassificationStatus status) {

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
                ClassificationStatus.CLASSIFIED);
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
                ClassificationStatus.CLASSIFIED);
    }

    /**
     * Creates an unclassified result (no criteria matched).
     */
    public static ClassificationResult unclassified(NodeId subjectId) {
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
                ClassificationStatus.UNCLASSIFIED);
    }

    /**
     * Creates a conflict result (multiple incompatible criteria matched).
     */
    public static ClassificationResult conflict(NodeId subjectId, List<Conflict> conflicts) {
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
                ClassificationStatus.CONFLICT);
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
}
