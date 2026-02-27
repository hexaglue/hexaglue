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

package io.hexaglue.plugin.rest.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.model.BindingKind;
import io.hexaglue.plugin.rest.model.ControllerSpec;
import io.hexaglue.plugin.rest.model.HttpMethod;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ControllerSpecBuilder}.
 */
@DisplayName("ControllerSpecBuilder")
class ControllerSpecBuilderTest {

    private final RestConfig config = RestConfig.defaults();

    @Test
    @DisplayName("should build controller from driving port with 2 use cases")
    void shouldBuildControllerFromDrivingPort() {
        UseCase query = TestUseCaseFactory.query("getAccount");
        UseCase command = TestUseCaseFactory.command("closeAccount");
        DrivingPort port =
                TestUseCaseFactory.drivingPort("com.acme.core.port.in.AccountUseCases", List.of(query, command));

        ControllerSpec spec = ControllerSpecBuilder.builder()
                .drivingPort(port)
                .config(config)
                .apiPackage("com.acme.api")
                .build();

        assertThat(spec.endpoints()).hasSize(2);
        assertThat(spec.endpoints().get(0).httpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(spec.endpoints().get(1).httpMethod()).isEqualTo(HttpMethod.POST);
    }

    @Test
    @DisplayName("should derive controller name from port name")
    void shouldDeriveControllerName() {
        DrivingPort port = TestUseCaseFactory.drivingPort(
                "com.acme.core.port.in.AccountUseCases", List.of(TestUseCaseFactory.query("getAccount")));

        ControllerSpec spec = ControllerSpecBuilder.builder()
                .drivingPort(port)
                .config(config)
                .apiPackage("com.acme.api")
                .build();

        assertThat(spec.className()).isEqualTo("AccountController");
    }

    @Test
    @DisplayName("should derive base path from stripped port name")
    void shouldDeriveBasePath() {
        DrivingPort port = TestUseCaseFactory.drivingPort(
                "com.acme.core.port.in.AccountUseCases", List.of(TestUseCaseFactory.query("getAccount")));

        ControllerSpec spec = ControllerSpecBuilder.builder()
                .drivingPort(port)
                .config(config)
                .apiPackage("com.acme.api")
                .build();

        assertThat(spec.basePath()).isEqualTo("/api/accounts");
    }

    @Test
    @DisplayName("should derive tag name")
    void shouldDeriveTagName() {
        DrivingPort port = TestUseCaseFactory.drivingPort(
                "com.acme.core.port.in.AccountUseCases", List.of(TestUseCaseFactory.query("getAccount")));

        ControllerSpec spec = ControllerSpecBuilder.builder()
                .drivingPort(port)
                .config(config)
                .apiPackage("com.acme.api")
                .build();

        assertThat(spec.tagName()).isEqualTo("Accounts");
        assertThat(spec.tagDescription()).isEqualTo("Account management operations");
    }

    @Nested
    @DisplayName("With request DTOs")
    class WithRequestDtos {

        @Test
        @DisplayName("should generate request DTO for command with params")
        void shouldGenerateRequestDtoForCommandWithParams() {
            Identifier customerId = TestUseCaseFactory.identifier("com.acme.CustomerId", "java.lang.Long");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(customerId);

            UseCase openAccount = TestUseCaseFactory.commandQueryWithParams(
                    "openAccount",
                    TypeRef.of("java.lang.Object"),
                    List.of(
                            Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")),
                            Parameter.of("type", TypeRef.of("com.acme.AccountType"))));
            DrivingPort port =
                    TestUseCaseFactory.drivingPort("com.acme.core.port.in.AccountUseCases", List.of(openAccount));

            ControllerSpec spec = ControllerSpecBuilder.builder()
                    .drivingPort(port)
                    .config(config)
                    .apiPackage("com.acme.api")
                    .domainIndex(domainIndex)
                    .build();

            assertThat(spec.requestDtos()).hasSize(1);
            assertThat(spec.requestDtos().get(0).className()).isEqualTo("OpenAccountRequest");
        }

        @Test
        @DisplayName("should set request DTO ref on endpoint")
        void shouldSetRequestDtoRefOnEndpoint() {
            Identifier customerId = TestUseCaseFactory.identifier("com.acme.CustomerId", "java.lang.Long");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(customerId);

            UseCase openAccount = TestUseCaseFactory.commandWithParams(
                    "openAccount", List.of(Parameter.of("customerId", TypeRef.of("com.acme.CustomerId"))));
            DrivingPort port =
                    TestUseCaseFactory.drivingPort("com.acme.core.port.in.AccountUseCases", List.of(openAccount));

            ControllerSpec spec = ControllerSpecBuilder.builder()
                    .drivingPort(port)
                    .config(config)
                    .apiPackage("com.acme.api")
                    .domainIndex(domainIndex)
                    .build();

            assertThat(spec.endpoints().get(0).requestDtoRef()).isEqualTo("OpenAccountRequest");
            assertThat(spec.endpoints().get(0).hasRequestBody()).isTrue();
        }

        @Test
        @DisplayName("should not generate DTO for query with params")
        void shouldNotGenerateDtoForQueryWithParams() {
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();

            UseCase getAccount = TestUseCaseFactory.queryWithParams(
                    "getAccount",
                    TypeRef.of("java.lang.Object"),
                    List.of(Parameter.of("id", TypeRef.of("java.lang.Long"))));
            DrivingPort port =
                    TestUseCaseFactory.drivingPort("com.acme.core.port.in.AccountUseCases", List.of(getAccount));

            ControllerSpec spec = ControllerSpecBuilder.builder()
                    .drivingPort(port)
                    .config(config)
                    .apiPackage("com.acme.api")
                    .domainIndex(domainIndex)
                    .build();

            assertThat(spec.requestDtos()).isEmpty();
            assertThat(spec.endpoints().get(0).hasRequestBody()).isFalse();
        }

        @Test
        @DisplayName("should build parameter bindings for command")
        void shouldBuildParameterBindingsForCommand() {
            Identifier customerId = TestUseCaseFactory.identifier("com.acme.CustomerId", "java.lang.Long");
            ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                    "com.acme.Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(customerId, money);

            UseCase deposit = TestUseCaseFactory.commandWithParams(
                    "deposit",
                    List.of(
                            Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")),
                            Parameter.of("amount", TypeRef.of("com.acme.Money"))));
            DrivingPort port =
                    TestUseCaseFactory.drivingPort("com.acme.core.port.in.AccountUseCases", List.of(deposit));

            ControllerSpec spec = ControllerSpecBuilder.builder()
                    .drivingPort(port)
                    .config(config)
                    .apiPackage("com.acme.api")
                    .domainIndex(domainIndex)
                    .build();

            assertThat(spec.endpoints().get(0).parameterBindings()).hasSize(2);
            assertThat(spec.endpoints().get(0).parameterBindings().get(0).kind())
                    .isEqualTo(BindingKind.CONSTRUCTOR_WRAP);
            assertThat(spec.endpoints().get(0).parameterBindings().get(1).kind())
                    .isEqualTo(BindingKind.FACTORY_WRAP);
            assertThat(spec.endpoints().get(0).parameterBindings().get(1).sourceFields())
                    .containsExactly("amount", "currency");
        }
    }
}
