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
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.model.ir.MethodKind;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MethodStrategyFactory}.
 *
 * <p>These tests validate that the factory correctly selects the appropriate
 * strategy based on the method kind using the Chain of Responsibility pattern.
 *
 * @since 3.0.0
 */
class MethodStrategyFactoryTest {

    private final MethodStrategyFactory factory = new MethodStrategyFactory();

    @Test
    void strategyFor_shouldReturnSaveStrategyForSaveKind() {
        // Given
        AdapterMethodSpec method = createMethodSpec("save", MethodKind.SAVE);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(SaveMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnFindByIdStrategyForFindByIdKind() {
        // Given
        AdapterMethodSpec method = createMethodSpec("findById", MethodKind.FIND_BY_ID);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(FindByIdMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnFindAllStrategyForFindAllKind() {
        // Given
        AdapterMethodSpec method = createMethodSpec("findAll", MethodKind.FIND_ALL);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(FindAllMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnDeleteStrategyForDeleteByIdKind() {
        // Given
        AdapterMethodSpec method = createMethodSpec("deleteById", MethodKind.DELETE_BY_ID);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(DeleteMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnExistsStrategyForExistsByIdKind() {
        // Given
        AdapterMethodSpec method = createMethodSpec("existsById", MethodKind.EXISTS_BY_ID);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(ExistsMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnCountStrategyForCountAllKind() {
        // Given
        AdapterMethodSpec method = createMethodSpec("count", MethodKind.COUNT_ALL);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(CountMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnFindByPropertyStrategyForFindByPropertyKind() {
        // Given
        AdapterMethodSpec method = createMethodSpec("findByEmail", MethodKind.FIND_BY_PROPERTY, "email");

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(FindByPropertyMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnExistsByPropertyStrategyForExistsByPropertyKind() {
        // Given
        AdapterMethodSpec method = createMethodSpec("existsByEmail", MethodKind.EXISTS_BY_PROPERTY, "email");

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(ExistsByPropertyMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnFallbackStrategyForCustomKind() {
        // Given
        AdapterMethodSpec method = createMethodSpec("findByCustomQuery", MethodKind.CUSTOM);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(FallbackMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldThrowForNullMethod() {
        // When/Then
        assertThatThrownBy(() -> factory.strategyFor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Method specification cannot be null");
    }

    @Test
    void strategyFor_shouldReturnConsistentStrategy() {
        // Given
        AdapterMethodSpec method = createMethodSpec("save", MethodKind.SAVE);

        // When: Call multiple times
        MethodBodyStrategy strategy1 = factory.strategyFor(method);
        MethodBodyStrategy strategy2 = factory.strategyFor(method);

        // Then: Should return strategies of the same type
        assertThat(strategy1).isInstanceOf(SaveMethodStrategy.class);
        assertThat(strategy2).isInstanceOf(SaveMethodStrategy.class);
    }

    @Test
    void strategyFor_shouldRespectStrategyChainOrder() {
        // Given: Multiple kinds
        AdapterMethodSpec saveMethod = createMethodSpec("save", MethodKind.SAVE);
        AdapterMethodSpec findMethod = createMethodSpec("findById", MethodKind.FIND_BY_ID);
        AdapterMethodSpec customMethod = createMethodSpec("custom", MethodKind.CUSTOM);

        // When
        MethodBodyStrategy saveStrategy = factory.strategyFor(saveMethod);
        MethodBodyStrategy findStrategy = factory.strategyFor(findMethod);
        MethodBodyStrategy customStrategy = factory.strategyFor(customMethod);

        // Then: Each should get the correct strategy
        assertThat(saveStrategy).isInstanceOf(SaveMethodStrategy.class);
        assertThat(findStrategy).isInstanceOf(FindByIdMethodStrategy.class);
        assertThat(customStrategy).isInstanceOf(FallbackMethodStrategy.class);
    }

    @Test
    void getStrategyCount_shouldReturn9Strategies() {
        // When
        int count = factory.getStrategyCount();

        // Then: 8 specific strategies + 1 fallback
        assertThat(count).isEqualTo(9);
    }

    @Test
    void strategyFor_shouldHandleSaveVariants() {
        // Given: Different SAVE kind methods
        String[] saveVariants = {"save", "create", "persist", "update"};

        for (String methodName : saveVariants) {
            // Given
            AdapterMethodSpec method = createMethodSpec(methodName, MethodKind.SAVE);

            // When
            MethodBodyStrategy strategy = factory.strategyFor(method);

            // Then
            assertThat(strategy)
                    .as("Method %s should use SaveMethodStrategy", methodName)
                    .isInstanceOf(SaveMethodStrategy.class);
        }
    }

    @Test
    void strategyFor_shouldHandleFindByIdVariants() {
        // Given: Different FIND_BY_ID kind methods
        String[] findByIdVariants = {"findById", "getById", "loadById"};

        for (String methodName : findByIdVariants) {
            // Given
            AdapterMethodSpec method = createMethodSpec(methodName, MethodKind.FIND_BY_ID);

            // When
            MethodBodyStrategy strategy = factory.strategyFor(method);

            // Then
            assertThat(strategy)
                    .as("Method %s should use FindByIdMethodStrategy", methodName)
                    .isInstanceOf(FindByIdMethodStrategy.class);
        }
    }

    @Test
    void strategyFor_shouldHandleDeleteVariants() {
        // Given: Different DELETE kind methods
        String[] deleteVariants = {"deleteById", "removeById"};

        for (String methodName : deleteVariants) {
            // Given
            AdapterMethodSpec method = createMethodSpec(methodName, MethodKind.DELETE_BY_ID);

            // When
            MethodBodyStrategy strategy = factory.strategyFor(method);

            // Then
            assertThat(strategy)
                    .as("Method %s should use DeleteMethodStrategy", methodName)
                    .isInstanceOf(DeleteMethodStrategy.class);
        }
    }

    /**
     * Helper method to create a method spec for testing.
     */
    private AdapterMethodSpec createMethodSpec(String methodName, MethodKind kind) {
        return createMethodSpec(methodName, kind, null);
    }

    /**
     * Helper method to create a method spec for testing with target property.
     */
    private AdapterMethodSpec createMethodSpec(String methodName, MethodKind kind, String targetProperty) {
        TypeName returnType =
                switch (kind) {
                    case SAVE, SAVE_ALL -> ClassName.get("com.example", "Order");
                    case FIND_BY_ID, FIND_BY_PROPERTY -> ClassName.get("java.util", "Optional");
                    case FIND_ALL, FIND_ALL_BY_ID, FIND_ALL_BY_PROPERTY -> ClassName.get("java.util", "List");
                    case DELETE_BY_ID, DELETE_ALL, DELETE_BY_PROPERTY -> TypeName.VOID;
                    case EXISTS_BY_ID, EXISTS_BY_PROPERTY -> TypeName.BOOLEAN;
                    case COUNT_ALL, COUNT_BY_PROPERTY -> TypeName.LONG;
                    default -> ClassName.get("com.example", "Order");
                };

        List<AdapterMethodSpec.ParameterInfo> parameters =
                switch (kind) {
                    case SAVE, SAVE_ALL ->
                        List.of(new AdapterMethodSpec.ParameterInfo(
                                "order", ClassName.get("com.example", "Order"), false));
                    case FIND_BY_ID, EXISTS_BY_ID, DELETE_BY_ID ->
                        List.of(new AdapterMethodSpec.ParameterInfo("id", TypeName.get(UUID.class), true));
                    case FIND_BY_PROPERTY, EXISTS_BY_PROPERTY, COUNT_BY_PROPERTY, DELETE_BY_PROPERTY ->
                        List.of(new AdapterMethodSpec.ParameterInfo(
                                "value", ClassName.get("java.lang", "String"), false));
                    case FIND_ALL, FIND_ALL_BY_ID, FIND_ALL_BY_PROPERTY, COUNT_ALL, DELETE_ALL -> List.of();
                    default ->
                        List.of(new AdapterMethodSpec.ParameterInfo(
                                "status", ClassName.get("java.lang", "String"), false));
                };

        return AdapterMethodSpec.of(methodName, returnType, parameters, kind, Optional.ofNullable(targetProperty));
    }
}
