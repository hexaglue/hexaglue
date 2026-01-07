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
 * Strategy for generating COUNT method implementations.
 *
 * <p>This strategy handles repository count operations that return the total
 * number of entities. It generates code that delegates directly to the
 * repository's count method.
 *
 * <h3>Generated Code Pattern:</h3>
 * <pre>{@code
 * @Override
 * public long count() {
 *     return repository.count();
 * }
 * }</pre>
 *
 * <h3>Supported Method Names:</h3>
 * <ul>
 *   <li>count()</li>
 *   <li>countAll()</li>
 * </ul>
 *
 * <h3>Return Type Requirements:</h3>
 * <p>COUNT methods typically return {@code long}. If the declared return type
 * is different, the generated code may fail to compile or require casting.
 *
 * @since 2.0.0
 */
public final class CountMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        return method.pattern() == MethodPattern.COUNT;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(method.returnType());

        // Add parameters if any (though COUNT typically has none)
        for (AdapterMethodSpec.ParameterInfo param : method.parameters()) {
            builder.addParameter(
                    ParameterSpec.builder(param.type(), param.name()).build());
        }

        // Generate method body
        // return repository.count();
        builder.addStatement("return $L.count()", context.repositoryFieldName());

        return builder.build();
    }
}
