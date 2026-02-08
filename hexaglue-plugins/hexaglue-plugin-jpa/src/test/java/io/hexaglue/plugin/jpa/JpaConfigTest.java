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

@DisplayName("JpaConfig")
class JpaConfigTest {

    @Nested
    @DisplayName("defaults()")
    class DefaultsTests {

        @Test
        @DisplayName("should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            JpaConfig config = JpaConfig.defaults();

            assertThat(config.entitySuffix()).isEqualTo("Entity");
            assertThat(config.repositorySuffix()).isEqualTo("JpaRepository");
            assertThat(config.adapterSuffix()).isEqualTo("Adapter");
            assertThat(config.mapperSuffix()).isEqualTo("Mapper");
            assertThat(config.tablePrefix()).isEmpty();
            assertThat(config.enableAuditing()).isFalse();
            assertThat(config.enableOptimisticLocking()).isFalse();
            assertThat(config.generateRepositories()).isTrue();
            assertThat(config.generateMappers()).isTrue();
            assertThat(config.generateAdapters()).isTrue();
            assertThat(config.targetModule()).isNull();
        }
    }

    @Nested
    @DisplayName("from(PluginConfig)")
    class FromPluginConfigTests {

        @Test
        @DisplayName("should use custom entitySuffix")
        void shouldUseCustomEntitySuffix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("entitySuffix", "JpaEntity"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.entitySuffix()).isEqualTo("JpaEntity");
        }

        @Test
        @DisplayName("should use custom repositorySuffix")
        void shouldUseCustomRepositorySuffix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("repositorySuffix", "Repository"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.repositorySuffix()).isEqualTo("Repository");
        }

        @Test
        @DisplayName("should use custom adapterSuffix")
        void shouldUseCustomAdapterSuffix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("adapterSuffix", "RepositoryImpl"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.adapterSuffix()).isEqualTo("RepositoryImpl");
        }

        @Test
        @DisplayName("should use custom mapperSuffix")
        void shouldUseCustomMapperSuffix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("mapperSuffix", "Converter"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.mapperSuffix()).isEqualTo("Converter");
        }

        @Test
        @DisplayName("should use custom tablePrefix")
        void shouldUseCustomTablePrefix() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("tablePrefix", "app_"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.tablePrefix()).isEqualTo("app_");
        }

        @Test
        @DisplayName("should enable auditing when configured")
        void shouldEnableAuditing() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("enableAuditing", true));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.enableAuditing()).isTrue();
        }

        @Test
        @DisplayName("should enable optimistic locking when configured")
        void shouldEnableOptimisticLocking() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("enableOptimisticLocking", true));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.enableOptimisticLocking()).isTrue();
        }

        @Test
        @DisplayName("should disable repository generation when configured")
        void shouldDisableRepositoryGeneration() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("generateRepositories", false));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.generateRepositories()).isFalse();
        }

        @Test
        @DisplayName("should disable mapper generation when configured")
        void shouldDisableMapperGeneration() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("generateMappers", false));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.generateMappers()).isFalse();
        }

        @Test
        @DisplayName("should disable adapter generation when configured")
        void shouldDisableAdapterGeneration() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("generateAdapters", false));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.generateAdapters()).isFalse();
        }

        @Test
        @DisplayName("should use custom targetModule")
        void shouldUseCustomTargetModule() {
            PluginConfig pluginConfig = createPluginConfig(Map.of("targetModule", "banking-persistence"));

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.targetModule()).isEqualTo("banking-persistence");
        }

        @Test
        @DisplayName("should have null targetModule by default")
        void shouldHaveNullTargetModuleByDefault() {
            PluginConfig pluginConfig = createPluginConfig(Map.of());

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.targetModule()).isNull();
        }

        @Test
        @DisplayName("should use all default values when config is empty")
        void shouldUseDefaultsForEmptyConfig() {
            PluginConfig pluginConfig = createPluginConfig(Map.of());

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.entitySuffix()).isEqualTo("Entity");
            assertThat(config.repositorySuffix()).isEqualTo("JpaRepository");
            assertThat(config.adapterSuffix()).isEqualTo("Adapter");
            assertThat(config.mapperSuffix()).isEqualTo("Mapper");
            assertThat(config.tablePrefix()).isEmpty();
            assertThat(config.enableAuditing()).isFalse();
            assertThat(config.enableOptimisticLocking()).isFalse();
            assertThat(config.generateRepositories()).isTrue();
            assertThat(config.generateMappers()).isTrue();
            assertThat(config.generateAdapters()).isTrue();
            assertThat(config.targetModule()).isNull();
        }

        @Test
        @DisplayName("should handle multiple custom values")
        void shouldHandleMultipleCustomValues() {
            Map<String, Object> values = new HashMap<>();
            values.put("entitySuffix", "Jpa");
            values.put("repositorySuffix", "Repo");
            values.put("tablePrefix", "tbl_");
            values.put("enableAuditing", true);
            values.put("enableOptimisticLocking", true);

            PluginConfig pluginConfig = createPluginConfig(values);

            JpaConfig config = JpaConfig.from(pluginConfig);

            assertThat(config.entitySuffix()).isEqualTo("Jpa");
            assertThat(config.repositorySuffix()).isEqualTo("Repo");
            assertThat(config.tablePrefix()).isEqualTo("tbl_");
            assertThat(config.enableAuditing()).isTrue();
            assertThat(config.enableOptimisticLocking()).isTrue();
        }
    }

    private PluginConfig createPluginConfig(Map<String, Object> values) {
        return new PluginConfig() {
            @Override
            public Optional<String> getString(String key) {
                Object value = values.get(key);
                return value != null ? Optional.of(value.toString()) : Optional.empty();
            }

            @Override
            public Optional<Boolean> getBoolean(String key) {
                Object value = values.get(key);
                if (value instanceof Boolean) {
                    return Optional.of((Boolean) value);
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
