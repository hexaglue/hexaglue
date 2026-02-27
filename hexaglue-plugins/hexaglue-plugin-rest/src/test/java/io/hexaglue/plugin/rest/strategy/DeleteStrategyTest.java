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
 * Tests for {@link DeleteStrategy}.
 */
@DisplayName("DeleteStrategy")
class DeleteStrategyTest {

    private final DeleteStrategy strategy = new DeleteStrategy();

    private AggregateRoot accountAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.AccountId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Account", idField, List.of(idField));
    }

    private AggregateRoot customerAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.CustomerId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Customer", idField, List.of(idField));
    }

    private AggregateRoot orderAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.OrderId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Order", idField, List.of(idField));
    }

    @Nested
    @DisplayName("Matching")
    class Matching {

        @Test
        @DisplayName("closeAccount(AccountId) as COMMAND void should match DELETE /{id} with 204")
        void closeAccount_command_void_shouldMatchDeleteIdWith204() {
            AggregateRoot aggregate = accountAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "closeAccount", List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.DELETE);
            assertThat(mapping.path()).isEqualTo("/{id}");
            assertThat(mapping.responseStatus()).isEqualTo(204);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("deleteCustomer(CustomerId) as COMMAND void should match DELETE /{id} with 204")
        void deleteCustomer_command_void_shouldMatchDeleteIdWith204() {
            AggregateRoot aggregate = customerAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "deleteCustomer", List.of(Parameter.of("customerId", TypeRef.of("com.acme.CustomerId"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/customers");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.DELETE);
            assertThat(mapping.path()).isEqualTo("/{id}");
            assertThat(mapping.responseStatus()).isEqualTo(204);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("archiveOrder(OrderId) as COMMAND void should match DELETE /{id} with 204")
        void archiveOrder_command_void_shouldMatchDeleteIdWith204() {
            AggregateRoot aggregate = orderAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "archiveOrder", List.of(Parameter.of("orderId", TypeRef.of("com.acme.OrderId"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/orders");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.DELETE);
            assertThat(mapping.path()).isEqualTo("/{id}");
            assertThat(mapping.responseStatus()).isEqualTo(204);
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
        @DisplayName("COMMAND_QUERY 'deleteAccount' with non-void return should not match")
        void commandQuery_deleteAccount_nonVoidReturn_shouldNotMatch() {
            AggregateRoot aggregate = accountAggregate();
            // commandQuery returns java.lang.Object (non-void), so isVoidReturn is false.
            // Furthermore, UseCaseType is COMMAND_QUERY, not strict COMMAND.
            UseCase useCase = TestUseCaseFactory.commandQueryWithParams(
                    "deleteAccount",
                    TypeRef.of("java.lang.Object"),
                    List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("closeAccount(String notAnId) where first param is not the aggregate identity should not match")
        void closeAccount_firstParamNotIdentity_shouldNotMatch() {
            AggregateRoot aggregate = accountAggregate();
            // First parameter is String, not AccountId — isFirstParamIdentity returns false.
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "closeAccount", List.of(Parameter.of("reason", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("closeAccount(AccountId, String extraParam) with 2 params should not match")
        void closeAccount_twoParams_shouldNotMatch() {
            AggregateRoot aggregate = accountAggregate();
            // Two parameters: strategy requires exactly 1.
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "closeAccount",
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("reason", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }
    }
}
