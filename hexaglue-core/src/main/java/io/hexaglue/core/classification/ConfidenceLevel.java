package io.hexaglue.core.classification;

/**
 * Confidence level of a classification.
 *
 * <p>Higher confidence means more certainty in the classification.
 * Used for tie-breaking when multiple criteria match.
 */
public enum ConfidenceLevel {

    /**
     * Explicit annotation present (e.g., @AggregateRoot).
     * Highest confidence - user explicitly declared intent.
     */
    EXPLICIT(100),

    /**
     * Strong heuristic match (e.g., used in Repository signature + has id field).
     * High confidence - multiple strong signals align.
     */
    HIGH(80),

    /**
     * Medium heuristic match (e.g., naming pattern + package location).
     * Moderate confidence - some signals present.
     */
    MEDIUM(60),

    /**
     * Weak heuristic match (e.g., only naming pattern).
     * Low confidence - minimal signals.
     */
    LOW(40);

    private final int weight;

    ConfidenceLevel(int weight) {
        this.weight = weight;
    }

    /**
     * Returns the numeric weight of this confidence level.
     * Higher weight = higher confidence.
     */
    public int weight() {
        return weight;
    }

    /**
     * Returns true if this confidence is at least as high as the given level.
     */
    public boolean isAtLeast(ConfidenceLevel other) {
        return this.weight >= other.weight;
    }

    /**
     * Returns true if this confidence is higher than the given level.
     */
    public boolean isHigherThan(ConfidenceLevel other) {
        return this.weight > other.weight;
    }
}
