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
import io.hexaglue.plugin.jpa.model.MethodPattern;
import javax.lang.model.element.Modifier;

/**
 * Strategy for generating FIND_BY_ID method implementations.
 *
 * <p>This strategy handles repository findById operations that retrieve a single
 * entity by its identifier. It generates code that:
 * <ol>
 *   <li>Calls repository.findById(id)</li>
 *   <li>Maps the Optional result using mapper::toDomain</li>
 * </ol>
 *
 * <h3>Generated Code Pattern:</h3>
 * <pre>{@code
 * @Override
 * public Optional<Order> findById(UUID id) {
 *     return repository.findById(id).map(mapper::toDomain);
 * }
 * }</pre>
 *
 * <h3>Supported Method Names:</h3>
 * <ul>
 *   <li>findById(id)</li>
 *   <li>getById(id)</li>
 *   <li>loadById(id)</li>
 *   <li>findOne(id)</li>
 *   <li>getOne(id)</li>
 * </ul>
 *
 * <h3>Return Type Requirements:</h3>
 * <p>This strategy expects the method to return {@code Optional<DomainType>}.
 * If the return type is not Optional, it will generate the code anyway but may
 * produce a compilation error in the generated class.
 *
 * @since 2.0.0
 */
public final class FindByIdMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        return method.pattern() == MethodPattern.FIND_BY_ID;
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

        // Generate method body
        // Convert wrapped ID if necessary: mapper.map(taskId)
        // return repository.findById(mapper.map(taskId)).map(mapper::toDomain);
        String idExpression = generateIdExpression(idParam, context);
        builder.addStatement(
                "return $L.findById($L).map($L::toDomain)",
                context.repositoryFieldName(),
                idExpression,
                context.mapperFieldName());

        return builder.build();
    }

    /**
     * Generates the expression to obtain the unwrapped ID value for repository operations.
     *
     * <p>This method handles wrapped ID types by using the mapper's conversion method:
     * <ul>
     *   <li>Wrapped ID parameter (e.g., TaskId) - converts: {@code mapper.map(taskId)}</li>
     *   <li>Primitive ID parameter (e.g., UUID) - uses directly: {@code id}</li>
     * </ul>
     *
     * @param param the ID parameter
     * @param context the adapter context
     * @return the expression to get the unwrapped ID
     */
    private String generateIdExpression(AdapterMethodSpec.ParameterInfo param, AdapterContext context) {
        // Convert ID using mapper - it will handle wrapped types
        // For primitive types, the mapper won't have a conversion method, but we need
        // to detect this. For now, always use the mapper.
        return String.format("%s.map(%s)", context.mapperFieldName(), param.name());
    }
}
