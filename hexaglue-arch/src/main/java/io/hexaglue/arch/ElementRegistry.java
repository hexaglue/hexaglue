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

package io.hexaglue.arch;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Central registry containing all architectural elements.
 *
 * <p>The registry is the single source of truth - no duplication of elements.
 * Once built, the registry is immutable.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ElementRegistry registry = ElementRegistry.builder()
 *     .add(aggregate)
 *     .add(repository)
 *     .build();
 *
 * Optional<ArchElement> element = registry.get(id);
 * Optional<Aggregate> agg = registry.get(id, Aggregate.class);
 * }</pre>
 *
 * @since 4.0.0
 */
public final class ElementRegistry {

    private final Map<ElementId, ArchElement> elements;
    private final Map<ElementKind, List<ElementId>> byKind;
    private final Map<String, List<ElementId>> byPackage;

    private ElementRegistry(Map<ElementId, ArchElement> elements) {
        this.elements = Map.copyOf(elements);
        this.byKind = indexByKind(elements);
        this.byPackage = indexByPackage(elements);
    }

    private static Map<ElementKind, List<ElementId>> indexByKind(Map<ElementId, ArchElement> elements) {
        return elements.values().stream()
                .collect(Collectors.groupingBy(
                        ArchElement::kind, Collectors.mapping(ArchElement::id, Collectors.toList())));
    }

    private static Map<String, List<ElementId>> indexByPackage(Map<ElementId, ArchElement> elements) {
        return elements.values().stream()
                .collect(Collectors.groupingBy(
                        ArchElement::packageName, Collectors.mapping(ArchElement::id, Collectors.toList())));
    }

    // ===== Access methods =====

    /**
     * Returns the element with the given identifier, if present.
     *
     * @param id the element identifier
     * @return an Optional containing the element, or empty if not found
     */
    public Optional<ArchElement> get(ElementId id) {
        return Optional.ofNullable(elements.get(id));
    }

    /**
     * Returns the element with the given identifier and type, if present and matching.
     *
     * @param id the element identifier
     * @param type the expected element type
     * @param <T> the element type
     * @return an Optional containing the element, or empty if not found or type doesn't match
     */
    public <T extends ArchElement> Optional<T> get(ElementId id, Class<T> type) {
        return get(id).filter(type::isInstance).map(type::cast);
    }

    /**
     * Resolves an element reference.
     *
     * @param ref the reference to resolve
     * @param <T> the expected element type
     * @return the resolution result
     */
    public <T extends ArchElement> ResolutionResult<T> resolve(ElementRef<T> ref) {
        return ref.resolve(this);
    }

    /**
     * Returns {@code true} if the registry contains an element with the given identifier.
     *
     * @param id the element identifier
     * @return true if the element exists
     */
    public boolean contains(ElementId id) {
        return elements.containsKey(id);
    }

    // ===== Iteration methods =====

    /**
     * Returns a stream of all elements in the registry.
     *
     * @return a stream of all elements
     */
    public Stream<ArchElement> all() {
        return elements.values().stream();
    }

    /**
     * Returns a stream of all elements of the given type.
     *
     * @param type the element type to filter by
     * @param <T> the element type
     * @return a stream of matching elements
     */
    public <T extends ArchElement> Stream<T> all(Class<T> type) {
        return all().filter(type::isInstance).map(type::cast);
    }

    /**
     * Returns a stream of all elements with the given kind.
     *
     * @param kind the element kind to filter by
     * @return a stream of matching elements
     */
    public Stream<ArchElement> ofKind(ElementKind kind) {
        return byKind.getOrDefault(kind, List.of()).stream().map(elements::get);
    }

    /**
     * Returns a stream of all elements in the given package.
     *
     * @param packageName the package name to filter by
     * @return a stream of matching elements
     */
    public Stream<ArchElement> inPackage(String packageName) {
        return byPackage.getOrDefault(packageName, List.of()).stream().map(elements::get);
    }

    // ===== Statistics =====

    /**
     * Returns the total number of elements in the registry.
     *
     * @return the number of elements
     */
    public int size() {
        return elements.size();
    }

    /**
     * Returns a map of element counts by kind.
     *
     * @return a map from kind to count
     */
    public Map<ElementKind, Long> countByKind() {
        return all().collect(Collectors.groupingBy(ArchElement::kind, Collectors.counting()));
    }

    // ===== Builder =====

    /**
     * Creates a new registry builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating an ElementRegistry.
     */
    public static final class Builder {
        private final Map<ElementId, ArchElement> elements = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Adds an element to the registry.
         *
         * @param element the element to add
         * @return this builder
         * @throws DuplicateElementException if an element with the same ID already exists
         */
        public Builder add(ArchElement element) {
            ElementId id = element.id();
            if (elements.containsKey(id)) {
                throw new DuplicateElementException(id);
            }
            elements.put(id, element);
            return this;
        }

        /**
         * Adds all elements from a collection to the registry.
         *
         * @param elements the elements to add
         * @return this builder
         * @throws DuplicateElementException if any element ID already exists
         */
        public Builder addAll(Collection<? extends ArchElement> elements) {
            elements.forEach(this::add);
            return this;
        }

        /**
         * Builds the registry.
         *
         * @return an immutable ElementRegistry
         */
        public ElementRegistry build() {
            return new ElementRegistry(elements);
        }
    }
}
