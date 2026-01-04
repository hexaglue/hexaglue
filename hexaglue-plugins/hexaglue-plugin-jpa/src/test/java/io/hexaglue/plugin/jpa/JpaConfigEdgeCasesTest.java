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

package io.hexaglue.plugin.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.plugin.PluginConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Edge case tests for {@link JpaConfig}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Invalid or edge case suffix values</li>
 *   <li>Table prefix edge cases</li>
 *   <li>Boolean string parsing</li>
 *   <li>Unknown configuration options</li>
 * </ul>
 */
@DisplayName("JpaConfig - Edge Cases")
class JpaConfigEdgeCasesTest {

    // =========================================================================
    // Suffix Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Suffix Edge Cases")
    class SuffixEdgeCasesTests {

        @Test
        @DisplayName("should accept empty suffix (no suffix)")
        void shouldAcceptEmptySuffix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("entitySuffix", ""));

            JpaConfig config = JpaConfig.from(pluginConfig);

            // Empty suffix is valid - means no suffix added
            assertThat(config.entitySuffix()).isEmpty();
        }

        @Test
        @DisplayName("should accept single character suffix")
        void shouldAcceptSingleCharSuffix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("entitySuffix", "E"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.entitySuffix()).isEqualTo("E");
        }

        @Test
        @DisplayName("should preserve whitespace in suffix")
        void shouldPreserveWhitespaceInSuffix() {
            // Whitespace should be preserved as-is (user responsibility)
            PluginConfig pluginConfig = createPluginConfig(Map.of("entitySuffix", "Entity "));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.entitySuffix()).isEqualTo("Entity ");
        }

        @Test
        @DisplayName("should handle suffix with numbers")
        void shouldHandleSuffixWithNumbers() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("entitySuffix", "EntityV2"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.entitySuffix()).isEqualTo("EntityV2");
        }

        @Test
        @DisplayName("should handle suffix with underscores")
        void shouldHandleSuffixWithUnderscores() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("entitySuffix", "Jpa_Entity"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.entitySuffix()).isEqualTo("Jpa_Entity");
        }
    }

    // =========================================================================
    // Table Prefix Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Table Prefix Edge Cases")
    class TablePrefixEdgeCasesTests {

        @Test
        @DisplayName("should handle underscore-only prefix")
        void shouldHandleUnderscoreOnlyPrefix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("tablePrefix", "_"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.tablePrefix()).isEqualTo("_");
        }

        @Test
        @DisplayName("should handle uppercase prefix")
        void shouldHandleUppercasePrefix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("tablePrefix", "APP_"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.tablePrefix()).isEqualTo("APP_");
        }

        @Test
        @DisplayName("should handle schema-qualified prefix")
        void shouldHandleSchemaQualifiedPrefix() {
            // Some databases support schema.table format
            PluginConfig pluginConfig = createPluginConfig(Map.of("tablePrefix", "myschema."));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.tablePrefix()).isEqualTo("myschema.");
        }

        @Test
        @DisplayName("should handle numeric prefix")
        void shouldHandleNumericPrefix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("tablePrefix", "t1_"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.tablePrefix()).isEqualTo("t1_");
        }
    }

    // =========================================================================
    // Boolean String Parsing
    // =========================================================================

    @Nested
    @DisplayName("Boolean String Parsing")
    class BooleanStringParsingTests {

        @Test
        @DisplayName("should parse 'true' string as boolean true")
        void shouldParseTrueStringAsBoolean() {
            // String "true" should be converted to boolean true
            Map<String, Object> values = new HashMap<>();
            values.put("enableAuditing", "true");

            PluginConfig pluginConfig = createPluginConfig(values);

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.enableAuditing()).isTrue();
        }

        @Test
        @DisplayName("should parse 'false' string as boolean false")
        void shouldParseFalseStringAsBoolean() {
            Map<String, Object> values = new HashMap<>();
            values.put("enableAuditing", "false");

            PluginConfig pluginConfig = createPluginConfig(values);

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.enableAuditing()).isFalse();
        }

        @Test
        @DisplayName("should handle Boolean.TRUE object")
        void shouldHandleBooleanTrueObject() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("enableAuditing", Boolean.TRUE));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.enableAuditing()).isTrue();
        }
    }

    // =========================================================================
    // Unknown Options
    // =========================================================================

    @Nested
    @DisplayName("Unknown Options")
    class UnknownOptionsTests {

        @Test
        @DisplayName("should ignore unknown options without error")
        void shouldIgnoreUnknownOptions() {
            Map<String, Object> values = new HashMap<>();
            values.put("entitySuffix", "Entity");
            values.put("unknownOption", "someValue");
            values.put("anotherUnknown", 42);

            PluginConfig pluginConfig = createPluginConfig(values);

            // Should not throw, should just ignore unknown options
            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.entitySuffix()).isEqualTo("Entity");
        }

        @Test
        @DisplayName("should ignore null values")
        void shouldIgnoreNullValues() {
            Map<String, Object> values = new HashMap<>();
            values.put("entitySuffix", null);
            values.put("enableAuditing", null);

            PluginConfig pluginConfig = createPluginConfig(values);

            JpaConfig config = JpaConfig.from(pluginConfig);

            // Should use defaults when value is null
            assertThat(config.entitySuffix()).isEqualTo("Entity");
            assertThat(config.enableAuditing()).isFalse();
        }
    }

    // =========================================================================
    // Combination Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Combination Edge Cases")
    class CombinationEdgeCasesTests {

        @Test
        @DisplayName("should handle all generation flags disabled")
        void shouldHandleAllGenerationFlagsDisabled() {
            Map<String, Object> values = new HashMap<>();
            values.put("generateRepositories", false);
            values.put("generateMappers", false);
            values.put("generateAdapters", false);

            PluginConfig pluginConfig = createPluginConfig(values);

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.generateRepositories()).isFalse();
            assertThat(config.generateMappers()).isFalse();
            assertThat(config.generateAdapters()).isFalse();
        }

        @Test
        @DisplayName("should handle auditing without optimistic locking")
        void shouldHandleAuditingWithoutOptimisticLocking() {
            Map<String, Object> values = new HashMap<>();
            values.put("enableAuditing", true);
            values.put("enableOptimisticLocking", false);

            PluginConfig pluginConfig = createPluginConfig(values);

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.enableAuditing()).isTrue();
            assertThat(config.enableOptimisticLocking()).isFalse();
        }

        @Test
        @DisplayName("should handle minimal valid configuration")
        void shouldHandleMinimalValidConfiguration() {
            // Just the required options (if any)
            PluginConfig pluginConfig = createPluginConfig(Map.of());

            JpaConfig config = JpaConfig.from(pluginConfig);

            // All defaults should work
            assertThat(config).isNotNull();
            assertThat(config.entitySuffix()).isNotNull();
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private PluginConfig createPluginConfig(Map<String, Object> values) {
        return new PluginConfig() {
            @Override
            public Optional<String> getString(String key) {
                Object value = values.get(key);
                if (value == null) {
                    return Optional.empty();
                }
                return Optional.of(value.toString());
            }

            @Override
            public Optional<Boolean> getBoolean(String key) {
                Object value = values.get(key);
                if (value instanceof Boolean) {
                    return Optional.of((Boolean) value);
                }
                if (value instanceof String str) {
                    if ("true".equalsIgnoreCase(str)) {
                        return Optional.of(true);
                    }
                    if ("false".equalsIgnoreCase(str)) {
                        return Optional.of(false);
                    }
                }
                return Optional.empty();
            }

            @Override
            public Optional<Integer> getInteger(String key) {
                Object value = values.get(key);
                if (value instanceof Integer) {
                    return Optional.of((Integer) value);
                }
                return Optional.empty();
            }
        };
    }
}
