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
 * Tests for {@link SubResourceActionStrategy}.
 */
@DisplayName("SubResourceActionStrategy")
class SubResourceActionStrategyTest {

    private final SubResourceActionStrategy strategy = new SubResourceActionStrategy();

    private AggregateRoot accountAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.AccountId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Account", idField, List.of(idField));
    }

    private AggregateRoot transferAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.TransferId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Transfer", idField, List.of(idField));
    }

    private AggregateRoot cardAggregate() {
        Field idField = Field.builder("id", TypeRef.of("com.acme.CardId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return TestUseCaseFactory.aggregateRoot("com.acme.Card", idField, List.of(idField));
    }

    @Nested
    @DisplayName("Matching")
    class Matching {

        @Test
        @DisplayName("deposit(AccountId, Money) as void COMMAND should match POST /{id}/deposit with 204")
        void deposit_voidCommand_shouldMatchPostIdDepositWith204() {
            AggregateRoot aggregate = accountAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "deposit",
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("amount", TypeRef.of("com.acme.Money"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("/{id}/deposit");
            assertThat(mapping.responseStatus()).isEqualTo(204);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("withdraw(AccountId, Money) as void COMMAND should match POST /{id}/withdraw with 204")
        void withdraw_voidCommand_shouldMatchPostIdWithdrawWith204() {
            AggregateRoot aggregate = accountAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "withdraw",
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("amount", TypeRef.of("com.acme.Money"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("/{id}/withdraw");
            assertThat(mapping.responseStatus()).isEqualTo(204);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("executeTransfer(TransferId) as void COMMAND should match POST /{id}/execute-transfer with 204")
        void executeTransfer_voidCommand_shouldMatchPostIdExecuteTransferWith204() {
            AggregateRoot aggregate = transferAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "executeTransfer", List.of(Parameter.of("transferId", TypeRef.of("com.acme.TransferId"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/transfers");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("/{id}/execute-transfer");
            assertThat(mapping.responseStatus()).isEqualTo(204);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("cancelTransfer(TransferId) as void COMMAND should match POST /{id}/cancel-transfer with 204")
        void cancelTransfer_voidCommand_shouldMatchPostIdCancelTransferWith204() {
            AggregateRoot aggregate = transferAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "cancelTransfer", List.of(Parameter.of("transferId", TypeRef.of("com.acme.TransferId"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/transfers");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("/{id}/cancel-transfer");
            assertThat(mapping.responseStatus()).isEqualTo(204);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }

        @Test
        @DisplayName("blockCard(CardId) as void COMMAND should match POST /{id}/block-card with 204")
        void blockCard_voidCommand_shouldMatchPostIdBlockCardWith204() {
            AggregateRoot aggregate = cardAggregate();
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "blockCard", List.of(Parameter.of("cardId", TypeRef.of("com.acme.CardId"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/cards");

            assertThat(result).isPresent();
            HttpMapping mapping = result.get();
            assertThat(mapping.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(mapping.path()).isEqualTo("/{id}/block-card");
            assertThat(mapping.responseStatus()).isEqualTo(204);
            assertThat(mapping.pathVariables()).hasSize(1);
            assertThat(mapping.pathVariables().get(0).name()).isEqualTo("id");
            assertThat(mapping.pathVariables().get(0).isIdentifier()).isTrue();
            assertThat(mapping.queryParams()).isEmpty();
        }
    }

    @Nested
    @DisplayName("NonMatching")
    class NonMatching {

        @Test
        @DisplayName("deposit(String notAnId) where first param is not the aggregate identity should not match")
        void deposit_firstParamNotIdentity_shouldNotMatch() {
            AggregateRoot aggregate = accountAggregate();
            // First parameter is String, not AccountId — isFirstParamIdentity returns false.
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "deposit", List.of(Parameter.of("reference", TypeRef.of("java.lang.String"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("QUERY 'getBalance' should not match (not a command type)")
        void query_getBalance_shouldNotMatch() {
            AggregateRoot aggregate = accountAggregate();
            UseCase useCase = TestUseCaseFactory.query("getBalance");

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("createItem(AccountId) matching create prefix should not match")
        void createItem_matchesCreatePrefix_shouldNotMatch() {
            AggregateRoot aggregate = accountAggregate();
            // "create" prefix is excluded — handled by CreateStrategy.
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "createItem", List.of(Parameter.of("accountId", TypeRef.of("com.acme.AccountId"))));

            Optional<HttpMapping> result = strategy.match(useCase, aggregate, "/api/accounts");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deposit(AccountId, Money) with null aggregate should not match")
        void deposit_nullAggregate_shouldNotMatch() {
            UseCase useCase = TestUseCaseFactory.commandWithParams(
                    "deposit",
                    List.of(
                            Parameter.of("accountId", TypeRef.of("com.acme.AccountId")),
                            Parameter.of("amount", TypeRef.of("com.acme.Money"))));

            Optional<HttpMapping> result = strategy.match(useCase, null, "/api/accounts");

            assertThat(result).isEmpty();
        }
    }
}
