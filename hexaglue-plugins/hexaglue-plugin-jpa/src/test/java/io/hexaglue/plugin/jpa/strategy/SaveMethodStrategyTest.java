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
import io.hexaglue.arch.model.ir.MethodKind;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SaveMethodStrategy}.
 *
 * <p>These tests validate the SAVE strategy implementation, ensuring it generates
 * correct method bodies with mapper and repository calls.
 *
 * @since 3.0.0
 */
class SaveMethodStrategyTest {

    private static final TypeName DOMAIN_TYPE = ClassName.get("com.example.domain", "Order");
    private static final TypeName ENTITY_TYPE = ClassName.get("com.example.infrastructure.jpa", "OrderEntity");

    private SaveMethodStrategy strategy;
    private AdapterContext context;

    @BeforeEach
    void setUp() {
        strategy = new SaveMethodStrategy();
        context = new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "repository", "mapper", null, true);
    }

    @Test
    void supports_shouldReturnTrueForSaveKind() {
        // Given
        AdapterMethodSpec method = createSaveMethod("save");

        // When
        boolean supports = strategy.supports(method);

        // Then
        assertThat(supports).isTrue();
    }

    @Test
    void supports_shouldReturnTrueForSaveAllKind() {
        // Given
        AdapterMethodSpec method = AdapterMethodSpec.of(
                "saveAll",
                ClassName.get("java.util", "List"),
                List.of(new AdapterMethodSpec.ParameterInfo("orders", ClassName.get("java.util", "List"), false)),
                MethodKind.SAVE_ALL,
                Optional.empty());

        // When
        boolean supports = strategy.supports(method);

        // Then
        assertThat(supports).isTrue();
    }

    @Test
    void supports_shouldReturnFalseForFindByIdKind() {
        // Given
        AdapterMethodSpec method = AdapterMethodSpec.of(
                "findById",
                ClassName.get("java.util", "Optional"),
                List.of(new AdapterMethodSpec.ParameterInfo("id", ClassName.get("java.util", "UUID"), true)),
                MethodKind.FIND_BY_ID,
                Optional.empty());

        // When
        boolean supports = strategy.supports(method);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    void supports_shouldReturnFalseForFindAllKind() {
        // Given
        AdapterMethodSpec method = AdapterMethodSpec.of(
                "findAll", ClassName.get("java.util", "List"), List.of(), MethodKind.FIND_ALL, Optional.empty());

        // When
        boolean supports = strategy.supports(method);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    void supports_shouldReturnFalseForDeleteByIdKind() {
        // Given
        AdapterMethodSpec method = AdapterMethodSpec.of(
                "deleteById",
                TypeName.VOID,
                List.of(new AdapterMethodSpec.ParameterInfo("id", ClassName.get("java.util", "UUID"), true)),
                MethodKind.DELETE_BY_ID,
                Optional.empty());

        // When
        boolean supports = strategy.supports(method);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    void supports_shouldReturnFalseForCustomKind() {
        // Given
        AdapterMethodSpec method = AdapterMethodSpec.of(
                "findByStatus",
                ClassName.get("java.util", "List"),
                List.of(new AdapterMethodSpec.ParameterInfo("status", ClassName.get("java.lang", "String"), false)),
                MethodKind.CUSTOM,
                Optional.empty());

        // When
        boolean supports = strategy.supports(method);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    void generate_shouldCreateMethodWithCorrectSignature() {
        // Given
        AdapterMethodSpec method = createSaveMethod("save");

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then
        assertThat(methodCode).contains("public com.example.domain.Order save(com.example.domain.Order order)");
    }

    @Test
    void generate_shouldAddOverrideAnnotation() {
        // Given
        AdapterMethodSpec method = createSaveMethod("save");

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then
        assertThat(methodCode).contains("@java.lang.Override");
    }

    @Test
    void generate_shouldGenerateMapperToEntityCall() {
        // Given
        AdapterMethodSpec method = createSaveMethod("save");

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then
        assertThat(methodCode).contains("var entity = mapper.toEntity(order)");
    }

    @Test
    void generate_shouldGenerateRepositorySaveCall() {
        // Given
        AdapterMethodSpec method = createSaveMethod("save");

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then
        assertThat(methodCode).contains("var saved = repository.save(entity)");
    }

    @Test
    void generate_shouldGenerateMapperToDomainCall() {
        // Given
        AdapterMethodSpec method = createSaveMethod("save");

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then
        assertThat(methodCode).contains("return mapper.toDomain(saved)");
    }

    @Test
    void generate_shouldGenerateCompleteMethodBody() {
        // Given
        AdapterMethodSpec method = createSaveMethod("save");

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then: Verify the complete three-step pattern
        assertThat(methodCode)
                .contains("var entity = mapper.toEntity(order)")
                .contains("var saved = repository.save(entity)")
                .contains("return mapper.toDomain(saved)");
    }

    @Test
    void generate_shouldHandleCreateMethodName() {
        // Given
        AdapterMethodSpec method = createSaveMethod("create");

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then
        assertThat(methodCode)
                .contains("public com.example.domain.Order create(")
                .contains("mapper.toEntity(order)")
                .contains("repository.save(entity)");
    }

    @Test
    void generate_shouldHandlePersistMethodName() {
        // Given
        AdapterMethodSpec method = createSaveMethod("persist");

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then
        assertThat(methodCode)
                .contains("public com.example.domain.Order persist(")
                .contains("mapper.toEntity(order)")
                .contains("repository.save(entity)");
    }

    @Test
    void generate_shouldUseFirstParameterForMapping() {
        // Given: Method with multiple parameters (edge case)
        AdapterMethodSpec method = AdapterMethodSpec.of(
                "save",
                DOMAIN_TYPE,
                List.of(
                        new AdapterMethodSpec.ParameterInfo("order", DOMAIN_TYPE, false),
                        new AdapterMethodSpec.ParameterInfo("flag", TypeName.BOOLEAN, false)),
                MethodKind.SAVE,
                Optional.empty());

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then: Should use first parameter (order)
        assertThat(methodCode)
                .contains("com.example.domain.Order order, boolean flag")
                .contains("mapper.toEntity(order)");
    }

    @Test
    void generate_shouldHandleVoidReturnType() {
        // Given: Save method with void return type
        AdapterMethodSpec method = AdapterMethodSpec.of(
                "save",
                TypeName.VOID,
                List.of(new AdapterMethodSpec.ParameterInfo("order", DOMAIN_TYPE, false)),
                MethodKind.SAVE,
                Optional.empty());

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then: Should not have return statement
        assertThat(methodCode)
                .contains("mapper.toEntity(order)")
                .contains("repository.save(entity)")
                .doesNotContain("return mapper.toDomain(saved)");
    }

    @Test
    void generate_shouldThrowForMethodWithoutParameters() {
        // Given
        AdapterMethodSpec method =
                AdapterMethodSpec.of("save", DOMAIN_TYPE, List.of(), MethodKind.SAVE, Optional.empty());

        // When/Then
        assertThatThrownBy(() -> strategy.generate(method, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SAVE method must have at least one parameter");
    }

    @Test
    void generate_shouldUseCustomFieldNames() {
        // Given
        AdapterContext customContext =
                new AdapterContext(DOMAIN_TYPE, ENTITY_TYPE, "jpaRepo", "entityMapper", null, true);
        AdapterMethodSpec method = createSaveMethod("save");

        // When
        MethodSpec generatedMethod = strategy.generate(method, customContext);
        String methodCode = generatedMethod.toString();

        // Then: Should use custom field names
        assertThat(methodCode)
                .contains("entityMapper.toEntity(order)")
                .contains("jpaRepo.save(entity)")
                .contains("entityMapper.toDomain(saved)");
    }

    @Test
    void generate_shouldPreserveParameterNames() {
        // Given: Method with custom parameter name
        AdapterMethodSpec method = AdapterMethodSpec.of(
                "save",
                DOMAIN_TYPE,
                List.of(new AdapterMethodSpec.ParameterInfo("domainOrder", DOMAIN_TYPE, false)),
                MethodKind.SAVE,
                Optional.empty());

        // When
        MethodSpec generatedMethod = strategy.generate(method, context);
        String methodCode = generatedMethod.toString();

        // Then: Should use the custom parameter name
        assertThat(methodCode)
                .contains("com.example.domain.Order domainOrder")
                .contains("mapper.toEntity(domainOrder)");
    }

    /**
     * Helper method to create a SAVE method spec.
     */
    private AdapterMethodSpec createSaveMethod(String methodName) {
        return AdapterMethodSpec.of(
                methodName,
                DOMAIN_TYPE,
                List.of(new AdapterMethodSpec.ParameterInfo("order", DOMAIN_TYPE, false)),
                MethodKind.SAVE,
                Optional.empty());
    }
}
