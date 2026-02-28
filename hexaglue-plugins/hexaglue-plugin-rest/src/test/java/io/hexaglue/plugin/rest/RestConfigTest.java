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

package io.hexaglue.plugin.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.plugin.PluginConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RestConfig}.
 */
@DisplayName("RestConfig")
class RestConfigTest {

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("should have correct default values")
        void shouldHaveCorrectDefaults() {
            RestConfig config = RestConfig.defaults();

            assertThat(config.apiPackage()).isNull();
            assertThat(config.controllerSuffix()).isEqualTo("Controller");
            assertThat(config.requestDtoSuffix()).isEqualTo("Request");
            assertThat(config.responseDtoSuffix()).isEqualTo("Response");
            assertThat(config.basePath()).isEqualTo("/api");
            assertThat(config.generateOpenApiAnnotations()).isTrue();
            assertThat(config.flattenValueObjects()).isTrue();
            assertThat(config.generateExceptionHandler()).isTrue();
            assertThat(config.exceptionHandlerClassName()).isEqualTo("GlobalExceptionHandler");
            assertThat(config.targetModule()).isNull();
            assertThat(config.exceptionMappings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Explicit Values")
    class ExplicitValues {

        @Test
        @DisplayName("should read all configured keys")
        void shouldReadAllConfiguredKeys() {
            Map<String, Object> values = new HashMap<>();
            values.put("apiPackage", "com.acme.banking.api");
            values.put("controllerSuffix", "RestController");
            values.put("requestDtoSuffix", "Cmd");
            values.put("responseDtoSuffix", "Dto");
            values.put("basePath", "/api/v1");
            values.put("generateOpenApiAnnotations", false);
            values.put("flattenValueObjects", false);
            values.put("generateExceptionHandler", false);
            values.put("exceptionHandlerClassName", "ApiExceptionHandler");
            values.put("targetModule", "banking-api");

            RestConfig config = RestConfig.from(createPluginConfig(values));

            assertThat(config.apiPackage()).isEqualTo("com.acme.banking.api");
            assertThat(config.controllerSuffix()).isEqualTo("RestController");
            assertThat(config.requestDtoSuffix()).isEqualTo("Cmd");
            assertThat(config.responseDtoSuffix()).isEqualTo("Dto");
            assertThat(config.basePath()).isEqualTo("/api/v1");
            assertThat(config.generateOpenApiAnnotations()).isFalse();
            assertThat(config.flattenValueObjects()).isFalse();
            assertThat(config.generateExceptionHandler()).isFalse();
            assertThat(config.exceptionHandlerClassName()).isEqualTo("ApiExceptionHandler");
            assertThat(config.targetModule()).isEqualTo("banking-api");
        }
    }

    @Nested
    @DisplayName("Package Resolution")
    class PackageResolution {

        @Test
        @DisplayName("should derive controller package from domain package")
        void shouldDeriveControllerPackage() {
            RestConfig config = RestConfig.defaults();
            assertThat(config.controllerPackage("com.acme.core.port.in")).isEqualTo("com.acme.api.controller");
        }

        @Test
        @DisplayName("should derive dto package from domain package")
        void shouldDeriveDtoPackage() {
            RestConfig config = RestConfig.defaults();
            assertThat(config.dtoPackage("com.acme.core.port.in")).isEqualTo("com.acme.api.dto");
        }

        @Test
        @DisplayName("should derive exception package from domain package")
        void shouldDeriveExceptionPackage() {
            RestConfig config = RestConfig.defaults();
            assertThat(config.exceptionPackage("com.acme.core.port.in")).isEqualTo("com.acme.api.exception");
        }

        @Test
        @DisplayName("should use explicit apiPackage when configured")
        void shouldUseExplicitApiPackage() {
            Map<String, Object> values = new HashMap<>();
            values.put("apiPackage", "com.acme.banking.api");
            RestConfig config = RestConfig.from(createPluginConfig(values));

            assertThat(config.controllerPackage("com.acme.core.port.in")).isEqualTo("com.acme.banking.api.controller");
            assertThat(config.dtoPackage("com.acme.core.port.in")).isEqualTo("com.acme.banking.api.dto");
        }
    }

    @Nested
    @DisplayName("Exception Mappings")
    class ExceptionMappings {

        @Test
        @DisplayName("should parse exception mappings from config")
        void shouldParseExceptionMappings() {
            Map<String, Object> values = new HashMap<>();
            values.put(
                    "exceptionMappings",
                    Map.of(
                            "com.acme.core.exception.PaymentFailedException",
                            502,
                            "com.acme.core.exception.RateLimitException",
                            429));

            RestConfig config = RestConfig.from(createPluginConfig(values));

            assertThat(config.exceptionMappings())
                    .containsEntry("com.acme.core.exception.PaymentFailedException", 502)
                    .containsEntry("com.acme.core.exception.RateLimitException", 429);
        }

        @Test
        @DisplayName("should default to empty map when no exception mappings configured")
        void shouldDefaultToEmptyMap() {
            RestConfig config = RestConfig.from(createPluginConfig(new HashMap<>()));

            assertThat(config.exceptionMappings()).isEmpty();
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private PluginConfig createPluginConfig(Map<String, Object> values) {
        return new PluginConfig() {
            @Override
            public Optional<String> getString(String key) {
                Object value = values.get(key);
                if (value == null) {
                    return Optional.empty();
                }
                return Optional.of(value.toString());
            }

            @Override
            public Optional<Boolean> getBoolean(String key) {
                Object value = values.get(key);
                if (value instanceof Boolean b) {
                    return Optional.of(b);
                }
                if (value instanceof String str) {
                    if ("true".equalsIgnoreCase(str)) {
                        return Optional.of(true);
                    }
                    if ("false".equalsIgnoreCase(str)) {
                        return Optional.of(false);
                    }
                }
                return Optional.empty();
            }

            @Override
            public Optional<Integer> getInteger(String key) {
                Object value = values.get(key);
                if (value instanceof Integer i) {
                    return Optional.of(i);
                }
                return Optional.empty();
            }

            @Override
            public Optional<Map<String, Integer>> getIntegerMap(String key) {
                Object value = values.get(key);
                if (value instanceof Map<?, ?> map) {
                    Map<String, Integer> result = new HashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getValue() instanceof Integer i) {
                            result.put(String.valueOf(entry.getKey()), i);
                        }
                    }
                    return result.isEmpty() ? Optional.empty() : Optional.of(Map.copyOf(result));
                }
                return Optional.empty();
            }
        };
    }
}
