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
import java.util.Optional;

/**
 * Strategy for determining the HTTP verb, path, and status for a use case method.
 *
 * <p>Strategies are chained in priority order. The first strategy that matches wins.
 *
 * @since 3.1.0
 */
public interface HttpVerbStrategy {

    /**
     * Attempts to match this strategy against a use case method.
     *
     * @param useCase   the use case to analyze
     * @param aggregate the associated aggregate root (nullable)
     * @param basePath  the controller base path (e.g., "/api/accounts")
     * @return the HTTP mapping if this strategy matches, or empty
     */
    Optional<HttpMapping> match(UseCase useCase, AggregateRoot aggregate, String basePath);

    /**
     * Priority order (lower = higher priority).
     *
     * @return the priority order
     */
    int order();
}
