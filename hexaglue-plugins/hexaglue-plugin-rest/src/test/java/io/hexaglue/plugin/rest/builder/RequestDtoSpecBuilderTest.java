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

import com.palantir.javapoet.ClassName;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.model.HttpMapping;
import io.hexaglue.plugin.rest.model.HttpMethod;
import io.hexaglue.plugin.rest.model.ProjectionKind;
import io.hexaglue.plugin.rest.model.RequestDtoSpec;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RequestDtoSpecBuilder}.
 */
@DisplayName("RequestDtoSpecBuilder")
class RequestDtoSpecBuilderTest {

    private static final String DTO_PACKAGE = "com.acme.api.dto";
    private final RestConfig config = RestConfig.defaults();
    private final HttpMapping defaultMapping =
            new HttpMapping(HttpMethod.POST, "/open-account", 200, List.of(), List.of());

    @Nested
    @DisplayName("DTO generation")
    class DtoGeneration {

        @Test
        @DisplayName("should generate DTO for command with params")
        void shouldGenerateDtoForCommandWithParams() {
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "openAccount",
                    List.of(
                            Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")),
                            Parameter.of("type", TypeRef.of("com.acme.AccountType")),
                            Parameter.of("accountNumber", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(
                    TestUseCaseFactory.identifier("com.acme.CustomerId", "java.lang.Long"));

            Optional<RequestDtoSpec> result =
                    RequestDtoSpecBuilder.build(useCase, defaultMapping, domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().fields()).hasSize(3);
        }

        @Test
        @DisplayName("should not generate DTO for command without params")
        void shouldNotGenerateDtoForCommandWithoutParams() {
            UseCase useCase = TestUseCaseFactory.command("closeAccount");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();

            Optional<RequestDtoSpec> result =
                    RequestDtoSpecBuilder.build(useCase, defaultMapping, domainIndex, config, DTO_PACKAGE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not generate DTO for query")
        void shouldNotGenerateDtoForQuery() {
            UseCase useCase = TestUseCaseFactory.queryWithParams(
                    "getAccount",
                    TypeRef.of("java.lang.Object"),
                    List.of(Parameter.of("id", TypeRef.of("java.lang.Long"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();

            Optional<RequestDtoSpec> result =
                    RequestDtoSpecBuilder.build(useCase, defaultMapping, domainIndex, config, DTO_PACKAGE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should generate DTO for command-query with params")
        void shouldGenerateDtoForCommandQueryWithParams() {
            UseCase useCase = TestUseCaseFactory.commandQueryWithParams(
                    "openAccount",
                    TypeRef.of("java.lang.Object"),
                    List.of(
                            Parameter.of("customerId", TypeRef.of("com.acme.CustomerId")),
                            Parameter.of("type", TypeRef.of("com.acme.AccountType"))));
            Identifier customerId = TestUseCaseFactory.identifier("com.acme.CustomerId", "java.lang.Long");
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(customerId);

            Optional<RequestDtoSpec> result =
                    RequestDtoSpecBuilder.build(useCase, defaultMapping, domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().fields()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Naming derivation")
    class NamingDerivation {

        @Test
        @DisplayName("should derive class name")
        void shouldDeriveClassName() {
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "openAccount", List.of(Parameter.of("type", TypeRef.of("com.acme.AccountType"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();

            Optional<RequestDtoSpec> result =
                    RequestDtoSpecBuilder.build(useCase, defaultMapping, domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().className()).isEqualTo("OpenAccountRequest");
        }

        @Test
        @DisplayName("should derive package name")
        void shouldDerivePackageName() {
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "openAccount", List.of(Parameter.of("type", TypeRef.of("com.acme.AccountType"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex();

            Optional<RequestDtoSpec> result =
                    RequestDtoSpecBuilder.build(useCase, defaultMapping, domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().packageName()).isEqualTo(DTO_PACKAGE);
        }
    }

    @Nested
    @DisplayName("Field derivation")
    class FieldDerivation {

        @Test
        @DisplayName("should unwrap identifier params")
        void shouldUnwrapIdentifierParams() {
            Identifier accountId = TestUseCaseFactory.identifier("com.acme.AccountId", "java.lang.Long");
            ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                    "com.acme.Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(accountId, money);
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "deposit",
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("amount", TypeRef.of("com.acme.Money"))));

            Optional<RequestDtoSpec> result =
                    RequestDtoSpecBuilder.build(useCase, defaultMapping, domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            assertThat(result.get().fields().get(0).fieldName()).isEqualTo("accountId");
            assertThat(result.get().fields().get(0).javaType()).isEqualTo(ClassName.get("java.lang", "Long"));
            assertThat(result.get().fields().get(0).projectionKind()).isEqualTo(ProjectionKind.IDENTITY_UNWRAP);
        }

        @Test
        @DisplayName("should flatten multi-field VO")
        void shouldFlattenMultiFieldVo() {
            Identifier accountId = TestUseCaseFactory.identifier("com.acme.AccountId", "java.lang.Long");
            ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                    "com.acme.Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(accountId, money);
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "deposit",
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("amount", TypeRef.of("com.acme.Money"))));

            Optional<RequestDtoSpec> result =
                    RequestDtoSpecBuilder.build(useCase, defaultMapping, domainIndex, config, DTO_PACKAGE);

            assertThat(result).isPresent();
            // 1 unwrapped identifier + 2 flattened VO fields = 3 total
            assertThat(result.get().fields()).hasSize(3);
            assertThat(result.get().fields().get(1).fieldName()).isEqualTo("amount");
            assertThat(result.get().fields().get(1).projectionKind()).isEqualTo(ProjectionKind.VALUE_OBJECT_FLATTEN);
            assertThat(result.get().fields().get(2).fieldName()).isEqualTo("currency");
            assertThat(result.get().fields().get(2).projectionKind()).isEqualTo(ProjectionKind.VALUE_OBJECT_FLATTEN);
        }
    }
}
