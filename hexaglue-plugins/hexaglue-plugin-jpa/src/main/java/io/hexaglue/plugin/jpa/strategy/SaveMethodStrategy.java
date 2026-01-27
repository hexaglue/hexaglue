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
 * Strategy for generating SAVE method implementations.
 *
 * <p>This strategy handles repository save operations that persist domain
 * objects to the database. It uses the MethodKind classification from the SPI
 * and generates the three-step pattern:
 * <ol>
 *   <li>Convert domain object to entity using mapper</li>
 *   <li>Save entity using repository</li>
 *   <li>Convert saved entity back to domain object</li>
 * </ol>
 *
 * <h3>Generated Code Pattern:</h3>
 * <pre>{@code
 * @Override
 * public Order save(Order domain) {
 *     var entity = mapper.toEntity(domain);
 *     var saved = repository.save(entity);
 *     return mapper.toDomain(saved);
 * }
 * }</pre>
 *
 * <h3>Supported MethodKinds:</h3>
 * <ul>
 *   <li>{@link MethodKind#SAVE} - save(domain), create(domain), etc.</li>
 *   <li>{@link MethodKind#SAVE_ALL} - saveAll(domains)</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class SaveMethodStrategy implements MethodBodyStrategy {

    @Override
    public boolean supports(AdapterMethodSpec method) {
        // Use MethodKind from SPI instead of local pattern inference
        return method.kind() == MethodKind.SAVE || method.kind() == MethodKind.SAVE_ALL;
    }

    @Override
    public MethodSpec generate(AdapterMethodSpec method, AdapterContext context) {
        if (!method.hasParameters()) {
            throw new IllegalArgumentException("SAVE method must have at least one parameter: " + method.name());
        }

        AdapterMethodSpec.ParameterInfo firstParam = method.firstParameter()
                .orElseThrow(() -> new IllegalArgumentException("SAVE method requires a parameter"));

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
        // var entity = mapper.toEntity(domain);
        builder.addStatement("var entity = $L.toEntity($L)", context.mapperFieldName(), firstParam.name());

        // var saved = repository.save(entity);
        builder.addStatement("var saved = $L.save(entity)", context.repositoryFieldName());

        // return mapper.toDomain(saved);
        if (method.hasReturnValue()) {
            builder.addStatement("return $L.toDomain(saved)", context.mapperFieldName());
        }

        return builder.build();
    }
}
