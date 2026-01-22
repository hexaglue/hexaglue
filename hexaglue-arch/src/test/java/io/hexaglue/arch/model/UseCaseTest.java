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

package io.hexaglue.arch.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UseCase}.
 *
 * @since 5.0.0
 */
@DisplayName("UseCase")
class UseCaseTest {

    private static final TypeRef VOID = new TypeRef("void", "void", List.of(), true, false, 0);
    private static final TypeRef STRING = new TypeRef("java.lang.String", "String", List.of(), false, false, 0);
    private static final TypeRef LONG = new TypeRef("java.lang.Long", "Long", List.of(), false, false, 0);
    private static final TypeRef BOOLEAN = new TypeRef("boolean", "boolean", List.of(), true, false, 0);

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create use case with all parameters")
        void shouldCreateWithAllParameters() {
            Method method = createMethod("createOrder", VOID, List.of(param("orderId", LONG)));
            UseCase useCase = new UseCase(method, Optional.of("Creates a new order"), UseCase.UseCaseType.COMMAND);

            assertThat(useCase.method()).isEqualTo(method);
            assertThat(useCase.description()).contains("Creates a new order");
            assertThat(useCase.type()).isEqualTo(UseCase.UseCaseType.COMMAND);
            assertThat(useCase.name()).isEqualTo("createOrder");
        }

        @Test
        @DisplayName("should create use case with factory method")
        void shouldCreateWithFactoryMethod() {
            Method method = createMethod("findOrder", STRING, List.of(param("id", LONG)));
            UseCase useCase = UseCase.of(method, UseCase.UseCaseType.QUERY);

            assertThat(useCase.method()).isEqualTo(method);
            assertThat(useCase.type()).isEqualTo(UseCase.UseCaseType.QUERY);
            assertThat(useCase.description()).isEmpty();
        }

        @Test
        @DisplayName("should create use case with auto-derived type")
        void shouldCreateWithAutoDerivedType() {
            Method method = createMethod("deleteOrder", VOID, List.of(param("id", LONG)));
            UseCase useCase = UseCase.from(method);

            assertThat(useCase.type()).isEqualTo(UseCase.UseCaseType.COMMAND);
        }

        @Test
        @DisplayName("should reject null method")
        void shouldRejectNullMethod() {
            assertThatThrownBy(() -> new UseCase(null, Optional.empty(), UseCase.UseCaseType.COMMAND))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("method must not be null");
        }

