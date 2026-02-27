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

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.rest.model.DtoFieldSpec;
import io.hexaglue.plugin.rest.model.RequestDtoSpec;
import io.hexaglue.plugin.rest.model.ValidationKind;
import io.hexaglue.plugin.rest.util.RestAnnotations;
import javax.lang.model.element.Modifier;

/**
 * Generates a Java record for a request DTO from a {@link RequestDtoSpec}.
 *
 * <p>This is a Stage 2 (codegen) class: it performs pure mechanical transformation
 * from spec to JavaPoet TypeSpec, with no business logic.
 *
 * @since 3.1.0
 */
public final class RequestDtoCodegen {

    private RequestDtoCodegen() {
        /* utility class */
    }

    /**
     * Generates a TypeSpec for the request DTO record.
     *
     * @param spec the request DTO specification
     * @return the JavaPoet TypeSpec
     */
    public static TypeSpec generate(RequestDtoSpec spec) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder();

        for (DtoFieldSpec field : spec.fields()) {
            ParameterSpec.Builder param = ParameterSpec.builder(field.javaType(), field.fieldName());
            addValidationAnnotation(param, field.validation());
            constructor.addParameter(param.build());
        }

        return TypeSpec.recordBuilder(spec.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RestAnnotations.generated())
                .recordConstructor(constructor.build())
                .build();
    }

    private static void addValidationAnnotation(ParameterSpec.Builder param, ValidationKind validation) {
        switch (validation) {
            case NOT_NULL -> param.addAnnotation(RestAnnotations.notNull());
            case NOT_BLANK -> param.addAnnotation(RestAnnotations.notBlank());
            case NOT_EMPTY -> param.addAnnotation(RestAnnotations.notEmpty());
            case POSITIVE -> param.addAnnotation(RestAnnotations.positive());
            case EMAIL -> {
                param.addAnnotation(RestAnnotations.notBlank());
                param.addAnnotation(RestAnnotations.email());
            }
            case NONE -> {
                /* no annotation */
            }
        }
    }
}
