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
 * Tests for {@link GetCollectionStrategy}.
 */
@DisplayName("GetCollectionStrategy")
class GetCollectionStrategyTest {

    private final GetCollectionStrategy strategy = new GetCollectionStrategy();

    @Nested
    @DisplayName("NoParam")
    class NoParam {

        @Test
        @DisplayName("getAllAccounts() with List return should match GET '' with 200")
        void getAllAccounts_withListReturn_shouldMatchGetCollection() {
            UseCase useCase =
                    TestUseCaseFactory.queryWithCollectionReturn("getAllAccounts", "com.acme.Account", List.of());

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).isEmpty();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("listAccounts() with List return should match GET '' with 200")
        void listAccounts_withListReturn_shouldMatchGetCollection() {
            UseCase useCase =
                    TestUseCaseFactory.queryWithCollectionReturn("listAccounts", "com.acme.Account", List.of());

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).isEmpty();
            assertThat(mapping.queryParams()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Filtered")
    class Filtered {

        @Test
        @DisplayName("getAllAccountsByCustomer(CustomerId) with List return should match GET '' with 1 QueryParam")
        void getAllAccountsByCustomer_withOneParam_shouldAddQueryParam() {
            UseCase useCase = TestUseCaseFactory.queryWithCollectionReturn(
                    "getAllAccountsByCustomer",
                    "com.acme.Account",
                    List.of(Parameter.of("customerId", TypeRef.of("com.acme.CustomerId"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).isEmpty();
            assertThat(mapping.queryParams()).hasSize(1);
            assertThat(mapping.queryParams().get(0).name()).isEqualTo("customerId");
            assertThat(mapping.queryParams().get(0).javaName()).isEqualTo("customerId");
            assertThat(mapping.queryParams().get(0).required()).isTrue();
            assertThat(mapping.queryParams().get(0).defaultValue()).isNull();
        }
    }

    @Nested
    @DisplayName("NonMatching")
    class NonMatching {

        @Test
        @DisplayName("getAllAccounts() with non-collection return should not match")
        void getAllAccounts_withNonCollectionReturn_shouldNotMatch() {
            UseCase useCase =
                    TestUseCaseFactory.queryWithParams("getAllAccounts", TypeRef.of("java.lang.Object"), List.of());

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("COMMAND 'getAllAccounts' should not match")
        void command_withMatchingName_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.command("getAllAccounts");

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }
    }
}
