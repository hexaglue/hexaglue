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
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.plugin.jpa.model.MethodPattern;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MethodStrategyFactory}.
 *
 * <p>These tests validate that the factory correctly selects the appropriate
 * strategy based on the method pattern using the Chain of Responsibility pattern.
 *
 * @since 2.0.0
 */
class MethodStrategyFactoryTest {

    private final MethodStrategyFactory factory = new MethodStrategyFactory();

    @Test
    void strategyFor_shouldReturnSaveStrategyForSavePattern() {
        // Given
        AdapterMethodSpec method = createMethodSpec("save", MethodPattern.SAVE);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(SaveMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnFindByIdStrategyForFindByIdPattern() {
        // Given
        AdapterMethodSpec method = createMethodSpec("findById", MethodPattern.FIND_BY_ID);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(FindByIdMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnFindAllStrategyForFindAllPattern() {
        // Given
        AdapterMethodSpec method = createMethodSpec("findAll", MethodPattern.FIND_ALL);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(FindAllMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnDeleteStrategyForDeletePattern() {
        // Given
        AdapterMethodSpec method = createMethodSpec("delete", MethodPattern.DELETE);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(DeleteMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnExistsStrategyForExistsPattern() {
        // Given
        AdapterMethodSpec method = createMethodSpec("existsById", MethodPattern.EXISTS);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(ExistsMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnCountStrategyForCountPattern() {
        // Given
        AdapterMethodSpec method = createMethodSpec("count", MethodPattern.COUNT);

        // When
        MethodBodyStrategy strategy = factory.strategyFor(method);

        // Then
        assertThat(strategy).isInstanceOf(CountMethodStrategy.class);
        assertThat(strategy.supports(method)).isTrue();
    }

    @Test
    void strategyFor_shouldReturnFallbackStrategyForCustomPattern() {
        // Given
        AdapterMethodSpec method = createMethodSpec("findByStatus", MethodPattern.CUSTOM);

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
    void strategyFor_shouldAlwaysReturnNonNullStrategy() {
        // Given: Test all method patterns
        MethodPattern[] allPatterns = MethodPattern.values();

        for (MethodPattern pattern : allPatterns) {
            // Given
            AdapterMethodSpec method = createMethodSpec("method", pattern);

            // When
            MethodBodyStrategy strategy = factory.strategyFor(method);

            // Then
            assertThat(strategy)
                    .as("Strategy for pattern %s should not be null", pattern)
                    .isNotNull();
            assertThat(strategy.supports(method))
                    .as("Strategy for pattern %s should support the method", pattern)
                    .isTrue();
        }
    }

    @Test
    void strategyFor_shouldReturnConsistentStrategy() {
        // Given
        AdapterMethodSpec method = createMethodSpec("save", MethodPattern.SAVE);

        // When: Call multiple times
        MethodBodyStrategy strategy1 = factory.strategyFor(method);
        MethodBodyStrategy strategy2 = factory.strategyFor(method);

        // Then: Should return strategies of the same type
        assertThat(strategy1).isInstanceOf(SaveMethodStrategy.class);
        assertThat(strategy2).isInstanceOf(SaveMethodStrategy.class);
    }

    @Test
    void strategyFor_shouldRespectStrategyChainOrder() {
        // Given: Multiple patterns
        AdapterMethodSpec saveMethod = createMethodSpec("save", MethodPattern.SAVE);
        AdapterMethodSpec findMethod = createMethodSpec("findById", MethodPattern.FIND_BY_ID);
        AdapterMethodSpec customMethod = createMethodSpec("custom", MethodPattern.CUSTOM);

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
    void getStrategyCount_shouldReturn7Strategies() {
        // When
        int count = factory.getStrategyCount();

        // Then: 6 specific strategies + 1 fallback
        assertThat(count).isEqualTo(7);
    }

    @Test
    void strategyFor_shouldHandleSaveVariants() {
        // Given: Different SAVE pattern methods
        String[] saveVariants = {"save", "create", "persist", "update"};

        for (String methodName : saveVariants) {
            // Given
            AdapterMethodSpec method = createMethodSpec(methodName, MethodPattern.SAVE);

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
        // Given: Different FIND_BY_ID pattern methods
        String[] findByIdVariants = {"findById", "getById", "loadById"};

        for (String methodName : findByIdVariants) {
            // Given
            AdapterMethodSpec method = createMethodSpec(methodName, MethodPattern.FIND_BY_ID);

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
        // Given: Different DELETE pattern methods
        String[] deleteVariants = {"delete", "deleteById", "remove", "removeById"};

        for (String methodName : deleteVariants) {
            // Given
            AdapterMethodSpec method = createMethodSpec(methodName, MethodPattern.DELETE);

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
    private AdapterMethodSpec createMethodSpec(String methodName, MethodPattern pattern) {
        TypeName returnType =
                switch (pattern) {
                    case SAVE -> ClassName.get("com.example", "Order");
                    case FIND_BY_ID -> ClassName.get("java.util", "Optional");
                    case FIND_ALL -> ClassName.get("java.util", "List");
                    case DELETE -> TypeName.VOID;
                    case EXISTS -> TypeName.BOOLEAN;
                    case COUNT -> TypeName.LONG;
                    case CUSTOM -> ClassName.get("com.example", "Order");
                };

        List<AdapterMethodSpec.ParameterInfo> parameters =
                switch (pattern) {
                    case SAVE ->
                        List.of(new AdapterMethodSpec.ParameterInfo("order", ClassName.get("com.example", "Order")));
                    case FIND_BY_ID, EXISTS ->
                        List.of(new AdapterMethodSpec.ParameterInfo("id", TypeName.get(UUID.class)));
                    case DELETE -> List.of(new AdapterMethodSpec.ParameterInfo("id", TypeName.get(UUID.class)));
                    case FIND_ALL, COUNT -> List.of();
                    case CUSTOM ->
                        List.of(new AdapterMethodSpec.ParameterInfo("status", ClassName.get("java.lang", "String")));
                };

        return new AdapterMethodSpec(methodName, returnType, parameters, pattern);
    }
}
