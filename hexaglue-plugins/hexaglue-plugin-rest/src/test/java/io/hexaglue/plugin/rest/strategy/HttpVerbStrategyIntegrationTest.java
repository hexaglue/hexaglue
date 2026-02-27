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
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration test for the full {@link HttpVerbStrategyFactory} chain using the banking case study.
 *
 * <p>Exercises all 17 banking use cases against the real strategy chain and verifies that each
 * use case is routed to the correct HTTP method, path, and response status.
 *
 * @since 3.1.0
 */
@DisplayName("HTTP Verb Strategy Integration - Banking Case Study")
class HttpVerbStrategyIntegrationTest {

    private final HttpVerbStrategyFactory factory = new HttpVerbStrategyFactory();

    @ParameterizedTest(name = "{0} → {2} {3} ({4})")
    @MethodSource("bankingCaseStudy")
    @DisplayName("should derive correct HTTP mapping for banking use cases")
    void should_derive_correct_http_mapping(
            String methodName,
            UseCase useCase,
            HttpMethod expectedMethod,
            String expectedPath,
            int expectedStatus,
            AggregateRoot aggregate) {
        HttpMapping mapping = factory.derive(useCase, aggregate, "/api/test");

        assertThat(mapping.httpMethod()).as("HTTP method for %s", methodName).isEqualTo(expectedMethod);
        assertThat(mapping.path()).as("path for %s", methodName).isEqualTo(expectedPath);
        assertThat(mapping.responseStatus()).as("status for %s", methodName).isEqualTo(expectedStatus);
    }

