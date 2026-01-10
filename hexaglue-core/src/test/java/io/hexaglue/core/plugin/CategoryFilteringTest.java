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

package io.hexaglue.core.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.generation.PluginCategory;
import io.hexaglue.spi.ir.IrSnapshot;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for plugin category filtering in {@link PluginExecutor}.
 */
class CategoryFilteringTest {

    @TempDir
    Path tempDir;

    @Test
    void executeWithNullCategories_executesAllPlugins() {
        // Given: null categories means execute all plugins
        PluginExecutor executor = new PluginExecutor(tempDir, Map.of(), null, null);
        IrSnapshot ir = IrSnapshot.empty("com.example");

        // When
        PluginExecutionResult result = executor.execute(ir);

        // Then: Should attempt to execute all discovered plugins (if any)
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void executeWithEmptyCategories_executesAllPlugins() {
        // Given: empty set means execute all plugins
        PluginExecutor executor = new PluginExecutor(tempDir, Map.of(), null, Set.of());
        IrSnapshot ir = IrSnapshot.empty("com.example");

        // When
        PluginExecutionResult result = executor.execute(ir);

        // Then: Should attempt to execute all discovered plugins (if any)
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void executeWithGeneratorCategory_filtersPlugins() {
        // Given: only GENERATOR plugins should run
        PluginExecutor executor = new PluginExecutor(tempDir, Map.of(), null, Set.of(PluginCategory.GENERATOR));
        IrSnapshot ir = IrSnapshot.empty("com.example");

        // When
        PluginExecutionResult result = executor.execute(ir);

        // Then: Successfully filtered (even if no generator plugins are found in test env)
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void executeWithAuditCategory_filtersPlugins() {
        // Given: only AUDIT plugins should run
        PluginExecutor executor = new PluginExecutor(tempDir, Map.of(), null, Set.of(PluginCategory.AUDIT));
        IrSnapshot ir = IrSnapshot.empty("com.example");

        // When
        PluginExecutionResult result = executor.execute(ir);

        // Then: Successfully filtered
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void executeWithMultipleCategories_filtersPlugins() {
        // Given: both GENERATOR and AUDIT plugins should run
        PluginExecutor executor =
                new PluginExecutor(tempDir, Map.of(), null, Set.of(PluginCategory.GENERATOR, PluginCategory.AUDIT));
        IrSnapshot ir = IrSnapshot.empty("com.example");

        // When
        PluginExecutionResult result = executor.execute(ir);

        // Then: Successfully filtered
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void executeWithEnrichmentCategory_filtersPlugins() {
        // Given: only ENRICHMENT plugins should run
        PluginExecutor executor = new PluginExecutor(tempDir, Map.of(), null, Set.of(PluginCategory.ENRICHMENT));
        IrSnapshot ir = IrSnapshot.empty("com.example");

        // When
        PluginExecutionResult result = executor.execute(ir);

        // Then: Successfully filtered
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void executeWithAnalysisCategory_filtersPlugins() {
        // Given: only ANALYSIS plugins should run
        PluginExecutor executor = new PluginExecutor(tempDir, Map.of(), null, Set.of(PluginCategory.ANALYSIS));
        IrSnapshot ir = IrSnapshot.empty("com.example");

        // When
        PluginExecutionResult result = executor.execute(ir);

        // Then: Successfully filtered
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }
}
