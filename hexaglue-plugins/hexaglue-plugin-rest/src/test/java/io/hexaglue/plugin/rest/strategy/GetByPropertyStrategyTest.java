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
 * Tests for {@link GetByPropertyStrategy}.
 */
@DisplayName("GetByPropertyStrategy")
class GetByPropertyStrategyTest {

    private final GetByPropertyStrategy strategy = new GetByPropertyStrategy();

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
        @DisplayName("getAccountByNumber(String accountNumber) should match GET /by-number/{accountNumber}")
        void getAccountByNumber_shouldMatchGetByProperty() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "getAccountByNumber",
                    TypeRef.of("com.acme.Account"),
                    List.of(Parameter.of("accountNumber", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/by-number/{accountNumber}");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("accountNumber");
            assertThat(mapping.pathVariables().get(0).javaName()).isEqualTo("accountNumber");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isFalse();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("findByEmail(Email email) should match GET /by-email/{email}")
        void findByEmail_shouldMatchGetByProperty() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "findByEmail",
                    TypeRef.of("com.acme.Customer"),
                    List.of(Parameter.of("email", TypeRef.of("com.acme.Email"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/customers");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/by-email/{email}");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("email");
            assertThat(mapping.pathVariables().get(0).javaName()).isEqualTo("email");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isFalse();
        }
    }

    @Nested
    @DisplayName("NonMatching")
    class NonMatching {

        @Test
        @DisplayName("QUERY without 'By' in name should not match")
        void queryWithoutBy_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "getAccount",
                    TypeRef.of("com.acme.Account"),
                    List.of(Parameter.of("number", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("QUERY where single parameter is the aggregate identity should not match")
        void queryWhereParamIsIdentity_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "getAccountById",
                    TypeRef.of("com.acme.Account"),
                    List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));
            AggregateRoot aggregate = accountAggregate();

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }
    }
}
