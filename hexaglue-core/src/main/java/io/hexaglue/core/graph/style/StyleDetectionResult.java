package io.hexaglue.core.graph.style;

import io.hexaglue.core.classification.ConfidenceLevel;
import java.util.Map;
import java.util.Objects;

/**
 * Result of package organization style detection.
 *
 * <p>Contains the detected style, confidence level, and evidence of what patterns
 * were found to support the detection.
 *
 * @param style the detected package organization style
 * @param confidence how confident we are in this detection
 * @param detectedPatterns map of pattern to match count (e.g., ".ports.in" -> 5)
 * @param basePackage the base package that was analyzed
 */
public record StyleDetectionResult(
        PackageOrganizationStyle style,
        ConfidenceLevel confidence,
        Map<String, Integer> detectedPatterns,
        String basePackage) {

    public StyleDetectionResult {
        Objects.requireNonNull(style, "style cannot be null");
        Objects.requireNonNull(confidence, "confidence cannot be null");
        Objects.requireNonNull(detectedPatterns, "detectedPatterns cannot be null");
        Objects.requireNonNull(basePackage, "basePackage cannot be null");
        detectedPatterns = Map.copyOf(detectedPatterns);
    }

    /**
     * Creates an UNKNOWN result with LOW confidence.
     *
     * @param basePackage the base package
     * @return an unknown style result
     */
    public static StyleDetectionResult unknown(String basePackage) {
        return new StyleDetectionResult(PackageOrganizationStyle.UNKNOWN, ConfidenceLevel.LOW, Map.of(), basePackage);
    }

    /**
     * Returns true if the detection is confident enough to rely on.
     *
     * <p>A confident detection has either HIGH or EXPLICIT confidence,
     * meaning the patterns strongly indicate a particular style.
     */
    public boolean isConfident() {
        return confidence == ConfidenceLevel.HIGH || confidence == ConfidenceLevel.EXPLICIT;
    }

    /**
     * Returns true if the style was successfully detected (not UNKNOWN).
     */
    public boolean isDetected() {
        return style.isKnown();
    }

    /**
     * Returns the total number of pattern matches across all detected patterns.
     */
    public int totalMatches() {
        return detectedPatterns.values().stream().mapToInt(Integer::intValue).sum();
    }
}
