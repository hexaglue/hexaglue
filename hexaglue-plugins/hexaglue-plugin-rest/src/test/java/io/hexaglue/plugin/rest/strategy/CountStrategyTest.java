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
 * Tests for {@link CountStrategy}.
 */
@DisplayName("CountStrategy")
class CountStrategyTest {

    private final CountStrategy strategy = new CountStrategy();

    @Nested
    @DisplayName("Matching")
    class Matching {

        @Test
        @DisplayName("countAccounts() with long return should match GET /count with 200")
        void countAccounts_noParams_longReturn_shouldMatchGetCount() {
            UseCase useCase = TestUseCaseFactory.queryReturningLong("countAccounts", List.of());

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/count");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).isEmpty();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("countByStatus(String status) with int return should match GET /count with 1 QueryParam")
        void countByStatus_withStringParam_intReturn_shouldMatchGetCountWithQueryParam() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "countByStatus",
                    TypeRef.of("int"),
                    List.of(Parameter.of("status", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mapping.path()).isEqualTo("/count");
            assertThat(mapping.responseStatus()).isEqualTo(200);
            assertThat(mapping.pathVariables()).isEmpty();
            assertThat(mapping.queryParams()).hasSize(1);
            assertThat(mapping.queryParams().get(0).name()).isEqualTo("status");
            assertThat(mapping.queryParams().get(0).javaName()).isEqualTo("status");
            assertThat(mapping.queryParams().get(0).required()).isTrue();
            assertThat(mapping.queryParams().get(0).defaultValue()).isNull();
        }
    }

    @Nested
    @DisplayName("NonMatching")
    class NonMatching {

        @Test
        @DisplayName("countAccounts() with Object return should not match (numeric return required)")
        void countAccounts_objectReturn_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.query("countAccounts");

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("COMMAND 'countSomething' with void return should not match")
        void command_withCountPrefix_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.command("countSomething");

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }
    }
}
