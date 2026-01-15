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

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ConfidenceLevel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Default implementation of {@link ElementQuery}.
 *
 * <p>This implementation is immutable. Each filter method returns a new
 * instance with the added filter. The query can be reused multiple times
 * since each terminal operation creates a fresh stream.</p>
 *
 * @param <T> the element type
 * @since 4.0.0
 */
public class DefaultElementQuery<T extends ArchElement> implements ElementQuery<T> {

    private final Supplier<Stream<T>> source;
    private final Predicate<T> filter;

    /**
     * Creates a new query with the given source.
     *
     * @param source the element source supplier
     */
    public DefaultElementQuery(Supplier<Stream<T>> source) {
        this(source, e -> true);
    }

    /**
     * Creates a new query with the given source and filter.
     *
     * @param source the element source supplier
     * @param filter the filter predicate
     */
    protected DefaultElementQuery(Supplier<Stream<T>> source, Predicate<T> filter) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.filter = Objects.requireNonNull(filter, "filter must not be null");
    }

    /**
     * Returns the source supplier.
     *
     * @return the source supplier
     */
    protected Supplier<Stream<T>> source() {
        return source;
    }

    /**
     * Returns the current filter predicate.
     *
     * @return the filter predicate
     */
    protected Predicate<T> filter() {
        return filter;
    }

    /**
     * Creates a new query with an additional filter.
     *
     * @param additionalFilter the filter to add
     * @return a new query instance
     */
    protected DefaultElementQuery<T> withFilter(Predicate<T> additionalFilter) {
        return new DefaultElementQuery<>(source, filter.and(additionalFilter));
    }

    @Override
    public ElementQuery<T> filter(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return withFilter(predicate);
    }

    @Override
    public ElementQuery<T> inPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName must not be null");
        return withFilter(e -> e.packageName().equals(packageName));
    }

    @Override
    public ElementQuery<T> withConfidence(ConfidenceLevel minLevel) {
        Objects.requireNonNull(minLevel, "minLevel must not be null");
        return withFilter(e -> {
            ConfidenceLevel elementLevel = e.classificationTrace().confidence();
            return elementLevel.compareTo(minLevel) >= 0;
        });
    }

    @Override
    public Stream<T> stream() {
        return source.get().filter(filter);
    }

    @Override
    public List<T> toList() {
        return stream().toList();
    }

    @Override
    public Optional<T> first() {
        return stream().findFirst();
    }

    @Override
    public Optional<T> single() {
        List<T> results = toList();
        if (results.size() > 1) {
            throw new IllegalStateException("Expected single result but found " + results.size() + " elements");
        }
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public long count() {
        return stream().count();
    }

    @Override
    public boolean exists() {
        return stream().findAny().isPresent();
    }

    @Override
    public boolean isEmpty() {
        return !exists();
    }

    @Override
    public void forEach(Consumer<T> action) {
        Objects.requireNonNull(action, "action must not be null");
        stream().forEach(action);
    }

    @Override
    public <R> List<R> map(Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return stream().map(mapper).toList();
    }
}
