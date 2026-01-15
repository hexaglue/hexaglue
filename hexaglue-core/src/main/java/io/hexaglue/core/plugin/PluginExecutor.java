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

import io.hexaglue.core.audit.DefaultArchitectureQuery;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.generation.PluginCategory;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers and executes HexaGlue plugins.
 *
 * <p>Plugins are discovered via {@link ServiceLoader} and executed in dependency order
 * using topological sorting. Plugins declaring dependencies via {@link
 * HexaGluePlugin#dependsOn()} will be executed after their dependencies.
 *
 * <p>If a plugin's dependency fails, the dependent plugin will be skipped.
 *
 * <p>Each plugin receives a {@link PluginContext} with the analyzed IR and
 * facilities for code generation.
 */
public final class PluginExecutor {

    private static final Logger log = LoggerFactory.getLogger(PluginExecutor.class);

    private final Path outputDirectory;
    private final Map<String, Map<String, Object>> pluginConfigs;
    private final ApplicationGraph graph;
    private final Set<PluginCategory> enabledCategories;

    /**
     * Creates a plugin executor.
     *
     * @param outputDirectory the directory for generated sources
     * @param pluginConfigs plugin configurations keyed by plugin ID
     * @param graph the application graph for architecture analysis (may be null)
     * @param enabledCategories plugin categories to execute (null or empty for all categories)
     */
    public PluginExecutor(
            Path outputDirectory,
            Map<String, Map<String, Object>> pluginConfigs,
            ApplicationGraph graph,
            Set<PluginCategory> enabledCategories) {
        this.outputDirectory = outputDirectory;
        this.pluginConfigs = pluginConfigs;
        this.graph = graph;
        this.enabledCategories = enabledCategories;
    }

    /**
     * Discovers and executes all plugins in dependency order.
     *
     * <p>Plugins are sorted topologically based on their declared dependencies.
     * If a plugin fails, any plugins depending on it will be skipped.
     *
     * @param ir the analyzed IR
     * @return the execution result
     * @throws PluginDependencyException if a plugin depends on an unknown plugin
     * @throws PluginCyclicDependencyException if cyclic dependencies are detected
     */
    public PluginExecutionResult execute(IrSnapshot ir) {
        return execute(ir, List.of());
    }

    /**
     * Discovers and executes all plugins in dependency order with primary classifications.
     *
     * <p>Primary classifications are made available to plugins via the PluginOutputStore,
     * enabling enrichment and secondary classification plugins to access the classification
     * results from the primary classifier.
     *
     * <p>Plugins are sorted topologically based on their declared dependencies.
     * If a plugin fails, any plugins depending on it will be skipped.
     *
     * @param ir the analyzed IR
     * @param primaryClassifications the primary classification results from the engine
     * @return the execution result
     * @throws PluginDependencyException if a plugin depends on an unknown plugin
     * @throws PluginCyclicDependencyException if cyclic dependencies are detected
     * @since 3.0.0
     */
    public PluginExecutionResult execute(
            IrSnapshot ir, List<io.hexaglue.spi.classification.PrimaryClassificationResult> primaryClassifications) {
        List<HexaGluePlugin> plugins = discoverPlugins();
        log.info("Discovered {} plugins", plugins.size());

        if (plugins.isEmpty()) {
            return PluginExecutionResult.empty();
        }

        // Filter plugins by enabled categories
        List<HexaGluePlugin> filteredPlugins = plugins;
        if (enabledCategories != null && !enabledCategories.isEmpty()) {
            filteredPlugins = plugins.stream()
                    .filter(p -> enabledCategories.contains(p.category()))
                    .toList();
            log.info(
                    "Filtered to {} plugins matching enabled categories: {}",
                    filteredPlugins.size(),
                    enabledCategories);
        }

        if (filteredPlugins.isEmpty()) {
            log.warn("No plugins match the enabled categories");
            return PluginExecutionResult.empty();
        }

        // Sort plugins by dependencies using topological sort
        List<HexaGluePlugin> sortedPlugins = sortByDependencies(filteredPlugins);
        log.debug(
                "Plugin execution order: {}",
                sortedPlugins.stream().map(HexaGluePlugin::id).toList());

        // Shared output store for plugin communication
        PluginOutputStore outputStore = new PluginOutputStore();

        // Pre-populate the store with primary classifications for enrichment/secondary classifiers
        if (!primaryClassifications.isEmpty()) {
            outputStore.set("io.hexaglue.engine", "primary-classifications", primaryClassifications);
            log.debug("Added {} primary classifications to plugin output store", primaryClassifications.size());
        }

        List<PluginResult> results = new ArrayList<>();
        Set<String> failedPlugins = new HashSet<>();

        for (HexaGluePlugin plugin : sortedPlugins) {
            // Skip if any dependency failed
            boolean dependencyFailed = plugin.dependsOn().stream().anyMatch(failedPlugins::contains);

            if (dependencyFailed) {
                log.warn("Skipping plugin {} because a dependency failed", plugin.id());
                results.add(PluginResult.skipped(plugin.id(), "Dependency failed, plugin skipped"));
                failedPlugins.add(plugin.id());
                continue;
            }

            PluginResult result = executePlugin(plugin, ir, outputStore, graph);
            results.add(result);

            if (!result.success()) {
                failedPlugins.add(plugin.id());
            }
        }

        return new PluginExecutionResult(results);
    }

    private List<HexaGluePlugin> discoverPlugins() {
        List<HexaGluePlugin> plugins = new ArrayList<>();
        ServiceLoader<HexaGluePlugin> loader = ServiceLoader.load(HexaGluePlugin.class);

        for (HexaGluePlugin plugin : loader) {
            plugins.add(plugin);
            log.debug("Discovered plugin: {}", plugin.id());
        }

        return plugins;
    }

    /**
     * Sorts plugins by dependencies using Kahn's topological sort algorithm.
     *
     * <p>This ensures that plugins are executed after all their dependencies.
     *
     * @param plugins the plugins to sort
     * @return the sorted plugin list
     * @throws PluginDependencyException if a plugin depends on an unknown plugin
     * @throws PluginCyclicDependencyException if cyclic dependencies are detected
     */
    private List<HexaGluePlugin> sortByDependencies(List<HexaGluePlugin> plugins) {
        // Build plugin lookup map
        Map<String, HexaGluePlugin> byId = plugins.stream().collect(Collectors.toMap(HexaGluePlugin::id, p -> p));

        // Validate that all dependencies exist
        for (HexaGluePlugin plugin : plugins) {
            for (String depId : plugin.dependsOn()) {
                if (!byId.containsKey(depId)) {
                    throw new PluginDependencyException(
                            "Plugin '%s' depends on unknown plugin '%s'".formatted(plugin.id(), depId));
                }
            }
        }

        // Build dependency graph: inEdges and outEdges
        Map<String, Set<String>> inEdges = new HashMap<>();
        Map<String, Set<String>> outEdges = new HashMap<>();

        for (HexaGluePlugin plugin : plugins) {
            String id = plugin.id();
            inEdges.putIfAbsent(id, new HashSet<>());
            outEdges.putIfAbsent(id, new HashSet<>());

            for (String depId : plugin.dependsOn()) {
                outEdges.get(depId).add(id);
                inEdges.get(id).add(depId);
            }
        }

        // Kahn's algorithm: collect nodes with no incoming edges
        Queue<String> ready = new LinkedList<>();
        for (String id : byId.keySet()) {
            if (inEdges.get(id).isEmpty()) {
                ready.add(id);
            }
        }

        // Process nodes in topological order
        List<HexaGluePlugin> sorted = new ArrayList<>();
        while (!ready.isEmpty()) {
            String current = ready.poll();
            sorted.add(byId.get(current));

            // Remove edges from current to its dependents
            for (String dependent : outEdges.get(current)) {
                inEdges.get(dependent).remove(current);
                if (inEdges.get(dependent).isEmpty()) {
                    ready.add(dependent);
                }
            }
        }

        // Detect cycles: if not all nodes were processed, there's a cycle
        if (sorted.size() != plugins.size()) {
            throw new PluginCyclicDependencyException("Cyclic dependency detected among plugins");
        }

        return sorted;
    }

    private PluginResult executePlugin(
            HexaGluePlugin plugin, IrSnapshot ir, PluginOutputStore outputStore, ApplicationGraph graph) {
        String pluginId = plugin.id();
        log.info("Executing plugin: {}", pluginId);

        CollectingDiagnosticReporter diagnostics = new CollectingDiagnosticReporter(pluginId);
        FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDirectory);
        MapPluginConfig config = new MapPluginConfig(pluginConfigs.getOrDefault(pluginId, Map.of()));

        // Create ArchitectureQuery from graph if available, with port and domain information
        ArchitectureQuery architectureQuery = graph != null
                ? new DefaultArchitectureQuery(graph, ir != null ? ir.ports() : null, ir != null ? ir.domain() : null)
                : null;

        PluginContext context =
                new DefaultPluginContext(pluginId, ir, config, writer, diagnostics, outputStore, architectureQuery);

        try {
            long start = System.currentTimeMillis();
            plugin.execute(context);
            long elapsed = System.currentTimeMillis() - start;

            log.info(
                    "Plugin {} completed in {}ms, generated {} files",
                    pluginId,
                    elapsed,
                    writer.getGeneratedFiles().size());

            // Capture plugin outputs for retrieval by the engine/mojos
            Map<String, Object> pluginOutputs = outputStore.getAll(pluginId);

            return new PluginResult(
                    pluginId,
                    true,
                    writer.getGeneratedFiles(),
                    diagnostics.getDiagnostics(),
                    elapsed,
                    null,
                    pluginOutputs);

        } catch (Exception e) {
            log.error("Plugin {} failed: {}", pluginId, e.getMessage(), e);
            diagnostics.error("Plugin execution failed: " + e.getMessage(), e);

            // Capture outputs even on failure (partial outputs may exist)
            Map<String, Object> pluginOutputs = outputStore.getAll(pluginId);

            return new PluginResult(
                    pluginId, false, writer.getGeneratedFiles(), diagnostics.getDiagnostics(), 0, e, pluginOutputs);
        }
    }
}
