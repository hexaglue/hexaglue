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

package io.hexaglue.plugin.jpa.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.spi.ir.MethodKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FindByIdMethodStrategy}.
 *
 * <p>These tests validate the FIND_BY_ID strategy implementation, ensuring it
 * correctly handles wrapped vs unwrapped identity types and generates proper
 * adapter method bodies.
 *
 * @since 3.0.0
 */
@DisplayName("FindByIdMethodStrategy")
class FindByIdMethodStrategyTest {

    private static final String TEST_PKG = "com.example.domain";
    private static final TypeName DOMAIN_TYPE = ClassName.get(TEST_PKG, "Order");
    private static final TypeName ENTITY_TYPE = ClassName.get("com.example.infrastructure.jpa", "OrderEntity");
    private static final TypeName OPTIONAL_ORDER = ClassName.get("java.util", "Optional");
    private static final TypeName UUID_TYPE = ClassName.get("java.util", "UUID");
    private static final TypeName ORDER_ID_TYPE = ClassName.get(TEST_PKG, "OrderId");

    private FindByIdMethodStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FindByIdMethodStrategy();
    }

    @Nested
    @DisplayName("supports()")
    class Supports {

        @Test
        @DisplayName("should return true for FIND_BY_ID kind")
        void shouldReturnTrueForFindByIdKind() {
            // Given
            AdapterMethodSpec method = createFindByIdMethod("findById", UUID_TYPE, true);

            // When
            boolean supports = strategy.supports(method);

            // Then
            assertThat(supports).isTrue();
        }

        @Test
        @DisplayName("should return false for FIND_BY_PROPERTY kind")
        void shouldReturnFalseForFindByPropertyKind() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findByEmail",
                    OPTIONAL_ORDER,
                    List.of(new AdapterMethodSpec.ParameterInfo("email", ClassName.get("java.lang", "String"), false)),
                    MethodKind.FIND_BY_PROPERTY,
                    Optional.of("email"));

            // When
            boolean supports = strategy.supports(method);

            // Then
            assertThat(supports).isFalse();
        }

        @Test
        @DisplayName("should return false for EXISTS_BY_ID kind")
        void shouldReturnFalseForExistsByIdKind() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "existsById",
                    TypeName.BOOLEAN,
                    List.of(new AdapterMethodSpec.ParameterInfo("id", UUID_TYPE, true)),
                    MethodKind.EXISTS_BY_ID,
                    Optional.empty());

            // When
            boolean supports = strategy.supports(method);

            // Then
            assertThat(supports).isFalse();
        }

        @Test
        @DisplayName("should return false for SAVE kind")
        void shouldReturnFalseForSaveKind() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "save",
                    DOMAIN_TYPE,
                    List.of(new AdapterMethodSpec.ParameterInfo("order", DOMAIN_TYPE, false)),
                    MethodKind.SAVE,
                    Optional.empty());

            // When
            boolean supports = strategy.supports(method);

            // Then
            assertThat(supports).isFalse();
        }

        @Test
        @DisplayName("should return false for FIND_ALL kind")
        void shouldReturnFalseForFindAllKind() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findAll",
                    ClassName.get("java.util", "List"),
                    List.of(),
                    MethodKind.FIND_ALL,
                    Optional.empty());

            // When
            boolean supports = strategy.supports(method);

            // Then
            assertThat(supports).isFalse();
        }
    }

    @Nested
    @DisplayName("generate() with unwrapped ID")
    class GenerateWithUnwrappedId {

        private AdapterContext context;

        @BeforeEach
        void setUp() {
            // Context for unwrapped ID (raw UUID)
            AdapterContext.IdInfo idInfo = AdapterContext.IdInfo.unwrapped(UUID_TYPE);
            context = new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "repository", "mapper", idInfo);
        }

        @Test
        @DisplayName("should generate correct method signature")
        void shouldGenerateCorrectMethodSignature() {
            // Given
            AdapterMethodSpec method = createFindByIdMethod("findById", UUID_TYPE, true);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("public java.util.Optional findById(java.util.UUID id)");
        }

        @Test
        @DisplayName("should add Override annotation")
        void shouldAddOverrideAnnotation() {
            // Given
            AdapterMethodSpec method = createFindByIdMethod("findById", UUID_TYPE, true);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("@java.lang.Override");
        }

        @Test
        @DisplayName("should use ID directly without mapper.map()")
        void shouldUseIdDirectlyWithoutMapperMap() {
            // Given: Unwrapped ID - should NOT use mapper.map()
            AdapterMethodSpec method = createFindByIdMethod("findById", UUID_TYPE, true);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Should pass id directly, not mapper.map(id)
            assertThat(methodCode)
                    .contains("return repository.findById(id)")
                    .doesNotContain("mapper.map(id)");
        }

        @Test
        @DisplayName("should include mapper::toDomain for result mapping")
        void shouldIncludeMapperToDomainForResultMapping() {
            // Given
            AdapterMethodSpec method = createFindByIdMethod("findById", UUID_TYPE, true);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains(".map(mapper::toDomain)");
        }

        @Test
        @DisplayName("should handle getById method name")
        void shouldHandleGetByIdMethodName() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "getById",
                    OPTIONAL_ORDER,
                    List.of(new AdapterMethodSpec.ParameterInfo("id", UUID_TYPE, true)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("public java.util.Optional getById(java.util.UUID id)");
        }

        @Test
        @DisplayName("should handle loadById method name")
        void shouldHandleLoadByIdMethodName() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "loadById",
                    OPTIONAL_ORDER,
                    List.of(new AdapterMethodSpec.ParameterInfo("id", UUID_TYPE, true)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("public java.util.Optional loadById(java.util.UUID id)");
        }
    }

    @Nested
    @DisplayName("generate() with wrapped ID")
    class GenerateWithWrappedId {

        private AdapterContext context;

        @BeforeEach
        void setUp() {
            // Context for wrapped ID (OrderId wrapping UUID)
            AdapterContext.IdInfo idInfo = new AdapterContext.IdInfo(ORDER_ID_TYPE, UUID_TYPE, true);
            context = new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "repository", "mapper", idInfo);
        }

        @Test
        @DisplayName("should generate correct method signature with wrapped ID")
        void shouldGenerateCorrectMethodSignatureWithWrappedId() {
            // Given
            AdapterMethodSpec method = createFindByIdMethod("findById", ORDER_ID_TYPE, true);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("findById(com.example.domain.OrderId orderId)");
        }

        @Test
        @DisplayName("should use mapper.map() to unwrap ID")
        void shouldUseMapperMapToUnwrapId() {
            // Given: Wrapped ID - should use mapper.map() to unwrap
            AdapterMethodSpec method = createFindByIdMethod("findById", ORDER_ID_TYPE, true);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Should use mapper.map(orderId) to unwrap
            assertThat(methodCode)
                    .contains("mapper.map(orderId)")
                    .contains("return repository.findById(mapper.map(orderId))");
        }

        @Test
        @DisplayName("should include mapper::toDomain for result mapping")
        void shouldIncludeMapperToDomainForResultMapping() {
            // Given
            AdapterMethodSpec method = createFindByIdMethod("findById", ORDER_ID_TYPE, true);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains(".map(mapper::toDomain)");
        }
    }

    @Nested
    @DisplayName("generate() edge cases")
    class GenerateEdgeCases {

        @Test
        @DisplayName("should throw for method without parameters")
        void shouldThrowForMethodWithoutParameters() {
            // Given
            AdapterContext context = new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "repository", "mapper", null);
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findById", OPTIONAL_ORDER, List.of(), MethodKind.FIND_BY_ID, Optional.empty());

            // When/Then
            assertThatThrownBy(() -> strategy.generate(method, context))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("FIND_BY_ID method must have at least one parameter");
        }

        @Test
        @DisplayName("should use custom field names from context")
        void shouldUseCustomFieldNamesFromContext() {
            // Given
            AdapterContext.IdInfo idInfo = AdapterContext.IdInfo.unwrapped(UUID_TYPE);
            AdapterContext customContext =
                    new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "jpaRepository", "entityMapper", idInfo);
            AdapterMethodSpec method = createFindByIdMethod("findById", UUID_TYPE, true);

            // When
            MethodSpec generatedMethod = strategy.generate(method, customContext);
            String methodCode = generatedMethod.toString();

            // Then: Should use custom field names
            assertThat(methodCode)
                    .contains("jpaRepository.findById(id)")
                    .contains("entityMapper::toDomain");
        }

        @Test
        @DisplayName("should preserve custom parameter name")
        void shouldPreserveCustomParameterName() {
            // Given
            AdapterContext.IdInfo idInfo = AdapterContext.IdInfo.unwrapped(UUID_TYPE);
            AdapterContext context = new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "repository", "mapper", idInfo);
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findById",
                    OPTIONAL_ORDER,
                    List.of(new AdapterMethodSpec.ParameterInfo("identifier", UUID_TYPE, true)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Should preserve custom parameter name
            assertThat(methodCode)
                    .contains("java.util.UUID identifier")
                    .contains("findById(identifier)");
        }

        @Test
        @DisplayName("should handle non-identity parameter correctly (no unwrapping)")
        void shouldHandleNonIdentityParameterCorrectly() {
            // Given: Parameter marked as NOT identity (edge case - unusual for findById)
            AdapterContext.IdInfo idInfo = new AdapterContext.IdInfo(ORDER_ID_TYPE, UUID_TYPE, true);
            AdapterContext context = new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "repository", "mapper", idInfo);
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findById",
                    OPTIONAL_ORDER,
                    List.of(new AdapterMethodSpec.ParameterInfo("id", UUID_TYPE, false)), // NOT identity
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Should NOT use mapper.map() even though context has wrapped ID
            assertThat(methodCode)
                    .contains("findById(id)")
                    .doesNotContain("mapper.map(id)");
        }
    }

    @Nested
    @DisplayName("Critical bug fix: sample-starwars")
    class CriticalBugFixSampleStarwars {

        @Test
        @DisplayName("getById with raw UUID should NOT use mapper.map()")
        void getByIdWithRawUuidShouldNotUseMapperMap() {
            // Given: This was causing "mapper.map(UUID) not found" errors in sample-starwars
            AdapterContext.IdInfo idInfo = AdapterContext.IdInfo.unwrapped(UUID_TYPE);
            AdapterContext context = new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "repository", "mapper", idInfo);

            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "getById",
                    OPTIONAL_ORDER,
                    List.of(new AdapterMethodSpec.ParameterInfo("id", UUID_TYPE, true)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Should NOT generate mapper.map() for raw UUID
            assertThat(methodCode)
                    .as("Raw UUID should be passed directly, not through mapper.map()")
                    .contains("repository.findById(id)")
                    .doesNotContain("mapper.map(id)");
        }

        @Test
        @DisplayName("getById with direct return type should use .orElse(null)")
        void getByIdWithDirectReturnShouldUseOrElseNull() {
            // Given: Port returns direct type (Fleet), not Optional<Fleet>
            AdapterContext.IdInfo idInfo = AdapterContext.IdInfo.unwrapped(UUID_TYPE);
            AdapterContext context = new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "repository", "mapper", idInfo);

            // Direct return type (not Optional)
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "getById",
                    DOMAIN_TYPE, // Direct return, not OPTIONAL_ORDER
                    List.of(new AdapterMethodSpec.ParameterInfo("id", UUID_TYPE, true)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Should use .orElse(null) for direct return type
            assertThat(methodCode)
                    .as("Direct return type should use .orElse(null)")
                    .contains(".orElse(null)");
        }
    }

    /**
     * Helper method to create a FIND_BY_ID method spec.
     */
    private AdapterMethodSpec createFindByIdMethod(String methodName, TypeName idType, boolean isIdentity) {
        return AdapterMethodSpec.of(
                methodName,
                OPTIONAL_ORDER,
                List.of(new AdapterMethodSpec.ParameterInfo(
                        idType.equals(ORDER_ID_TYPE) ? "orderId" : "id", idType, isIdentity)),
                MethodKind.FIND_BY_ID,
                Optional.empty());
    }
}
