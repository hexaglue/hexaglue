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

import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.RelationshipStore;
import io.hexaglue.arch.domain.Aggregate;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Default implementation of {@link AggregateQuery}.
 *
 * <p>This implementation is immutable. Each filter method returns a new
 * instance with the added filter. Uses {@link RelationshipStore} for O(1)
 * repository lookups.</p>
 *
 * @since 4.0.0
 */
public final class DefaultAggregateQuery extends DefaultElementQuery<Aggregate> implements AggregateQuery {

    private final RelationshipStore relationships;

    /**
     * Creates a new aggregate query.
     *
     * @param source the aggregate source supplier
     * @param relationships the relationship store for O(1) lookups
     */
    public DefaultAggregateQuery(Supplier<Stream<Aggregate>> source, RelationshipStore relationships) {
        super(source);
        this.relationships = Objects.requireNonNull(relationships, "relationships must not be null");
    }

    private DefaultAggregateQuery(
            Supplier<Stream<Aggregate>> source, Predicate<Aggregate> filter, RelationshipStore relationships) {
        super(source, filter);
        this.relationships = relationships;
    }

    @Override
    protected DefaultAggregateQuery withFilter(Predicate<Aggregate> additionalFilter) {
        return new DefaultAggregateQuery(source(), filter().and(additionalFilter), relationships);
    }

    @Override
    public AggregateQuery withRepository() {
        return withFilter(this::hasRepository);
    }

    @Override
    public AggregateQuery withoutRepository() {
        return withFilter(agg -> !hasRepository(agg));
    }

    @Override
    public AggregateQuery publishingEvents() {
        return withFilter(agg -> !agg.publishedEvents().isEmpty());
    }

    @Override
    public AggregateQuery withExternalReferences() {
        return withFilter(agg -> !agg.externalReferences().isEmpty());
    }

    @Override
    public AggregateQuery inPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName must not be null");
        return withFilter(agg -> agg.packageName().equals(packageName));
    }

    @Override
    public AggregateQuery filter(Predicate<Aggregate> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return withFilter(predicate);
    }

    @Override
    public AggregateQuery withConfidence(ConfidenceLevel minLevel) {
        Objects.requireNonNull(minLevel, "minLevel must not be null");
        return withFilter(agg -> {
            ConfidenceLevel elementLevel = agg.classificationTrace().confidence();
            return elementLevel.compareTo(minLevel) >= 0;
        });
    }

    /**
     * Checks if an aggregate has a repository (O(1) lookup via RelationshipStore).
     *
     * @param agg the aggregate to check
     * @return true if the aggregate has a repository
     */
    private boolean hasRepository(Aggregate agg) {
        return relationships.repositoryFor(agg.id()).isPresent();
    }
}