    static Stream<Arguments> bankingCaseStudy() {
        AggregateRoot account = buildAccountAggregate();
        AggregateRoot customer = buildCustomerAggregate();
        AggregateRoot transfer = buildTransferAggregate();

        return Stream.of(
                // --- Account use cases ---

                // COMMAND_QUERY: first param is CustomerId (not AccountId) → CreateStrategy
                Arguments.of(
                        "openAccount",
                        TestUseCaseFactory.commandQueryWithParams(
                                "openAccount",
                                TypeRef.of("com.acme.AccountId"),
                                List.of(
                                        Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")),
                                        Parameter.of("type", TypeRef.of("com.acme.AccountType")),
                                        Parameter.of("currency", TypeRef.of("java.lang.String")))),
                        HttpMethod.POST,
                        "",
                        201,
                        account),

                // COMMAND: single param is AccountId (identity) + delete prefix → DeleteStrategy
                Arguments.of(
                        "closeAccount",
                        TestUseCaseFactory.commandWithParams(
                                "closeAccount", List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId")))),
                        HttpMethod.DELETE,
                        "/{id}",
                        204,
                        account),

                // QUERY: single identity param → GetByIdStrategy
                Arguments.of(
                        "getAccount",
                        TestUseCaseFactory.queryWithParams(
                                "getAccount",
                                TypeRef.of("com.acme.Account"),
                                List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId")))),
                        HttpMethod.GET,
                        "/{id}",
                        200,
                        account),

                // QUERY: contains "By" + non-identity param → GetByPropertyStrategy
                Arguments.of(
                        "getAccountByNumber",
                        TestUseCaseFactory.queryWithParams(
                                "getAccountByNumber",
                                TypeRef.of("com.acme.Account"),
                                List.of(Parameter.of("accountNumber", TypeRef.of("java.lang.String")))),
                        HttpMethod.GET,
                        "/by-number/{accountNumber}",
                        200,
                        account),

                // QUERY: collection return + getAll prefix → GetCollectionStrategy
                Arguments.of(
                        "getAllAccounts",
                        TestUseCaseFactory.queryWithCollectionReturn("getAllAccounts", "com.acme.Account", List.of()),
                        HttpMethod.GET,
                        "",
                        200,
                        account),

                // COMMAND: first param is identity + action verb (deposit) → SubResourceActionStrategy
                Arguments.of(
                        "deposit",
                        TestUseCaseFactory.commandWithParams(
                                "deposit",
                                List.of(
                                        Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                                        Parameter.of("amount", TypeRef.of("com.acme.Money")))),
                        HttpMethod.POST,
                        "/{id}/deposit",
                        200,
                        account),

                // COMMAND: first param is identity + action verb (withdraw) → SubResourceActionStrategy
                Arguments.of(
                        "withdraw",
                        TestUseCaseFactory.commandWithParams(
                                "withdraw",
                                List.of(
                                        Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                                        Parameter.of("amount", TypeRef.of("com.acme.Money")))),
                        HttpMethod.POST,
                        "/{id}/withdraw",
                        200,
                        account),

                // --- Customer use cases ---

                // COMMAND_QUERY: no identity param as first → CreateStrategy
                Arguments.of(
                        "createCustomer",
                        TestUseCaseFactory.commandQueryWithParams(
                                "createCustomer",
                                TypeRef.of("com.acme.CustomerId"),
                                List.of(
                                        Parameter.of("firstName", TypeRef.of("java.lang.String")),
                                        Parameter.of("lastName", TypeRef.of("java.lang.String")),
                                        Parameter.of("email", TypeRef.of("com.acme.Email")),
                                        Parameter.of("phone", TypeRef.of("java.lang.String")))),
                        HttpMethod.POST,
                        "",
                        201,
                        customer),

                // COMMAND: first param is CustomerId (identity) + update prefix → UpdateStrategy
                Arguments.of(
                        "updateCustomer",
                        TestUseCaseFactory.commandWithParams(
                                "updateCustomer",
                                List.of(
                                        Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")),
                                        Parameter.of("firstName", TypeRef.of("java.lang.String")),
                                        Parameter.of("lastName", TypeRef.of("java.lang.String")),
                                        Parameter.of("email", TypeRef.of("com.acme.Email")),
                                        Parameter.of("phone", TypeRef.of("java.lang.String")))),
                        HttpMethod.PUT,
                        "/{id}",
                        200,
                        customer),

                // QUERY: single identity param → GetByIdStrategy
                Arguments.of(
                        "getCustomer",
                        TestUseCaseFactory.queryWithParams(
                                "getCustomer",
                                TypeRef.of("com.acme.Customer"),
                                List.of(Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")))),
                        HttpMethod.GET,
                        "/{id}",
                        200,
                        customer),

                // QUERY: contains "By" + non-identity param (Email) → GetByPropertyStrategy
                Arguments.of(
                        "getCustomerByEmail",
                        TestUseCaseFactory.queryWithParams(
                                "getCustomerByEmail",
                                TypeRef.of("com.acme.Customer"),
                                List.of(Parameter.of("email", TypeRef.of("com.acme.Email")))),
                        HttpMethod.GET,
                        "/by-email/{email}",
                        200,
                        customer),

                // --- Transfer use cases ---

                // COMMAND_QUERY: first params are AccountId (not TransferId) → CreateStrategy
                Arguments.of(
                        "initiateTransfer",
                        TestUseCaseFactory.commandQueryWithParams(
                                "initiateTransfer",
                                TypeRef.of("com.acme.TransferId"),
                                List.of(
                                        Parameter.of("sourceAccountId", TypeRef.of("com.acme.AccountId")),
                                        Parameter.of("targetAccountId", TypeRef.of("com.acme.AccountId")),
                                        Parameter.of("amount", TypeRef.of("com.acme.Money")),
                                        Parameter.of("reference", TypeRef.of("java.lang.String")))),
                        HttpMethod.POST,
                        "",
                        201,
                        transfer),

                // COMMAND: first param is TransferId + action verb → SubResourceActionStrategy
                Arguments.of(
                        "executeTransfer",
                        TestUseCaseFactory.commandWithParams(
                                "executeTransfer",
                                List.of(Parameter.of("transferId", TypeRef.of("com.acme.TransferId")))),
                        HttpMethod.POST,
                        "/{id}/execute-transfer",
                        200,
                        transfer),

                // COMMAND: first param is TransferId + action verb → SubResourceActionStrategy
                Arguments.of(
                        "cancelTransfer",
                        TestUseCaseFactory.commandWithParams(
                                "cancelTransfer",
                                List.of(Parameter.of("transferId", TypeRef.of("com.acme.TransferId")))),
                        HttpMethod.POST,
                        "/{id}/cancel-transfer",
                        200,
                        transfer),

                // QUERY: single identity param → GetByIdStrategy
                Arguments.of(
                        "getTransfer",
                        TestUseCaseFactory.queryWithParams(
                                "getTransfer",
                                TypeRef.of("com.acme.Transfer"),
                                List.of(Parameter.of("transferId", TypeRef.of("com.acme.TransferId")))),
                        HttpMethod.GET,
                        "/{id}",
                        200,
                        transfer),

                // --- Scalar query use cases (Account) ---

                // QUERY: count prefix + long return → CountStrategy
                Arguments.of(
                        "countAccounts",
                        TestUseCaseFactory.queryReturningLong("countAccounts", List.of()),
                        HttpMethod.GET,
                        "/count",
                        200,
                        account),

                // QUERY: exists/is/has prefix + boolean return → ExistsStrategy
                Arguments.of(
                        "existsById",
                        TestUseCaseFactory.queryReturningBoolean(
                                "existsById", List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId")))),
                        HttpMethod.GET,
                        "/{id}/exists",
                        200,
                        account));
    }

    /**
     * Builds the Account aggregate with {@code AccountId} wrapping {@code Long}.
     */
    private static AggregateRoot buildAccountAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.AccountId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Account", idField, List.of(idField));
    }

    /**
     * Builds the Customer aggregate with {@code CustomerId} wrapping {@code Long}.
     */
    private static AggregateRoot buildCustomerAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.CustomerId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Customer", idField, List.of(idField));
    }

    /**
     * Builds the Transfer aggregate with {@code TransferId} wrapping {@code UUID}.
     */
    private static AggregateRoot buildTransferAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.TransferId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Transfer", idField, List.of(idField));
    }
}
