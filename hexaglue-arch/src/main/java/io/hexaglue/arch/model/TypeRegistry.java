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

package io.hexaglue.arch.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * A registry of all architectural types in the model.
 *
 * <p>The TypeRegistry provides efficient lookup of types by their {@link TypeId}
 * and supports filtering by type class or {@link ArchKind}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * TypeRegistry registry = model.typeRegistry();
 *
 * // Lookup by id
 * Optional<ArchType> order = registry.get(TypeId.of("com.example.Order"));
 *
 * // Get typed result
 * Optional<AggregateRoot> aggregate = registry.get(orderId, AggregateRoot.class);
 *
 * // Stream all types
 * registry.all().forEach(System.out::println);
 *
 * // Filter by kind
 * registry.ofKind(ArchKind.AGGREGATE_ROOT).forEach(this::processAggregate);
 *
 * // Filter by type class (including subtypes)
 * registry.all(DomainType.class).forEach(this::processDomainType);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class TypeRegistry {

    private final Map<TypeId, ArchType> typesByid;

    private TypeRegistry(Map<TypeId, ArchType> typesByid) {
        this.typesByid = Collections.unmodifiableMap(new TreeMap<>(typesByid));
    }

    /**
     * Returns the type with the given id, if present.
     *
     * @param id the type id
     * @return an optional containing the type, or empty if not found
     */
    public Optional<ArchType> get(TypeId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(typesByid.get(id));
    }

    /**
     * Returns the type with the given id cast to the specified type, if present and of correct type.
     *
     * @param <T> the expected type
     * @param id the type id
     * @param type the expected class
     * @return an optional containing the type, or empty if not found or wrong type
     */
    public <T extends ArchType> Optional<T> get(TypeId id, Class<T> type) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        return Optional.ofNullable(typesByid.get(id)).filter(type::isInstance).map(type::cast);
    }

    /**
     * Returns a stream of all types in the registry.
     *
     * @return a stream of all types
     */
    public Stream<ArchType> all() {
        return typesByid.values().stream();
    }

    /**
     * Returns a stream of all types that are instances of the specified class.
     *
     * <p>This supports filtering by sealed interface types. For example,
     * {@code all(DomainType.class)} returns all aggregate roots, entities,
     * value objects, identifiers, domain events, and domain services.</p>
     *
     * @param <T> the type to filter by
     * @param type the class to filter by
     * @return a stream of matching types
     */
    public <T extends ArchType> Stream<T> all(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return typesByid.values().stream().filter(type::isInstance).map(type::cast);
    }

    /**
     * Returns a stream of all types of the specified kind.
     *
     * @param kind the architectural kind
     * @return a stream of types with that kind
     */
    public Stream<ArchType> ofKind(ArchKind kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        return typesByid.values().stream().filter(t -> t.kind() == kind);
    }

    /**
     * Returns the number of types in the registry.
     *
     * @return the size
     */
    public int size() {
        return typesByid.size();
    }

    /**
     * Creates a new builder for constructing a TypeRegistry.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link TypeRegistry} instances.
     *
     * @since 4.1.0
     */
    public static final class Builder {
        private final Map<TypeId, ArchType> typesByid = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Adds a type to the registry.
         *
         * @param type the type to add
         * @return this builder
         * @throws NullPointerException if type is null
         * @throws IllegalStateException if a type with the same id already exists (on build)
         */
        public Builder add(ArchType type) {
            Objects.requireNonNull(type, "type must not be null");
            ArchType existing = typesByid.put(type.id(), type);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate type id: " + type.id() + " (existing: " + existing + ", new: " + type + ")");
            }
            return this;
        }

        /**
         * Adds all types from the collection to the registry.
         *
         * @param types the types to add
         * @return this builder
         * @throws NullPointerException if types or any type is null
         */
        public Builder addAll(Collection<? extends ArchType> types) {
            Objects.requireNonNull(types, "types must not be null");
            types.forEach(this::add);
            return this;
        }

        /**
         * Builds the TypeRegistry.
         *
         * @return a new TypeRegistry
         */
        public TypeRegistry build() {
            return new TypeRegistry(typesByid);
        }
    }
}
