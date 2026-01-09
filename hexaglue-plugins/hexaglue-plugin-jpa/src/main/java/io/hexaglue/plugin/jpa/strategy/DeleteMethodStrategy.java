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
 * Strategy for generating DELETE method implementations.
 *
 * <p>This strategy handles repository delete operations that remove entities
 * from the database. It generates code that delegates directly to the repository's
 * deleteById method.
 *
 * <h3>Generated Code Pattern:</h3>
 * <pre>{@code
 * @Override
 * public void deleteById(UUID id) {
 *     repository.deleteById(id);
 * }
 * }</pre>
 *
 * <h3>Supported Method Names:</h3>
 * <ul>
 *   <li>delete(id) or delete(domain)</li>
 *   <li>deleteById(id)</li>
 *   <li>remove(id) or remove(domain)</li>
 *   <li>removeById(id)</li>
 * </ul>
 *
 * <h3>Return Type:</h3>
 * <p>DELETE methods typically return void. If a return value is expected,
 * this strategy will not generate a return statement, which may cause
 * compilation errors in the generated code.
 *
 * @since 2.0.0
 */
public final class DeleteMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        return method.pattern() == MethodPattern.DELETE;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        if (!method.hasParameters()) {
            throw new IllegalArgumentException("DELETE method must have at least one parameter: " + method.name());
        }

        AdapterMethodSpec.ParameterInfo firstParam = method.firstParameter()
                .orElseThrow(() -> new IllegalArgumentException("DELETE method requires a parameter"));

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
        // If parameter is domain object, extract and convert ID: mapper.map(task.getId())
        // If parameter is ID type, convert it: mapper.map(taskId)
        // Otherwise use parameter directly: id
        String idExpression = generateIdExpression(firstParam, context);
        builder.addStatement("$L.deleteById($L)", context.repositoryFieldName(), idExpression);

        return builder.build();
    }

    /**
     * Generates the expression to obtain the unwrapped ID value for repository operations.
     *
     * <p>This method handles three cases:
     * <ul>
     *   <li>Domain object parameter (e.g., Task) - extracts ID and converts: {@code mapper.map(task.getId())}</li>
     *   <li>Wrapped ID parameter (e.g., TaskId) - converts directly: {@code mapper.map(taskId)}</li>
     *   <li>Primitive ID parameter (e.g., UUID) - uses directly: {@code id}</li>
     * </ul>
     *
     * @param param the method parameter
     * @param context the adapter context
     * @return the expression to get the unwrapped ID
     */
    private String generateIdExpression(AdapterMethodSpec.ParameterInfo param, AdapterContext context) {
        // If parameter type is the domain class, extract ID: task.getId()
        if (param.type().equals(context.domainClass())) {
            return String.format("%s.map(%s.getId())", context.mapperFieldName(), param.name());
        }

        // Otherwise assume it's an ID type and convert it: taskId
        // The mapper's map() method will handle wrapped -> unwrapped conversion
        return String.format("%s.map(%s)", context.mapperFieldName(), param.name());
    }
}
