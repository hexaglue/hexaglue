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

package io.hexaglue.spi.audit;

import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Execution context provided to audit plugins.
 *
 * <p>This record encapsulates all the resources and information an audit
 * plugin needs to perform code quality and architecture analysis. It provides
 * access to:
 * <ul>
 *   <li>The codebase to audit</li>
 *   <li>The audit rules to apply</li>
 *   <li>Diagnostic reporting capabilities</li>
 *   <li>Plugin-specific configuration</li>
 *   <li>Optional architecture query from core analysis</li>
 * </ul>
 *
 * @param codebase          the codebase being audited
 * @param rules             the audit rules to apply
 * @param diagnostics       the diagnostic reporter
 * @param config            the plugin-specific configuration
 * @param architectureQuery optional architecture query from core (may be null)
 * @since 3.0.0
 */
public record AuditContext(
        Codebase codebase,
        List<AuditRule> rules,
        DiagnosticReporter diagnostics,
        PluginConfig config,
        ArchitectureQuery architectureQuery) {

    /**
     * Compact constructor with validation and defensive copies.
     *
     * @throws NullPointerException if required parameter is null
     */
    public AuditContext {
        Objects.requireNonNull(codebase, "codebase required");
        rules = rules != null ? List.copyOf(rules) : List.of();
        Objects.requireNonNull(diagnostics, "diagnostics required");
        Objects.requireNonNull(config, "config required");
        // architectureQuery may be null for backward compatibility
    }

    /**
     * Legacy constructor for backward compatibility.
     *
     * @param codebase    the codebase being audited
     * @param rules       the audit rules to apply
     * @param diagnostics the diagnostic reporter
     * @param config      the plugin-specific configuration
     */
    public AuditContext(
            Codebase codebase, List<AuditRule> rules, DiagnosticReporter diagnostics, PluginConfig config) {
        this(codebase, rules, diagnostics, config, null);
    }

    /**
     * Returns the architecture query if available.
     *
     * <p>When running within HexaGlue's plugin system, this returns the core's
     * rich architecture analysis capabilities (Lakos metrics, coupling analysis,
     * cycle detection, etc.). If not available, plugins should fall back to
     * their own implementation.
     *
     * @return the architecture query, or empty if not available
     */
    public Optional<ArchitectureQuery> query() {
        return Optional.ofNullable(architectureQuery);
    }
}
