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

package io.hexaglue.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.core.ClassificationConfig;
import io.hexaglue.spi.generation.PluginCategory;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link EngineConfig} category filtering feature.
 */
class EngineConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void minimalConfig_hasNullCategories() {
        // Given
        EngineConfig config = EngineConfig.minimal(tempDir, "com.example");

        // Then: minimal config has no category filter
        assertThat(config.enabledCategories()).isNull();
    }

    @Test
    void withPluginsConfig_hasNullCategories() {
        // Given
        Path outputDir = tempDir.resolve("output");
        EngineConfig config = EngineConfig.withPlugins(tempDir, "com.example", outputDir, java.util.Map.of());

        // Then: withPlugins config has no category filter
        assertThat(config.enabledCategories()).isNull();
    }

    @Test
    void withClassificationConfig_hasNullCategories() {
        // Given
        ClassificationConfig classificationConfig =
                ClassificationConfig.builder().failOnUnclassified().build();
        EngineConfig config = EngineConfig.withClassificationConfig(tempDir, "com.example", classificationConfig);

        // Then: withClassificationConfig has no category filter
        assertThat(config.enabledCategories()).isNull();
        assertThat(config.classificationConfig()).isEqualTo(classificationConfig);
    }

    @Test
    void onlyGenerators_returnsConfigWithGeneratorCategory() {
        // Given
        EngineConfig baseConfig = EngineConfig.minimal(tempDir, "com.example");

        // When
        EngineConfig filtered = baseConfig.onlyGenerators();

        // Then: filtered config contains only GENERATOR category
        assertThat(filtered.enabledCategories()).isNotNull().containsExactly(PluginCategory.GENERATOR);

        // And: other properties are preserved
        assertThat(filtered.basePackage()).isEqualTo("com.example");
        assertThat(filtered.sourceRoots()).isEqualTo(baseConfig.sourceRoots());
    }

    @Test
    void onlyAuditors_returnsConfigWithAuditCategory() {
        // Given
        EngineConfig baseConfig = EngineConfig.minimal(tempDir, "com.example");

        // When
        EngineConfig filtered = baseConfig.onlyAuditors();

        // Then: filtered config contains only AUDIT category
        assertThat(filtered.enabledCategories()).isNotNull().containsExactly(PluginCategory.AUDIT);
    }

    @Test
    void allCategories_returnsConfigWithNullCategories() {
        // Given: start with a filtered config
        EngineConfig filtered = EngineConfig.minimal(tempDir, "com.example").onlyGenerators();

        // When: reset to all categories
        EngineConfig reset = filtered.allCategories();

        // Then: enabledCategories is null (all categories enabled)
        assertThat(reset.enabledCategories()).isNull();
    }

    @Test
    void enabledCategories_isImmutable() {
        // Given
        Set<PluginCategory> mutableSet = new java.util.HashSet<>();
        mutableSet.add(PluginCategory.GENERATOR);

        EngineConfig config = new EngineConfig(
                java.util.List.of(tempDir),
                java.util.List.of(),
                21,
                "com.example",
                null, // projectName
                null, // projectVersion
                null, // outputDirectory
                null,
                java.util.Map.of(),
                java.util.Map.of(),
                null,
                mutableSet,
                false,
                java.util.List.of(),
                false);

        // When: try to modify the original set
        mutableSet.add(PluginCategory.AUDIT);

        // Then: config's categories are unchanged
        assertThat(config.enabledCategories()).containsExactly(PluginCategory.GENERATOR);
    }

    @Test
    void onlyGenerators_chainingPreservesOtherProperties() {
        // Given: config with specific classification config and output directory
        Path outputDir = tempDir.resolve("output");
        ClassificationConfig classificationConfig =
                ClassificationConfig.builder().failOnUnclassified().build();
        EngineConfig config = new EngineConfig(
                java.util.List.of(tempDir),
                java.util.List.of(),
                21,
                "com.example",
                "Test Project", // projectName
                "1.0.0", // projectVersion
                outputDir,
                null,
                java.util.Map.of("plugin1", java.util.Map.of("key", "value")),
                java.util.Map.of("option", "value"),
                classificationConfig,
                null,
                false,
                java.util.List.of(),
                false);

        // When: apply category filter
        EngineConfig filtered = config.onlyGenerators();

        // Then: all properties preserved except categories
        assertThat(filtered.sourceRoots()).isEqualTo(config.sourceRoots());
        assertThat(filtered.classpathEntries()).isEqualTo(config.classpathEntries());
        assertThat(filtered.javaVersion()).isEqualTo(config.javaVersion());
        assertThat(filtered.basePackage()).isEqualTo(config.basePackage());
        assertThat(filtered.projectName()).isEqualTo(config.projectName());
        assertThat(filtered.projectVersion()).isEqualTo(config.projectVersion());
        assertThat(filtered.outputDirectory()).isEqualTo(config.outputDirectory());
        assertThat(filtered.pluginConfigs()).isEqualTo(config.pluginConfigs());
        assertThat(filtered.options()).isEqualTo(config.options());
        assertThat(filtered.classificationConfig()).isEqualTo(config.classificationConfig());
        assertThat(filtered.enabledCategories()).containsExactly(PluginCategory.GENERATOR);
        assertThat(filtered.includeGenerated()).isEqualTo(config.includeGenerated());
    }

    @Test
    void minimalConfig_hasIncludeGeneratedFalse() {
        // Given
        EngineConfig config = EngineConfig.minimal(tempDir, "com.example");

        // Then
        assertThat(config.includeGenerated()).isFalse();
    }

    @Test
    void withGeneratedTypes_returnsConfigWithIncludeGeneratedTrue() {
        // Given
        EngineConfig baseConfig = EngineConfig.minimal(tempDir, "com.example");
        assertThat(baseConfig.includeGenerated()).isFalse();

        // When
        EngineConfig withGenerated = baseConfig.withGeneratedTypes();

        // Then
        assertThat(withGenerated.includeGenerated()).isTrue();
        assertThat(withGenerated.basePackage()).isEqualTo("com.example");
        assertThat(withGenerated.sourceRoots()).isEqualTo(baseConfig.sourceRoots());
    }

    @Test
    void onlyGenerators_preservesIncludeGenerated() {
        // Given: config with includeGenerated=true
        EngineConfig baseConfig = EngineConfig.minimal(tempDir, "com.example").withGeneratedTypes();
        assertThat(baseConfig.includeGenerated()).isTrue();

        // When
        EngineConfig filtered = baseConfig.onlyGenerators();

        // Then: includeGenerated is preserved
        assertThat(filtered.includeGenerated()).isTrue();
        assertThat(filtered.enabledCategories()).containsExactly(PluginCategory.GENERATOR);
    }

    @Test
    void onlyAuditors_preservesIncludeGenerated() {
        // Given: config with includeGenerated=true
        EngineConfig baseConfig = EngineConfig.minimal(tempDir, "com.example").withGeneratedTypes();

        // When
        EngineConfig filtered = baseConfig.onlyAuditors();

        // Then
        assertThat(filtered.includeGenerated()).isTrue();
        assertThat(filtered.enabledCategories()).containsExactly(PluginCategory.AUDIT);
    }

    @Nested
    @DisplayName("multi-module support")
    class MultiModuleSupport {

        @Test
        @DisplayName("isMultiModule should return false when moduleSourceSets is empty")
        void isMultiModuleShouldReturnFalseWhenEmpty() {
            EngineConfig config = EngineConfig.minimal(tempDir, "com.example");

            assertThat(config.isMultiModule()).isFalse();
            assertThat(config.moduleSourceSets()).isEmpty();
        }

        @Test
        @DisplayName("isMultiModule should return true when moduleSourceSets is non-empty")
        void isMultiModuleShouldReturnTrueWhenNonEmpty() {
            Path moduleBase = tempDir;
            io.hexaglue.core.engine.ModuleSourceSet module = new io.hexaglue.core.engine.ModuleSourceSet(
                    "core",
                    io.hexaglue.arch.model.index.ModuleRole.DOMAIN,
                    java.util.List.of(tempDir),
                    java.util.List.of(),
                    tempDir.resolve("output"),
                    moduleBase);

            EngineConfig config = new EngineConfig(
                    java.util.List.of(tempDir),
                    java.util.List.of(),
                    21,
                    "com.example",
                    null,
                    null,
                    tempDir.resolve("output"),
                    null,
                    java.util.Map.of(),
                    java.util.Map.of(),
                    null,
                    null,
                    false,
                    java.util.List.of(module),
                    false);

            assertThat(config.isMultiModule()).isTrue();
            assertThat(config.moduleSourceSets()).hasSize(1);
        }

        @Test
        @DisplayName("moduleSourceSets should default to empty list when null")
        void moduleSourceSetsShouldDefaultToEmptyListWhenNull() {
            EngineConfig config = new EngineConfig(
                    java.util.List.of(tempDir),
                    java.util.List.of(),
                    21,
                    "com.example",
                    null,
                    null,
                    null,
                    null,
                    java.util.Map.of(),
                    java.util.Map.of(),
                    null,
                    null,
                    false,
                    null, // null moduleSourceSets
                    false);

            assertThat(config.moduleSourceSets()).isNotNull().isEmpty();
            assertThat(config.isMultiModule()).isFalse();
        }
    }

    @Nested
    @DisplayName("pluginsEnabled")
    class PluginsEnabled {

        @Test
        @DisplayName("should return true when outputDirectory is set")
        void shouldReturnTrueWhenOutputDirectoryIsSet() {
            EngineConfig config = new EngineConfig(
                    java.util.List.of(tempDir),
                    java.util.List.of(),
                    21,
                    "com.example",
                    null,
                    null,
                    tempDir.resolve("output"),
                    null,
                    java.util.Map.of(),
                    java.util.Map.of(),
                    null,
                    null,
                    false,
                    java.util.List.of(),
                    false);

            assertThat(config.pluginsEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return true when only reportsOutputDirectory is set (audit-only)")
        void shouldReturnTrueWhenOnlyReportsOutputDirectoryIsSet() {
            EngineConfig config = new EngineConfig(
                    java.util.List.of(tempDir),
                    java.util.List.of(),
                    21,
                    "com.example",
                    null,
                    null,
                    null,
                    tempDir.resolve("reports"),
                    java.util.Map.of(),
                    java.util.Map.of(),
                    null,
                    null,
                    false,
                    java.util.List.of(),
                    false);

            assertThat(config.pluginsEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return false when both output directories are null")
        void shouldReturnFalseWhenBothOutputDirectoriesAreNull() {
            EngineConfig config = new EngineConfig(
                    java.util.List.of(tempDir),
                    java.util.List.of(),
                    21,
                    "com.example",
                    null,
                    null,
                    null,
                    null,
                    java.util.Map.of(),
                    java.util.Map.of(),
                    null,
                    null,
                    false,
                    java.util.List.of(),
                    false);

            assertThat(config.pluginsEnabled()).isFalse();
        }
    }
}
