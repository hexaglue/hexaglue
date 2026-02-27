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
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.model.ControllerSpec;
import io.hexaglue.plugin.rest.model.EndpointSpec;
import io.hexaglue.plugin.rest.model.ParameterBindingSpec;
import io.hexaglue.plugin.rest.model.PathVariableSpec;
import io.hexaglue.plugin.rest.model.QueryParamSpec;
import io.hexaglue.plugin.rest.util.NamingConventions;
import io.hexaglue.plugin.rest.util.RestAnnotations;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/**
 * Generates a Spring MVC {@code @RestController} from a {@link ControllerSpec}.
 *
 * <p>This is a Stage 2 (codegen) class: it performs pure mechanical transformation
 * from spec to JavaPoet TypeSpec, with no business logic.
 *
 * @since 3.1.0
 */
public final class RestControllerCodegen {

    private static final ClassName RESPONSE_ENTITY = ClassName.get("org.springframework.http", "ResponseEntity");

    private RestControllerCodegen() {
        /* utility class */
    }

    /**
     * Generates a TypeSpec for the REST controller.
     *
     * @param spec   the controller specification
     * @param config the REST plugin configuration
     * @return the JavaPoet TypeSpec
     */
    public static TypeSpec generate(ControllerSpec spec, RestConfig config) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(spec.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RestAnnotations.generated())
                .addAnnotation(RestAnnotations.restController())
                .addAnnotation(RestAnnotations.requestMapping(spec.basePath()));

        if (config.generateOpenApiAnnotations()) {
            builder.addAnnotation(RestAnnotations.tag(spec.tagName(), spec.tagDescription()));
        }

        // Port field
        String fieldName = NamingConventions.decapitalize(spec.drivingPortType().simpleName());
        FieldSpec portField = FieldSpec.builder(spec.drivingPortType(), fieldName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
        builder.addField(portField);

        // Constructor
        builder.addMethod(generateConstructor(spec, fieldName));

        // Endpoints
        for (EndpointSpec endpoint : spec.endpoints()) {
            builder.addMethod(generateEndpointMethod(endpoint, fieldName, spec, config));
        }

        return builder.build();
    }

    private static MethodSpec generateConstructor(ControllerSpec spec, String fieldName) {
        ParameterSpec param =
                ParameterSpec.builder(spec.drivingPortType(), fieldName).build();
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(param)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build();
    }

    private static MethodSpec generateEndpointMethod(
            EndpointSpec endpoint, String portFieldName, ControllerSpec spec, RestConfig config) {
        ParameterizedTypeName returnType =
                ParameterizedTypeName.get(RESPONSE_ENTITY, ClassName.get("java.lang", "Object"));

        MethodSpec.Builder method = MethodSpec.methodBuilder(endpoint.methodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType);

        // OpenAPI annotations
        if (config.generateOpenApiAnnotations()) {
            method.addAnnotation(RestAnnotations.operation(endpoint.operationSummary()));
            method.addAnnotation(RestAnnotations.apiResponse(
                    String.valueOf(endpoint.responseStatus()), deriveResponseDescription(endpoint)));
        }

        // HTTP method mapping
        method.addAnnotation(RestAnnotations.httpMethodMapping(endpoint.httpMethod(), endpoint.path()));

        // @PathVariable parameters
        for (PathVariableSpec pv : endpoint.pathVariables()) {
            ParameterSpec pathVar = ParameterSpec.builder(pv.javaType(), pv.javaName())
                    .addAnnotation(RestAnnotations.pathVariable())
                    .build();
            method.addParameter(pathVar);
        }

        // @RequestParam parameters
        for (QueryParamSpec qp : endpoint.queryParams()) {
            ParameterSpec queryParam = ParameterSpec.builder(qp.javaType(), qp.javaName())
                    .addAnnotation(RestAnnotations.requestParam(qp.required()))
                    .build();
            method.addParameter(queryParam);
        }

        // @Valid @RequestBody parameter
        if (endpoint.hasRequestBody()) {
            ClassName dtoType = resolveDtoType(endpoint.requestDtoRef(), spec);
            ParameterSpec requestParam = ParameterSpec.builder(dtoType, "request")
                    .addAnnotation(RestAnnotations.valid())
                    .addAnnotation(RestAnnotations.requestBody())
                    .build();
            method.addParameter(requestParam);
        }

        // Delegation to port
        if (endpoint.isMergedCollection()) {
            // Merged collection endpoint: conditional dispatch
            generateMergedCollectionBody(method, endpoint, portFieldName, spec);
        } else if (endpoint.parameterBindings().isEmpty()) {
            // No parameters: simple delegation
            if (endpoint.isVoid()) {
                method.addStatement("$N.$N()", portFieldName, endpoint.methodName());
                method.addStatement("return $T.noContent().build()", RESPONSE_ENTITY);
            } else {
                method.addStatement("var result = $N.$N()", portFieldName, endpoint.methodName());
                addReturnStatement(method, endpoint, spec);
            }
        } else {
            // Delegation with parameter reconstruction
            CodeBlock args = endpoint.parameterBindings().stream()
                    .map(RestControllerCodegen::generateBindingExpression)
                    .collect(CodeBlock.joining(",\n"));

            if (endpoint.isVoid()) {
                method.addStatement(CodeBlock.of("$N.$N(\n$>$>$L$<$<)", portFieldName, endpoint.methodName(), args));
                method.addStatement("return $T.noContent().build()", RESPONSE_ENTITY);
            } else {
                method.addStatement(
                        CodeBlock.of("var result = $N.$N(\n$>$>$L$<$<)", portFieldName, endpoint.methodName(), args));
                addReturnStatement(method, endpoint, spec);
            }
        }

        return method.build();
    }

