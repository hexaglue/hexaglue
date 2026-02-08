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

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TargetModuleValidator}.
 *
 * @since 5.0.0
 */
@DisplayName("TargetModuleValidator")
class TargetModuleValidatorTest {

    private static final Set<String> KNOWN_MODULES = Set.of("banking-core", "banking-persistence", "banking-service");

    @Nested
    @DisplayName("Valid configurations")
    class ValidConfigurations {

        @Test
        @DisplayName("should accept config with no targetModule")
        void shouldAcceptConfigWithNoTargetModule() {
            Map<String, Map<String, Object>> pluginConfigs =
                    Map.of("io.hexaglue.plugin.jpa", Map.of("entitySuffix", "Entity"));

            TargetModuleValidator.ValidationResult result =
                    TargetModuleValidator.validate(pluginConfigs, KNOWN_MODULES);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("should accept config with valid targetModule")
        void shouldAcceptConfigWithValidTargetModule() {
            Map<String, Map<String, Object>> pluginConfigs =
                    Map.of("io.hexaglue.plugin.jpa", Map.of("targetModule", "banking-persistence"));

            TargetModuleValidator.ValidationResult result =
                    TargetModuleValidator.validate(pluginConfigs, KNOWN_MODULES);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("should accept empty plugin configs")
        void shouldAcceptEmptyPluginConfigs() {
            TargetModuleValidator.ValidationResult result = TargetModuleValidator.validate(Map.of(), KNOWN_MODULES);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should accept empty plugin config values")
        void shouldAcceptEmptyPluginConfigValues() {
            Map<String, Map<String, Object>> pluginConfigs = Map.of("io.hexaglue.plugin.jpa", Map.of());

            TargetModuleValidator.ValidationResult result =
                    TargetModuleValidator.validate(pluginConfigs, KNOWN_MODULES);

            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Invalid configurations")
    class InvalidConfigurations {

        @Test
        @DisplayName("should reject invalid targetModule")
        void shouldRejectInvalidTargetModule() {
            Map<String, Map<String, Object>> pluginConfigs =
                    Map.of("io.hexaglue.plugin.jpa", Map.of("targetModule", "non-existent-module"));

            TargetModuleValidator.ValidationResult result =
                    TargetModuleValidator.validate(pluginConfigs, KNOWN_MODULES);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0)).contains("non-existent-module").contains("io.hexaglue.plugin.jpa");
        }

        @Test
        @DisplayName("should report multiple invalid targetModules")
        void shouldReportMultipleInvalidTargetModules() {
            Map<String, Map<String, Object>> pluginConfigs = Map.of(
                    "io.hexaglue.plugin.jpa", Map.of("targetModule", "bad-module-1"),
                    "io.hexaglue.plugin.living-doc", Map.of("targetModule", "bad-module-2"));

            TargetModuleValidator.ValidationResult result =
                    TargetModuleValidator.validate(pluginConfigs, KNOWN_MODULES);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(2);
        }

        @Test
        @DisplayName("should include known modules in error message")
        void shouldIncludeKnownModulesInErrorMessage() {
            Map<String, Map<String, Object>> pluginConfigs =
                    Map.of("io.hexaglue.plugin.jpa", Map.of("targetModule", "unknown"));

            TargetModuleValidator.ValidationResult result =
                    TargetModuleValidator.validate(pluginConfigs, KNOWN_MODULES);

            assertThat(result.errors().get(0))
                    .contains("banking-core")
                    .contains("banking-persistence")
                    .contains("banking-service");
        }
    }

    @Nested
    @DisplayName("Non-string targetModule values")
    class NonStringValues {

        @Test
        @DisplayName("should ignore non-string targetModule values")
        void shouldIgnoreNonStringTargetModuleValues() {
            Map<String, Map<String, Object>> pluginConfigs =
                    Map.of("io.hexaglue.plugin.jpa", Map.of("targetModule", 42));

            TargetModuleValidator.ValidationResult result =
                    TargetModuleValidator.validate(pluginConfigs, KNOWN_MODULES);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should ignore null targetModule values")
        void shouldIgnoreNullTargetModuleValues() {
            // Create a map that allows null values
            Map<String, Object> config = new java.util.HashMap<>();
            config.put("targetModule", null);
            Map<String, Map<String, Object>> pluginConfigs = Map.of("io.hexaglue.plugin.jpa", config);

            TargetModuleValidator.ValidationResult result =
                    TargetModuleValidator.validate(pluginConfigs, KNOWN_MODULES);

            assertThat(result.isValid()).isTrue();
        }
    }
}
