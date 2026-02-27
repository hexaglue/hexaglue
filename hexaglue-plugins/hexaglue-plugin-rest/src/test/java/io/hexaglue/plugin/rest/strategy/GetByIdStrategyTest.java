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
 * Tests for {@link GetByIdStrategy}.
 */
@DisplayName("GetByIdStrategy")
class GetByIdStrategyTest {

    private final GetByIdStrategy strategy = new GetByIdStrategy();

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
        @DisplayName("getAccount(AccountId) should match GET /{id} with 200")
        void getAccount_shouldMatchGetById() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "getAccount",
                    TypeRef.of("com.acme.Account"),
                    List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            assertThat(result.get().httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(result.get().path()).isEqualTo("/{id}");
            assertThat(result.get().responseStatus()).isEqualTo(200);
            assertThat(result.get().pathVariables()).hasSize(1);
            assertThat(result.get().pathVariables().get(0).name()).isEqualTo("id");
            assertThat(result.get().pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(result.get().queryParams()).isEmpty();
        }

        @Test
        @DisplayName("findAccount(AccountId) should match GET /{id} with 200")
        void findAccount_shouldMatchGetById() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "findAccount",
                    TypeRef.of("com.acme.Account"),
                    List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            assertThat(result.get().httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(result.get().path()).isEqualTo("/{id}");
            assertThat(result.get().responseStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("loadAccount(AccountId) should match GET /{id} with 200")
        void loadAccount_shouldMatchGetById() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "loadAccount",
                    TypeRef.of("com.acme.Account"),
                    List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            assertThat(result.get().httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(result.get().path()).isEqualTo("/{id}");
            assertThat(result.get().responseStatus()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("NonMatching")
    class NonMatching {

        @Test
        @DisplayName("COMMAND with matching name should not match")
        void command_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "getAccount", List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("QUERY without identity parameter should not match")
        void queryWithNonIdentityParam_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "getAccount",
                    TypeRef.of("com.acme.Account"),
                    List.of(Parameter.of("number", TypeRef.of("java.lang.String"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("QUERY with two or more parameters should not match")
        void queryWithMultipleParams_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "getAccount",
                    TypeRef.of("com.acme.Account"),
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("extra", TypeRef.of("java.lang.String"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("QUERY with null aggregate should not match")
        void queryWithNullAggregate_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "getAccount",
                    TypeRef.of("com.acme.Account"),
                    List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }
    }
}
