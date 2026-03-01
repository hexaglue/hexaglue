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

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.rest.model.DtoFieldSpec;
import io.hexaglue.plugin.rest.model.ProjectionKind;
import io.hexaglue.plugin.rest.model.ResponseDtoSpec;
import io.hexaglue.plugin.rest.model.ValidationKind;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResponseDtoCodegen}.
 *
 * @since 6.0.0
 */
@DisplayName("ResponseDtoCodegen")
class ResponseDtoCodegenTest {

    private static final String PACKAGE = "com.example.api.dto";
    private static final ClassName DOMAIN_TYPE = ClassName.get("com.example.domain", "Order");

    @Nested
    @DisplayName("VALUE_OBJECT_FLATTEN null-safety")
    class ValueObjectFlattenNullSafety {

        @Test
        @DisplayName("should generate null-check for VALUE_OBJECT_FLATTEN with nested accessor")
        void shouldGenerateNullCheckForFlattenedValueObject() {
            DtoFieldSpec flattenedField = new DtoFieldSpec(
                    "street",
                    ClassName.get(String.class),
                    "billingAddress",
                    "getBillingAddress().street()",
                    ValidationKind.NONE,
                    ProjectionKind.VALUE_OBJECT_FLATTEN);

            ResponseDtoSpec spec =
                    new ResponseDtoSpec("OrderResponse", PACKAGE, List.of(flattenedField), DOMAIN_TYPE, "Order");

            TypeSpec generated = ResponseDtoCodegen.generate(spec);
            String code = generated.toString();

            assertThat(code)
                    .contains("source.getBillingAddress() != null ? source.getBillingAddress().street() : null");
        }

        @Test
        @DisplayName("should not add null-check for DIRECT fields")
        void shouldNotAddNullCheckForDirectFields() {
            DtoFieldSpec directField = new DtoFieldSpec(
                    "name",
                    ClassName.get(String.class),
                    "name",
                    "getName()",
                    ValidationKind.NONE,
                    ProjectionKind.DIRECT);

            ResponseDtoSpec spec =
                    new ResponseDtoSpec("OrderResponse", PACKAGE, List.of(directField), DOMAIN_TYPE, "Order");

            TypeSpec generated = ResponseDtoCodegen.generate(spec);
            String code = generated.toString();

            assertThat(code).contains("source.getName()");
            assertThat(code).doesNotContain("!= null");
        }

        @Test
        @DisplayName("should generate null-check for IDENTITY_UNWRAP fields")
        void shouldGenerateNullCheckForIdentityUnwrap() {
            DtoFieldSpec unwrapField = new DtoFieldSpec(
                    "id",
                    ClassName.get("java.util", "UUID"),
                    "orderId",
                    "orderId().value()",
                    ValidationKind.NONE,
                    ProjectionKind.IDENTITY_UNWRAP);

            ResponseDtoSpec spec =
                    new ResponseDtoSpec("OrderResponse", PACKAGE, List.of(unwrapField), DOMAIN_TYPE, "Order");

            TypeSpec generated = ResponseDtoCodegen.generate(spec);
            String code = generated.toString();

            assertThat(code).contains("source.orderId() != null ? source.orderId().value() : null");
        }
    }
}
