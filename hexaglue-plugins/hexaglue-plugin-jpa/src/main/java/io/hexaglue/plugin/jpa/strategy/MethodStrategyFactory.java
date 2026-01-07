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

import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import java.util.List;

/**
 * Factory for selecting the appropriate method generation strategy.
 *
 * <p>This factory implements the Chain of Responsibility pattern to find the
 * first strategy that supports a given method specification. It maintains an
 * ordered list of strategies, with specific patterns first and the fallback
 * strategy last.
 *
 * <h3>Strategy Chain Order:</h3>
 * <ol>
 *   <li>{@link SaveMethodStrategy} - Handles save/create/update operations</li>
 *   <li>{@link FindByIdMethodStrategy} - Handles findById/getById operations</li>
 *   <li>{@link FindAllMethodStrategy} - Handles findAll/getAll operations</li>
 *   <li>{@link DeleteMethodStrategy} - Handles delete/remove operations</li>
 *   <li>{@link ExistsMethodStrategy} - Handles exists/contains checks</li>
 *   <li>{@link CountMethodStrategy} - Handles count operations</li>
 *   <li>{@link FallbackMethodStrategy} - Catch-all for custom methods</li>
 * </ol>
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li>Immutable Strategy List: Strategies are initialized once and reused</li>
 *   <li>Thread-Safe: No mutable state, safe for concurrent use</li>
 *   <li>Fail-Safe: Fallback strategy guarantees a result is always returned</li>
 *   <li>Open/Closed Principle: New strategies can be added by modifying only this class</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * MethodStrategyFactory factory = new MethodStrategyFactory();
 * AdapterMethodSpec method = ...;
 * MethodBodyStrategy strategy = factory.strategyFor(method);
 * MethodSpec generated = strategy.generate(method, context);
 * }</pre>
 *
 * @since 2.0.0
 * @see MethodBodyStrategy
 */
public final class MethodStrategyFactory {

    /**
     * Ordered list of strategies to try.
     *
     * <p>IMPORTANT: FallbackMethodStrategy must be last as it always returns true
     * in its supports() method. This ensures specific strategies are checked first.
     */
    private final List<MethodBodyStrategy> strategies = List.of(
            new SaveMethodStrategy(),
            new FindByIdMethodStrategy(),
            new FindAllMethodStrategy(),
            new DeleteMethodStrategy(),
            new ExistsMethodStrategy(),
            new CountMethodStrategy(),
            new FallbackMethodStrategy() // Must be last
            );

    /**
     * Finds the appropriate strategy for the given method specification.
     *
     * <p>This method iterates through the strategy chain and returns the first
     * strategy that supports the method. Since the fallback strategy always
     * returns true, this method is guaranteed to return a non-null result.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Iterate through strategies in order</li>
     *   <li>For each strategy, call supports(method)</li>
     *   <li>Return the first strategy that returns true</li>
     *   <li>Fallback strategy ensures a result is always found</li>
     * </ol>
     *
     * @param method the method specification to analyze
     * @return the first supporting strategy (never null)
     * @throws IllegalArgumentException if method is null
     */
    public MethodBodyStrategy strategyFor(AdapterMethodSpec method) {
        if (method == null) {
            throw new IllegalArgumentException("Method specification cannot be null");
        }

        return strategies.stream()
                .filter(strategy -> strategy.supports(method))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No strategy found for method: " + method.name()
                        + " (This should never happen - fallback strategy is missing)"));
    }

    /**
     * Returns the number of registered strategies.
     *
     * <p>Useful for testing and diagnostics.
     *
     * @return the count of strategies in the chain
     */
    public int getStrategyCount() {
        return strategies.size();
    }
}
