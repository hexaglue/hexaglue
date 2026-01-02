package io.hexaglue.spi.plugin;

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
     * <p>Dependencies are executed before this plugin. If a dependency is missing
     * or fails, this plugin will not be executed.
     *
     * <p>Example:
     * <pre>{@code
     * public List<String> dependsOn() {
     *     return List.of("io.hexaglue.plugin.jpa"); // Depends on JPA plugin
     * }
     * }</pre>
     *
     * @return list of plugin IDs (empty if no dependencies)
     */
    default List<String> dependsOn() {
        return List.of();
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
