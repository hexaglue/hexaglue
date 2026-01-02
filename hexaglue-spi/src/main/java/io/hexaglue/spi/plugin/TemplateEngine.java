package io.hexaglue.spi.plugin;

import java.util.Map;
import java.util.function.Function;

/**
 * Simple template engine for code generation.
 *
 * <p>Provides basic template rendering capabilities for plugins.
 * Templates use a simple placeholder syntax: {@code ${variableName}}.
 *
 * <p>Example:
 * <pre>{@code
 * String template = """
 *     package ${package};
 *
 *     public class ${className} {
 *         private ${idType} id;
 *     }
 *     """;
 *
 * String code = context.templates().render(template, Map.of(
 *     "package", "com.example.infrastructure",
 *     "className", "OrderEntity",
 *     "idType", "UUID"
 * ));
 * }</pre>
 *
 * <p>For more advanced templating (loops, conditionals), plugins can use
 * external libraries like Mustache or FreeMarker.
 */
public interface TemplateEngine {

    /**
     * Renders a template with the given context variables.
     *
     * @param template the template string with ${variable} placeholders
     * @param context the variable values
     * @return the rendered string
     */
    String render(String template, Map<String, Object> context);

    /**
     * Registers a custom helper function.
     *
     * <p>Helpers can transform values during rendering. For example:
     * <pre>{@code
     * templates.registerHelper("capitalize", s ->
     *     s.substring(0, 1).toUpperCase() + s.substring(1));
     *
     * // In template: ${helper:capitalize:name}
     * }</pre>
     *
     * @param name the helper name
     * @param helper the helper function
     */
    void registerHelper(String name, Function<String, String> helper);

    /**
     * Loads a template from a resource path.
     *
     * <p>Templates are loaded from the classpath, typically from
     * the plugin's JAR resources.
     *
     * @param resourcePath the resource path (e.g., "templates/entity.java.template")
     * @return the template content
     * @throws TemplateNotFoundException if the template is not found
     */
    default String loadTemplate(String resourcePath) {
        throw new TemplateNotFoundException("Template loading not implemented: " + resourcePath);
    }

    /**
     * Exception thrown when a template cannot be found.
     */
    class TemplateNotFoundException extends RuntimeException {
        public TemplateNotFoundException(String message) {
            super(message);
        }

        public TemplateNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
