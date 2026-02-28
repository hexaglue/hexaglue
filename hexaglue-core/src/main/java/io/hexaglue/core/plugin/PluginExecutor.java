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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.core.audit.DefaultArchitectureQuery;
import io.hexaglue.core.engine.ModuleSourceSet;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.generation.PluginCategory;
import io.hexaglue.spi.plugin.CodeWriter;
import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * <p>Each plugin receives a {@link PluginContext} with the architectural model and
 * facilities for code generation.
 *
 * @since 4.0.0 - Removed IrSnapshot, uses ArchitecturalModel exclusively
 */
public final class PluginExecutor {

    private static final Logger log = LoggerFactory.getLogger(PluginExecutor.class);

    private final Path outputDirectory;
    private final Path reportsDirectory;
    private final Map<String, Map<String, Object>> pluginConfigs;
    private final ApplicationGraph graph;
    private final Set<PluginCategory> enabledCategories;
    private final ArchitecturalModel architecturalModel;
    private final List<ModuleSourceSet> moduleSourceSets;

    /**
     * Creates a plugin executor with the v4 ArchitecturalModel.
     *
     * @param outputDirectory the directory for generated sources
     * @param pluginConfigs plugin configurations keyed by plugin ID
     * @param graph the application graph for architecture analysis (may be null)
     * @param enabledCategories plugin categories to execute (null or empty for all categories)
     * @param architecturalModel the v4 architectural model (must not be null)
     * @since 4.0.0
     */
    public PluginExecutor(
            Path outputDirectory,
            Map<String, Map<String, Object>> pluginConfigs,
            ApplicationGraph graph,
            Set<PluginCategory> enabledCategories,
            ArchitecturalModel architecturalModel) {
        this(outputDirectory, pluginConfigs, graph, enabledCategories, architecturalModel, List.of());
    }

    /**
     * Creates a plugin executor with multi-module support.
     *
     * @param outputDirectory the directory for generated sources (default/fallback)
     * @param pluginConfigs plugin configurations keyed by plugin ID
     * @param graph the application graph for architecture analysis (may be null)
     * @param enabledCategories plugin categories to execute (null or empty for all categories)
     * @param architecturalModel the architectural model (must not be null)
     * @param moduleSourceSets module source sets for multi-module routing (empty for mono-module)
     * @since 5.0.0
     */
    public PluginExecutor(
            Path outputDirectory,
            Map<String, Map<String, Object>> pluginConfigs,
            ApplicationGraph graph,
            Set<PluginCategory> enabledCategories,
            ArchitecturalModel architecturalModel,
            List<ModuleSourceSet> moduleSourceSets) {
        this(outputDirectory, null, pluginConfigs, graph, enabledCategories, architecturalModel, moduleSourceSets);
    }

    /**
     * Creates a plugin executor with explicit reports directory.
     *
     * <p>In mono-module mode, the reports directory is passed to the {@link FileSystemCodeWriter}
     * so that documentation/audit reports are written to the correct location regardless of
     * the generated-sources output directory layout.
     *
     * @param outputDirectory the directory for generated sources (default/fallback)
     * @param reportsDirectory explicit reports directory (may be null for derived)
     * @param pluginConfigs plugin configurations keyed by plugin ID
     * @param graph the application graph for architecture analysis (may be null)
     * @param enabledCategories plugin categories to execute (null or empty for all categories)
     * @param architecturalModel the architectural model (must not be null)
     * @param moduleSourceSets module source sets for multi-module routing (empty for mono-module)
     * @since 6.0.0
     */
    public PluginExecutor(
            Path outputDirectory,
            Path reportsDirectory,
            Map<String, Map<String, Object>> pluginConfigs,
            ApplicationGraph graph,
            Set<PluginCategory> enabledCategories,
            ArchitecturalModel architecturalModel,
            List<ModuleSourceSet> moduleSourceSets) {
        this.outputDirectory = outputDirectory;
        this.reportsDirectory = reportsDirectory;
        this.pluginConfigs = pluginConfigs;
        this.graph = graph;
        this.enabledCategories = enabledCategories;
        this.architecturalModel = Objects.requireNonNull(architecturalModel, "architecturalModel must not be null");
        this.moduleSourceSets = moduleSourceSets != null ? moduleSourceSets : List.of();
    }

