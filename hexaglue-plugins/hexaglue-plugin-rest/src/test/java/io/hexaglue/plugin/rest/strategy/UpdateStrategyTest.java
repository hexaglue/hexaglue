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

package io.hexaglue.plugin.rest.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.model.HttpMapping;
import io.hexaglue.plugin.rest.model.HttpMethod;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UpdateStrategy}.
 */
@DisplayName("UpdateStrategy")
class UpdateStrategyTest {

    private final UpdateStrategy strategy = new UpdateStrategy();

    private AggregateRoot customerAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.CustomerId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Customer", idField, List.of(idField));
    }

    private AggregateRoot accountAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.AccountId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Account", idField, List.of(idField));
    }

    @Nested
    @DisplayName("Matching")
    class Matching {

        @Test
        @DisplayName(
                "updateCustomer(CustomerId, String, String, Email, String) as COMMAND should match PUT /{id} with 200")
        void updateCustomer_command_shouldMatchPutIdWith200() {
            AggregateRoot aggregate = customerAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "updateCustomer",
                    List.of(
                            Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String")),
                            Parameter.of("lastName", TypeRef.of("java.lang.String")),
                            Parameter.of("email", TypeRef.of("com.acme.Email")),
                            Parameter.of("phone", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/customers");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.PUT);
            assertThat(mapping.path()).isEqualTo("/{id}");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("modifyAccount(AccountId, String, BigDecimal) as COMMAND should match PUT /{id} with 200")
        void modifyAccount_command_shouldMatchPutIdWith200() {
            AggregateRoot aggregate = accountAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "modifyAccount",
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("currency", TypeRef.of("java.lang.String")),
                            Parameter.of("limit", TypeRef.of("java.math.BigDecimal"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.PUT);
            assertThat(mapping.path()).isEqualTo("/{id}");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }
    }

    @Nested
    @DisplayName("NonMatching")
    class NonMatching {

        @Test
        @DisplayName("updateSettings(String, String) where first param is not the aggregate identity should not match")
        void updateSettings_firstParamNotIdentity_shouldNotMatch() {
            AggregateRoot aggregate = customerAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "updateSettings",
                    List.of(
                            Parameter.of("key", TypeRef.of("java.lang.String")),
                            Parameter.of("value", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/customers");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("QUERY 'updateAccount' should not match (not a command)")
        void query_updateAccount_shouldNotMatch() {
            AggregateRoot aggregate = accountAggregate();
            UseCase useCase = TestUseCaseFactory.query("updateAccount");

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("updateCustomer(CustomerId, ...) with null aggregate should not match")
        void updateCustomer_nullAggregate_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "updateCustomer",
                    List.of(
                            Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/customers");

            assertThat(result).isEmpty();
        }
    }
}
