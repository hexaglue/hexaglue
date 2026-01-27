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
import io.hexaglue.arch.model.ir.MethodKind;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import javax.lang.model.element.Modifier;

/**
 * Strategy for generating DELETE method implementations.
 *
 * <p>This strategy handles repository delete operations that remove entities
 * from the database. It uses the MethodKind classification from the SPI
 * and properly handles wrapped vs unwrapped identity types.
 *
 * <h3>Generated Code Pattern (Wrapped ID):</h3>
 * <pre>{@code
 * @Override
 * public void deleteById(OrderId orderId) {
 *     repository.deleteById(mapper.map(orderId));
 * }
 * }</pre>
 *
 * <h3>Generated Code Pattern (Unwrapped ID):</h3>
 * <pre>{@code
 * @Override
 * public void deleteById(UUID id) {
 *     repository.deleteById(id);
 * }
 * }</pre>
 *
 * <h3>Supported MethodKinds:</h3>
 * <ul>
 *   <li>{@link MethodKind#DELETE_BY_ID} - deleteById(id), removeById(id)</li>
 *   <li>{@link MethodKind#DELETE_ALL} - deleteAll()</li>
 *   <li>{@link MethodKind#DELETE_BY_PROPERTY} - deleteByStatus(status)</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class DeleteMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        // Use MethodKind from SPI instead of local pattern inference
        return method.kind() == MethodKind.DELETE_BY_ID
                || method.kind() == MethodKind.DELETE_ALL
                || method.kind() == MethodKind.DELETE_BY_PROPERTY;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(method.returnType());

        // Add parameters
        for (AdapterMethodSpec.ParameterInfo param : method.parameters()) {
            builder.addParameter(
                    ParameterSpec.builder(param.type(), param.name()).build());
        }

        // Handle different delete types
        if (method.kind() == MethodKind.DELETE_ALL) {
            builder.addStatement("$L.deleteAll()", context.repositoryFieldName());
        } else if (method.kind() == MethodKind.DELETE_BY_PROPERTY) {
            // For property-based delete, delegate directly to repository
            AdapterMethodSpec.ParameterInfo firstParam = method.firstParameter()
                    .orElseThrow(() -> new IllegalArgumentException("DELETE_BY_PROPERTY requires a parameter"));
            builder.addStatement("$L.$L($L)", context.repositoryFieldName(), method.name(), firstParam.name());
        } else {
            // DELETE_BY_ID - handle wrapped vs unwrapped ID
            if (!method.hasParameters()) {
                throw new IllegalArgumentException(
                        "DELETE_BY_ID method must have at least one parameter: " + method.name());
            }

            AdapterMethodSpec.ParameterInfo firstParam = method.firstParameter()
                    .orElseThrow(() -> new IllegalArgumentException("DELETE method requires a parameter"));

            String idExpression = generateIdExpression(firstParam, context);
            builder.addStatement("$L.deleteById($L)", context.repositoryFieldName(), idExpression);
        }

        return builder.build();
    }

    /**
     * Generates the expression to obtain the unwrapped ID value for repository operations.
     *
     * <p>This method uses information from both the parameter (isIdentity) and the context
     * (IdInfo) to determine whether to use mapper conversion:
     * <ul>
     *   <li>Domain object parameter (e.g., Task) - extracts ID: {@code task.id()}</li>
     *   <li>Wrapped ID parameter with isIdentity=true and context.hasWrappedId()
     *       - converts: {@code mapper.map(taskId)}</li>
     *   <li>Primitive ID parameter - uses directly: {@code id}</li>
     * </ul>
     *
     * @param param the method parameter
     * @param context the adapter context
     * @return the expression to get the unwrapped ID
     */
    private String generateIdExpression(AdapterMethodSpec.ParameterInfo param, AdapterContext context) {
        // If parameter type is the domain class, extract ID and convert
        if (param.type().equals(context.domainClass())) {
            String idAccessor = context.idAccessorMethod();
            if (context.hasWrappedId()) {
                return String.format("%s.map(%s.%s())", context.mapperFieldName(), param.name(), idAccessor);
            }
            return String.format("%s.%s()", param.name(), idAccessor);
        }

        // If the parameter is marked as identity AND the context indicates a wrapped ID,
        // we need to use mapper.map() to unwrap
        if (param.isIdentity() && context.hasWrappedId()) {
            return String.format("%s.map(%s)", context.mapperFieldName(), param.name());
        }

        // Otherwise, use the parameter directly (unwrapped ID)
        return param.name();
    }
}
