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

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateAssociator}.
 *
 * @since 3.1.0
 */
@DisplayName("AggregateAssociator")
class AggregateAssociatorTest {

    @Nested
    @DisplayName("Naming convention")
    class NamingConvention {

        @Test
        @DisplayName("should associate port with UseCases suffix")
        void shouldAssociatePortWithUseCasesSuffix() {
            Field idField = Field.builder("id", TypeRef.of("com.acme.core.model.AccountId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            AggregateRoot account =
                    TestUseCaseFactory.aggregateRoot("com.acme.core.model.Account", idField, List.of(idField));

            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.AccountUseCases", List.of(TestUseCaseFactory.query("getAccount")));

            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(account);
            AggregateAssociator associator = new AggregateAssociator(domainIndex);

            Optional<AggregateRoot> result = associator.associate(port);

            assertThat(result).isPresent();
            assertThat(result.get().id().simpleName()).isEqualTo("Account");
        }

        @Test
        @DisplayName("should associate port with Service suffix")
        void shouldAssociatePortWithServiceSuffix() {
            Field idField = Field.builder("id", TypeRef.of("com.acme.core.model.AccountId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            AggregateRoot account =
                    TestUseCaseFactory.aggregateRoot("com.acme.core.model.Account", idField, List.of(idField));

            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.AccountService", List.of(TestUseCaseFactory.query("getAccount")));

            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(account);
            AggregateAssociator associator = new AggregateAssociator(domainIndex);

            Optional<AggregateRoot> result = associator.associate(port);

            assertThat(result).isPresent();
            assertThat(result.get().id().simpleName()).isEqualTo("Account");
        }
    }

    @Nested
    @DisplayName("Type analysis fallback")
    class TypeAnalysisFallback {

        @Test
        @DisplayName("should fallback to return type analysis")
        void shouldFallbackToReturnTypeAnalysis() {
            Field idField = Field.builder("id", TypeRef.of("com.acme.core.model.AccountId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            AggregateRoot account =
                    TestUseCaseFactory.aggregateRoot("com.acme.core.model.Account", idField, List.of(idField));

            UseCase getAccount = TestUseCaseFactory.queryWithParams(
                    "getAccount",
                    TypeRef.of("com.acme.core.model.Account"),
                    List.of(Parameter.of("id", TypeRef.of("java.lang.Long"))));

            DrivingPort port =
                    TestUseCaseFactory.drivingPort("com.acme.core.port.in.BankingOperations", List.of(getAccount));

            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(account);
            AggregateAssociator associator = new AggregateAssociator(domainIndex);

            Optional<AggregateRoot> result = associator.associate(port);

            assertThat(result).isPresent();
            assertThat(result.get().id().simpleName()).isEqualTo("Account");
        }

        @Test
        @DisplayName("should return empty when no match")
        void shouldReturnEmptyWhenNoMatch() {
            Field idField = Field.builder("id", TypeRef.of("com.acme.core.model.AccountId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            AggregateRoot account =
                    TestUseCaseFactory.aggregateRoot("com.acme.core.model.Account", idField, List.of(idField));

            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.NotificationService",
                    List.of(TestUseCaseFactory.command("sendNotification")));

            DomainIndex domainIndex = TestUseCaseFactory.domainIndex(account);
            AggregateAssociator associator = new AggregateAssociator(domainIndex);

            Optional<AggregateRoot> result = associator.associate(port);

            assertThat(result).isEmpty();
        }
    }
}
