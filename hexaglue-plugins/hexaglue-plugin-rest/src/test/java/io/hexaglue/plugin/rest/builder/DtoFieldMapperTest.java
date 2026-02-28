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
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.model.DtoFieldSpec;
import io.hexaglue.plugin.rest.model.ProjectionKind;
import io.hexaglue.plugin.rest.model.ValidationKind;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Set;
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

    @Nested
    @DisplayName("Response mapping")
    class ResponseMapping {

        /** Record parent structure: uses record-style accessors (fieldName()). */
        private final TypeStructure recordParent =
                TypeStructure.builder(TypeNature.RECORD).build();

        /** Class parent structure: uses JavaBean-style getters (getFieldName()). */
        private final TypeStructure classParent =
                TypeStructure.builder(TypeNature.CLASS).build();

        @Test
        @DisplayName("should unwrap identity field to id for record parent")
        void shouldUnwrapIdentityFieldToId() {
            Identifier accountId = TestUseCaseFactory.identifier("com.acme.AccountId", "java.lang.Long");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(accountId);
            Field field = Field.builder("id", TypeRef.of("com.acme.AccountId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, recordParent);

            assertThat(fields).hasSize(1);
            DtoFieldSpec spec = fields.get(0);
            assertThat(spec.fieldName()).isEqualTo("id");
            assertThat(spec.javaType()).isEqualTo(ClassName.get("java.lang", "Long"));
            assertThat(spec.accessorChain()).isEqualTo("id().value()");
            assertThat(spec.validation()).isEqualTo(ValidationKind.NONE);
            assertThat(spec.projectionKind()).isEqualTo(ProjectionKind.IDENTITY_UNWRAP);
        }

        @Test
        @DisplayName("should use getter for identity field in class parent")
        void shouldUseGetterForIdentityFieldInClassParent() {
            Identifier accountId = TestUseCaseFactory.identifier("com.acme.AccountId", "java.lang.Long");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(accountId);
            Field field = Field.builder("id", TypeRef.of("com.acme.AccountId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, classParent);

            assertThat(fields).hasSize(1);
            assertThat(fields.get(0).accessorChain()).isEqualTo("getId().value()");
        }

        @Test
        @DisplayName("should map direct string field for record parent")
        void shouldMapDirectStringField() {
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();
            Field field = Field.of("accountNumber", TypeRef.of("java.lang.String"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, recordParent);

            assertThat(fields).hasSize(1);
            DtoFieldSpec spec = fields.get(0);
            assertThat(spec.fieldName()).isEqualTo("accountNumber");
            assertThat(spec.javaType()).isEqualTo(ClassName.get(String.class));
            assertThat(spec.accessorChain()).isEqualTo("accountNumber()");
            assertThat(spec.validation()).isEqualTo(ValidationKind.NONE);
            assertThat(spec.projectionKind()).isEqualTo(ProjectionKind.DIRECT);
        }

        @Test
        @DisplayName("should use getter for direct string field in class parent")
        void shouldUseGetterForDirectStringFieldInClassParent() {
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();
            Field field = Field.of("accountNumber", TypeRef.of("java.lang.String"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, classParent);

            assertThat(fields).hasSize(1);
            assertThat(fields.get(0).accessorChain()).isEqualTo("getAccountNumber()");
        }

        @Test
        @DisplayName("should map direct primitive field for record parent")
        void shouldMapDirectPrimitiveField() {
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();
            Field field = Field.of("active", TypeRef.of("boolean"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, recordParent);

            assertThat(fields).hasSize(1);
            DtoFieldSpec spec = fields.get(0);
            assertThat(spec.fieldName()).isEqualTo("active");
            assertThat(spec.javaType()).isEqualTo(TypeName.BOOLEAN);
            assertThat(spec.accessorChain()).isEqualTo("active()");
            assertThat(spec.validation()).isEqualTo(ValidationKind.NONE);
            assertThat(spec.projectionKind()).isEqualTo(ProjectionKind.DIRECT);
        }

        @Test
        @DisplayName("should use getter for primitive field in class parent")
        void shouldUseGetterForPrimitiveFieldInClassParent() {
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();
            Field field = Field.of("active", TypeRef.of("boolean"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, classParent);

            assertThat(fields).hasSize(1);
            assertThat(fields.get(0).accessorChain()).isEqualTo("getActive()");
        }

        @Test
        @DisplayName("should flatten multi-field VO with prefix for record parent")
        void shouldFlattenMultiFieldVoWithPrefix() {
            ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                    "com.acme.Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(money);
            Field field = Field.of("balance", TypeRef.of("com.acme.Money"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, recordParent);

            assertThat(fields).hasSize(2);
            assertThat(fields.get(0).fieldName()).isEqualTo("balanceAmount");
            assertThat(fields.get(0).javaType()).isEqualTo(ClassName.get("java.math", "BigDecimal"));
            assertThat(fields.get(0).accessorChain()).isEqualTo("balance().amount()");
            assertThat(fields.get(0).projectionKind()).isEqualTo(ProjectionKind.VALUE_OBJECT_FLATTEN);

            assertThat(fields.get(1).fieldName()).isEqualTo("balanceCurrency");
            assertThat(fields.get(1).javaType()).isEqualTo(ClassName.get(String.class));
            assertThat(fields.get(1).accessorChain()).isEqualTo("balance().currency()");
            assertThat(fields.get(1).projectionKind()).isEqualTo(ProjectionKind.VALUE_OBJECT_FLATTEN);
        }

        @Test
        @DisplayName("should use getter prefix for flattened VO in class parent")
        void shouldUseGetterPrefixForFlattenedVoInClassParent() {
            ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                    "com.acme.Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(money);
            Field field = Field.of("balance", TypeRef.of("com.acme.Money"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, classParent);

            assertThat(fields).hasSize(2);
            // Top-level uses getter; VO sub-fields remain record-style (VOs are always records)
            assertThat(fields.get(0).accessorChain()).isEqualTo("getBalance().amount()");
            assertThat(fields.get(1).accessorChain()).isEqualTo("getBalance().currency()");
        }

        @Test
        @DisplayName("should unwrap aggregate reference for record parent")
        void shouldUnwrapAggregateReference() {
            Identifier customerId = TestUseCaseFactory.identifier("com.acme.CustomerId", "java.lang.Long");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(customerId);
            Field field = Field.builder("customerId", TypeRef.of("com.acme.CustomerId"))
                    .roles(Set.of(FieldRole.AGGREGATE_REFERENCE))
                    .build();

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, recordParent);

            assertThat(fields).hasSize(1);
            DtoFieldSpec spec = fields.get(0);
            assertThat(spec.fieldName()).isEqualTo("customerId");
            assertThat(spec.javaType()).isEqualTo(ClassName.get("java.lang", "Long"));
            assertThat(spec.accessorChain()).isEqualTo("customerId().value()");
            assertThat(spec.validation()).isEqualTo(ValidationKind.NONE);
            assertThat(spec.projectionKind()).isEqualTo(ProjectionKind.AGGREGATE_REFERENCE);
        }

        @Test
        @DisplayName("should use getter for aggregate reference in class parent")
        void shouldUseGetterForAggregateReferenceInClassParent() {
            Identifier customerId = TestUseCaseFactory.identifier("com.acme.CustomerId", "java.lang.Long");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(customerId);
            Field field = Field.builder("customerId", TypeRef.of("com.acme.CustomerId"))
                    .roles(Set.of(FieldRole.AGGREGATE_REFERENCE))
                    .build();

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, classParent);

            assertThat(fields).hasSize(1);
            assertThat(fields.get(0).accessorChain()).isEqualTo("getCustomerId().value()");
        }

        @Test
        @DisplayName("should unwrap single-field VO for record parent")
        void shouldUnwrapSingleFieldVo() {
            ValueObject email =
                    TestUseCaseFactory.singleFieldValueObject("com.acme.Email", "value", "java.lang.String");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(email);
            Field field = Field.of("email", TypeRef.of("com.acme.Email"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, recordParent);

            assertThat(fields).hasSize(1);
            DtoFieldSpec spec = fields.get(0);
            assertThat(spec.fieldName()).isEqualTo("email");
            assertThat(spec.javaType()).isEqualTo(ClassName.get(String.class));
            assertThat(spec.accessorChain()).isEqualTo("email().value()");
            assertThat(spec.validation()).isEqualTo(ValidationKind.NONE);
            assertThat(spec.projectionKind()).isEqualTo(ProjectionKind.IDENTITY_UNWRAP);
        }

        @Test
        @DisplayName("should use getter for single-field VO in class parent")
        void shouldUseGetterForSingleFieldVoInClassParent() {
            ValueObject email =
                    TestUseCaseFactory.singleFieldValueObject("com.acme.Email", "value", "java.lang.String");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(email);
            Field field = Field.of("email", TypeRef.of("com.acme.Email"));

            List<DtoFieldSpec> fields = DtoFieldMapper.mapForResponse(field, domainIndex, config, classParent);

            assertThat(fields).hasSize(1);
            assertThat(fields.get(0).accessorChain()).isEqualTo("getEmail().value()");
        }
    }
}
