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

package io.hexaglue.plugin.rest.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.model.DtoFieldSpec;
import io.hexaglue.plugin.rest.model.ProjectionKind;
import io.hexaglue.plugin.rest.model.ValidationKind;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DtoFieldMapper}.
 */
@DisplayName("DtoFieldMapper")
class DtoFieldMapperTest {

    private final RestConfig config = RestConfig.defaults();

    @Nested
    @DisplayName("Request mapping")
    class RequestMapping {

        @Test
        @DisplayName("should unwrap identifier to wrapped type with @NotNull")
        void shouldUnwrapIdentifierToWrappedType() {
            Identifier customerId = TestUseCaseFactory.identifier("com.acme.CustomerId", "java.lang.Long");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(customerId);
            Parameter param = Parameter.of("customerId", TypeRef.of("com.acme.CustomerId"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForRequest(param, domainIndex, config);

            assertThat(fields).hasSize(1);
            DtoFieldSpec field = fields.get(0);
            assertThat(field.fieldName()).isEqualTo("customerId");
            assertThat(field.javaType()).isEqualTo(ClassName.get("java.lang", "Long"));
            assertThat(field.validation()).isEqualTo(ValidationKind.NOT_NULL);
            assertThat(field.projectionKind()).isEqualTo(ProjectionKind.IDENTITY_UNWRAP);
        }

        @Test
        @DisplayName("should unwrap single-field VO with @NotBlank for String")
        void shouldUnwrapSingleFieldVoWithNotBlank() {
            ValueObject email =
                    TestUseCaseFactory.singleFieldValueObject("com.acme.Email", "value", "java.lang.String");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(email);
            Parameter param = Parameter.of("email", TypeRef.of("com.acme.Email"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForRequest(param, domainIndex, config);

            assertThat(fields).hasSize(1);
            DtoFieldSpec field = fields.get(0);
            assertThat(field.fieldName()).isEqualTo("email");
            assertThat(field.javaType()).isEqualTo(ClassName.get(String.class));
            assertThat(field.validation()).isEqualTo(ValidationKind.NOT_BLANK);
            assertThat(field.projectionKind()).isEqualTo(ProjectionKind.IDENTITY_UNWRAP);
        }

        @Test
        @DisplayName("should flatten multi-field VO")
        void shouldFlattenMultiFieldVo() {
            ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                    "com.acme.Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(money);
            Parameter param = Parameter.of("amount", TypeRef.of("com.acme.Money"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForRequest(param, domainIndex, config);

            assertThat(fields).hasSize(2);
            assertThat(fields.get(0).fieldName()).isEqualTo("amount");
            assertThat(fields.get(0).javaType()).isEqualTo(ClassName.get("java.math", "BigDecimal"));
            assertThat(fields.get(0).validation()).isEqualTo(ValidationKind.NOT_NULL);
            assertThat(fields.get(0).projectionKind()).isEqualTo(ProjectionKind.VALUE_OBJECT_FLATTEN);

            assertThat(fields.get(1).fieldName()).isEqualTo("currency");
            assertThat(fields.get(1).javaType()).isEqualTo(ClassName.get(String.class));
            assertThat(fields.get(1).validation()).isEqualTo(ValidationKind.NOT_BLANK);
            assertThat(fields.get(1).projectionKind()).isEqualTo(ProjectionKind.VALUE_OBJECT_FLATTEN);
        }

        @Test
        @DisplayName("should map enum with @NotNull")
        void shouldMapEnumWithNotNull() {
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();
            Parameter param = Parameter.of("type", TypeRef.of("com.acme.AccountType"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForRequest(param, domainIndex, config);

            assertThat(fields).hasSize(1);
            DtoFieldSpec field = fields.get(0);
            assertThat(field.fieldName()).isEqualTo("type");
            assertThat(field.javaType()).isEqualTo(ClassName.get("com.acme", "AccountType"));
            assertThat(field.validation()).isEqualTo(ValidationKind.NOT_NULL);
            assertThat(field.projectionKind()).isEqualTo(ProjectionKind.DIRECT);
        }

        @Test
        @DisplayName("should map String with @NotBlank")
        void shouldMapStringWithNotBlank() {
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();
            Parameter param = Parameter.of("accountNumber", TypeRef.of("java.lang.String"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForRequest(param, domainIndex, config);

            assertThat(fields).hasSize(1);
            DtoFieldSpec field = fields.get(0);
            assertThat(field.fieldName()).isEqualTo("accountNumber");
            assertThat(field.javaType()).isEqualTo(ClassName.get(String.class));
            assertThat(field.validation()).isEqualTo(ValidationKind.NOT_BLANK);
            assertThat(field.projectionKind()).isEqualTo(ProjectionKind.DIRECT);
        }

        @Test
        @DisplayName("should map primitive with no validation")
        void shouldMapPrimitiveWithNoValidation() {
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();
            Parameter param = Parameter.of("count", TypeRef.of("int"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForRequest(param, domainIndex, config);

            assertThat(fields).hasSize(1);
            DtoFieldSpec field = fields.get(0);
            assertThat(field.fieldName()).isEqualTo("count");
            assertThat(field.javaType()).isEqualTo(TypeName.INT);
            assertThat(field.validation()).isEqualTo(ValidationKind.NONE);
            assertThat(field.projectionKind()).isEqualTo(ProjectionKind.DIRECT);
        }
    }
}