    /**
     * Generates the body for a merged collection endpoint with conditional dispatch.
     *
     * <p>Produces an if/else structure: when the first query param is non-null, delegates
     * to the filtered method with reconstructed parameters; otherwise delegates to the
     * default (no-arg) method.
     */
    private static void generateMergedCollectionBody(
            MethodSpec.Builder method, EndpointSpec endpoint, String portFieldName, ControllerSpec spec) {
        String firstQueryParam = endpoint.queryParams().get(0).javaName();

        // Filtered branch: if (param != null)
        CodeBlock filteredArgs = endpoint.parameterBindings().stream()
                .map(RestControllerCodegen::generateBindingExpression)
                .collect(CodeBlock.joining(", "));

        method.beginControlFlow("if ($N != null)", firstQueryParam);
        method.addStatement(CodeBlock.of(
                "var result = $N.$N($L)",
                portFieldName,
                endpoint.filteredDelegateName().orElseThrow(),
                filteredArgs));
        addReturnStatement(method, endpoint, spec);
        method.endControlFlow();

        // Default branch: no args
        method.addStatement("var result = $N.$N()", portFieldName, endpoint.methodName());
        addReturnStatement(method, endpoint, spec);
    }

    private static CodeBlock generateBindingExpression(ParameterBindingSpec binding) {
        return switch (binding.kind()) {
            case DIRECT -> CodeBlock.of("request.$N()", binding.sourceFields().get(0));
            case CONSTRUCTOR_WRAP ->
                CodeBlock.of(
                        "new $T(request.$N())",
                        binding.domainType(),
                        binding.sourceFields().get(0));
            case FACTORY_WRAP -> {
                String accessors = binding.sourceFields().stream()
                        .map(f -> "request." + f + "()")
                        .collect(Collectors.joining(", "));
                yield CodeBlock.of("$T.of($L)", binding.domainType(), accessors);
            }
            case PATH_VARIABLE_WRAP ->
                CodeBlock.of(
                        "new $T($N)",
                        binding.domainType(),
                        binding.sourceFields().get(0));
            case QUERY_PARAM -> CodeBlock.of("$N", binding.sourceFields().get(0));
        };
    }

    private static void addReturnStatement(MethodSpec.Builder method, EndpointSpec endpoint, ControllerSpec spec) {
        if (endpoint.hasResponseBody()) {
            ClassName responseDtoType = resolveResponseDtoType(endpoint.responseDtoRef(), spec);
            method.addStatement("return $T.ok($T.from(result))", RESPONSE_ENTITY, responseDtoType);
        } else {
            method.addStatement("return $T.ok(result)", RESPONSE_ENTITY);
        }
    }

    private static ClassName resolveResponseDtoType(String dtoClassName, ControllerSpec spec) {
        return spec.responseDtos().stream()
                .filter(dto -> dto.className().equals(dtoClassName))
                .map(dto -> ClassName.get(dto.packageName(), dto.className()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Response DTO not found: " + dtoClassName));
    }

    private static ClassName resolveDtoType(String dtoClassName, ControllerSpec spec) {
        return spec.requestDtos().stream()
                .filter(dto -> dto.className().equals(dtoClassName))
                .map(dto -> ClassName.get(dto.packageName(), dto.className()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("DTO not found: " + dtoClassName));
    }

    private static String deriveResponseDescription(EndpointSpec endpoint) {
        String kebab = NamingConventions.toKebabCase(endpoint.methodName());
        String[] words = kebab.split("-");
        StringBuilder sb = new StringBuilder();
        sb.append(NamingConventions.capitalize(words[0]));
        for (int i = 1; i < words.length; i++) {
            sb.append(' ').append(words[i]);
        }
        sb.append(" completed successfully");
        return sb.toString();
    }
}
