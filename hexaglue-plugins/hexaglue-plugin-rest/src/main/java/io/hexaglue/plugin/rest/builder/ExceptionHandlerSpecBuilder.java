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

package io.hexaglue.plugin.rest.builder;

import com.palantir.javapoet.ClassName;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.model.ExceptionHandlerSpec;
import io.hexaglue.plugin.rest.model.ExceptionMappingSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds an {@link ExceptionHandlerSpec} from exception mappings collected across driving ports.
 *
 * <p>Applies name-based heuristics to derive HTTP status codes, deduplicates exceptions,
 * and automatically includes {@code IllegalArgumentException} (400) and {@code Exception} (500).
 *
 * @since 3.1.0
 */
public final class ExceptionHandlerSpecBuilder {

    private ExceptionHandlerSpecBuilder() {
        /* use builder() */
    }

    /**
     * Creates a new builder.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Derives an {@link ExceptionMappingSpec} from an exception ClassName using name-based heuristics.
     *
     * @param exceptionType the exception class name
     * @return the derived mapping
     */
    public static ExceptionMappingSpec deriveMapping(ClassName exceptionType) {
        String simpleName = exceptionType.simpleName();
        int httpStatus = deriveHttpStatus(simpleName);
        String errorCode = deriveErrorCode(simpleName);
        String handlerMethod = deriveHandlerMethod(simpleName);
        return new ExceptionMappingSpec(exceptionType, httpStatus, errorCode, handlerMethod);
    }

    private static int deriveHttpStatus(String simpleName) {
        if (simpleName.endsWith("NotFoundException")) return 404;
        if (simpleName.endsWith("AlreadyExistsException")) return 409;
        if (simpleName.endsWith("DuplicateException")) return 409;
        if (simpleName.endsWith("ConflictException")) return 409;
        if (simpleName.endsWith("ForbiddenException")) return 403;
        if (simpleName.endsWith("UnauthorizedException")) return 401;
        if (simpleName.endsWith("AccessDeniedException")) return 403;
        if (simpleName.endsWith("ValidationException")) return 400;
        if (simpleName.endsWith("InvalidException")) return 400;
        if (simpleName.endsWith("InsufficientException") || simpleName.contains("Insufficient")) return 400;
        if (simpleName.endsWith("RejectedException")) return 422;
        if (simpleName.endsWith("ExpiredException")) return 410;
        if ("IllegalArgumentException".equals(simpleName)) return 400;
        if ("IllegalStateException".equals(simpleName)) return 409;
        return 500;
    }

    private static String deriveErrorCode(String simpleName) {
        if (simpleName.endsWith("NotFoundException")) return "NOT_FOUND";
        if (simpleName.endsWith("AlreadyExistsException")) return "ALREADY_EXISTS";
        if (simpleName.endsWith("DuplicateException")) return "DUPLICATE";
        if (simpleName.endsWith("ConflictException")) return "CONFLICT";
        if (simpleName.endsWith("ForbiddenException")) return "FORBIDDEN";
        if (simpleName.endsWith("UnauthorizedException")) return "UNAUTHORIZED";
        if (simpleName.endsWith("AccessDeniedException")) return "ACCESS_DENIED";
        if (simpleName.endsWith("ValidationException")) return "VALIDATION_ERROR";
        if (simpleName.endsWith("InvalidException")) return "INVALID_REQUEST";
        if (simpleName.endsWith("InsufficientException") || simpleName.contains("Insufficient"))
            return "INSUFFICIENT_RESOURCE";
        if (simpleName.endsWith("RejectedException")) return "REJECTED";
        if (simpleName.endsWith("ExpiredException")) return "EXPIRED";
        if ("IllegalArgumentException".equals(simpleName)) return "BAD_REQUEST";
        if ("IllegalStateException".equals(simpleName)) return "ILLEGAL_STATE";
        return "INTERNAL_ERROR";
    }

    /**
     * Derives the handler method name from an exception simple name.
     *
     * <p>Rule: {@code "handle"} + simpleName without the {@code "Exception"} suffix.
     */
    private static String deriveHandlerMethod(String simpleName) {
        String stripped =
                simpleName.endsWith("Exception") ? simpleName.substring(0, simpleName.length() - 9) : simpleName;
        return "handle" + stripped;
    }

    /**
     * Builder for ExceptionHandlerSpec.
     */
    public static final class Builder {

        private List<ExceptionMappingSpec> exceptionMappings = List.of();
        private RestConfig config;
        private String apiPackage;

        /**
         * Sets the exception mappings collected from all controllers.
         *
         * @param mappings the exception mappings
         * @return this builder
         */
        public Builder exceptionMappings(List<ExceptionMappingSpec> mappings) {
            this.exceptionMappings = mappings;
            return this;
        }

        /**
         * Sets the REST plugin configuration.
         *
         * @param config the config
         * @return this builder
         */
        public Builder config(RestConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Sets the API package.
         *
         * @param apiPackage the package
         * @return this builder
         */
        public Builder apiPackage(String apiPackage) {
            this.apiPackage = apiPackage;
            return this;
        }

        /**
         * Builds the exception handler spec.
         *
         * <p>Deduplicates exceptions by qualified name, then appends
         * {@code IllegalArgumentException} (400) and {@code Exception} (500) as catch-alls.
         *
         * @return the built ExceptionHandlerSpec
         * @throws NullPointerException if required fields are missing
         */
        public ExceptionHandlerSpec build() {
            Objects.requireNonNull(config, "config is required");
            Objects.requireNonNull(apiPackage, "apiPackage is required");

            String className = config.exceptionHandlerClassName();
            String packageName = apiPackage + ".exception";

            // Deduplicate by fully qualified name, preserving insertion order
            Map<String, ExceptionMappingSpec> deduped = new LinkedHashMap<>();
            for (ExceptionMappingSpec mapping : exceptionMappings) {
                String qualifiedName = mapping.exceptionType().canonicalName();
                deduped.putIfAbsent(qualifiedName, mapping);
            }

            // Auto-include IllegalArgumentException (400) and Exception (500)
            ClassName illegalArg = ClassName.get("java.lang", "IllegalArgumentException");
            deduped.putIfAbsent(
                    illegalArg.canonicalName(),
                    new ExceptionMappingSpec(illegalArg, 400, "BAD_REQUEST", "handleIllegalArgument"));

            ClassName genericException = ClassName.get("java.lang", "Exception");
            deduped.putIfAbsent(
                    genericException.canonicalName(),
                    new ExceptionMappingSpec(genericException, 500, "INTERNAL_SERVER_ERROR", "handleGenericException"));

            return new ExceptionHandlerSpec(className, packageName, new ArrayList<>(deduped.values()));
        }
    }
}
