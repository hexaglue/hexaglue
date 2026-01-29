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

package io.hexaglue.plugin.jpa.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.model.ir.MethodKind;
import io.hexaglue.arch.model.ir.MethodParameter;
import io.hexaglue.arch.model.ir.PortMethod;
import io.hexaglue.arch.model.ir.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AdapterMethodSpec}.
 *
 * <p>These tests validate that AdapterMethodSpec correctly consumes information
 * from the SPI (PortMethod) without re-inferring classification.
 *
 * @since 3.0.0
 */
@DisplayName("AdapterMethodSpec")
class AdapterMethodSpecTest {

    private static final String TEST_PKG = "com.example.domain";

    @Nested
    @DisplayName("from(PortMethod)")
    class FromPortMethod {

        @Test
        @DisplayName("should preserve method name from SPI")
        void shouldPreserveMethodName() {
            // Given
            PortMethod portMethod = PortMethod.of(
                    "findByEmail",
                    TypeRef.of("java.util.Optional"),
                    List.of(MethodParameter.simple("email", TypeRef.of("java.lang.String"))),
                    MethodKind.FIND_BY_PROPERTY);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then
            assertThat(spec.name()).isEqualTo("findByEmail");
        }

        @Test
        @DisplayName("should consume MethodKind directly from SPI")
        void shouldConsumeMethodKindFromSpi() {
            // Given: existsByEmail classified as EXISTS_BY_PROPERTY by Core
            PortMethod portMethod = PortMethod.withProperty(
                    "existsByEmail",
                    TypeRef.of("boolean"),
                    List.of(MethodParameter.simple("email", TypeRef.of(TEST_PKG + ".Email"))),
                    MethodKind.EXISTS_BY_PROPERTY,
                    "email");

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then: Must NOT be re-classified as EXISTS_BY_ID
            assertThat(spec.kind()).isEqualTo(MethodKind.EXISTS_BY_PROPERTY);
        }

        @Test
        @DisplayName("should preserve target property from SPI")
        void shouldPreserveTargetProperty() {
            // Given
            PortMethod portMethod = PortMethod.withProperty(
                    "findByStatus",
                    TypeRef.of("java.util.List"),
                    List.of(MethodParameter.simple("status", TypeRef.of("java.lang.String"))),
                    MethodKind.FIND_BY_PROPERTY,
                    "status");

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then
            assertThat(spec.targetProperty()).isPresent();
            assertThat(spec.targetProperty().get()).isEqualTo("status");
        }

        @Test
        @DisplayName("should map parameter isIdentity flag from SPI")
        void shouldMapParameterIsIdentityFlag() {
            // Given: findById with ID parameter marked as identity
            MethodParameter idParam = MethodParameter.of("orderId", TypeRef.of(TEST_PKG + ".OrderId"), true);
            PortMethod portMethod = PortMethod.of(
                    "findById", TypeRef.of("java.util.Optional"), List.of(idParam), MethodKind.FIND_BY_ID);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then
            assertThat(spec.parameters()).hasSize(1);
            assertThat(spec.firstParameter()).isPresent();
            assertThat(spec.firstParameter().get().isIdentity()).isTrue();
        }

        @Test
        @DisplayName("should map non-identity parameter correctly")
        void shouldMapNonIdentityParameter() {
            // Given: findByEmail with non-identity parameter
            MethodParameter emailParam = MethodParameter.simple("email", TypeRef.of("java.lang.String"));
            PortMethod portMethod = PortMethod.of(
                    "findByEmail", TypeRef.of("java.util.Optional"), List.of(emailParam), MethodKind.FIND_BY_PROPERTY);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then
            assertThat(spec.firstParameter()).isPresent();
            assertThat(spec.firstParameter().get().isIdentity()).isFalse();
        }

        @Test
        @DisplayName("should handle multiple parameters")
        void shouldHandleMultipleParameters() {
            // Given: custom method with multiple parameters
            List<MethodParameter> params = List.of(
                    MethodParameter.simple("status", TypeRef.of("java.lang.String")),
                    MethodParameter.simple("limit", TypeRef.of("int")));
            PortMethod portMethod =
                    PortMethod.of("findByStatusWithLimit", TypeRef.of("java.util.List"), params, MethodKind.CUSTOM);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then
            assertThat(spec.parameters()).hasSize(2);
            assertThat(spec.parameters().get(0).name()).isEqualTo("status");
            assertThat(spec.parameters().get(1).name()).isEqualTo("limit");
        }
    }

