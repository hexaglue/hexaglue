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
 * Unit tests for {@link FindByPropertyMethodStrategy}.
 *
 * <p>These tests validate the FIND_BY_PROPERTY strategy implementation, ensuring
 * it generates correct adapter method bodies for property-based queries.
 *
 * @since 3.0.0
 */
@DisplayName("FindByPropertyMethodStrategy")
class FindByPropertyMethodStrategyTest {

    private static final String TEST_PKG = "com.example.domain";
    private static final TypeName DOMAIN_TYPE = ClassName.get(TEST_PKG, "User");
    private static final TypeName ENTITY_TYPE = ClassName.get("com.example.infrastructure.jpa", "UserEntity");
    private static final TypeName OPTIONAL_USER = ClassName.get("java.util", "Optional");
    private static final TypeName LIST_USER = ClassName.get("java.util", "List");

    private FindByPropertyMethodStrategy strategy;
    private AdapterContext context;

    @BeforeEach
    void setUp() {
        strategy = new FindByPropertyMethodStrategy();
        context = new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "repository", "mapper", null, true);
    }

    @Nested
    @DisplayName("supports()")
    class Supports {

        @Test
        @DisplayName("should return true for FIND_BY_PROPERTY kind")
        void shouldReturnTrueForFindByPropertyKind() {
            // Given
            AdapterMethodSpec method = createFindByPropertyMethod("findByEmail", "email", OPTIONAL_USER);

            // When
            boolean supports = strategy.supports(method);

            // Then
            assertThat(supports).isTrue();
        }

        @Test
        @DisplayName("should return true for FIND_ALL_BY_PROPERTY kind")
        void shouldReturnTrueForFindAllByPropertyKind() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findAllByStatus",
                    LIST_USER,
                    List.of(new AdapterMethodSpec.ParameterInfo("status", ClassName.get("java.lang", "String"), false)),
                    MethodKind.FIND_ALL_BY_PROPERTY,
                    Optional.of("status"));

            // When
            boolean supports = strategy.supports(method);

            // Then
            assertThat(supports).isTrue();
        }

        @Test
        @DisplayName("should return false for FIND_BY_ID kind")
        void shouldReturnFalseForFindByIdKind() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findById",
                    OPTIONAL_USER,
                    List.of(new AdapterMethodSpec.ParameterInfo("id", ClassName.get("java.util", "UUID"), true)),
                    MethodKind.FIND_BY_ID,
                    Optional.empty());

            // When
            boolean supports = strategy.supports(method);

            // Then
            assertThat(supports).isFalse();
        }

        @Test
        @DisplayName("should return false for EXISTS_BY_PROPERTY kind")
        void shouldReturnFalseForExistsByPropertyKind() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "existsByEmail",
                    TypeName.BOOLEAN,
                    List.of(new AdapterMethodSpec.ParameterInfo("email", ClassName.get("java.lang", "String"), false)),
                    MethodKind.EXISTS_BY_PROPERTY,
                    Optional.of("email"));

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
                    List.of(new AdapterMethodSpec.ParameterInfo("user", DOMAIN_TYPE, false)),
                    MethodKind.SAVE,
                    Optional.empty());

            // When
            boolean supports = strategy.supports(method);

            // Then
            assertThat(supports).isFalse();
        }
    }

    @Nested
    @DisplayName("generate() with Optional return")
    class GenerateWithOptionalReturn {

        @Test
        @DisplayName("should generate correct method signature")
        void shouldGenerateCorrectMethodSignature() {
            // Given
            AdapterMethodSpec method = createFindByPropertyMethod("findByEmail", "email", OPTIONAL_USER);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("public java.util.Optional findByEmail(java.lang.String email)");
        }

        @Test
        @DisplayName("should add Override annotation")
        void shouldAddOverrideAnnotation() {
            // Given
            AdapterMethodSpec method = createFindByPropertyMethod("findByEmail", "email", OPTIONAL_USER);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("@java.lang.Override");
        }

        @Test
        @DisplayName("should delegate to repository method with same name")
        void shouldDelegateToRepositoryMethodWithSameName() {
            // Given
            AdapterMethodSpec method = createFindByPropertyMethod("findByEmail", "email", OPTIONAL_USER);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("repository.findByEmail(email)");
        }

        @Test
        @DisplayName("should include .map(mapper::toDomain) for Optional result")
        void shouldIncludeMapToDomainForOptionalResult() {
            // Given
            AdapterMethodSpec method = createFindByPropertyMethod("findByEmail", "email", OPTIONAL_USER);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains(".map(mapper::toDomain)");
        }

        @Test
        @DisplayName("should generate complete statement for Optional return")
        void shouldGenerateCompleteStatementForOptionalReturn() {
            // Given
            AdapterMethodSpec method = createFindByPropertyMethod("findByEmail", "email", OPTIONAL_USER);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("return repository.findByEmail(email).map(mapper::toDomain)");
        }
    }

    @Nested
    @DisplayName("generate() with List return")
    class GenerateWithListReturn {

        @Test
        @DisplayName("should generate correct method signature")
        void shouldGenerateCorrectMethodSignature() {
            // Given
            AdapterMethodSpec method = createFindByPropertyMethod("findByStatus", "status", LIST_USER);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("public java.util.List findByStatus(java.lang.String status)");
        }

        @Test
        @DisplayName("should include .stream().map().toList() for List result")
        void shouldIncludeStreamMapToListForListResult() {
            // Given
            AdapterMethodSpec method = createFindByPropertyMethod("findByStatus", "status", LIST_USER);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode)
                    .contains(".stream()")
                    .contains(".map(mapper::toDomain)")
                    .contains(".toList()");
        }

        @Test
        @DisplayName("should generate complete statement for List return")
        void shouldGenerateCompleteStatementForListReturn() {
            // Given
            AdapterMethodSpec method = createFindByPropertyMethod("findByStatus", "status", LIST_USER);

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode)
                    .contains("return repository.findByStatus(status).stream().map(mapper::toDomain).toList()");
        }

        @Test
        @DisplayName("should handle findAllByProperty method")
        void shouldHandleFindAllByPropertyMethod() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findAllByActive",
                    LIST_USER,
                    List.of(new AdapterMethodSpec.ParameterInfo("active", TypeName.BOOLEAN, false)),
                    MethodKind.FIND_ALL_BY_PROPERTY,
                    Optional.of("active"));

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode)
                    .contains("public java.util.List findAllByActive(boolean active)")
                    .contains("return repository.findAllByActive(active).stream().map(mapper::toDomain).toList()");
        }
    }

    @Nested
    @DisplayName("generate() with direct return (nullable)")
    class GenerateWithDirectReturn {

        @Test
        @DisplayName("should handle direct return type with null check")
        void shouldHandleDirectReturnTypeWithNullCheck() {
            // Given: Method with direct domain return type (not Optional, not List)
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findByUsername",
                    DOMAIN_TYPE,
                    List.of(new AdapterMethodSpec.ParameterInfo(
                            "username", ClassName.get("java.lang", "String"), false)),
                    MethodKind.FIND_BY_PROPERTY,
                    Optional.of("username"));

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Should include null check
            assertThat(methodCode)
                    .contains("var entity = repository.findByUsername(username)")
                    .contains("return entity != null ? mapper.toDomain(entity) : null");
        }
    }

    @Nested
    @DisplayName("generate() with multiple parameters")
    class GenerateWithMultipleParameters {

        @Test
        @DisplayName("should handle multiple parameters correctly")
        void shouldHandleMultipleParametersCorrectly() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findByFirstNameAndLastName",
                    OPTIONAL_USER,
                    List.of(
                            new AdapterMethodSpec.ParameterInfo(
                                    "firstName", ClassName.get("java.lang", "String"), false),
                            new AdapterMethodSpec.ParameterInfo(
                                    "lastName", ClassName.get("java.lang", "String"), false)),
                    MethodKind.FIND_BY_PROPERTY,
                    Optional.of("firstName"));

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Code may be formatted with line breaks between parameters
            assertThat(methodCode)
                    .contains("java.lang.String firstName")
                    .contains("java.lang.String lastName")
                    .contains("repository.findByFirstNameAndLastName(firstName, lastName)");
        }
    }

    @Nested
    @DisplayName("generate() edge cases")
    class GenerateEdgeCases {

        @Test
        @DisplayName("should throw for method without parameters")
        void shouldThrowForMethodWithoutParameters() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findByEmail", OPTIONAL_USER, List.of(), MethodKind.FIND_BY_PROPERTY, Optional.of("email"));

            // When/Then
            assertThatThrownBy(() -> strategy.generate(method, context))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("FIND_BY_PROPERTY method must have at least one parameter");
        }

        @Test
        @DisplayName("should use custom field names from context")
        void shouldUseCustomFieldNamesFromContext() {
            // Given
            AdapterContext customContext =
                    new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "jpaRepo", "entityMapper", null, true);
            AdapterMethodSpec method = createFindByPropertyMethod("findByEmail", "email", OPTIONAL_USER);

            // When
            MethodSpec generatedMethod = strategy.generate(method, customContext);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("jpaRepo.findByEmail(email)").contains("entityMapper::toDomain");
        }

        @Test
        @DisplayName("should preserve custom parameter names")
        void shouldPreserveCustomParameterNames() {
            // Given
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findByEmail",
                    OPTIONAL_USER,
                    List.of(new AdapterMethodSpec.ParameterInfo(
                            "emailAddress", ClassName.get("java.lang", "String"), false)),
                    MethodKind.FIND_BY_PROPERTY,
                    Optional.of("email"));

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then
            assertThat(methodCode).contains("java.lang.String emailAddress").contains("findByEmail(emailAddress)");
        }

        @Test
        @DisplayName("should handle Set return type as collection")
        void shouldHandleSetReturnTypeAsCollection() {
            // Given
            TypeName setUser = ClassName.get("java.util", "Set");
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findByRole",
                    setUser,
                    List.of(new AdapterMethodSpec.ParameterInfo("role", ClassName.get("java.lang", "String"), false)),
                    MethodKind.FIND_BY_PROPERTY,
                    Optional.of("role"));

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Should use stream().map().toList() pattern (Set is a collection)
            assertThat(methodCode)
                    .contains(".stream()")
                    .contains(".map(mapper::toDomain)")
                    .contains(".toList()");
        }
    }

    @Nested
    @DisplayName("Critical bug fix: sample-multi-aggregate")
    class CriticalBugFixSampleMultiAggregate {

        @Test
        @DisplayName("findByEmail should NOT delegate to existsById")
        void findByEmailShouldNotDelegateToExistsById() {
            // Given: This was causing issues where findByEmail was incorrectly handled
            AdapterMethodSpec method = AdapterMethodSpec.of(
                    "findByEmail",
                    OPTIONAL_USER,
                    List.of(new AdapterMethodSpec.ParameterInfo("email", ClassName.get(TEST_PKG, "Email"), false)),
                    MethodKind.FIND_BY_PROPERTY,
                    Optional.of("email"));

            // When
            MethodSpec generatedMethod = strategy.generate(method, context);
            String methodCode = generatedMethod.toString();

            // Then: Should delegate to repository.findByEmail, NOT repository.findById
            assertThat(methodCode)
                    .as("findByEmail should delegate to repository.findByEmail()")
                    .contains("repository.findByEmail(email)")
                    .doesNotContain("repository.findById")
                    .doesNotContain("repository.existsById");
        }
    }

    /**
     * Helper method to create a FIND_BY_PROPERTY method spec.
     */
    private AdapterMethodSpec createFindByPropertyMethod(
            String methodName, String targetProperty, TypeName returnType) {
        return AdapterMethodSpec.of(
                methodName,
                returnType,
                List.of(new AdapterMethodSpec.ParameterInfo(
                        targetProperty, ClassName.get("java.lang", "String"), false)),
                MethodKind.FIND_BY_PROPERTY,
                Optional.of(targetProperty));
    }
}
