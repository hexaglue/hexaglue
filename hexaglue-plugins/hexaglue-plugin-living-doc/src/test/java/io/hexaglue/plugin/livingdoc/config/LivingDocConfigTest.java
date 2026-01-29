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

package io.hexaglue.plugin.livingdoc.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.plugin.PluginConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LivingDocConfig}.
 *
 * @since 5.0.0
 */
@DisplayName("LivingDocConfig")
class LivingDocConfigTest {

    @Nested
    @DisplayName("defaults()")
    class Defaults {

        @Test
        @DisplayName("should provide default output directory")
        void shouldProvideDefaultOutputDir() {
            LivingDocConfig config = LivingDocConfig.defaults();
            assertThat(config.outputDir()).isEqualTo("living-doc");
        }

        @Test
        @DisplayName("should enable diagrams by default")
        void shouldEnableDiagramsByDefault() {
            LivingDocConfig config = LivingDocConfig.defaults();
            assertThat(config.generateDiagrams()).isTrue();
        }

        @Test
        @DisplayName("should set max properties to 5 by default")
        void shouldSetMaxPropertiesToFiveByDefault() {
            LivingDocConfig config = LivingDocConfig.defaults();
            assertThat(config.maxPropertiesInDiagram()).isEqualTo(5);
        }

        @Test
        @DisplayName("should enable debug sections by default")
        void shouldEnableDebugSectionsByDefault() {
            LivingDocConfig config = LivingDocConfig.defaults();
            assertThat(config.includeDebugSections()).isTrue();
        }
    }

    @Nested
    @DisplayName("from(PluginConfig)")
    class FromPluginConfig {

        @Test
        @DisplayName("should use defaults when plugin config is empty")
        void shouldUseDefaultsWhenEmpty() {
            PluginConfig pluginConfig = emptyConfig();

            LivingDocConfig config = LivingDocConfig.from(pluginConfig);

            assertThat(config.outputDir()).isEqualTo("living-doc");
            assertThat(config.generateDiagrams()).isTrue();
            assertThat(config.maxPropertiesInDiagram()).isEqualTo(5);
            assertThat(config.includeDebugSections()).isTrue();
        }

        @Test
        @DisplayName("should read outputDir from plugin config")
        void shouldReadOutputDir() {
            PluginConfig pluginConfig = configWith("outputDir", "custom-docs");

            LivingDocConfig config = LivingDocConfig.from(pluginConfig);

            assertThat(config.outputDir()).isEqualTo("custom-docs");
        }

        @Test
        @DisplayName("should read generateDiagrams from plugin config")
        void shouldReadGenerateDiagrams() {
            PluginConfig pluginConfig = configWithBoolean("generateDiagrams", false);

            LivingDocConfig config = LivingDocConfig.from(pluginConfig);

            assertThat(config.generateDiagrams()).isFalse();
        }

        @Test
        @DisplayName("should read maxPropertiesInDiagram from plugin config")
        void shouldReadMaxProperties() {
            PluginConfig pluginConfig = configWithInteger("maxPropertiesInDiagram", 10);

            LivingDocConfig config = LivingDocConfig.from(pluginConfig);

            assertThat(config.maxPropertiesInDiagram()).isEqualTo(10);
        }

        @Test
        @DisplayName("should read includeDebugSections from plugin config")
        void shouldReadIncludeDebugSections() {
            PluginConfig pluginConfig = configWithBoolean("includeDebugSections", false);

            LivingDocConfig config = LivingDocConfig.from(pluginConfig);

            assertThat(config.includeDebugSections()).isFalse();
        }
    }

    // --- Test helpers ---

    private static PluginConfig emptyConfig() {
        return new MapPluginConfig(Map.of());
    }

    private static PluginConfig configWith(String key, String value) {
        return new MapPluginConfig(Map.of(key, value));
    }

    private static PluginConfig configWithBoolean(String key, boolean value) {
        return new MapPluginConfig(Map.of(key, value));
    }

    private static PluginConfig configWithInteger(String key, int value) {
        return new MapPluginConfig(Map.of(key, value));
    }

    /**
     * Simple map-based PluginConfig for testing.
     */
    private record MapPluginConfig(Map<String, Object> values) implements PluginConfig {

        MapPluginConfig(Map<String, Object> values) {
            this.values = new HashMap<>(values);
        }

        @Override
        public Optional<String> getString(String key) {
            Object value = values.get(key);
            return value instanceof String s ? Optional.of(s) : Optional.empty();
        }

        @Override
        public Optional<Boolean> getBoolean(String key) {
            Object value = values.get(key);
            return value instanceof Boolean b ? Optional.of(b) : Optional.empty();
        }

        @Override
        public Optional<Integer> getInteger(String key) {
            Object value = values.get(key);
            return value instanceof Integer i ? Optional.of(i) : Optional.empty();
        }
    }
}
