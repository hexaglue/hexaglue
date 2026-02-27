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

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.plugin.rest.model.HttpMapping;
import java.util.List;
import java.util.Optional;

/**
 * Chain of responsibility for HTTP verb derivation.
 *
 * <p>Iterates through ordered strategies and returns the first match.
 * The {@link FallbackStrategy} guarantees a result is always produced.
 *
 * @since 3.1.0
 */
public final class HttpVerbStrategyFactory {

    private final List<HttpVerbStrategy> strategies;

    /**
     * Creates the factory with the full strategy chain.
     *
     * <p>Strategies are ordered by priority (lower order = higher priority).
     * The {@link FallbackStrategy} is always last and guarantees a match.
     */
    public HttpVerbStrategyFactory() {
        this.strategies = List.of(
                new GetByIdStrategy(), // order 100
                new GetByPropertyStrategy(), // order 200
                new GetCollectionStrategy(), // order 300
                new SearchStrategy(), // order 400
                new CountStrategy(), // order 500
                new ExistsStrategy(), // order 600
                new CreateStrategy(), // order 700
                new UpdateStrategy(), // order 800
                new DeleteStrategy(), // order 900
                new SubResourceActionStrategy(), // order 1000
                new FallbackStrategy()); // order Integer.MAX_VALUE
    }

    /**
     * Derives the HTTP mapping for a use case.
     *
     * @param useCase   the use case
     * @param aggregate the associated aggregate (nullable)
     * @param basePath  the controller base path
     * @return the HTTP mapping (always returns a result due to FallbackStrategy)
     */
    public HttpMapping derive(UseCase useCase, AggregateRoot aggregate, String basePath) {
        for (HttpVerbStrategy strategy : strategies) {
            Optional<HttpMapping> mapping = strategy.match(useCase, aggregate, basePath);
            if (mapping.isPresent()) {
                return mapping.get();
            }
        }
        throw new IllegalStateException("FallbackStrategy should always match");
    }
}
