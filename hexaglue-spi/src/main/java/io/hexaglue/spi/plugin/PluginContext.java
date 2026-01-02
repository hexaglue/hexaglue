package io.hexaglue.spi.plugin;

import io.hexaglue.spi.ir.IrSnapshot;
import java.util.Optional;

/**
 * Execution context provided to plugins.
 *
 * <p>This interface provides access to:
 * <ul>
 *   <li>The analyzed application model ({@link IrSnapshot})</li>
 *   <li>Plugin-specific configuration</li>
 *   <li>Code generation facilities</li>
 *   <li>Diagnostic reporting</li>
 *   <li>Output sharing between plugins</li>
 * </ul>
 */
public interface PluginContext {

    /**
     * Returns the analyzed application model.
     *
     * <p>The IR is immutable and represents the complete analysis result.
     *
     * @return the IR snapshot (never null)
     */
    IrSnapshot ir();

    /**
     * Returns the plugin configuration.
     *
     * @return the configuration (never null, may be empty)
     */
    PluginConfig config();

    /**
     * Returns the code writer for generating files.
     *
     * @return the code writer (never null)
     */
    CodeWriter writer();

    /**
     * Returns the diagnostic reporter.
     *
     * @return the diagnostic reporter (never null)
     */
    DiagnosticReporter diagnostics();

    /**
     * Stores an output value that can be retrieved by other plugins.
     *
     * <p>This enables plugins to share computed data. For example, a JPA plugin
     * might share a map of generated entity classes for use by a Liquibase plugin.
     *
     * <p>Example:
     * <pre>{@code
     * // In JPA plugin
     * Map<String, JpaEntity> entities = generateEntities();
     * context.setOutput("generated-entities", entities);
     *
     * // In Liquibase plugin (which depends on JPA plugin)
     * Optional<Map<String, JpaEntity>> entities = context.getOutput(
     *     "io.hexaglue.plugin.jpa", "generated-entities", Map.class);
     * }</pre>
     *
     * @param key the output key (unique within this plugin)
     * @param value the value to store
     * @param <T> the value type
     */
    <T> void setOutput(String key, T value);

    /**
     * Retrieves an output value stored by another plugin.
     *
     * <p>The plugin must be listed in {@link HexaGluePlugin#dependsOn()} to ensure
     * it has already executed before trying to read its output.
     *
     * @param pluginId the ID of the plugin that stored the output
     * @param key the output key
     * @param type the expected value type
     * @param <T> the value type
     * @return the output value, or empty if not found or wrong type
     */
    <T> Optional<T> getOutput(String pluginId, String key, Class<T> type);

    /**
     * Returns the ID of the currently executing plugin.
     *
     * @return the plugin ID
     */
    String currentPluginId();

    /**
     * Returns the template engine for code generation.
     *
     * <p>The template engine provides simple placeholder-based rendering.
     * For more advanced templating needs, plugins can use external libraries.
     *
     * @return the template engine (never null)
     */
    TemplateEngine templates();
}
