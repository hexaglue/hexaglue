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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Simple template engine implementation using ${variable} placeholder syntax.
 *
 * <p>Supports:
 * <ul>
 *   <li>Simple variables: ${name}</li>
 *   <li>Nested properties: ${user.name} (via Map or bean-style access)</li>
 *   <li>Helpers: ${helper:uppercase:name}</li>
 *   <li>Default values: ${name:-default}</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * SimpleTemplateEngine engine = new SimpleTemplateEngine();
 * engine.registerHelper("upper", String::toUpperCase);
 *
 * String result = engine.render("Hello ${helper:upper:name}!", Map.of("name", "world"));
 * // Result: "Hello WORLD!"
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe. Helper registration
 * and template rendering can be performed concurrently from multiple threads.
 */
public final class SimpleTemplateEngine implements TemplateEngine {

    private static final Logger LOG = Logger.getLogger(SimpleTemplateEngine.class.getName());

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern HELPER_PATTERN = Pattern.compile("helper:([^:]+):(.+)");
    private static final Pattern DEFAULT_PATTERN = Pattern.compile("(.+):-(.*)");

    private final Map<String, Function<String, String>> helpers = new ConcurrentHashMap<>();

    public SimpleTemplateEngine() {
        // Register built-in helpers
        registerHelper("upper", String::toUpperCase);
        registerHelper("lower", String::toLowerCase);
        registerHelper("capitalize", this::capitalize);
        registerHelper("uncapitalize", this::uncapitalize);
        registerHelper("camelCase", this::toCamelCase);
        registerHelper("pascalCase", this::toPascalCase);
        registerHelper("snakeCase", this::toSnakeCase);
        registerHelper("kebabCase", this::toKebabCase);
    }

    @Override
    public String render(String template, Map<String, Object> context) {
        if (template == null || context == null) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String expression = matcher.group(1);
            String replacement = evaluateExpression(expression, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    @Override
    public void registerHelper(String name, Function<String, String> helper) {
        helpers.put(name, helper);
    }

    @Override
    public String loadTemplate(String resourcePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = SimpleTemplateEngine.class.getClassLoader();
        }

        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new TemplateNotFoundException("Template not found: " + resourcePath);
            }
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new TemplateNotFoundException("Failed to load template: " + resourcePath, e);
        }
    }

    private String evaluateExpression(String expression, Map<String, Object> context) {
        // Check for default value syntax: ${name:-default}
        Matcher defaultMatcher = DEFAULT_PATTERN.matcher(expression);
        String defaultValue = null;
        if (defaultMatcher.matches()) {
            expression = defaultMatcher.group(1);
            defaultValue = defaultMatcher.group(2);
        }

        // Check for helper syntax: ${helper:name:value}
        Matcher helperMatcher = HELPER_PATTERN.matcher(expression);
        if (helperMatcher.matches()) {
            String helperName = helperMatcher.group(1);
            String variableName = helperMatcher.group(2);
            Optional<String> value = resolveValue(variableName, context);
            Function<String, String> helper = helpers.get(helperName);
            if (helper != null && value.isPresent()) {
                return helper.apply(value.get());
            }
            return defaultValue != null ? defaultValue : "${" + expression + "}";
        }

        // Simple variable lookup
        Optional<String> value = resolveValue(expression, context);
        if (value.isPresent()) {
            return value.get();
        }

        return defaultValue != null ? defaultValue : "${" + expression + "}";
    }

    @SuppressWarnings("unchecked")
    private Optional<String> resolveValue(String path, Map<String, Object> context) {
        String[] parts = path.split("\\.");
        Object current = context;

        for (String part : parts) {
            if (current == null) {
                return Optional.empty();
            }
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                // Try bean-style property access
                current = getProperty(current, part).orElse(null);
            }
        }

        return current != null ? Optional.of(current.toString()) : Optional.empty();
    }

    private Optional<Object> getProperty(Object obj, String propertyName) {
        try {
            String getterName = "get" + capitalize(propertyName);
            return Optional.ofNullable(obj.getClass().getMethod(getterName).invoke(obj));
        } catch (Exception e) {
            LOG.log(
                    Level.FINEST,
                    () -> String.format(
                            "Getter '%s' not found on %s, trying field access",
                            propertyName, obj.getClass().getSimpleName()));
            try {
                // Try direct field access
                return Optional.ofNullable(obj.getClass().getField(propertyName).get(obj));
            } catch (Exception ex) {
                LOG.log(
                        Level.FINEST,
                        () -> String.format(
                                "Field '%s' not found on %s",
                                propertyName, obj.getClass().getSimpleName()));
                return Optional.empty();
            }
        }
    }

    // === Built-in helper implementations ===

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String uncapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private String toCamelCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : s.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    private String toPascalCase(String s) {
        String camel = toCamelCase(s);
        return capitalize(camel);
    }

    private String toSnakeCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    private String toKebabCase(String s) {
        return toSnakeCase(s).replace('_', '-');
    }
}
