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

package io.hexaglue.plugin.jpa.strategy;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.arch.model.ir.MethodKind;
import javax.lang.model.element.Modifier;

/**
 * Strategy for generating FIND_BY_PROPERTY method implementations.
 *
 * <p>This strategy handles property-based query methods like {@code findByEmail(email)}.
 * Unlike ID-based methods, these delegate directly to the repository's derived query
 * method without special ID handling.
 *
 * <h3>Generated Code Pattern (Optional return):</h3>
 * <pre>{@code
 * @Override
 * public Optional<User> findByEmail(String email) {
 *     return repository.findByEmail(email).map(mapper::toDomain);
 * }
 * }</pre>
 *
 * <h3>Generated Code Pattern (List return):</h3>
 * <pre>{@code
 * @Override
 * public List<Order> findByStatus(String status) {
 *     return repository.findByStatus(status).stream()
 *         .map(mapper::toDomain)
 *         .toList();
 * }
 * }</pre>
 *
 * <h3>Supported MethodKinds:</h3>
 * <ul>
 *   <li>{@link MethodKind#FIND_BY_PROPERTY} - findByEmail(email), findByStatus(status)</li>
 *   <li>{@link MethodKind#FIND_ALL_BY_PROPERTY} - findAllByStatus(status)</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class FindByPropertyMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        return method.kind() == MethodKind.FIND_BY_PROPERTY || method.kind() == MethodKind.FIND_ALL_BY_PROPERTY;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        if (!method.hasParameters()) {
            throw new IllegalArgumentException(
                    "FIND_BY_PROPERTY method must have at least one parameter: " + method.name());
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(method.returnType());

        // Add parameters
        for (AdapterMethodSpec.ParameterInfo param : method.parameters()) {
            builder.addParameter(
                    ParameterSpec.builder(param.type(), param.name()).build());
        }

        // Build parameter list for delegation
        // C4 fix: Convert Identifier parameters to their wrapped types
        // - Aggregate's own identity → mapper.map(param)
        // - Cross-aggregate identity → param.value() directly
        String paramList = method.parameters().stream()
                .map(param -> convertIdentifierParam(param, context))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        // Generate method body based on return type
        if (method.returnsOptional()) {
            builder.addStatement(
                    "return $L.$L($L).map($L::toDomain)",
                    context.repositoryFieldName(),
                    method.name(),
                    paramList,
                    context.mapperFieldName());
        } else if (method.returnsCollection()) {
            builder.addStatement(
                    "return $L.$L($L).stream().map($L::toDomain).toList()",
                    context.repositoryFieldName(),
                    method.name(),
                    paramList,
                    context.mapperFieldName());
        } else {
            // Direct return (nullable)
            builder.addStatement("var entity = $L.$L($L)", context.repositoryFieldName(), method.name(), paramList);
            builder.addStatement("return entity != null ? $L.toDomain(entity) : null", context.mapperFieldName());
        }

        return builder.build();
    }

    /**
     * Converts an identifier parameter to the appropriate form for repository delegation.
     *
     * <p>For the aggregate's own identity type, uses mapper.map(param).
     * For cross-aggregate identities, uses param.value() to extract the wrapped value.
     * For non-identity parameters, returns the parameter name as-is.
     *
     * @param param the parameter info
     * @param context the adapter context with identity information
     * @return the parameter expression for the repository call
     */
    private String convertIdentifierParam(AdapterMethodSpec.ParameterInfo param, AdapterContext context) {
        if (!param.isIdentity()) {
            return param.name();
        }

        // Check if this is the aggregate's own identity type
        if (context.hasIdInfo()
                && context.idInfo().isWrapped()
                && param.type().equals(context.idInfo().wrappedType())) {
            // Aggregate's own identity - use mapper.map()
            return context.mapperFieldName() + ".map(" + param.name() + ")";
        }

        // Cross-aggregate identity - use param.value() to extract wrapped value
        return param.name() + ".value()";
    }
}
