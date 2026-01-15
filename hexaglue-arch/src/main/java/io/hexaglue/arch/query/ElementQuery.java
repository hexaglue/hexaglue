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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Base query interface for any architectural element.
 *
 * <p>Provides fluent, immutable query operations on elements. Each filter
 * method returns a new query instance, making queries reusable.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * List<ArchElement> elements = query.elements()
 *     .inPackage("com.example.domain")
 *     .withConfidence(ConfidenceLevel.HIGH)
 *     .toList();
 * }</pre>
 *
 * @param <T> the element type
 * @since 4.0.0
 */
public interface ElementQuery<T extends ArchElement> {

    // === Filters ===

    /**
     * Filters elements matching a predicate.
     *
     * @param predicate the filter predicate
     * @return a new query with the filter applied
     */
    ElementQuery<T> filter(Predicate<T> predicate);

    /**
     * Filters elements in a specific package.
     *
     * @param packageName the package name to match
     * @return a new query with the filter applied
     */
    ElementQuery<T> inPackage(String packageName);

    /**
     * Filters elements with a minimum confidence level.
     *
     * @param minLevel the minimum confidence level
     * @return a new query with the filter applied
     */
    ElementQuery<T> withConfidence(ConfidenceLevel minLevel);

    // === Terminal Operations ===

    /**
     * Returns a stream of matching elements.
     *
     * <p>Each call creates a new stream, making the query reusable.</p>
     *
     * @return a stream of elements
     */
    Stream<T> stream();

    /**
     * Collects all matching elements to a list.
     *
     * @return an immutable list of elements
     */
    List<T> toList();

    /**
     * Returns the first matching element.
     *
     * @return the first element, or empty if none
     */
    Optional<T> first();

    /**
     * Returns the single matching element.
     *
     * @return the single element, or empty if none
     * @throws IllegalStateException if multiple elements match
     */
    Optional<T> single();

    /**
     * Counts matching elements.
     *
     * @return the count
     */
    long count();

    /**
     * Returns whether any element matches.
     *
     * @return true if at least one element matches
     */
    boolean exists();

    /**
     * Returns whether no elements match.
     *
     * @return true if no elements match
     */
    boolean isEmpty();

    // === Actions ===

    /**
     * Performs an action on each matching element.
     *
     * @param action the action to perform
     */
    void forEach(Consumer<T> action);

    /**
     * Maps elements to a list of results.
     *
     * @param mapper the mapping function
     * @param <R> the result type
     * @return a list of mapped results
     */
    <R> List<R> map(Function<T, R> mapper);
}
