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

/**
 * Configuration for controlling the depth of type resolution during analysis.
 *
 * <p>Type resolution is a recursive process that can become expensive for deeply nested
 * or highly connected type graphs. This configuration allows tuning the trade-off between
 * analysis completeness and performance.
 *
 * <p>Depth values:
 * <ul>
 *   <li><b>-1</b>: Unlimited depth (full resolution)</li>
 *   <li><b>0</b>: No resolution (skip entirely)</li>
 *   <li><b>N &gt; 0</b>: Resolve up to N levels deep</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Fast analysis for large codebases
 * ResolutionConfig config = ResolutionConfig.fast();
 *
 * // Complete analysis for thorough classification
 * ResolutionConfig config = ResolutionConfig.complete();
 *
 * // Custom configuration
 * ResolutionConfig config = ResolutionConfig.builder()
 *     .maxDepthForMemberTypes(2)
 *     .maxDepthForGenericTypes(3)
 *     .build();
 * }</pre>
 *
 * @since 3.0.0
 */
public record ResolutionConfig(
        int maxDepthForMemberTypes,
        int maxDepthForSupertypes,
        int maxDepthForAnnotations,
        int maxDepthForGenericTypes,
        int maxDepthForMethodBodies) {

    /**
     * Creates a resolution config with validation.
     *
     * @throws IllegalArgumentException if any depth is less than -1
     */
    public ResolutionConfig {
        validateDepth("maxDepthForMemberTypes", maxDepthForMemberTypes);
        validateDepth("maxDepthForSupertypes", maxDepthForSupertypes);
        validateDepth("maxDepthForAnnotations", maxDepthForAnnotations);
        validateDepth("maxDepthForGenericTypes", maxDepthForGenericTypes);
        validateDepth("maxDepthForMethodBodies", maxDepthForMethodBodies);
    }

    /**
     * Returns the default configuration.
     *
     * <p>Default settings:
     * <ul>
     *   <li>Member types: 1 level (fields and their direct types)</li>
     *   <li>Supertypes: Unlimited (full inheritance chain)</li>
     *   <li>Annotations: Unlimited (all annotations and meta-annotations)</li>
     *   <li>Generic types: 1 level (e.g., {@code List<Order>} but not nested)</li>
     *   <li>Method bodies: 1 level (direct invocations only)</li>
     * </ul>
     *
     * @return the default configuration
     */
    public static ResolutionConfig defaults() {
        return new ResolutionConfig(1, -1, -1, 1, 1);
    }

    /**
     * Returns a fast configuration optimized for large codebases.
     *
     * <p>Fast settings minimize analysis depth while preserving essential classification signals:
     * <ul>
     *   <li>Member types: 1 level (immediate field types only)</li>
     *   <li>Supertypes: 2 levels (direct parent and grandparent)</li>
     *   <li>Annotations: 1 level (direct annotations only, no meta-annotations)</li>
     *   <li>Generic types: 1 level (e.g., {@code List<Order>} but not nested)</li>
     *   <li>Method bodies: 0 (skip method body analysis)</li>
     * </ul>
     *
     * <p>Expected performance: ~100ms for 1000 types
     *
     * @return the fast configuration
     */
    public static ResolutionConfig fast() {
        return new ResolutionConfig(1, 2, 1, 1, 0);
    }

    /**
     * Returns a complete configuration for thorough analysis.
     *
     * <p>Complete settings enable unlimited depth for all dimensions, providing
     * maximum classification accuracy at the cost of performance:
     * <ul>
     *   <li>Member types: Unlimited (full transitive closure)</li>
     *   <li>Supertypes: Unlimited (full inheritance chain)</li>
     *   <li>Annotations: Unlimited (all meta-annotations)</li>
     *   <li>Generic types: Unlimited (deeply nested generics)</li>
     *   <li>Method bodies: Unlimited (full call graph traversal)</li>
     * </ul>
     *
     * <p>Expected performance: May exceed 1s for complex graphs with 1000+ types
     *
     * @return the complete configuration
     */
    public static ResolutionConfig complete() {
        return new ResolutionConfig(-1, -1, -1, -1, -1);
    }

    /**
     * Returns a builder for custom configuration.
     *
     * @return a new builder initialized with default values
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if member type resolution is enabled.
     *
     * @return true if maxDepthForMemberTypes is non-zero
     */
    public boolean resolveMemberTypes() {
        return maxDepthForMemberTypes != 0;
    }

    /**
     * Returns true if supertype resolution is enabled.
     *
     * @return true if maxDepthForSupertypes is non-zero
     */
    public boolean resolveSupertypes() {
        return maxDepthForSupertypes != 0;
    }

    /**
     * Returns true if annotation resolution is enabled.
     *
     * @return true if maxDepthForAnnotations is non-zero
     */
    public boolean resolveAnnotations() {
        return maxDepthForAnnotations != 0;
    }

    /**
     * Returns true if generic type resolution is enabled.
     *
     * @return true if maxDepthForGenericTypes is non-zero
     */
    public boolean resolveGenericTypes() {
        return maxDepthForGenericTypes != 0;
    }

    /**
     * Returns true if method body analysis is enabled.
     *
     * @return true if maxDepthForMethodBodies is non-zero
     */
    public boolean resolveMethodBodies() {
        return maxDepthForMethodBodies != 0;
    }

    /**
     * Returns true if this configuration allows unlimited resolution for all dimensions.
     *
     * @return true if all depths are -1 (unlimited)
     */
    public boolean isComplete() {
        return maxDepthForMemberTypes == -1
                && maxDepthForSupertypes == -1
                && maxDepthForAnnotations == -1
                && maxDepthForGenericTypes == -1
                && maxDepthForMethodBodies == -1;
    }

    /**
     * Returns true if this configuration is optimized for fast analysis.
     *
     * @return true if this matches the fast() preset
     */
    public boolean isFast() {
        return equals(fast());
    }

    private static void validateDepth(String fieldName, int depth) {
        if (depth < -1) {
            throw new IllegalArgumentException(
                    fieldName + " must be >= -1 (got " + depth + "). Use -1 for unlimited, 0 to disable.");
        }
    }

    /**
     * Builder for creating custom {@link ResolutionConfig} instances.
     *
     * @since 3.0.0
     */
    public static final class Builder {
        private int maxDepthForMemberTypes = 1;
        private int maxDepthForSupertypes = -1;
        private int maxDepthForAnnotations = -1;
        private int maxDepthForGenericTypes = 1;
        private int maxDepthForMethodBodies = 1;

        private Builder() {}

        /**
         * Sets the maximum depth for resolving member types.
         *
         * @param depth the maximum depth (-1 for unlimited, 0 to disable, N &gt; 0 for N levels)
         * @return this builder
         * @throws IllegalArgumentException if depth &lt; -1
         */
        public Builder maxDepthForMemberTypes(int depth) {
            validateDepth("maxDepthForMemberTypes", depth);
            this.maxDepthForMemberTypes = depth;
            return this;
        }

        /**
         * Sets the maximum depth for resolving supertypes.
         *
         * @param depth the maximum depth (-1 for unlimited, 0 to disable, N &gt; 0 for N levels)
         * @return this builder
         * @throws IllegalArgumentException if depth &lt; -1
         */
        public Builder maxDepthForSupertypes(int depth) {
            validateDepth("maxDepthForSupertypes", depth);
            this.maxDepthForSupertypes = depth;
            return this;
        }

        /**
         * Sets the maximum depth for resolving annotations.
         *
         * @param depth the maximum depth (-1 for unlimited, 0 to disable, N &gt; 0 for N levels)
         * @return this builder
         * @throws IllegalArgumentException if depth &lt; -1
         */
        public Builder maxDepthForAnnotations(int depth) {
            validateDepth("maxDepthForAnnotations", depth);
            this.maxDepthForAnnotations = depth;
            return this;
        }

        /**
         * Sets the maximum depth for resolving generic types.
         *
         * @param depth the maximum depth (-1 for unlimited, 0 to disable, N &gt; 0 for N levels)
         * @return this builder
         * @throws IllegalArgumentException if depth &lt; -1
         */
        public Builder maxDepthForGenericTypes(int depth) {
            validateDepth("maxDepthForGenericTypes", depth);
            this.maxDepthForGenericTypes = depth;
            return this;
        }

        /**
         * Sets the maximum depth for method body analysis.
         *
         * @param depth the maximum depth (-1 for unlimited, 0 to disable, N &gt; 0 for N levels)
         * @return this builder
         * @throws IllegalArgumentException if depth &lt; -1
         */
        public Builder maxDepthForMethodBodies(int depth) {
            validateDepth("maxDepthForMethodBodies", depth);
            this.maxDepthForMethodBodies = depth;
            return this;
        }

        /**
         * Builds the resolution config.
         *
         * @return the built configuration
         */
        public ResolutionConfig build() {
            return new ResolutionConfig(
                    maxDepthForMemberTypes,
                    maxDepthForSupertypes,
                    maxDepthForAnnotations,
                    maxDepthForGenericTypes,
                    maxDepthForMethodBodies);
        }
    }
}
