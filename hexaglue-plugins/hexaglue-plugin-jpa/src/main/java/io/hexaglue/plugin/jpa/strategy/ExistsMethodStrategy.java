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
 * Strategy for generating EXISTS method implementations.
 *
 * <p>This strategy handles repository exists operations that check for the
 * presence of an entity by its identifier. It generates code that delegates
 * directly to the repository's existsById method.
 *
 * <h3>Generated Code Pattern:</h3>
 * <pre>{@code
 * @Override
 * public boolean existsById(UUID id) {
 *     return repository.existsById(id);
 * }
 * }</pre>
 *
 * <h3>Supported Method Names:</h3>
 * <ul>
 *   <li>exists(id)</li>
 *   <li>existsById(id)</li>
 *   <li>contains(id)</li>
 * </ul>
 *
 * <h3>Return Type Requirements:</h3>
 * <p>EXISTS methods must return {@code boolean}. If the declared return type
 * is different, the generated code may fail to compile.
 *
 * @since 2.0.0
 */
public final class ExistsMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        return method.pattern() == MethodPattern.EXISTS;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        if (!method.hasParameters()) {
            throw new IllegalArgumentException("EXISTS method must have at least one parameter: " + method.name());
        }

        AdapterMethodSpec.ParameterInfo idParam = method.firstParameter()
                .orElseThrow(() -> new IllegalArgumentException("EXISTS method requires an ID parameter"));

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
        // return repository.existsById(id);
        builder.addStatement("return $L.existsById($L)", context.repositoryFieldName(), idParam.name());

        return builder.build();
    }
}
