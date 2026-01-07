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
 * Strategy for generating FIND_ALL method implementations.
 *
 * <p>This strategy handles repository findAll operations that retrieve all
 * entities of a type. It generates code that:
 * <ol>
 *   <li>Calls repository.findAll()</li>
 *   <li>Streams the results</li>
 *   <li>Maps each entity to domain using mapper::toDomain</li>
 *   <li>Collects to an immutable List</li>
 * </ol>
 *
 * <h3>Generated Code Pattern:</h3>
 * <pre>{@code
 * @Override
 * public List<Order> findAll() {
 *     return repository.findAll().stream()
 *         .map(mapper::toDomain)
 *         .toList();
 * }
 * }</pre>
 *
 * <h3>Supported Method Names:</h3>
 * <ul>
 *   <li>findAll()</li>
 *   <li>getAll()</li>
 *   <li>list()</li>
 *   <li>listAll()</li>
 *   <li>all()</li>
 * </ul>
 *
 * <h3>Return Type Requirements:</h3>
 * <p>This strategy expects the method to return {@code List<DomainType>} or
 * {@code Set<DomainType>}. The implementation always uses {@code .toList()}
 * regardless of the declared return type.
 *
 * @since 2.0.0
 */
public final class FindAllMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        return method.pattern() == MethodPattern.FIND_ALL;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(method.returnType());

        // Add parameters if any (though FIND_ALL typically has none)
        for (AdapterMethodSpec.ParameterInfo param : method.parameters()) {
            builder.addParameter(
                    ParameterSpec.builder(param.type(), param.name()).build());
        }

        // Generate method body
        // return repository.findAll().stream().map(mapper::toDomain).toList();
        builder.addStatement(
                "return $L.findAll().stream().map($L::toDomain).toList()",
                context.repositoryFieldName(),
                context.mapperFieldName());

        return builder.build();
    }
}
