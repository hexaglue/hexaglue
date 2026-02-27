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
 * Tests for {@link ExistsStrategy}.
 */
@DisplayName("ExistsStrategy")
class ExistsStrategyTest {

    private final ExistsStrategy strategy = new ExistsStrategy();

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
        @DisplayName("existsById(AccountId) with boolean return and aggregate should match GET /{id}/exists")
        void existsById_booleanReturn_withAggregate_shouldMatchGetIdExists() {
            UseCase useCase = TestUseCaseFactory.queryReturningBoolean(
                    "existsById", List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/{id}/exists");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("isActive(AccountId) with boolean return and aggregate should match GET /{id}/exists")
        void isActive_booleanReturn_withAggregate_shouldMatchGetIdExists() {
            UseCase useCase = TestUseCaseFactory.queryReturningBoolean(
                    "isActive", List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/{id}/exists");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
        }

        @Test
        @DisplayName("hasPermission(AccountId) with boolean return and aggregate should match GET /{id}/exists")
        void hasPermission_booleanReturn_withAggregate_shouldMatchGetIdExists() {
            UseCase useCase = TestUseCaseFactory.queryReturningBoolean(
                    "hasPermission", List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/{id}/exists");
            assertThat(mapping.responseStatus()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("NonMatching")
    class NonMatching {

        @Test
        @DisplayName("existsById(AccountId) with String return should not match (boolean required)")
        void existsById_stringReturn_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "existsById",
                    TypeRef.of("java.lang.String"),
                    List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }
    }
}
