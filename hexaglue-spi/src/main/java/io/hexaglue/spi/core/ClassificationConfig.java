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

package io.hexaglue.spi.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Configuration for controlling classification behavior.
 *
 * <p>This configuration allows users to:
 * <ul>
 *   <li>Exclude types from classification using glob patterns</li>
 *   <li>Explicitly classify types by fully qualified name</li>
 *   <li>Configure validation behavior (fail on unclassified, etc.)</li>
 * </ul>
 *
 * <p>Example YAML configuration:
 * <pre>{@code
 * hexaglue:
 *   classification:
 *     exclude:
 *       - "*.shared.DomainEvent"
 *       - "**.*Exception"
 *     explicit:
 *       com.example.order.domain.OrderDetails: ENTITY
 *       com.example.payment.domain.PaymentStatus: VALUE_OBJECT
 *     validation:
 *       failOnUnclassified: true
 *       allowInferred: true
 * }</pre>
 *
 * @param excludePatterns glob patterns for types to exclude from classification
 * @param explicitClassifications explicit type-to-kind mappings (FQN to DomainKind name)
 * @param validationConfig validation settings
 * @since 3.0.0
 */
public record ClassificationConfig(
        List<String> excludePatterns,
        Map<String, String> explicitClassifications,
        ValidationConfig validationConfig) {

    /**
     * Creates a classification config with validation.
     */
    public ClassificationConfig {
        excludePatterns = excludePatterns == null ? List.of() : List.copyOf(excludePatterns);
        explicitClassifications = explicitClassifications == null ? Map.of() : Map.copyOf(explicitClassifications);
        validationConfig = validationConfig == null ? ValidationConfig.defaults() : validationConfig;
    }

    /**
     * Returns the default configuration (no exclusions, no explicit classifications, default validation).
     *
     * @return the default configuration
     */
    public static ClassificationConfig defaults() {
        return new ClassificationConfig(List.of(), Map.of(), ValidationConfig.defaults());
    }

    /**
     * Returns a builder for custom configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if a type should be excluded from classification.
     *
     * @param qualifiedName the fully qualified type name
     * @return true if the type matches any exclude pattern
     */
    public boolean shouldExclude(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName cannot be null");
        for (String pattern : excludePatterns) {
            if (matchesGlob(qualifiedName, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the explicit classification for a type, if defined.
     *
     * @param qualifiedName the fully qualified type name
     * @return the explicit kind name, or empty if not explicitly classified
     */
    public Optional<String> getExplicitKind(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName cannot be null");
        return Optional.ofNullable(explicitClassifications.get(qualifiedName));
    }

    /**
     * Returns true if there are any exclusion patterns defined.
     *
     * @return true if exclusions are configured
     */
    public boolean hasExclusions() {
        return !excludePatterns.isEmpty();
    }

    /**
     * Returns true if there are any explicit classifications defined.
     *
     * @return true if explicit classifications are configured
     */
    public boolean hasExplicitClassifications() {
        return !explicitClassifications.isEmpty();
    }

    /**
     * Matches a qualified name against a glob pattern.
     *
     * <p>Glob patterns support:
     * <ul>
     *   <li>{@code *} - matches any sequence of characters except dots</li>
     *   <li>{@code **} - matches any sequence of characters including dots</li>
     * </ul>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code *.shared.DomainEvent} matches {@code com.shared.DomainEvent}</li>
     *   <li>{@code **.*Exception} matches {@code com.example.app.MyException}</li>
     *   <li>{@code com.example.*.model.*} matches {@code com.example.order.model.Order}</li>
     * </ul>
     *
     * @param qualifiedName the qualified name to match
     * @param glob the glob pattern
     * @return true if the name matches the pattern
     */
    private boolean matchesGlob(String qualifiedName, String glob) {
        // Convert glob to regex
        // ** matches anything (including dots)
        // * matches anything except dots
        String regex = glob
                .replace(".", "\\.")
                .replace("**", "<<<DOUBLESTAR>>>")
                .replace("*", "[^.]*")
                .replace("<<<DOUBLESTAR>>>", ".*");

        return Pattern.matches(regex, qualifiedName);
    }

    /**
     * Validation settings for classification.
     *
     * @param failOnUnclassified if true, build fails when unclassified types remain
     * @param allowInferred if true, accept inferred classifications (not just explicit)
     */
    public record ValidationConfig(boolean failOnUnclassified, boolean allowInferred) {

        /**
         * Returns the default validation configuration.
         *
         * <p>Defaults:
         * <ul>
         *   <li>failOnUnclassified: false (don't fail by default)</li>
         *   <li>allowInferred: true (accept inferred classifications)</li>
         * </ul>
         *
         * @return the default validation config
         */
        public static ValidationConfig defaults() {
            return new ValidationConfig(false, true);
        }

        /**
         * Returns a strict validation configuration.
         *
         * <p>Strict settings:
         * <ul>
         *   <li>failOnUnclassified: true (fail if any type is unclassified)</li>
         *   <li>allowInferred: true (accept inferred classifications)</li>
         * </ul>
         *
         * @return the strict validation config
         */
        public static ValidationConfig strict() {
            return new ValidationConfig(true, true);
        }

        /**
         * Returns an explicit-only validation configuration.
         *
         * <p>Explicit-only settings:
         * <ul>
         *   <li>failOnUnclassified: true</li>
         *   <li>allowInferred: false (only accept explicit annotations)</li>
         * </ul>
         *
         * @return the explicit-only validation config
         */
        public static ValidationConfig explicitOnly() {
            return new ValidationConfig(true, false);
        }
    }

    /**
     * Builder for creating custom {@link ClassificationConfig} instances.
     *
     * @since 3.0.0
     */
    public static final class Builder {
        private List<String> excludePatterns = List.of();
        private Map<String, String> explicitClassifications = Map.of();
        private ValidationConfig validationConfig = ValidationConfig.defaults();

        private Builder() {}

        /**
         * Sets the exclude patterns.
         *
         * @param patterns glob patterns for types to exclude
         * @return this builder
         */
        public Builder excludePatterns(List<String> patterns) {
            this.excludePatterns = patterns;
            return this;
        }

        /**
         * Sets the explicit classifications.
         *
         * @param classifications map of FQN to DomainKind name
         * @return this builder
         */
        public Builder explicitClassifications(Map<String, String> classifications) {
            this.explicitClassifications = classifications;
            return this;
        }

        /**
         * Sets the validation configuration.
         *
         * @param config the validation config
         * @return this builder
         */
        public Builder validationConfig(ValidationConfig config) {
            this.validationConfig = config;
            return this;
        }

        /**
         * Enables fail-on-unclassified validation.
         *
         * @return this builder
         */
        public Builder failOnUnclassified() {
            this.validationConfig = new ValidationConfig(true, validationConfig.allowInferred());
            return this;
        }

        /**
         * Disables inferred classifications (explicit only).
         *
         * @return this builder
         */
        public Builder explicitOnly() {
            this.validationConfig = new ValidationConfig(validationConfig.failOnUnclassified(), false);
            return this;
        }

        /**
         * Builds the classification config.
         *
         * @return the built configuration
         */
        public ClassificationConfig build() {
            return new ClassificationConfig(excludePatterns, explicitClassifications, validationConfig);
        }
    }
}
