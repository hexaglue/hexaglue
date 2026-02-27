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
 * Tests for {@link CreateStrategy}.
 */
@DisplayName("CreateStrategy")
class CreateStrategyTest {

    private final CreateStrategy strategy = new CreateStrategy();

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
                "openAccount(CustomerId, AccountType, String, BigDecimal) as COMMAND_QUERY should match POST empty with 201")
        void openAccount_commandQuery_shouldMatchPost201() {
            UseCase useCase = TestUseCaseFactory.commandQueryWithParams(
                    "openAccount",
                    TypeRef.of("com.acme.AccountId"),
                    List.of(
                            Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")),
                            Parameter.of("type", TypeRef.of("com.acme.AccountType")),
                            Parameter.of("currency", TypeRef.of("java.lang.String")),
                            Parameter.of("initialDeposit", TypeRef.of("java.math.BigDecimal"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("");
            assertThat(mapping.responseStatus()).isEqualTo(201);
            assertThat(mapping.pathVariables()).isEmpty();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("createCustomer(String, String, Email, String) as COMMAND_QUERY should match POST empty with 201")
        void createCustomer_commandQuery_shouldMatchPost201() {
            UseCase useCase = TestUseCaseFactory.commandQueryWithParams(
                    "createCustomer",
                    TypeRef.of("com.acme.CustomerId"),
                    List.of(
                            Parameter.of("firstName", TypeRef.of("java.lang.String")),
                            Parameter.of("lastName", TypeRef.of("java.lang.String")),
                            Parameter.of("email", TypeRef.of("com.acme.Email")),
                            Parameter.of("phone", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/customers");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("");
            assertThat(mapping.responseStatus()).isEqualTo(201);
            assertThat(mapping.pathVariables()).isEmpty();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName(
                "initiateTransfer(AccountId, AccountId, Money, String) as COMMAND_QUERY should match POST empty with 201")
        void initiateTransfer_commandQuery_shouldMatchPost201() {
            UseCase useCase = TestUseCaseFactory.commandQueryWithParams(
                    "initiateTransfer",
                    TypeRef.of("com.acme.TransferId"),
                    List.of(
                            Parameter.of("sourceAccountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("targetAccountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("amount", TypeRef.of("com.acme.Money")),
                            Parameter.of("reference", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/transfers");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("");
            assertThat(mapping.responseStatus()).isEqualTo(201);
        }

        @Test
        @DisplayName("registerUser(String, String, String) as COMMAND should match POST empty with 201")
        void registerUser_command_shouldMatchPost201() {
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "registerUser",
                    List.of(
                            Parameter.of("username", TypeRef.of("java.lang.String")),
                            Parameter.of("email", TypeRef.of("java.lang.String")),
                            Parameter.of("password", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/users");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("");
            assertThat(mapping.responseStatus()).isEqualTo(201);
            assertThat(mapping.pathVariables()).isEmpty();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("issueCard(AccountId, String) as COMMAND_QUERY should match POST empty with 201")
        void issueCard_commandQuery_shouldMatchPost201() {
            UseCase useCase = TestUseCaseFactory.commandQueryWithParams(
                    "issueCard",
                    TypeRef.of("com.acme.CardId"),
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("cardType", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/cards");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("");
            assertThat(mapping.responseStatus()).isEqualTo(201);
        }
    }

    @Nested
    @DisplayName("NonMatching")
    class NonMatching {

        @Test
        @DisplayName("QUERY 'createAccount' should not match (not a command)")
        void query_createAccount_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.query("createAccount");

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("COMMAND with first param equal to aggregate identity should not match (this is an update)")
        void command_withFirstParamAsIdentity_shouldNotMatch() {
            AggregateRoot aggregate = accountAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "createAccount",
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("currency", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }
    }
}
