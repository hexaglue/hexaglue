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

import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.model.HttpMapping;
import io.hexaglue.plugin.rest.model.HttpMethod;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SearchStrategy}.
 */
@DisplayName("SearchStrategy")
class SearchStrategyTest {

    private final SearchStrategy strategy = new SearchStrategy();

    @Nested
    @DisplayName("Matching")
    class Matching {

        @Test
        @DisplayName("searchAccounts(p1, p2) with List return should match GET /search with 2 QueryParams")
        void searchAccounts_withTwoParams_shouldMatchGetSearch() {
            UseCase useCase = TestUseCaseFactory.queryWithCollectionReturn(
                    "searchAccounts",
                    "com.acme.Account",
                    List.of(
                            Parameter.of("status", TypeRef.of("java.lang.String")),
                            Parameter.of("customerId", TypeRef.of("com.acme.CustomerId"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/search");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).isEmpty();
            assertThat(mapping.queryParams()).hasSize(2);
            assertThat(mapping.queryParams().get(0).name()).isEqualTo("status");
            assertThat(mapping.queryParams().get(0).javaName()).isEqualTo("status");
            assertThat(mapping.queryParams().get(0).required()).isTrue();
            assertThat(mapping.queryParams().get(0).defaultValue()).isNull();
            assertThat(mapping.queryParams().get(1).name()).isEqualTo("customerId");
            assertThat(mapping.queryParams().get(1).javaName()).isEqualTo("customerId");
            assertThat(mapping.queryParams().get(1).required()).isTrue();
            assertThat(mapping.queryParams().get(1).defaultValue()).isNull();
        }

        @Test
        @DisplayName("queryTransactions(p1, p2) with List return should match GET /search with 200")
        void queryTransactions_withTwoParams_shouldMatchGetSearch() {
            UseCase useCase = TestUseCaseFactory.queryWithCollectionReturn(
                    "queryTransactions",
                    "com.acme.Transaction",
                    List.of(
                            Parameter.of("fromDate", TypeRef.of("java.time.LocalDate")),
                            Parameter.of("toDate", TypeRef.of("java.time.LocalDate"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/transactions");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/search");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.queryParams()).hasSize(2);
        }

        @Test
        @DisplayName("searchAccounts with 1 parameter should match GET /search with 1 QueryParam")
        void searchAccounts_withOneParam_shouldMatch() {
            UseCase useCase = TestUseCaseFactory.queryWithCollectionReturn(
                    "searchAccounts",
                    "com.acme.Account",
                    List.of(Parameter.of("status", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/search");
            assertThat(mapping.queryParams()).hasSize(1);
            assertThat(mapping.queryParams().get(0).name()).isEqualTo("status");
        }
    }

    @Nested
    @DisplayName("NonMatching")
    class NonMatching {

        @Test
        @DisplayName("searchAccounts with 2 parameters but non-collection return should not match")
        void searchAccounts_withNonCollectionReturn_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "searchAccounts",
                    TypeRef.of("java.lang.Object"),
                    List.of(
                            Parameter.of("status", TypeRef.of("java.lang.String")),
                            Parameter.of("customerId", TypeRef.of("com.acme.CustomerId"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("COMMAND 'searchAccounts' should not match")
        void command_withMatchingName_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.command("searchAccounts");

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }
    }
}
