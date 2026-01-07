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
        // repository.deleteById(id);
        builder.addStatement("$L.deleteById($L)", context.repositoryFieldName(), firstParam.name());

        return builder.build();
    }
}
