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

package io.hexaglue.plugin.rest.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.rest.model.ExceptionHandlerSpec;
import io.hexaglue.plugin.rest.model.ExceptionMappingSpec;
import io.hexaglue.plugin.rest.util.RestAnnotations;
import java.util.Map;
import javax.lang.model.element.Modifier;

/**
 * Generates a Spring {@code @RestControllerAdvice} from an {@link ExceptionHandlerSpec}.
 *
 * <p>Each exception mapping produces an {@code @ExceptionHandler} method that returns
 * a {@code Map<String, Object>} with error code, message, and HTTP status.
 *
 * <p>This is a Stage 2 (codegen) class: it performs pure mechanical transformation
 * from spec to JavaPoet TypeSpec, with no business logic.
 *
 * @since 3.1.0
 */
public final class ExceptionHandlerCodegen {

    private static final ClassName MAP = ClassName.get(Map.class);
    private static final ClassName STRING = ClassName.get(String.class);
    private static final ClassName OBJECT = ClassName.get(Object.class);

    private ExceptionHandlerCodegen() {
        /* utility class */
    }

    /**
     * Generates a TypeSpec for the global exception handler.
     *
     * @param spec the exception handler specification
     * @return the JavaPoet TypeSpec
     */
    public static TypeSpec generate(ExceptionHandlerSpec spec) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(spec.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RestAnnotations.generated())
                .addAnnotation(RestAnnotations.restControllerAdvice());

        for (ExceptionMappingSpec mapping : spec.mappings()) {
            builder.addMethod(generateHandlerMethod(mapping));
        }

        return builder.build();
    }

    private static MethodSpec generateHandlerMethod(ExceptionMappingSpec mapping) {
        String statusName = httpStatusName(mapping.httpStatus());
        ParameterizedTypeName returnType = ParameterizedTypeName.get(MAP, STRING, OBJECT);

        boolean isCatchAll =
                "java.lang.Exception".equals(mapping.exceptionType().canonicalName());
        String messageExpr = isCatchAll ? "\"An unexpected error occurred: \" + ex.getMessage()" : "ex.getMessage()";

        return MethodSpec.methodBuilder(mapping.handlerMethod())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RestAnnotations.exceptionHandler(mapping.exceptionType()))
                .addAnnotation(RestAnnotations.responseStatus(statusName))
                .returns(returnType)
                .addParameter(mapping.exceptionType(), "ex")
                .addStatement(
                        "return $T.of(\n\"error\", $S,\n\"message\", $L,\n\"status\", $L)",
                        Map.class,
                        mapping.errorCode(),
                        messageExpr,
                        mapping.httpStatus())
                .build();
    }

    /**
     * Converts an HTTP status code to the Spring HttpStatus enum constant name.
     */
    private static String httpStatusName(int httpStatus) {
        return switch (httpStatus) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 410 -> "GONE";
            case 422 -> "UNPROCESSABLE_ENTITY";
            case 500 -> "INTERNAL_SERVER_ERROR";
            default -> "INTERNAL_SERVER_ERROR";
        };
    }
}
