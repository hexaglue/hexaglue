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

import io.hexaglue.spi.generation.PluginCategory;
import java.nio.file.Path;
import java.util.Set;
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
    void withProfileConfig_hasNullCategories() {
        // Given
        EngineConfig config = EngineConfig.withProfile(tempDir, "com.example", "default");

        // Then: withProfile config has no category filter
        assertThat(config.enabledCategories()).isNull();
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
                java.util.Map.of(),
                java.util.Map.of(),
                null,
                mutableSet);

        // When: try to modify the original set
        mutableSet.add(PluginCategory.AUDIT);

        // Then: config's categories are unchanged
        assertThat(config.enabledCategories()).containsExactly(PluginCategory.GENERATOR);
    }

    @Test
    void onlyGenerators_chainingPreservesOtherProperties() {
        // Given: config with specific profile and output directory
        Path outputDir = tempDir.resolve("output");
        EngineConfig config = new EngineConfig(
                java.util.List.of(tempDir),
                java.util.List.of(),
                21,
                "com.example",
                "Test Project", // projectName
                "1.0.0",        // projectVersion
                outputDir,
                java.util.Map.of("plugin1", java.util.Map.of("key", "value")),
                java.util.Map.of("option", "value"),
                "strict",
                null);

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
        assertThat(filtered.classificationProfile()).isEqualTo(config.classificationProfile());
        assertThat(filtered.enabledCategories()).containsExactly(PluginCategory.GENERATOR);
    }
}
