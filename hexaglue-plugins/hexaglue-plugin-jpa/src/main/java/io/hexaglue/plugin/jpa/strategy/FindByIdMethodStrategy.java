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
 * Strategy for generating FIND_BY_ID method implementations.
 *
 * <p>This strategy handles repository findById operations that retrieve a single
 * entity by its identifier. It uses the MethodKind classification from the SPI
 * and properly handles wrapped vs unwrapped identity types.
 *
 * <h3>Generated Code Pattern (Wrapped ID):</h3>
 * <pre>{@code
 * @Override
 * public Optional<Order> findById(OrderId orderId) {
 *     return repository.findById(mapper.map(orderId)).map(mapper::toDomain);
 * }
 * }</pre>
 *
 * <h3>Generated Code Pattern (Unwrapped ID):</h3>
 * <pre>{@code
 * @Override
 * public Optional<Order> findById(UUID id) {
 *     return repository.findById(id).map(mapper::toDomain);
 * }
 * }</pre>
 *
 * <h3>Generated Code Pattern (Direct Return - nullable):</h3>
 * <pre>{@code
 * @Override
 * public Order getById(UUID id) {
 *     return repository.findById(id).map(mapper::toDomain).orElse(null);
 * }
 * }</pre>
 *
 * <h3>Supported MethodKinds:</h3>
 * <ul>
 *   <li>{@link MethodKind#FIND_BY_ID} - findById(id), getById(id), loadById(id)</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class FindByIdMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        // Use MethodKind from SPI instead of local pattern inference
        return method.kind() == MethodKind.FIND_BY_ID;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        if (!method.hasParameters()) {
            throw new IllegalArgumentException("FIND_BY_ID method must have at least one parameter: " + method.name());
        }

        AdapterMethodSpec.ParameterInfo idParam = method.firstParameter()
                .orElseThrow(() -> new IllegalArgumentException("FIND_BY_ID method requires an ID parameter"));

        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(method.returnType());

        // Add parameters
        for (AdapterMethodSpec.ParameterInfo param : method.parameters()) {
            builder.addParameter(
                    ParameterSpec.builder(param.type(), param.name()).build());
        }

        // Generate method body with correct ID handling
        String idExpression = generateIdExpression(idParam, context);

        // Check if return type is Optional or direct
        if (method.returnsOptional()) {
            // Optional return: use .map(mapper::toDomain)
            builder.addStatement(
                    "return $L.findById($L).map($L::toDomain)",
                    context.repositoryFieldName(),
                    idExpression,
                    context.mapperFieldName());
        } else {
            // Direct return: use .map(mapper::toDomain).orElse(null)
            builder.addStatement(
                    "return $L.findById($L).map($L::toDomain).orElse(null)",
                    context.repositoryFieldName(),
                    idExpression,
                    context.mapperFieldName());
        }

        return builder.build();
    }

    /**
     * Generates the expression to obtain the unwrapped ID value for repository operations.
     *
     * <p>This method uses information from both the parameter (isIdentity) and the context
     * (IdInfo) to determine whether to use mapper conversion:
     * <ul>
     *   <li>Wrapped ID parameter (e.g., TaskId) with isIdentity=true and context.hasWrappedId()
     *       - converts: {@code mapper.map(taskId)}</li>
     *   <li>Primitive ID parameter (e.g., UUID) with isIdentity=true and !context.hasWrappedId()
     *       - uses directly: {@code id}</li>
     *   <li>Non-identity parameter - uses directly: {@code param}</li>
     * </ul>
     *
     * @param param the ID parameter
     * @param context the adapter context with IdInfo
     * @return the expression to get the unwrapped ID
     */
    private String generateIdExpression(AdapterMethodSpec.ParameterInfo param, AdapterContext context) {
        // If the parameter is marked as identity AND the context indicates a wrapped ID,
        // we need to use mapper.map() to unwrap
        if (param.isIdentity() && context.hasWrappedId()) {
            return String.format("%s.map(%s)", context.mapperFieldName(), param.name());
        }
        // Otherwise, use the parameter directly (unwrapped ID or non-identity parameter)
        return param.name();
    }
}
