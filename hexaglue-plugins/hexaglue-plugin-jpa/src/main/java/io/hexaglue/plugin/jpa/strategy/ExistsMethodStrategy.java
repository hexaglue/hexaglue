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
 * Strategy for generating EXISTS_BY_ID method implementations.
 *
 * <p>This strategy handles repository exists operations that check for the
 * presence of an entity by its identifier. It uses the MethodKind classification
 * from the SPI and properly handles wrapped vs unwrapped identity types.
 *
 * <h3>Generated Code Pattern (Wrapped ID):</h3>
 * <pre>{@code
 * @Override
 * public boolean existsById(OrderId orderId) {
 *     return repository.existsById(mapper.map(orderId));
 * }
 * }</pre>
 *
 * <h3>Generated Code Pattern (Unwrapped ID):</h3>
 * <pre>{@code
 * @Override
 * public boolean existsById(UUID id) {
 *     return repository.existsById(id);
 * }
 * }</pre>
 *
 * <h3>Supported MethodKinds:</h3>
 * <ul>
 *   <li>{@link MethodKind#EXISTS_BY_ID} - existsById(id), exists(id)</li>
 * </ul>
 *
 * <p>Note: EXISTS_BY_PROPERTY is handled by {@link ExistsByPropertyMethodStrategy}.
 *
 * @since 3.0.0
 */
public final class ExistsMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        // Only handle EXISTS_BY_ID - EXISTS_BY_PROPERTY is handled by ExistsByPropertyMethodStrategy
        return method.kind() == MethodKind.EXISTS_BY_ID;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        if (!method.hasParameters()) {
            throw new IllegalArgumentException(
                    "EXISTS_BY_ID method must have at least one parameter: " + method.name());
        }

        AdapterMethodSpec.ParameterInfo idParam = method.firstParameter()
                .orElseThrow(() -> new IllegalArgumentException("EXISTS_BY_ID method requires an ID parameter"));

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
        builder.addStatement("return $L.existsById($L)", context.repositoryFieldName(), idExpression);

        return builder.build();
    }

    /**
     * Generates the expression to obtain the unwrapped ID value for repository operations.
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
        // Otherwise, use the parameter directly (unwrapped ID)
        return param.name();
    }
}
