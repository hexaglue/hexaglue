package io.hexaglue.core.plugin;

import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers and executes HexaGlue plugins.
 *
 * <p>Plugins are discovered via {@link ServiceLoader} and executed in order.
 * Each plugin receives a {@link PluginContext} with the analyzed IR and
 * facilities for code generation.
 */
public final class PluginExecutor {

    private static final Logger log = LoggerFactory.getLogger(PluginExecutor.class);

    private final Path outputDirectory;
    private final Map<String, Map<String, Object>> pluginConfigs;

    /**
     * Creates a plugin executor.
     *
     * @param outputDirectory the directory for generated sources
     * @param pluginConfigs plugin configurations keyed by plugin ID
     */
    public PluginExecutor(Path outputDirectory, Map<String, Map<String, Object>> pluginConfigs) {
        this.outputDirectory = outputDirectory;
        this.pluginConfigs = pluginConfigs;
    }

    /**
     * Discovers and executes all plugins.
     *
     * @param ir the analyzed IR
     * @return the execution result
     */
    public PluginExecutionResult execute(IrSnapshot ir) {
        List<HexaGluePlugin> plugins = discoverPlugins();
        log.info("Discovered {} plugins", plugins.size());

        if (plugins.isEmpty()) {
            return PluginExecutionResult.empty();
        }

        // Shared output store for plugin communication
        PluginOutputStore outputStore = new PluginOutputStore();

        List<PluginResult> results = new ArrayList<>();

        for (HexaGluePlugin plugin : plugins) {
            PluginResult result = executePlugin(plugin, ir, outputStore);
            results.add(result);
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

    private PluginResult executePlugin(HexaGluePlugin plugin, IrSnapshot ir, PluginOutputStore outputStore) {
        String pluginId = plugin.id();
        log.info("Executing plugin: {}", pluginId);

        CollectingDiagnosticReporter diagnostics = new CollectingDiagnosticReporter(pluginId);
        FileSystemCodeWriter writer = new FileSystemCodeWriter(outputDirectory);
        MapPluginConfig config = new MapPluginConfig(pluginConfigs.getOrDefault(pluginId, Map.of()));

        PluginContext context = new DefaultPluginContext(pluginId, ir, config, writer, diagnostics, outputStore);

        try {
            long start = System.currentTimeMillis();
            plugin.execute(context);
            long elapsed = System.currentTimeMillis() - start;

            log.info(
                    "Plugin {} completed in {}ms, generated {} files",
                    pluginId,
                    elapsed,
                    writer.getGeneratedFiles().size());

            return new PluginResult(
                    pluginId, true, writer.getGeneratedFiles(), diagnostics.getDiagnostics(), elapsed, null);

        } catch (Exception e) {
            log.error("Plugin {} failed: {}", pluginId, e.getMessage(), e);
            diagnostics.error("Plugin execution failed: " + e.getMessage(), e);

            return new PluginResult(pluginId, false, writer.getGeneratedFiles(), diagnostics.getDiagnostics(), 0, e);
        }
    }
}