    /**
     * Discovers and executes all plugins in dependency order.
     *
     * <p>Plugins are sorted topologically based on their declared dependencies.
     * If a plugin fails, any plugins depending on it will be skipped.
     *
     * @return the execution result
     * @throws PluginDependencyException if a plugin depends on an unknown plugin
     * @throws PluginCyclicDependencyException if cyclic dependencies are detected
     * @since 4.0.0
     */
    public PluginExecutionResult execute() {
        return execute(List.of());
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
     * @param primaryClassifications the primary classification results from the engine
     * @return the execution result
     * @throws PluginDependencyException if a plugin depends on an unknown plugin
     * @throws PluginCyclicDependencyException if cyclic dependencies are detected
     * @since 4.0.0
     */
    public PluginExecutionResult execute(
            List<io.hexaglue.spi.classification.PrimaryClassificationResult> primaryClassifications) {
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

            PluginResult result = executePlugin(plugin, outputStore, graph);
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

    private PluginResult executePlugin(HexaGluePlugin plugin, PluginOutputStore outputStore, ApplicationGraph graph) {
        String pluginId = plugin.id();
        log.info("Executing plugin: {}", pluginId);

        CollectingDiagnosticReporter diagnostics = new CollectingDiagnosticReporter(pluginId);
        MapPluginConfig config = new MapPluginConfig(pluginConfigs.getOrDefault(pluginId, Map.of()));

        // Create per-plugin CodeWriter (may have plugin-specific output directory)
        CodeWriter writer = createCodeWriter(pluginId);

        // Create ArchitectureQuery from graph if available
        // Note: PortModel and DomainModel are no longer used (v4 uses ArchitecturalModel)
        ArchitectureQuery architectureQuery = graph != null ? new DefaultArchitectureQuery(graph) : null;

        // Create context with v4 ArchitecturalModel
        PluginContext context = new DefaultPluginContext(
                pluginId, architecturalModel, config, writer, diagnostics, outputStore, architectureQuery);

        try {
            long start = System.currentTimeMillis();
            plugin.execute(context);
            long elapsed = System.currentTimeMillis() - start;

            List<Path> generatedFiles = extractGeneratedFiles(writer);
            Set<Path> sourceRoots = extractUsedSourceRoots(writer, moduleSourceSets);

            log.info("Plugin {} completed in {}ms, generated {} files", pluginId, elapsed, generatedFiles.size());

            // Capture plugin outputs for retrieval by the engine/mojos
            Map<String, Object> pluginOutputs = outputStore.getAll(pluginId);

            return new PluginResult(
                    pluginId,
                    true,
                    generatedFiles,
                    diagnostics.getDiagnostics(),
                    elapsed,
                    null,
                    pluginOutputs,
                    sourceRoots);

        } catch (Exception e) {
            log.error("Plugin {} failed: {}", pluginId, e.getMessage(), e);
            diagnostics.error("Plugin execution failed: " + e.getMessage(), e);

            List<Path> generatedFiles = extractGeneratedFiles(writer);
            Set<Path> sourceRoots = extractUsedSourceRoots(writer, moduleSourceSets);

            // Capture outputs even on failure (partial outputs may exist)
            Map<String, Object> pluginOutputs = outputStore.getAll(pluginId);

            return new PluginResult(
                    pluginId, false, generatedFiles, diagnostics.getDiagnostics(), 0, e, pluginOutputs, sourceRoots);
        }
    }

    /**
     * Creates a {@link CodeWriter} for a specific plugin.
     *
     * <p>If the plugin configuration contains an {@code outputDirectory} entry, it is used
     * as the sources output directory instead of the global default. In multi-module mode,
     * a per-plugin {@link MultiModuleCodeWriter} is created with module-specific directories.</p>
     *
     * @param pluginId the plugin identifier
     * @return the code writer to use for this plugin
     * @since 5.0.0
     */
    private CodeWriter createCodeWriter(String pluginId) {
        Map<String, Object> pluginConfig = pluginConfigs.getOrDefault(pluginId, Map.of());
        Path pluginOutputOverride = resolvePluginOutputOverride(pluginConfig);

        if (!moduleSourceSets.isEmpty()) {
            log.debug("Using MultiModuleCodeWriter for plugin {}", pluginId);
            // In multi-module mode, the override is resolved per-module:
            // relative paths resolve against each module's baseDir,
            // absolute paths are used directly.
            return new MultiModuleCodeWriter(moduleSourceSets, outputDirectory, pluginOutputOverride);
        } else {
            // Mono-module: use the 3-arg FileSystemCodeWriter for explicit directory control
            Path effectiveSourcesDir = pluginOutputOverride != null ? pluginOutputOverride : outputDirectory;
            return new FileSystemCodeWriter(
                    effectiveSourcesDir,
                    deriveResourcesDirectory(effectiveSourcesDir),
                    reportsDirectory != null ? reportsDirectory : deriveReportsDirectory(effectiveSourcesDir));
        }
    }

    /**
     * Resolves the per-plugin output directory override, or null if no override is configured.
     */
    static Path resolvePluginOutputOverride(Map<String, Object> pluginConfig) {
        Object override = pluginConfig.get("outputDirectory");
        if (override instanceof String dir && !dir.isBlank()) {
            return Path.of(dir);
        }
        return null;
    }

    /**
     * Derives the resources directory from a sources directory following the Maven convention.
     *
     * <p>Example: {@code target/generated-sources/hexaglue} → {@code target/generated-resources/hexaglue}
     */
    static Path deriveResourcesDirectory(Path sourcesDir) {
        Path parent = sourcesDir.getParent();
        String toolName = sourcesDir.getFileName().toString();
        if (parent != null) {
            Path targetDir = parent.getParent();
            if (targetDir != null) {
                return targetDir.resolve("generated-resources").resolve(toolName);
            }
        }
        // Fallback: sibling directory
        return sourcesDir.resolveSibling("generated-resources");
    }

    /**
     * Derives the reports directory from a sources directory.
     *
     * <p>Example: {@code target/generated-sources/hexaglue} → {@code target/hexaglue/reports}
     */
    static Path deriveReportsDirectory(Path sourcesDir) {
        Path parent = sourcesDir.getParent();
        String toolName = sourcesDir.getFileName().toString();
        if (parent != null) {
            Path targetDir = parent.getParent();
            if (targetDir != null) {
                return targetDir.resolve(toolName).resolve("reports");
            }
        }
        // Fallback: sibling directory
        return sourcesDir.resolveSibling("reports");
    }

    /**
     * Extracts generated files from the code writer, handling both mono and multi-module writers.
     */
    private static List<Path> extractGeneratedFiles(CodeWriter writer) {
        if (writer instanceof MultiModuleCodeWriter mmWriter) {
            return mmWriter.getGeneratedFiles();
        } else if (writer instanceof FileSystemCodeWriter fsWriter) {
            return fsWriter.getGeneratedFiles();
        }
        return List.of();
    }

    /**
     * Extracts the source root directories that were actually used by a writer.
     *
     * <p>For a mono-module writer, this returns the single output directory.
     * For a multi-module writer, this returns all module output directories
     * that contain at least one generated file.</p>
     *
     * @since 5.0.0
     */
    static Set<Path> extractUsedSourceRoots(CodeWriter writer, List<ModuleSourceSet> moduleSourceSets) {
        List<Path> generatedFiles = extractGeneratedFiles(writer);
        if (generatedFiles.isEmpty()) {
            return Set.of();
        }

        if (writer instanceof MultiModuleCodeWriter mmWriter) {
            // Collect distinct output directories from all modules that have generated files
            Set<Path> roots = new HashSet<>();
            roots.add(mmWriter.getOutputDirectory()); // default output
            for (ModuleSourceSet mss : moduleSourceSets) {
                Path moduleOutput = mmWriter.getOutputDirectory(mss.moduleId());
                // Only include if we actually wrote files there
                for (Path file : generatedFiles) {
                    if (file.startsWith(moduleOutput)) {
                        roots.add(moduleOutput);
                        break;
                    }
                }
            }
            return Set.copyOf(roots);
        } else if (writer instanceof FileSystemCodeWriter fsWriter) {
            return Set.of(fsWriter.getOutputDirectory());
        }
        return Set.of();
    }
}