    @Nested
    @DisplayName("Return type helpers")
    class ReturnTypeHelpers {

        @Test
        @DisplayName("returnsOptional should return true for Optional return type")
        void returnsOptionalShouldReturnTrueForOptional() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "findById",
                    ClassName.get("java.util", "Optional"),
                    List.of(new AdapterMethodSpec.ParameterInfo("id", ClassName.get("java.util", "UUID"), true)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // Then
            assertThat(spec.returnsOptional()).isTrue();
            assertThat(spec.returnsCollection()).isFalse();
        }

        @Test
        @DisplayName("returnsCollection should return true for List return type")
        void returnsCollectionShouldReturnTrueForList() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "findAll", ClassName.get("java.util", "List"), List.of(), MethodKind.FIND_ALL, Optional.empty());

            // Then
            assertThat(spec.returnsCollection()).isTrue();
            assertThat(spec.returnsOptional()).isFalse();
        }

        @Test
        @DisplayName("returnsCollection should return true for Set return type")
        void returnsCollectionShouldReturnTrueForSet() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "findAllActive",
                    ClassName.get("java.util", "Set"),
                    List.of(),
                    MethodKind.FIND_ALL,
                    Optional.empty());

            // Then
            assertThat(spec.returnsCollection()).isTrue();
        }

        @Test
        @DisplayName("hasReturnValue should return false for void")
        void hasReturnValueShouldReturnFalseForVoid() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "delete",
                    TypeName.VOID,
                    List.of(new AdapterMethodSpec.ParameterInfo("entity", ClassName.get(TEST_PKG, "Order"), false)),
                    MethodKind.DELETE_ALL,
                    Optional.empty());

            // Then
            assertThat(spec.hasReturnValue()).isFalse();
        }

        @Test
        @DisplayName("hasReturnValue should return true for non-void")
        void hasReturnValueShouldReturnTrueForNonVoid() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "save",
                    ClassName.get(TEST_PKG, "Order"),
                    List.of(new AdapterMethodSpec.ParameterInfo("order", ClassName.get(TEST_PKG, "Order"), false)),
                    MethodKind.SAVE,
                    Optional.empty());

            // Then
            assertThat(spec.hasReturnValue()).isTrue();
        }
    }

    @Nested
    @DisplayName("Parameter helpers")
    class ParameterHelpers {

        @Test
        @DisplayName("hasParameters should return true when parameters exist")
        void hasParametersShouldReturnTrueWhenParametersExist() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "findById",
                    ClassName.get("java.util", "Optional"),
                    List.of(new AdapterMethodSpec.ParameterInfo("id", ClassName.get("java.util", "UUID"), true)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // Then
            assertThat(spec.hasParameters()).isTrue();
        }

        @Test
        @DisplayName("hasParameters should return false when no parameters")
        void hasParametersShouldReturnFalseWhenNoParameters() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "findAll", ClassName.get("java.util", "List"), List.of(), MethodKind.FIND_ALL, Optional.empty());

            // Then
            assertThat(spec.hasParameters()).isFalse();
        }

        @Test
        @DisplayName("firstParameter should return empty for method without parameters")
        void firstParameterShouldReturnEmptyForMethodWithoutParameters() {
            // Given
            AdapterMethodSpec spec =
                    AdapterMethodSpec.of("count", TypeName.LONG, List.of(), MethodKind.COUNT_ALL, Optional.empty());

            // Then
            assertThat(spec.firstParameter()).isEmpty();
        }

        @Test
        @DisplayName("firstParameter should return first parameter when exists")
        void firstParameterShouldReturnFirstParameterWhenExists() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "findById",
                    ClassName.get("java.util", "Optional"),
                    List.of(
                            new AdapterMethodSpec.ParameterInfo("id", ClassName.get("java.util", "UUID"), true),
                            new AdapterMethodSpec.ParameterInfo("flag", TypeName.BOOLEAN, false)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // Then
            assertThat(spec.firstParameter()).isPresent();
            assertThat(spec.firstParameter().get().name()).isEqualTo("id");
        }

        @Test
        @DisplayName("hasIdentityParameter should return true when first param is identity")
        void hasIdentityParameterShouldReturnTrueWhenFirstParamIsIdentity() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "findById",
                    ClassName.get("java.util", "Optional"),
                    List.of(new AdapterMethodSpec.ParameterInfo("id", ClassName.get("java.util", "UUID"), true)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // Then
            assertThat(spec.hasIdentityParameter()).isTrue();
        }

        @Test
        @DisplayName("hasIdentityParameter should return false when first param is not identity")
        void hasIdentityParameterShouldReturnFalseWhenFirstParamIsNotIdentity() {
            // Given
            AdapterMethodSpec spec = AdapterMethodSpec.of(
                    "findByEmail",
                    ClassName.get("java.util", "Optional"),
                    List.of(new AdapterMethodSpec.ParameterInfo("email", ClassName.get("java.lang", "String"), false)),
                    MethodKind.FIND_BY_PROPERTY,
                    Optional.of("email"));

            // Then
            assertThat(spec.hasIdentityParameter()).isFalse();
        }
    }

    @Nested
    @DisplayName("fromV5(Method) - V5 architectural model conversion")
    class FromV5Method {

        @Test
        @DisplayName("C1 BUG: getById should be classified as FIND_BY_ID, not CUSTOM")
        void getByIdShouldBeClassifiedAsFindById() {
            // Given: Port method getById(UUID id) -> Fleet
            // This is the exact scenario from sample-starwars causing UnsupportedOperationException
            io.hexaglue.arch.model.Parameter idParam =
                    io.hexaglue.arch.model.Parameter.of("id", io.hexaglue.syntax.TypeRef.of("java.util.UUID"));
            io.hexaglue.arch.model.Method method = new io.hexaglue.arch.model.Method(
                    "getById",
                    io.hexaglue.syntax.TypeRef.of("rebelsrescue.fleet.Fleet"),
                    List.of(idParam),
                    java.util.Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    java.util.Set.of(),
                    java.util.OptionalInt.empty(),
                    java.util.Optional.empty());

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.fromV5(method);

            // Then: Must be FIND_BY_ID, not CUSTOM (which causes UnsupportedOperationException)
            assertThat(spec.kind())
                    .as("getById must be classified as FIND_BY_ID to generate proper implementation")
                    .isEqualTo(MethodKind.FIND_BY_ID);
        }

        @Test
        @DisplayName("loadById should be classified as FIND_BY_ID")
        void loadByIdShouldBeClassifiedAsFindById() {
            // Given: Alternative naming convention loadById
            io.hexaglue.arch.model.Parameter idParam =
                    io.hexaglue.arch.model.Parameter.of("id", io.hexaglue.syntax.TypeRef.of("java.util.UUID"));
            io.hexaglue.arch.model.Method method = new io.hexaglue.arch.model.Method(
                    "loadById",
                    io.hexaglue.syntax.TypeRef.of("com.example.Order"),
                    List.of(idParam),
                    java.util.Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    java.util.Set.of(),
                    java.util.OptionalInt.empty(),
                    java.util.Optional.empty());

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.fromV5(method);

            // Then
            assertThat(spec.kind())
                    .as("loadById must be classified as FIND_BY_ID")
                    .isEqualTo(MethodKind.FIND_BY_ID);
        }

        @Test
        @DisplayName("findById should still be classified as FIND_BY_ID")
        void findByIdShouldStillBeClassifiedAsFindById() {
            // Given: Standard naming convention findById
            io.hexaglue.arch.model.Parameter idParam =
                    io.hexaglue.arch.model.Parameter.of("id", io.hexaglue.syntax.TypeRef.of("java.util.UUID"));
            io.hexaglue.arch.model.Method method = new io.hexaglue.arch.model.Method(
                    "findById",
                    io.hexaglue.syntax.TypeRef.of("java.util.Optional"),
                    List.of(idParam),
                    java.util.Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    java.util.Set.of(),
                    java.util.OptionalInt.empty(),
                    java.util.Optional.empty());

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.fromV5(method);

            // Then
            assertThat(spec.kind())
                    .as("findById must remain classified as FIND_BY_ID")
                    .isEqualTo(MethodKind.FIND_BY_ID);
        }

        @Test
        @DisplayName("fetchById should be classified as FIND_BY_ID")
        void fetchByIdShouldBeClassifiedAsFindById() {
            // Given: Alternative naming convention fetchById
            io.hexaglue.arch.model.Parameter idParam =
                    io.hexaglue.arch.model.Parameter.of("id", io.hexaglue.syntax.TypeRef.of("java.util.UUID"));
            io.hexaglue.arch.model.Method method = new io.hexaglue.arch.model.Method(
                    "fetchById",
                    io.hexaglue.syntax.TypeRef.of("com.example.Order"),
                    List.of(idParam),
                    java.util.Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    java.util.Set.of(),
                    java.util.OptionalInt.empty(),
                    java.util.Optional.empty());

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.fromV5(method);

            // Then
            assertThat(spec.kind())
                    .as("fetchById must be classified as FIND_BY_ID")
                    .isEqualTo(MethodKind.FIND_BY_ID);
        }
    }

    @Nested
    @DisplayName("Critical bug fixes validation")
    class CriticalBugFixes {

        @Test
        @DisplayName("getById with direct Fleet return should have SINGLE cardinality")
        void getByIdWithDirectReturnShouldHaveSingleCardinality() {
            // Given: Port method getById returns Fleet (not Optional<Fleet>)
            // This is the sample-starwars integration test scenario
            MethodParameter idParam = MethodParameter.of("id", TypeRef.of("java.util.UUID"), true);
            PortMethod portMethod = PortMethod.of(
                    "getById",
                    TypeRef.of("rebelsrescue.fleet.Fleet"), // Direct return, not Optional
                    List.of(idParam),
                    MethodKind.FIND_BY_ID);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then: returnsOptional must be false for direct return type
            assertThat(spec.returnsOptional())
                    .as("Direct return type Fleet should NOT be detected as Optional")
                    .isFalse();
            assertThat(spec.returnCardinality())
                    .as("Direct return type should have SINGLE cardinality")
                    .isEqualTo(io.hexaglue.arch.model.ir.Cardinality.SINGLE);
        }

        @Test
        @DisplayName("findById with Optional<Fleet> return should have OPTIONAL cardinality")
        void findByIdWithOptionalReturnShouldHaveOptionalCardinality() {
            // Given: Port method findById returns Optional<Fleet>
            MethodParameter idParam = MethodParameter.of("id", TypeRef.of("java.util.UUID"), true);
            PortMethod portMethod = PortMethod.of(
                    "findById",
                    TypeRef.parameterized("java.util.Optional", TypeRef.of("rebelsrescue.fleet.Fleet")),
                    List.of(idParam),
                    MethodKind.FIND_BY_ID);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then: returnsOptional must be true for Optional return type
            assertThat(spec.returnsOptional())
                    .as("Optional<Fleet> return type should be detected as Optional")
                    .isTrue();
            assertThat(spec.returnCardinality())
                    .as("Optional return type should have OPTIONAL cardinality")
                    .isEqualTo(io.hexaglue.arch.model.ir.Cardinality.OPTIONAL);
        }

        @Test
        @DisplayName("existsByEmail should NOT be classified as EXISTS_BY_ID")
        void existsByEmailShouldNotBeClassifiedAsExistsById() {
            // Given: This was the bug - existsByEmail was treated as existsById
            // causing "Email cannot be converted to UUID" errors
            PortMethod portMethod = PortMethod.withProperty(
                    "existsByEmail",
                    TypeRef.of("boolean"),
                    List.of(MethodParameter.simple("email", TypeRef.of(TEST_PKG + ".Email"))),
                    MethodKind.EXISTS_BY_PROPERTY, // Correct classification from Core
                    "email");

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then: Must consume SPI classification, not re-infer
            assertThat(spec.kind())
                    .as("existsByEmail should be EXISTS_BY_PROPERTY, not EXISTS_BY_ID")
                    .isEqualTo(MethodKind.EXISTS_BY_PROPERTY);
            assertThat(spec.targetProperty()).isPresent();
            assertThat(spec.targetProperty().get()).isEqualTo("email");
        }

        @Test
        @DisplayName("findById with wrapped ID should have identity parameter marked")
        void findByIdWithWrappedIdShouldHaveIdentityParameterMarked() {
            // Given: findById with TaskId (wrapped UUID)
            MethodParameter wrappedIdParam = MethodParameter.of("taskId", TypeRef.of(TEST_PKG + ".TaskId"), true);
            PortMethod portMethod = PortMethod.of(
                    "findById", TypeRef.of("java.util.Optional"), List.of(wrappedIdParam), MethodKind.FIND_BY_ID);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then: isIdentity flag must be preserved for ID handling
            assertThat(spec.firstParameter()).isPresent();
            assertThat(spec.firstParameter().get().isIdentity())
                    .as("Wrapped ID parameter should be marked as identity")
                    .isTrue();
        }

        @Test
        @DisplayName("findById with unwrapped ID should have identity parameter marked")
        void findByIdWithUnwrappedIdShouldHaveIdentityParameterMarked() {
            // Given: findById with raw UUID
            MethodParameter rawIdParam = MethodParameter.of("id", TypeRef.of("java.util.UUID"), true);
            PortMethod portMethod = PortMethod.of(
                    "findById", TypeRef.of("java.util.Optional"), List.of(rawIdParam), MethodKind.FIND_BY_ID);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then: isIdentity flag must be preserved
            assertThat(spec.firstParameter()).isPresent();
            assertThat(spec.firstParameter().get().isIdentity())
                    .as("Unwrapped ID parameter should be marked as identity")
                    .isTrue();
        }

        @Test
        @DisplayName("findByProperty parameter should NOT be marked as identity")
        void findByPropertyParameterShouldNotBeMarkedAsIdentity() {
            // Given: findByEmail - email is NOT an identity
            MethodParameter emailParam = MethodParameter.simple("email", TypeRef.of("java.lang.String"));
            PortMethod portMethod = PortMethod.withProperty(
                    "findByEmail",
                    TypeRef.of("java.util.Optional"),
                    List.of(emailParam),
                    MethodKind.FIND_BY_PROPERTY,
                    "email");

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then: Email is NOT identity - this affects ID handling logic
            assertThat(spec.firstParameter()).isPresent();
            assertThat(spec.firstParameter().get().isIdentity())
                    .as("Property parameter (email) should NOT be marked as identity")
                    .isFalse();
        }

        @Test
        @DisplayName("returnType should be ParameterizedTypeName for Optional<Pokemon>")
        void returnTypeShouldBeParameterizedTypeNameForOptional() {
            // Given: findById returns Optional<Pokemon> - this is the sample-pokedex scenario
            MethodParameter idParam = MethodParameter.of("pokemonId", TypeRef.of("java.lang.Integer"), true);
            TypeRef optionalPokemon = TypeRef.parameterized(
                    "java.util.Optional", TypeRef.of("com.example.myhexagonalpokedex.domain.pokemon.Pokemon"));
            PortMethod portMethod = PortMethod.of("findById", optionalPokemon, List.of(idParam), MethodKind.FIND_BY_ID);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then: returnType must be ParameterizedTypeName with Pokemon as type argument
            assertThat(spec.returnType())
                    .as("returnType should be a ParameterizedTypeName")
                    .isInstanceOf(ParameterizedTypeName.class);

            ParameterizedTypeName parameterizedType = (ParameterizedTypeName) spec.returnType();
            assertThat(parameterizedType.rawType())
                    .as("Raw type should be Optional")
                    .isEqualTo(ClassName.get("java.util", "Optional"));
            assertThat(parameterizedType.typeArguments())
                    .as("Type arguments should contain Pokemon")
                    .hasSize(1);
            assertThat(parameterizedType.typeArguments().get(0).toString())
                    .as("Type argument should be Pokemon")
                    .isEqualTo("com.example.myhexagonalpokedex.domain.pokemon.Pokemon");
        }

        @Test
        @DisplayName("returnType should be ParameterizedTypeName for List<Order>")
        void returnTypeShouldBeParameterizedTypeNameForList() {
            // Given: findAll returns List<Order>
            TypeRef listOrder = TypeRef.parameterized("java.util.List", TypeRef.of("com.example.domain.order.Order"));
            PortMethod portMethod = PortMethod.of("findAll", listOrder, List.of(), MethodKind.FIND_ALL);

            // When
            AdapterMethodSpec spec = AdapterMethodSpec.from(portMethod);

            // Then: returnType must be ParameterizedTypeName with Order as type argument
            assertThat(spec.returnType())
                    .as("returnType should be a ParameterizedTypeName")
                    .isInstanceOf(ParameterizedTypeName.class);

            ParameterizedTypeName parameterizedType = (ParameterizedTypeName) spec.returnType();
            assertThat(parameterizedType.rawType())
                    .as("Raw type should be List")
                    .isEqualTo(ClassName.get("java.util", "List"));
            assertThat(parameterizedType.typeArguments())
                    .as("Type arguments should contain Order")
                    .hasSize(1);
            assertThat(parameterizedType.typeArguments().get(0).toString())
                    .as("Type argument should be Order")
                    .isEqualTo("com.example.domain.order.Order");
        }
    }
}
