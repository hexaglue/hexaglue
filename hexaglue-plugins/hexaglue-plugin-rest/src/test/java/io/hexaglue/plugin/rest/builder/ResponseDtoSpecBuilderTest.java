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

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.model.ResponseDtoSpec;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResponseDtoSpecBuilder}.
 *
 * @since 3.1.0
 */
@DisplayName("ResponseDtoSpecBuilder")
class ResponseDtoSpecBuilderTest {

    private static final String DTO_PACKAGE = "com.acme.api.dto";
    private final RestConfig config = RestConfig.defaults();

    @Nested
    @DisplayName("DTO generation")
    class DtoGeneration {

        @Test
        @DisplayName("should generate response DTO from aggregate")
        void shouldGenerateResponseDtoFromAggregate() {
            Field idField = Field.builder("id", TypeRef.of("com.acme.core.model.AccountId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            Field accountNumber = Field.of("accountNumber", TypeRef.of("java.lang.String"));
            Field balance = Field.of("balance", TypeRef.of("com.acme.core.model.Money"));
            Field type = Field.of("type", TypeRef.of("com.acme.core.model.AccountType"));
            Field active = Field.of("active", TypeRef.of("boolean"));
            Field customerId = Field.builder("customerId", TypeRef.of("com.acme.core.model.CustomerId"))
                    .roles(Set.of(FieldRole.AGGREGATE_REFERENCE))
                    .build();

            AggregateRoot account = TestUseCaseFactory.aggregateRoot(
                    "com.acme.core.model.Account",
                    idField,
                    List.of(idField, accountNumber, balance, type, active, customerId));

            Identifier accountId = TestUseCaseFactory.identifier("com.acme.core.model.AccountId", "java.lang.Long");
            Identifier customerIdType =
                    TestUseCaseFactory.identifier("com.acme.core.model.CustomerId", "java.lang.Long");
            ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                    "com.acme.core.model.Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(account, accountId, customerIdType, money);

            Optional<ResponseDtoSpec> result = ResponseDtoSpecBuilder.build(
                    TypeRef.of("com.acme.core.model.Account"), domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            ResponseDtoSpec spec = result.get();
            assertThat(spec.className()).isEqualTo("AccountResponse");
            // id, accountNumber, balanceAmount, balanceCurrency, type, active, customerId = 7
            assertThat(spec.fields()).hasSize(7);
        }

        @Test
        @DisplayName("should skip audit fields")
        void shouldSkipAuditFields() {
            Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            Field name = Field.of("name", TypeRef.of("java.lang.String"));
            Field createdAt = Field.builder("createdAt", TypeRef.of("java.time.Instant"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();
            Field updatedAt = Field.builder("updatedAt", TypeRef.of("java.time.Instant"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();

            AggregateRoot entity = TestUseCaseFactory.aggregateRoot(
                    "com.acme.core.model.Customer", idField, List.of(idField, name, createdAt, updatedAt));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(entity);

            Optional<ResponseDtoSpec> result = ResponseDtoSpecBuilder.build(
                    TypeRef.of("com.acme.core.model.Customer"), domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().fields()).hasSize(2);
        }

        @Test
        @DisplayName("should return empty for non-domain type")
        void shouldReturnEmptyForNonDomainType() {
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();

            Optional<ResponseDtoSpec> result =
                    ResponseDtoSpecBuilder.build(TypeRef.of("java.lang.String"), domainIndex, config, DTO_PACKAGE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should generate response DTO from value object")
        void shouldGenerateResponseDtoFromValueObject() {
            ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                    "com.acme.core.model.Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(money);

            Optional<ResponseDtoSpec> result = ResponseDtoSpecBuilder.build(
                    TypeRef.of("com.acme.core.model.Money"), domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().className()).isEqualTo("MoneyResponse");
            assertThat(result.get().fields()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Naming derivation")
    class NamingDerivation {

        @Test
        @DisplayName("should derive class name with suffix")
        void shouldDeriveClassNameWithSuffix() {
            Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            AggregateRoot account =
                    TestUseCaseFactory.aggregateRoot("com.acme.core.model.Account", idField, List.of(idField));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(account);

            Optional<ResponseDtoSpec> result = ResponseDtoSpecBuilder.build(
                    TypeRef.of("com.acme.core.model.Account"), domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().className()).isEqualTo("AccountResponse");
        }

        @Test
        @DisplayName("should use correct package")
        void shouldUseCorrectPackage() {
            Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            AggregateRoot account =
                    TestUseCaseFactory.aggregateRoot("com.acme.core.model.Account", idField, List.of(idField));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(account);

            Optional<ResponseDtoSpec> result = ResponseDtoSpecBuilder.build(
                    TypeRef.of("com.acme.core.model.Account"), domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().packageName()).isEqualTo(DTO_PACKAGE);
        }
    }

    @Nested
    @DisplayName("Field projection")
    class FieldProjection {

        @Test
        @DisplayName("should flatten VO fields with prefix")
        void shouldFlattenVoFieldsWithPrefix() {
            Field idField = Field.builder("id", TypeRef.of("com.acme.core.model.AccountId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            Field balance = Field.of("balance", TypeRef.of("com.acme.core.model.Money"));

            AggregateRoot account =
                    TestUseCaseFactory.aggregateRoot("com.acme.core.model.Account", idField, List.of(idField, balance));

            Identifier accountId = TestUseCaseFactory.identifier("com.acme.core.model.AccountId", "java.lang.Long");
            ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                    "com.acme.core.model.Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(account, accountId, money);

            Optional<ResponseDtoSpec> result = ResponseDtoSpecBuilder.build(
                    TypeRef.of("com.acme.core.model.Account"), domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().fields()).hasSize(3); // id, balanceAmount, balanceCurrency
            assertThat(result.get().fields().get(1).fieldName()).isEqualTo("balanceAmount");
            assertThat(result.get().fields().get(2).fieldName()).isEqualTo("balanceCurrency");
        }
    }
}
