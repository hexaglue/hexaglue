package io.hexaglue.core.graph.model;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.graph.style.PackageOrganizationStyle;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata about the application graph.
 *
 * @param basePackage the base package that was analyzed
 * @param javaVersion the Java version used for analysis
 * @param buildTimestamp when the graph was built
 * @param sourceCount number of source types analyzed
 * @param style the detected package organization style
 * @param styleConfidence confidence level of style detection
 * @param detectedPatterns patterns found during style detection (pattern -> count)
 */
public record GraphMetadata(
        String basePackage,
        int javaVersion,
        Instant buildTimestamp,
        int sourceCount,
        PackageOrganizationStyle style,
        ConfidenceLevel styleConfidence,
        Map<String, Integer> detectedPatterns) {

    public GraphMetadata {
        Objects.requireNonNull(basePackage, "basePackage cannot be null");
        Objects.requireNonNull(buildTimestamp, "buildTimestamp cannot be null");
        Objects.requireNonNull(style, "style cannot be null");
        Objects.requireNonNull(styleConfidence, "styleConfidence cannot be null");
        Objects.requireNonNull(detectedPatterns, "detectedPatterns cannot be null");
        if (javaVersion < 8) {
            throw new IllegalArgumentException("javaVersion must be at least 8");
        }
        if (sourceCount < 0) {
            throw new IllegalArgumentException("sourceCount cannot be negative");
        }
        detectedPatterns = Map.copyOf(detectedPatterns);
    }

    /**
     * Creates metadata with current timestamp and unknown style.
     */
    public static GraphMetadata of(String basePackage, int javaVersion, int sourceCount) {
        return new GraphMetadata(
                basePackage,
                javaVersion,
                Instant.now(),
                sourceCount,
                PackageOrganizationStyle.UNKNOWN,
                ConfidenceLevel.LOW,
                Map.of());
    }

    /**
     * Creates a builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if style detection was confident.
     */
    public boolean hasConfidentStyle() {
        return styleConfidence == ConfidenceLevel.HIGH || styleConfidence == ConfidenceLevel.EXPLICIT;
    }

    /**
     * Returns true if a known style was detected.
     */
    public boolean hasKnownStyle() {
        return style.isKnown();
    }

    public static final class Builder {
        private String basePackage = "";
        private int javaVersion = 17;
        private Instant buildTimestamp = Instant.now();
        private int sourceCount = 0;
        private PackageOrganizationStyle style = PackageOrganizationStyle.UNKNOWN;
        private ConfidenceLevel styleConfidence = ConfidenceLevel.LOW;
        private Map<String, Integer> detectedPatterns = Map.of();

        private Builder() {}

        public Builder basePackage(String basePackage) {
            this.basePackage = basePackage;
            return this;
        }

        public Builder javaVersion(int javaVersion) {
            this.javaVersion = javaVersion;
            return this;
        }

        public Builder buildTimestamp(Instant buildTimestamp) {
            this.buildTimestamp = buildTimestamp;
            return this;
        }

        public Builder sourceCount(int sourceCount) {
            this.sourceCount = sourceCount;
            return this;
        }

        public Builder style(PackageOrganizationStyle style) {
            this.style = style;
            return this;
        }

        public Builder styleConfidence(ConfidenceLevel styleConfidence) {
            this.styleConfidence = styleConfidence;
            return this;
        }

        public Builder detectedPatterns(Map<String, Integer> detectedPatterns) {
            this.detectedPatterns = detectedPatterns;
            return this;
        }

        public String basePackage() {
            return basePackage;
        }

        public GraphMetadata build() {
            return new GraphMetadata(
                    basePackage, javaVersion, buildTimestamp, sourceCount, style, styleConfidence, detectedPatterns);
        }
    }
}
