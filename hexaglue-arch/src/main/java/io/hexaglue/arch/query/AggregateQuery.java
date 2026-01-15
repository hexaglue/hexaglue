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

package io.hexaglue.arch.query;

import io.hexaglue.arch.domain.Aggregate;

/**
 * Specialized query interface for aggregates.
 *
 * <p>Provides aggregate-specific filters such as checking for repository
 * presence or event publishing.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * List<Aggregate> aggregates = query.aggregates()
 *     .withRepository()
 *     .inPackage("com.example.domain")
 *     .toList();
 * }</pre>
 *
 * @since 4.0.0
 */
public interface AggregateQuery extends ElementQuery<Aggregate> {

    /**
     * Filters aggregates that have a repository.
     *
     * @return a new query with the filter applied
     */
    AggregateQuery withRepository();

    /**
     * Filters aggregates that do not have a repository.
     *
     * @return a new query with the filter applied
     */
    AggregateQuery withoutRepository();

    /**
     * Filters aggregates that publish domain events.
     *
     * @return a new query with the filter applied
     */
    AggregateQuery publishingEvents();

    /**
     * Filters aggregates that reference other aggregates.
     *
     * @return a new query with the filter applied
     */
    AggregateQuery withExternalReferences();

    /**
     * Filters aggregates in a specific package.
     *
     * @param packageName the package name to match
     * @return a new query with the filter applied
     */
    @Override
    AggregateQuery inPackage(String packageName);
}
