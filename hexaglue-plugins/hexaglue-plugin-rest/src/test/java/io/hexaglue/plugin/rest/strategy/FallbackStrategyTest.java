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

import io.hexaglue.arch.model.UseCase;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.model.HttpMapping;
import io.hexaglue.plugin.rest.model.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FallbackStrategy}.
 */
@DisplayName("FallbackStrategy")
class FallbackStrategyTest {

    private final FallbackStrategy strategy = new FallbackStrategy();

    @Test
    @DisplayName("should map QUERY to GET")
    void shouldMapQueryToGet() {
        UseCase useCase = TestUseCaseFactory.query("getAccount");

        HttpMapping mapping = strategy.match(useCase, null, "/api/accounts").orElseThrow();

        assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(mapping.path()).isEqualTo("/get-account");
        assertThat(mapping.responseStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("should map COMMAND to POST")
    void shouldMapCommandToPost() {
        UseCase useCase = TestUseCaseFactory.command("closeAccount");

        HttpMapping mapping = strategy.match(useCase, null, "/api/accounts").orElseThrow();

        assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(mapping.path()).isEqualTo("/close-account");
        assertThat(mapping.responseStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("should map COMMAND_QUERY to POST")
    void shouldMapCommandQueryToPost() {
        UseCase useCase = TestUseCaseFactory.commandQuery("openAccount");

        HttpMapping mapping = strategy.match(useCase, null, "/api/accounts").orElseThrow();

        assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(mapping.path()).isEqualTo("/open-account");
        assertThat(mapping.responseStatus()).isEqualTo(200);
    }
}
