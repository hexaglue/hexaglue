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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.audit.ArchitectureMetrics;
import io.hexaglue.arch.model.audit.AuditMetadata;
import io.hexaglue.arch.model.audit.AuditSnapshot;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.DetectedArchitectureStyle;
import io.hexaglue.arch.model.audit.QualityMetrics;
import io.hexaglue.arch.model.audit.RuleViolation;
import io.hexaglue.arch.model.audit.Severity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AuditFailureResolver}.
 *
 * @since 5.1.0
 */
@DisplayName("AuditFailureResolver")
class AuditFailureResolverTest {

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("should use defaults when all Maven params are null and no YAML config")
        void shouldUseDefaults() {
            AuditFailureResolver resolver = AuditFailureResolver.resolve(null, null, null, Map.of());

            assertThat(resolver.failOnError()).isTrue();
            assertThat(resolver.errorOnBlocker()).isTrue();
            assertThat(resolver.errorOnCritical()).isFalse();
        }

        @Test
        @DisplayName("should use defaults when pluginConfigs is null")
        void shouldUseDefaultsWhenPluginConfigsNull() {
            AuditFailureResolver resolver = AuditFailureResolver.resolve(null, null, null, null);

            assertThat(resolver.failOnError()).isTrue();
            assertThat(resolver.errorOnBlocker()).isTrue();
            assertThat(resolver.errorOnCritical()).isFalse();
        }
    }

    @Nested
    @DisplayName("Maven precedence")
    class MavenPrecedence {

        @Test
        @DisplayName("should use Maven values when all are provided")
        void shouldUseMavenValues() {
            AuditFailureResolver resolver = AuditFailureResolver.resolve(false, false, true, Map.of());

            assertThat(resolver.failOnError()).isFalse();
            assertThat(resolver.errorOnBlocker()).isFalse();
            assertThat(resolver.errorOnCritical()).isTrue();
        }

        @Test
        @DisplayName("should override YAML with Maven values")
        void shouldOverrideYamlWithMaven() {
            Map<String, Map<String, Object>> pluginConfigs = Map.of(
                    "io.hexaglue.plugin.audit",
                    Map.of("failOnError", true, "errorOnBlocker", true, "errorOnCritical", true));

            AuditFailureResolver resolver = AuditFailureResolver.resolve(false, false, false, pluginConfigs);

            assertThat(resolver.failOnError()).isFalse();
            assertThat(resolver.errorOnBlocker()).isFalse();
            assertThat(resolver.errorOnCritical()).isFalse();
        }
    }

    @Nested
    @DisplayName("YAML precedence")
    class YamlPrecedence {

        @Test
        @DisplayName("should use YAML values when Maven params are null (full plugin ID)")
        void shouldUseYamlWithFullPluginId() {
            Map<String, Map<String, Object>> pluginConfigs = Map.of(
                    "io.hexaglue.plugin.audit",
                    Map.of("failOnError", false, "errorOnBlocker", false, "errorOnCritical", true));

            AuditFailureResolver resolver = AuditFailureResolver.resolve(null, null, null, pluginConfigs);

            assertThat(resolver.failOnError()).isFalse();
            assertThat(resolver.errorOnBlocker()).isFalse();
            assertThat(resolver.errorOnCritical()).isTrue();
        }

        @Test
        @DisplayName("should use YAML values when Maven params are null (short plugin ID)")
        void shouldUseYamlWithShortPluginId() {
            Map<String, Map<String, Object>> pluginConfigs =
                    Map.of("audit", Map.of("failOnError", false, "errorOnCritical", true));

            AuditFailureResolver resolver = AuditFailureResolver.resolve(null, null, null, pluginConfigs);

            assertThat(resolver.failOnError()).isFalse();
            assertThat(resolver.errorOnBlocker()).isTrue(); // default
            assertThat(resolver.errorOnCritical()).isTrue();
        }

        @Test
        @DisplayName("should prefer full plugin ID over short ID")
        void shouldPreferFullPluginId() {
            Map<String, Map<String, Object>> pluginConfigs = Map.of(
                    "io.hexaglue.plugin.audit", Map.of("failOnError", false),
                    "audit", Map.of("failOnError", true));

            AuditFailureResolver resolver = AuditFailureResolver.resolve(null, null, null, pluginConfigs);

            assertThat(resolver.failOnError()).isFalse();
        }

        @Test
        @DisplayName("should use defaults for non-boolean YAML values")
        void shouldIgnoreNonBooleanYamlValues() {
            Map<String, Map<String, Object>> pluginConfigs =
                    Map.of("io.hexaglue.plugin.audit", Map.of("failOnError", "not-a-boolean", "errorOnBlocker", 42));

            AuditFailureResolver resolver = AuditFailureResolver.resolve(null, null, null, pluginConfigs);

            assertThat(resolver.failOnError()).isTrue(); // default
            assertThat(resolver.errorOnBlocker()).isTrue(); // default
        }
    }

    @Nested
    @DisplayName("countErrors")
    class CountErrors {

        @Test
        @DisplayName("should count only BLOCKER when errorOnBlocker=true and errorOnCritical=false")
        void shouldCountOnlyBlockers() {
            AuditFailureResolver resolver = new AuditFailureResolver(true, true, false);
            AuditSnapshot snapshot = snapshotWith(
                    violation(Severity.BLOCKER),
                    violation(Severity.BLOCKER),
                    violation(Severity.CRITICAL),
                    violation(Severity.MAJOR));

            assertThat(resolver.countErrors(snapshot)).isEqualTo(2);
        }

        @Test
        @DisplayName("should count only CRITICAL when errorOnBlocker=false and errorOnCritical=true")
        void shouldCountOnlyCritical() {
            AuditFailureResolver resolver = new AuditFailureResolver(true, false, true);
            AuditSnapshot snapshot = snapshotWith(
                    violation(Severity.BLOCKER),
                    violation(Severity.CRITICAL),
                    violation(Severity.CRITICAL),
                    violation(Severity.MAJOR));

            assertThat(resolver.countErrors(snapshot)).isEqualTo(2);
        }

        @Test
        @DisplayName("should count both BLOCKER and CRITICAL when both enabled")
        void shouldCountBoth() {
            AuditFailureResolver resolver = new AuditFailureResolver(true, true, true);
            AuditSnapshot snapshot = snapshotWith(
                    violation(Severity.BLOCKER),
                    violation(Severity.CRITICAL),
                    violation(Severity.CRITICAL),
                    violation(Severity.MAJOR));

            assertThat(resolver.countErrors(snapshot)).isEqualTo(3);
        }

        @Test
        @DisplayName("should return zero when both disabled")
        void shouldReturnZeroWhenBothDisabled() {
            AuditFailureResolver resolver = new AuditFailureResolver(true, false, false);
            AuditSnapshot snapshot =
                    snapshotWith(violation(Severity.BLOCKER), violation(Severity.CRITICAL), violation(Severity.MAJOR));

            assertThat(resolver.countErrors(snapshot)).isZero();
        }

        @Test
        @DisplayName("should return zero for empty violations")
        void shouldReturnZeroForEmptyViolations() {
            AuditFailureResolver resolver = new AuditFailureResolver(true, true, true);
            AuditSnapshot snapshot = snapshotWith();

            assertThat(resolver.countErrors(snapshot)).isZero();
        }
    }

    // --- Test helpers ---

    private static RuleViolation violation(Severity severity) {
        return new RuleViolation("test:rule", severity, "Test violation", List.of("TestType"), null, "");
    }

    private static AuditSnapshot snapshotWith(RuleViolation... violations) {
        return new AuditSnapshot(
                new Codebase("test", "com.test", List.of(), Map.of()),
                DetectedArchitectureStyle.HEXAGONAL,
                List.of(violations),
                new QualityMetrics(0.0, 0.0, 0, 0.0),
                new ArchitectureMetrics(0, 0.0, 0.0, 0),
                new AuditMetadata(Instant.now(), "test", Duration.ZERO));
    }
}