        @Test
        @DisplayName("should reject null description optional")
        void shouldRejectNullDescriptionOptional() {
            Method method = createMethod("test", VOID, List.of());
            assertThatThrownBy(() -> new UseCase(method, null, UseCase.UseCaseType.COMMAND))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject null type")
        void shouldRejectNullType() {
            Method method = createMethod("test", VOID, List.of());
            assertThatThrownBy(() -> new UseCase(method, Optional.empty(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Type Derivation")
    class TypeDerivation {

        @Test
        @DisplayName("void method should be COMMAND")
        void voidMethodShouldBeCommand() {
            Method method = createMethod("deleteOrder", VOID, List.of(param("id", LONG)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.COMMAND);
        }

        @Test
        @DisplayName("void method without parameters should be COMMAND")
        void voidMethodWithoutParametersShouldBeCommand() {
            Method method = createMethod("resetAll", VOID, List.of());
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.COMMAND);
        }

        @Test
        @DisplayName("getter without parameters should be QUERY")
        void getterWithoutParametersShouldBeQuery() {
            Method method = createMethod("getOrders", STRING, List.of());
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("find method with parameters should be QUERY")
        void findMethodWithParametersShouldBeQuery() {
            Method method = createMethod("findById", STRING, List.of(param("id", LONG)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("list method with parameters should be QUERY")
        void listMethodWithParametersShouldBeQuery() {
            Method method = createMethod("listByStatus", STRING, List.of(param("status", STRING)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("search method should be QUERY")
        void searchMethodShouldBeQuery() {
            Method method = createMethod("searchOrders", STRING, List.of(param("query", STRING)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("count method should be QUERY")
        void countMethodShouldBeQuery() {
            Method method = createMethod("countActive", LONG, List.of());
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("exists method should be QUERY")
        void existsMethodShouldBeQuery() {
            Method method = createMethod("existsById", BOOLEAN, List.of(param("id", LONG)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("is method should be QUERY")
        void isMethodShouldBeQuery() {
            Method method = createMethod("isActive", BOOLEAN, List.of(param("id", LONG)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("has method should be QUERY")
        void hasMethodShouldBeQuery() {
            Method method = createMethod("hasPermission", BOOLEAN, List.of(param("userId", LONG)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("load method should be QUERY")
        void loadMethodShouldBeQuery() {
            Method method = createMethod("loadUser", STRING, List.of(param("id", LONG)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("fetch method should be QUERY")
        void fetchMethodShouldBeQuery() {
            Method method = createMethod("fetchData", STRING, List.of(param("id", LONG)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.QUERY);
        }

        @Test
        @DisplayName("method with return and non-query name should be COMMAND_QUERY")
        void methodWithReturnAndNonQueryNameShouldBeCommandQuery() {
            Method method = createMethod("createAndReturn", STRING, List.of(param("data", STRING)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.COMMAND_QUERY);
        }

        @Test
        @DisplayName("update method with return should be COMMAND_QUERY")
        void updateMethodWithReturnShouldBeCommandQuery() {
            Method method = createMethod("updateOrder", STRING, List.of(param("order", STRING)));
            assertThat(UseCase.deriveType(method)).isEqualTo(UseCase.UseCaseType.COMMAND_QUERY);
        }

        @Test
        @DisplayName("deriveType should reject null method")
        void deriveTypeShouldRejectNullMethod() {
            assertThatThrownBy(() -> UseCase.deriveType(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("UseCaseType")
    class UseCaseTypeTests {

        @Test
        @DisplayName("COMMAND should be command but not query")
        void commandShouldBeCommandButNotQuery() {
            assertThat(UseCase.UseCaseType.COMMAND.isCommand()).isTrue();
            assertThat(UseCase.UseCaseType.COMMAND.isQuery()).isFalse();
        }

        @Test
        @DisplayName("QUERY should be query but not command")
        void queryShouldBeQueryButNotCommand() {
            assertThat(UseCase.UseCaseType.QUERY.isQuery()).isTrue();
            assertThat(UseCase.UseCaseType.QUERY.isCommand()).isFalse();
        }

        @Test
        @DisplayName("COMMAND_QUERY should be both")
        void commandQueryShouldBeBoth() {
            assertThat(UseCase.UseCaseType.COMMAND_QUERY.isCommand()).isTrue();
            assertThat(UseCase.UseCaseType.COMMAND_QUERY.isQuery()).isTrue();
        }

        @Test
        @DisplayName("should have exactly 3 values")
        void shouldHaveExactlyThreeValues() {
            assertThat(UseCase.UseCaseType.values()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("UseCase Behavior")
    class UseCaseBehavior {

        @Test
        @DisplayName("isCommand should delegate to type")
        void isCommandShouldDelegateToType() {
            Method method = createMethod("test", VOID, List.of());
            UseCase command = UseCase.of(method, UseCase.UseCaseType.COMMAND);
            UseCase query = UseCase.of(method, UseCase.UseCaseType.QUERY);

            assertThat(command.isCommand()).isTrue();
            assertThat(query.isCommand()).isFalse();
        }

        @Test
        @DisplayName("isQuery should delegate to type")
        void isQueryShouldDelegateToType() {
            Method method = createMethod("test", STRING, List.of());
            UseCase query = UseCase.of(method, UseCase.UseCaseType.QUERY);
            UseCase command = UseCase.of(method, UseCase.UseCaseType.COMMAND);

            assertThat(query.isQuery()).isTrue();
            assertThat(command.isQuery()).isFalse();
        }

        @Test
        @DisplayName("hasDescription should return true when description present")
        void hasDescriptionShouldReturnTrueWhenPresent() {
            Method method = createMethod("test", VOID, List.of());
            UseCase withDesc = new UseCase(method, Optional.of("Description"), UseCase.UseCaseType.COMMAND);
            UseCase withoutDesc = new UseCase(method, Optional.empty(), UseCase.UseCaseType.COMMAND);

            assertThat(withDesc.hasDescription()).isTrue();
            assertThat(withoutDesc.hasDescription()).isFalse();
        }

        @Test
        @DisplayName("name should return method name")
        void nameShouldReturnMethodName() {
            Method method = createMethod("processOrder", VOID, List.of());
            UseCase useCase = UseCase.of(method, UseCase.UseCaseType.COMMAND);

            assertThat(useCase.name()).isEqualTo("processOrder");
        }
    }

    private Method createMethod(String name, TypeRef returnType, List<Parameter> parameters) {
        return new Method(
                name, returnType, parameters, Set.of(), List.of(), Optional.empty(), List.of(), Set.of(),
                OptionalInt.empty());
    }

    private Parameter param(String name, TypeRef type) {
        return new Parameter(name, type, List.of());
    }
}
