package io.hexaglue.core.classification;

import java.util.List;
import java.util.Objects;

/**
 * Result of evaluating a classification criteria.
 *
 * <p>Contains whether the criteria matched, and if so, the confidence
 * level and supporting evidence.
 *
 * @param matched whether the criteria matched
 * @param confidence confidence level (null if not matched)
 * @param justification human-readable justification (null if not matched)
 * @param evidence list of evidence supporting the match
 */
public record MatchResult(boolean matched, ConfidenceLevel confidence, String justification, List<Evidence> evidence) {

    /**
     * Canonical constructor with validation.
     */
    public MatchResult {
        if (matched) {
            Objects.requireNonNull(confidence, "confidence required when matched");
            Objects.requireNonNull(justification, "justification required when matched");
        }
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    /**
     * Creates a no-match result.
     */
    public static MatchResult noMatch() {
        return new MatchResult(false, null, null, List.of());
    }

    /**
     * Creates a match result with the given confidence and justification.
     */
    public static MatchResult match(ConfidenceLevel confidence, String justification) {
        return new MatchResult(true, confidence, justification, List.of());
    }

    /**
     * Creates a match result with confidence, justification, and evidence.
     */
    public static MatchResult match(ConfidenceLevel confidence, String justification, Evidence... evidence) {
        return new MatchResult(true, confidence, justification, List.of(evidence));
    }

    /**
     * Creates a match result with confidence, justification, and evidence list.
     */
    public static MatchResult match(ConfidenceLevel confidence, String justification, List<Evidence> evidence) {
        return new MatchResult(true, confidence, justification, evidence);
    }

    /**
     * Creates an explicit match (highest confidence) from an annotation.
     */
    public static MatchResult explicitAnnotation(String annotationName, io.hexaglue.core.graph.model.NodeId nodeId) {
        return new MatchResult(
                true,
                ConfidenceLevel.EXPLICIT,
                "Explicitly annotated with @" + annotationName,
                List.of(Evidence.fromAnnotation(annotationName, nodeId)));
    }

    /**
     * Returns true if this result matched with at least the given confidence.
     */
    public boolean matchedWithAtLeast(ConfidenceLevel minConfidence) {
        return matched && confidence != null && confidence.isAtLeast(minConfidence);
    }

    /**
     * Returns true if this is a no-match result.
     */
    public boolean isNoMatch() {
        return !matched;
    }
}
