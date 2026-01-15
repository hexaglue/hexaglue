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
import io.hexaglue.spi.ir.MethodKind;
import javax.lang.model.element.Modifier;

/**
 * Strategy for generating COUNT method implementations.
 *
 * <p>This strategy handles repository count operations that return the total
 * number of entities. It uses the MethodKind classification from the SPI
 * and delegates directly to the repository's count method.
 *
 * <h3>Generated Code Pattern:</h3>
 * <pre>{@code
 * @Override
 * public long count() {
 *     return repository.count();
 * }
 * }</pre>
 *
 * <h3>Supported MethodKinds:</h3>
 * <ul>
 *   <li>{@link MethodKind#COUNT_ALL} - count(), countAll()</li>
 *   <li>{@link MethodKind#COUNT_BY_PROPERTY} - countByStatus(status)</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class CountMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        // Use MethodKind from SPI instead of local pattern inference
        return method.kind() == MethodKind.COUNT_ALL || method.kind() == MethodKind.COUNT_BY_PROPERTY;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(method.returnType());

        // Add parameters if any (though COUNT_ALL typically has none)
        for (AdapterMethodSpec.ParameterInfo param : method.parameters()) {
            builder.addParameter(
                    ParameterSpec.builder(param.type(), param.name()).build());
        }

        // Generate method body
        if (method.kind() == MethodKind.COUNT_BY_PROPERTY) {
            // For property-based count, delegate directly to repository
            AdapterMethodSpec.ParameterInfo firstParam = method.firstParameter()
                    .orElseThrow(() -> new IllegalArgumentException("COUNT_BY_PROPERTY requires a parameter"));
            builder.addStatement("return $L.$L($L)", context.repositoryFieldName(), method.name(), firstParam.name());
        } else {
            // COUNT_ALL
            builder.addStatement("return $L.count()", context.repositoryFieldName());
        }

        return builder.build();
    }
}
