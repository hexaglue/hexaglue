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

package io.hexaglue.spi.plugin;

import io.hexaglue.spi.generation.PluginCategory;
import java.util.List;

/**
 * Service Provider Interface for HexaGlue plugins.
 *
 * <p>Plugins are discovered via {@link java.util.ServiceLoader} and executed
 * after the analysis phase completes. Each plugin receives an immutable
 * {@link PluginContext} containing the analyzed application model.
 *
 * <p>Plugins can declare dependencies on other plugins using {@link #dependsOn()}.
 * The plugin executor ensures that dependencies are executed first.
 *
 * <p>Registration: Create {@code META-INF/services/io.hexaglue.spi.plugin.HexaGluePlugin}
 * containing the fully-qualified class name of your implementation.
 *
 * @since 1.0.0
 */
public interface HexaGluePlugin {

    /**
     * Unique identifier for this plugin.
     *
     * <p>Convention: use reverse domain notation, e.g., {@code io.hexaglue.plugin.jpa}
     *
     * @return the plugin identifier
     */
    String id();

    /**
     * Returns the list of plugin IDs this plugin depends on.
     *
     * <p><strong>Contract:</strong>
     * <ul>
     *   <li>Dependencies are executed <em>before</em> this plugin</li>
     *   <li>If a dependency is missing, execution fails with
     *       {@code PluginDependencyException}</li>
     *   <li>If a dependency fails, this plugin is <em>skipped</em></li>
     *   <li>Circular dependencies cause {@code PluginCyclicDependencyException}</li>
     * </ul>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * @Override
     * public List<String> dependsOn() {
     *     return List.of("io.hexaglue.plugin.jpa");
     * }
     * }</pre>
     *
     * @return plugin IDs this plugin depends on; empty list if no dependencies
     * @since 3.0.0
     */
    default List<String> dependsOn() {
        return List.of();
    }

    /**
     * Returns the category of this plugin.
     *
     * <p><strong>Contract:</strong>
     * <ul>
     *   <li>Used for targeted execution (e.g., "run only generators")</li>
     *   <li>Configurable via {@code EngineConfig.enabledCategories()}</li>
     *   <li>Plugins of disabled categories are silently skipped</li>
     * </ul>
     *
     * <p>Categories:
     * <ul>
     *   <li>{@link PluginCategory#GENERATOR} - Generates code artifacts</li>
     *   <li>{@link PluginCategory#AUDIT} - Validates architecture compliance</li>
     *   <li>{@link PluginCategory#ENRICHMENT} - Adds semantic labels</li>
     *   <li>{@link PluginCategory#ANALYSIS} - Custom analysis without code generation</li>
     * </ul>
     *
     * @return plugin category; defaults to {@link PluginCategory#GENERATOR}
     * @since 3.0.0
     */
    default PluginCategory category() {
        return PluginCategory.GENERATOR;
    }

    /**
     * Executes the plugin with the given context.
     *
     * <p>Plugins typically:
     * <ol>
     *   <li>Read the IR from {@link PluginContext#ir()}</li>
     *   <li>Read configuration from {@link PluginContext#config()}</li>
     *   <li>Generate code using {@link PluginContext#writer()}</li>
     *   <li>Report issues via {@link PluginContext#diagnostics()}</li>
     * </ol>
     *
     * @param context the execution context (never null)
     */
    void execute(PluginContext context);
}
