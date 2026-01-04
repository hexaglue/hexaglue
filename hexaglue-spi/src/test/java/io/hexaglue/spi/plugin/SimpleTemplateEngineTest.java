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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SimpleTemplateEngineTest {

    private SimpleTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SimpleTemplateEngine();
    }

    @Nested
    @DisplayName("Simple Variable Substitution")
    class SimpleVariableTest {

        @Test
        @DisplayName("should replace single variable")
        void shouldReplaceSingleVariable() {
            String result = engine.render("Hello ${name}!", Map.of("name", "World"));
            assertThat(result).isEqualTo("Hello World!");
        }

        @Test
        @DisplayName("should replace multiple variables")
        void shouldReplaceMultipleVariables() {
            String result = engine.render("${greeting} ${name}!", Map.of("greeting", "Hello", "name", "World"));
            assertThat(result).isEqualTo("Hello World!");
        }

        @Test
        @DisplayName("should leave unresolved variables as-is")
        void shouldLeaveUnresolvedVariables() {
            String result = engine.render("Hello ${name}!", Map.of());
            assertThat(result).isEqualTo("Hello ${name}!");
        }

        @Test
        @DisplayName("should handle null template")
        void shouldHandleNullTemplate() {
            String result = engine.render(null, Map.of("name", "World"));
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle template with no variables")
        void shouldHandleTemplateWithNoVariables() {
            String result = engine.render("Hello World!", Map.of("name", "Test"));
            assertThat(result).isEqualTo("Hello World!");
        }
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTest {

        @Test
        @DisplayName("should use value when present")
        void shouldUseValueWhenPresent() {
            String result = engine.render("Hello ${name:-Guest}!", Map.of("name", "John"));
            assertThat(result).isEqualTo("Hello John!");
        }

        @Test
        @DisplayName("should use default when value is missing")
        void shouldUseDefaultWhenMissing() {
            String result = engine.render("Hello ${name:-Guest}!", Map.of());
            assertThat(result).isEqualTo("Hello Guest!");
        }

        @Test
        @DisplayName("should handle empty default")
        void shouldHandleEmptyDefault() {
            String result = engine.render("Hello ${name:-}World!", Map.of());
            assertThat(result).isEqualTo("Hello World!");
        }
    }

    @Nested
    @DisplayName("Nested Properties")
    class NestedPropertiesTest {

        @Test
        @DisplayName("should resolve nested map properties")
        void shouldResolveNestedMapProperties() {
            Map<String, Object> user = Map.of("name", "John", "email", "john@example.com");
            String result = engine.render("${user.name} <${user.email}>", Map.of("user", user));
            assertThat(result).isEqualTo("John <john@example.com>");
        }

        @Test
        @DisplayName("should return placeholder for missing nested property")
        void shouldReturnPlaceholderForMissingNestedProperty() {
            Map<String, Object> user = Map.of("name", "John");
            String result = engine.render("${user.email}", Map.of("user", user));
            assertThat(result).isEqualTo("${user.email}");
        }
    }

    @Nested
    @DisplayName("Built-in Helpers")
    class BuiltInHelpersTest {

        @Test
        @DisplayName("should apply upper helper")
        void shouldApplyUpperHelper() {
            String result = engine.render("${helper:upper:name}", Map.of("name", "hello"));
            assertThat(result).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("should apply lower helper")
        void shouldApplyLowerHelper() {
            String result = engine.render("${helper:lower:name}", Map.of("name", "HELLO"));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("should apply capitalize helper")
        void shouldApplyCapitalizeHelper() {
            String result = engine.render("${helper:capitalize:name}", Map.of("name", "hello"));
            assertThat(result).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should apply uncapitalize helper")
        void shouldApplyUncapitalizeHelper() {
            String result = engine.render("${helper:uncapitalize:name}", Map.of("name", "Hello"));
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("should apply camelCase helper")
        void shouldApplyCamelCaseHelper() {
            String result = engine.render("${helper:camelCase:name}", Map.of("name", "hello_world"));
            assertThat(result).isEqualTo("helloWorld");
        }

        @Test
        @DisplayName("should apply pascalCase helper")
        void shouldApplyPascalCaseHelper() {
            String result = engine.render("${helper:pascalCase:name}", Map.of("name", "hello_world"));
            assertThat(result).isEqualTo("HelloWorld");
        }

        @Test
        @DisplayName("should apply snakeCase helper")
        void shouldApplySnakeCaseHelper() {
            String result = engine.render("${helper:snakeCase:name}", Map.of("name", "HelloWorld"));
            assertThat(result).isEqualTo("hello_world");
        }

        @Test
        @DisplayName("should apply kebabCase helper")
        void shouldApplyKebabCaseHelper() {
            String result = engine.render("${helper:kebabCase:name}", Map.of("name", "HelloWorld"));
            assertThat(result).isEqualTo("hello-world");
        }
    }

    @Nested
    @DisplayName("Custom Helpers")
    class CustomHelpersTest {

        @Test
        @DisplayName("should apply custom helper")
        void shouldApplyCustomHelper() {
            engine.registerHelper("reverse", s -> new StringBuilder(s).reverse().toString());
            String result = engine.render("${helper:reverse:name}", Map.of("name", "hello"));
            assertThat(result).isEqualTo("olleh");
        }

        @Test
        @DisplayName("should override built-in helper")
        void shouldOverrideBuiltInHelper() {
            engine.registerHelper("upper", s -> "CUSTOM:" + s);
            String result = engine.render("${helper:upper:name}", Map.of("name", "test"));
            assertThat(result).isEqualTo("CUSTOM:test");
        }
    }

    @Nested
    @DisplayName("Code Generation Example")
    class CodeGenerationTest {

        @Test
        @DisplayName("should render Java class template")
        void shouldRenderJavaClassTemplate() {
            String template = """
                    package ${package};

                    public class ${className} {
                        private ${idType} id;

                        public ${idType} getId() {
                            return id;
                        }

                        public void setId(${idType} id) {
                            this.id = id;
                        }
                    }
                    """;

            String result = engine.render(
                    template,
                    Map.of(
                            "package", "com.example.infrastructure",
                            "className", "OrderEntity",
                            "idType", "UUID"));

            assertThat(result)
                    .contains("package com.example.infrastructure;")
                    .contains("public class OrderEntity")
                    .contains("private UUID id;")
                    .contains("public UUID getId()");
        }
    }

    @Nested
    @DisplayName("Template Loading")
    class TemplateLoadingTest {

        @Test
        @DisplayName("should throw TemplateNotFoundException for missing template")
        void shouldThrowForMissingTemplate() {
            assertThatThrownBy(() -> engine.loadTemplate("non-existent-template.txt"))
                    .isInstanceOf(TemplateEngine.TemplateNotFoundException.class)
                    .hasMessageContaining("non-existent-template.txt");
        }
    }
}
