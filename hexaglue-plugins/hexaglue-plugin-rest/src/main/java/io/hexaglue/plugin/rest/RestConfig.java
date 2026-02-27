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

import io.hexaglue.spi.plugin.PluginConfig;
import java.util.Arrays;
import java.util.Map;

/**
 * Configuration for the REST plugin, loaded from hexaglue.yaml.
 *
 * @param apiPackage                 base package for generated code (null = auto-detect)
 * @param controllerSuffix           suffix for controller classes
 * @param requestDtoSuffix           suffix for request DTO records
 * @param responseDtoSuffix          suffix for response DTO records
 * @param basePath                   URL prefix for all endpoints
 * @param generateOpenApiAnnotations whether to generate OpenAPI annotations
 * @param flattenValueObjects        whether to flatten multi-field VOs in DTOs
 * @param generateExceptionHandler   whether to generate the global exception handler
 * @param exceptionHandlerClassName  class name of the exception handler
 * @param targetModule               target module for multi-module routing (null = auto)
 * @param exceptionMappings          custom exception-to-HTTP-status mappings
 * @since 3.1.0
 */
public record RestConfig(
        String apiPackage,
        String controllerSuffix,
        String requestDtoSuffix,
        String responseDtoSuffix,
        String basePath,
        boolean generateOpenApiAnnotations,
        boolean flattenValueObjects,
        boolean generateExceptionHandler,
        String exceptionHandlerClassName,
        String targetModule,
        Map<String, Integer> exceptionMappings) {

    /**
     * Creates a RestConfig from plugin configuration.
     *
     * @param config the plugin configuration
     * @return the REST config
     */
    public static RestConfig from(PluginConfig config) {
        return new RestConfig(
                config.getString("apiPackage").orElse(null),
                config.getString("controllerSuffix", "Controller"),
                config.getString("requestDtoSuffix", "Request"),
                config.getString("responseDtoSuffix", "Response"),
                config.getString("basePath", "/api"),
                config.getBoolean("generateOpenApiAnnotations", true),
                config.getBoolean("flattenValueObjects", true),
                config.getBoolean("generateExceptionHandler", true),
                config.getString("exceptionHandlerClassName", "GlobalExceptionHandler"),
                config.getString("targetModule").orElse(null),
                config.getIntegerMap("exceptionMappings").orElse(Map.of()));
    }

    /**
     * Returns default configuration.
     *
     * @return a RestConfig with all defaults
     */
    public static RestConfig defaults() {
        return new RestConfig(
                null,
                "Controller",
                "Request",
                "Response",
                "/api",
                true,
                true,
                true,
                "GlobalExceptionHandler",
                null,
                Map.of());
    }

    /**
     * Resolves the controller package from apiPackage or domain package.
     *
     * @param domainPackage the driving port's package
     * @return the controller package name
     */
    public String controllerPackage(String domainPackage) {
        if (apiPackage != null) {
            return apiPackage + ".controller";
        }
        return deriveApiPackage(domainPackage) + ".controller";
    }

    /**
     * Resolves the DTO package from apiPackage or domain package.
     *
     * @param domainPackage the driving port's package
     * @return the DTO package name
     */
    public String dtoPackage(String domainPackage) {
        if (apiPackage != null) {
            return apiPackage + ".dto";
        }
        return deriveApiPackage(domainPackage) + ".dto";
    }

    /**
     * Resolves the exception package from apiPackage or domain package.
     *
     * @param domainPackage the driving port's package
     * @return the exception package name
     */
    public String exceptionPackage(String domainPackage) {
        if (apiPackage != null) {
            return apiPackage + ".exception";
        }
        return deriveApiPackage(domainPackage) + ".exception";
    }

    private static String deriveApiPackage(String domainPackage) {
        String[] parts = domainPackage.split("\\.");
        // Find the first (leftmost) "core", "domain", "model", or "port" segment
        for (int i = 0; i < parts.length; i++) {
            if ("core".equals(parts[i])
                    || "domain".equals(parts[i])
                    || "model".equals(parts[i])
                    || "port".equals(parts[i])) {
                return String.join(".", Arrays.copyOf(parts, i)) + ".api";
            }
        }
        return domainPackage.replace(".core", ".api").replace(".domain", ".api");
    }
}
