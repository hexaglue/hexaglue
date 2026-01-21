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

package io.hexaglue.arch.model.query;

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Fluent query builder for aggregate roots.
 *
 * <p>Provides chainable filters for finding aggregates matching specific
 * criteria. All filter operations return new query instances (immutable).</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * List<AggregateRoot> result = query.aggregates()
 *     .withRepository()
 *     .withEvents()
 *     .inPackage("com.example.order")
 *     .toList();
 *
 * // Count matching
 * long count = query.aggregates().withRepository().count();
 *
 * // Stream for custom processing
 * query.aggregates().stream().forEach(agg -> process(agg));
 * }</pre>
 *
 * @since 5.0.0
 */
public final class AggregateQuery {

    private final DomainIndex domainIndex;
    private final PortIndex portIndex;
    private final Predicate<AggregateRoot> filter;

    private AggregateQuery(DomainIndex domainIndex, PortIndex portIndex, Predicate<AggregateRoot> filter) {
        this.domainIndex = Objects.requireNonNull(domainIndex, "domainIndex must not be null");
        this.portIndex = Objects.requireNonNull(portIndex, "portIndex must not be null");
        this.filter = Objects.requireNonNull(filter, "filter must not be null");
    }

    /**
     * Creates a new AggregateQuery with no filters.
     *
     * @param domainIndex the domain index
     * @param portIndex the port index
     * @return a new AggregateQuery
     */
    static AggregateQuery of(DomainIndex domainIndex, PortIndex portIndex) {
        return new AggregateQuery(domainIndex, portIndex, agg -> true);
    }

    /**
     * Filters aggregates that have a repository.
     *
     * @return a new query with the filter applied
     */
    public AggregateQuery withRepository() {
        return addFilter(agg -> portIndex.repositoryFor(agg.id()).isPresent());
    }

    /**
     * Filters aggregates that do not have a repository.
     *
     * @return a new query with the filter applied
     */
    public AggregateQuery withoutRepository() {
        return addFilter(agg -> portIndex.repositoryFor(agg.id()).isEmpty());
    }

    /**
     * Filters aggregates that have associated domain events.
     *
     * @return a new query with the filter applied
     */
    public AggregateQuery withEvents() {
        return addFilter(agg -> !agg.domainEvents().isEmpty());
    }

    /**
     * Filters aggregates that have child entities.
     *
     * @return a new query with the filter applied
     */
    public AggregateQuery withEntities() {
        return addFilter(agg -> !agg.entities().isEmpty());
    }

    /**
     * Filters aggregates that have value objects.
     *
     * @return a new query with the filter applied
     */
    public AggregateQuery withValueObjects() {
        return addFilter(agg -> !agg.valueObjects().isEmpty());
    }

    /**
     * Filters aggregates that have an identity field.
     *
     * <p>All aggregate roots should have an identity field, so this
     * filter is primarily for consistency with other query APIs.</p>
     *
     * @return a new query with the filter applied
     */
    public AggregateQuery withIdentity() {
        return addFilter(agg -> agg.identityField() != null);
    }

    /**
     * Filters aggregates in a specific package.
     *
     * @param packageName the package name to match (exact match)
     * @return a new query with the filter applied
     */
    public AggregateQuery inPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName must not be null");
        return addFilter(agg -> packageName.equals(agg.packageName()));
    }

    /**
     * Filters aggregates in a package or its subpackages.
     *
     * @param packagePrefix the package prefix to match
     * @return a new query with the filter applied
     */
    public AggregateQuery inPackageTree(String packagePrefix) {
        Objects.requireNonNull(packagePrefix, "packagePrefix must not be null");
        return addFilter(agg -> {
            String pkg = agg.packageName();
            return pkg.equals(packagePrefix) || pkg.startsWith(packagePrefix + ".");
        });
    }

    /**
     * Filters aggregates whose name matches a pattern.
     *
     * @param namePattern a regex pattern to match against simple name
     * @return a new query with the filter applied
     */
    public AggregateQuery nameMatches(String namePattern) {
        Objects.requireNonNull(namePattern, "namePattern must not be null");
        return addFilter(agg -> agg.simpleName().matches(namePattern));
    }

    /**
     * Applies a custom filter predicate.
     *
     * @param predicate the filter predicate
     * @return a new query with the filter applied
     */
    public AggregateQuery where(Predicate<AggregateRoot> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return addFilter(predicate);
    }

    /**
     * Returns a stream of matching aggregates.
     *
     * @return a stream of aggregates matching all filters
     */
    public Stream<AggregateRoot> stream() {
        return domainIndex.aggregateRoots().filter(filter);
    }

    /**
     * Returns a list of matching aggregates.
     *
     * @return an unmodifiable list of matching aggregates
     */
    public List<AggregateRoot> toList() {
        return stream().toList();
    }

    /**
     * Returns the count of matching aggregates.
     *
     * @return the count
     */
    public long count() {
        return stream().count();
    }

    /**
     * Returns whether any aggregate matches the filters.
     *
     * @return true if at least one aggregate matches
     */
    public boolean exists() {
        return stream().findAny().isPresent();
    }

    /**
     * Returns whether no aggregate matches the filters.
     *
     * @return true if no aggregate matches
     */
    public boolean isEmpty() {
        return !exists();
    }

    private AggregateQuery addFilter(Predicate<AggregateRoot> additional) {
        return new AggregateQuery(domainIndex, portIndex, filter.and(additional));
    }
}
