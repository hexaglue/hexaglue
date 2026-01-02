package io.hexaglue.core.classification;

/**
 * Status of a classification attempt.
 */
public enum ClassificationStatus {

    /**
     * Type was successfully classified with a single winning criteria.
     */
    CLASSIFIED,

    /**
     * No criteria matched - type remains unclassified.
     */
    UNCLASSIFIED,

    /**
     * Multiple conflicting criteria matched - classification is ambiguous.
     */
    CONFLICT
}
