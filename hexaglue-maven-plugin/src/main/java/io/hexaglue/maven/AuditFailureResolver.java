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

package io.hexaglue.maven;

import io.hexaglue.arch.model.audit.AuditSnapshot;
import io.hexaglue.arch.model.audit.Severity;
import java.util.Map;

/**
 * Resolves audit failure behavior with Maven &gt; YAML &gt; defaults precedence.
 *
 * <p>Precedence chain for each property:
 * <ol>
 *   <li>Maven {@code -D} system property or POM {@code <configuration>} (if non-null)</li>
 *   <li>YAML {@code hexaglue.yaml} under {@code plugins: io.hexaglue.plugin.audit:} (if present)</li>
 *   <li>Code defaults: failOnError=true, errorOnBlocker=true, errorOnCritical=false</li>
 * </ol>
 *
 * @param failOnError    whether audit errors should fail the Maven build
 * @param errorOnBlocker whether BLOCKER violations count as errors
 * @param errorOnCritical whether CRITICAL violations count as errors
 * @since 5.1.0
 */
public record AuditFailureResolver(boolean failOnError, boolean errorOnBlocker, boolean errorOnCritical) {

    private static final String AUDIT_PLUGIN_ID = "io.hexaglue.plugin.audit";
    private static final String AUDIT_SHORT_ID = "audit";

    /**
     * Resolves failure settings from Maven parameters and YAML config.
     *
     * <p>Looks up the audit plugin configuration in the YAML plugin configs map
     * using both the full plugin ID ({@code io.hexaglue.plugin.audit}) and the
     * short form ({@code audit}).
     *
     * @param mavenFailOnError    Maven-configured failOnError (null if not set)
     * @param mavenErrorOnBlocker Maven-configured errorOnBlocker (null if not set)
     * @param mavenErrorOnCritical Maven-configured errorOnCritical (null if not set)
     * @param pluginConfigs       plugin configurations loaded from hexaglue.yaml
     * @return resolved failure settings
     */
    public static AuditFailureResolver resolve(
            Boolean mavenFailOnError,
            Boolean mavenErrorOnBlocker,
            Boolean mavenErrorOnCritical,
            Map<String, Map<String, Object>> pluginConfigs) {

        Map<String, Object> auditYaml = resolveAuditYaml(pluginConfigs);

        boolean resolvedFailOnError = resolveBoolean(mavenFailOnError, auditYaml, "failOnError", true);
        boolean resolvedBlocker = resolveBoolean(mavenErrorOnBlocker, auditYaml, "errorOnBlocker", true);
        boolean resolvedCritical = resolveBoolean(mavenErrorOnCritical, auditYaml, "errorOnCritical", false);

        return new AuditFailureResolver(resolvedFailOnError, resolvedBlocker, resolvedCritical);
    }

    /**
     * Resolves the audit plugin YAML configuration from the plugin configs map.
     *
     * <p>Checks the full plugin ID first, then falls back to the short form.
     */
    private static Map<String, Object> resolveAuditYaml(Map<String, Map<String, Object>> pluginConfigs) {
        if (pluginConfigs == null) {
            return Map.of();
        }
        Map<String, Object> yaml = pluginConfigs.get(AUDIT_PLUGIN_ID);
        if (yaml != null) {
            return yaml;
        }
        yaml = pluginConfigs.get(AUDIT_SHORT_ID);
        return yaml != null ? yaml : Map.of();
    }

    private static boolean resolveBoolean(
            Boolean mavenValue, Map<String, Object> yaml, String key, boolean defaultValue) {
        if (mavenValue != null) {
            return mavenValue;
        }
        Object yamlValue = yaml.get(key);
        if (yamlValue instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }

    /**
     * Counts the number of error-level violations in the snapshot.
     *
     * <p>A violation counts as an error if its severity is enabled:
     * <ul>
     *   <li>BLOCKER violations count if {@code errorOnBlocker} is true</li>
     *   <li>CRITICAL violations count if {@code errorOnCritical} is true</li>
     * </ul>
     *
     * @param snapshot the audit snapshot
     * @return the number of error-level violations
     */
    public long countErrors(AuditSnapshot snapshot) {
        long count = 0;
        if (errorOnBlocker) {
            count += snapshot.violationsOfSeverity(Severity.BLOCKER).size();
        }
        if (errorOnCritical) {
            count += snapshot.violationsOfSeverity(Severity.CRITICAL).size();
        }
        return count;
    }
}
