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

package io.hexaglue.plugin.rest.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.rest.model.DtoFieldSpec;
import io.hexaglue.plugin.rest.model.ResponseDtoSpec;
import io.hexaglue.plugin.rest.util.RestAnnotations;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/**
 * Generates a Java record for a response DTO from a {@link ResponseDtoSpec}.
 *
 * <p>The generated record includes a static {@code from(DomainType source)} factory method
 * that projects domain fields to DTO fields with null-safety for unwrapped types.
 *
 * <p>This is a Stage 2 (codegen) class: it performs pure mechanical transformation
 * from spec to JavaPoet TypeSpec, with no business logic.
 *
 * @since 3.1.0
 */
public final class ResponseDtoCodegen {

    private ResponseDtoCodegen() {
        /* utility class */
    }

    /**
     * Generates a TypeSpec for the response DTO record with a {@code from()} factory.
     *
     * @param spec the response DTO specification
     * @return the JavaPoet TypeSpec
     */
    public static TypeSpec generate(ResponseDtoSpec spec) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder();
        for (DtoFieldSpec field : spec.fields()) {
            constructor.addParameter(
                    ParameterSpec.builder(field.javaType(), field.fieldName()).build());
        }

        return TypeSpec.recordBuilder(spec.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RestAnnotations.generated())
                .recordConstructor(constructor.build())
                .addMethod(generateFromMethod(spec))
                .build();
    }

    private static MethodSpec generateFromMethod(ResponseDtoSpec spec) {
        ClassName returnType = ClassName.get(spec.packageName(), spec.className());

        MethodSpec.Builder method = MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnType)
                .addParameter(spec.domainType(), "source");

        CodeBlock args = spec.fields().stream()
                .map(ResponseDtoCodegen::generateFieldExpression)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> CodeBlock.join(list, ",\n")));

        method.addStatement("return new $L(\n$>$>$L$<$<)", spec.className(), args);

        return method.build();
    }

    private static CodeBlock generateFieldExpression(DtoFieldSpec field) {
        return switch (field.projectionKind()) {
            case IDENTITY_UNWRAP, AGGREGATE_REFERENCE -> {
                // Derive the null-check accessor from the full accessor chain by stripping
                // the terminal .value() call. This handles both record-style accessors
                // (e.g., "id().value()" → "id()") and JavaBean-style accessors
                // (e.g., "getId().value()" → "getId()"), so the null-check always uses
                // the same style as the accessor chain itself.
                String nullCheckAccessor = field.accessorChain().replace(".value()", "");
                yield CodeBlock.of("source.$L != null ? source.$L : null", nullCheckAccessor, field.accessorChain());
            }
            case DIRECT, VALUE_OBJECT_FLATTEN -> CodeBlock.of("source.$L", field.accessorChain());
            default -> CodeBlock.of("source.$L", field.accessorChain());
        };
    }
}
