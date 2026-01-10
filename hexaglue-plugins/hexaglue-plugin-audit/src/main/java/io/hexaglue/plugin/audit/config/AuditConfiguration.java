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

package io.hexaglue.plugin.audit.config;

import io.hexaglue.spi.plugin.PluginConfig;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for audit plugin execution.
 *
 * <p>This record encapsulates all configuration options for the audit plugin:
 * <ul>
 *   <li>Which constraints to execute</li>
 *   <li>Which metrics to calculate</li>
 *   <li>Whether CRITICAL violations should fail the build</li>
 *   <li>Severity overrides for specific constraints</li>
 * </ul>
 *
 * @param enabledConstraints     the set of constraint IDs to execute (empty = all)
 * @param enabledMetrics         the set of metric names to calculate (empty = all)
 * @param allowCriticalViolations whether CRITICAL violations are allowed
 * @param severityOverrides      custom severity levels for specific constraints
 * @since 1.0.0
 */
public record AuditConfiguration(
        Set<String> enabledConstraints,
        Set<String> enabledMetrics,
        boolean allowCriticalViolations,
        Map<String, String> severityOverrides) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public AuditConfiguration {
        enabledConstraints = enabledConstraints != null ? Set.copyOf(enabledConstraints) : Set.of();
        enabledMetrics = enabledMetrics != null ? Set.copyOf(enabledMetrics) : Set.of();
        severityOverrides = severityOverrides != null ? Map.copyOf(severityOverrides) : Map.of();
    }

    /**
     * Returns the default configuration (all constraints and metrics enabled).
     *
     * @return default configuration
     */
    public static AuditConfiguration defaults() {
        return new AuditConfiguration(
                Set.of(), // Empty = all enabled
                Set.of(), // Empty = all enabled
                false, // Don't allow CRITICAL violations
                Map.of() // No overrides
                );
    }

    /**
     * Creates configuration from plugin config properties.
     *
     * <p>Supported properties:
     * <ul>
     *   <li>{@code audit.allowCriticalViolations} - boolean</li>
     *   <li>{@code audit.enabledConstraints} - comma-separated list</li>
     *   <li>{@code audit.enabledMetrics} - comma-separated list</li>
     *   <li>{@code audit.severity.{constraintId}} - severity override</li>
     * </ul>
     *
     * @param config the plugin configuration
     * @return audit configuration
     */
    public static AuditConfiguration fromPluginConfig(PluginConfig config) {
        Objects.requireNonNull(config, "config required");

        // Parse allowCriticalViolations
        boolean allowCritical =
                config.getBoolean("audit.allowCriticalViolations").orElse(false);

        // Parse enabled constraints (comma-separated list)
        Set<String> enabledConstraints = config.getString("audit.enabledConstraints")
                .map(s -> Set.copyOf(java.util.Arrays.asList(s.split(","))))
                .orElse(Set.of());

        // Parse enabled metrics (comma-separated list)
        Set<String> enabledMetrics = config.getString("audit.enabledMetrics")
                .map(s -> Set.copyOf(java.util.Arrays.asList(s.split(","))))
                .orElse(Set.of());

        // Severity overrides would require getAll() which doesn't exist yet
        // For MVP, just use empty map
        Map<String, String> severityOverrides = Map.of();

        return new AuditConfiguration(enabledConstraints, enabledMetrics, allowCritical, severityOverrides);
    }
}
