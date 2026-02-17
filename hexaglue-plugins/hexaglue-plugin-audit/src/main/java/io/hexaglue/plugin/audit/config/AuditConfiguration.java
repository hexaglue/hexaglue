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

/**
 * Configuration for audit plugin execution.
 *
 * <p>This record encapsulates all configuration options for the audit plugin:
 * <ul>
 *   <li>Severity overrides for specific constraints</li>
 * </ul>
 *
 * <p>All constraints and all metrics are always executed. Report formats
 * (JSON, HTML, Markdown) are always generated.
 *
 * <p>Build failure decisions (failOnError, errorOnBlocker, errorOnCritical) are
 * handled by the Maven mojos, not by this plugin configuration.
 *
 * @param severityOverrides custom severity levels for specific constraints
 * @since 1.0.0
 * @since 5.1.0 - Removed allowCriticalViolations (now handled by Maven mojos)
 */
public record AuditConfiguration(Map<String, String> severityOverrides) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public AuditConfiguration {
        severityOverrides = severityOverrides != null ? Map.copyOf(severityOverrides) : Map.of();
    }

    /**
     * Returns the default configuration.
     *
     * @return default configuration
     */
    public static AuditConfiguration defaults() {
        return new AuditConfiguration(Map.of());
    }

    /**
     * Creates configuration from plugin config properties.
     *
     * @param config the plugin configuration
     * @return audit configuration
     */
    public static AuditConfiguration fromPluginConfig(PluginConfig config) {
        Objects.requireNonNull(config, "config required");

        // Severity overrides would require getAll() which doesn't exist yet
        // For MVP, just use empty map
        return new AuditConfiguration(Map.of());
    }
}
