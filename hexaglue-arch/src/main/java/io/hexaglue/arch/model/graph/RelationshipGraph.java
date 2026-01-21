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

package io.hexaglue.arch.model.graph;

import io.hexaglue.arch.model.TypeId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A graph of relationships between architectural types.
 *
 * <p>This immutable graph structure captures all relationships between types
 * in the domain model, enabling navigation, analysis, and visualization of
 * the architectural structure.</p>
 *
 * <h2>Graph Properties</h2>
 * <ul>
 *   <li>Directed - relationships have a source and target</li>
 *   <li>Multi-edge - multiple relationships can exist between the same pair</li>
 *   <li>Labeled - each relationship has a {@link RelationType}</li>
 *   <li>Immutable - created via {@link Builder}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Navigate from source
 * graph.from(orderId)
 *     .filter(r -> r.type() == RelationType.CONTAINS)
 *     .map(Relationship::target)
 *     .forEach(System.out::println);
 *
 * // Navigate to target
 * graph.to(orderLineId)
 *     .filter(r -> r.type() == RelationType.CONTAINS)
 *     .map(Relationship::source)
 *     .forEach(parent -> System.out.println("Contained by: " + parent));
 *
 * // Check specific relationship
 * if (graph.hasRelation(orderId, orderLineId, RelationType.CONTAINS)) {
 *     System.out.println("Order contains OrderLine");
 * }
 *
 * // Build a graph
 * RelationshipGraph graph = RelationshipGraph.builder()
 *     .add(orderId, orderLineId, RelationType.CONTAINS)
 *     .add(orderId, orderCreatedId, RelationType.EMITS)
 *     .build();
 * }</pre>
 *
 * @since 5.0.0
 */
public final class RelationshipGraph {

    private final List<Relationship> relationships;
    private final Map<TypeId, List<Relationship>> fromIndex;
    private final Map<TypeId, List<Relationship>> toIndex;

    private RelationshipGraph(List<Relationship> relationships) {
        this.relationships = List.copyOf(relationships);

        // Build indexes for fast lookup
        Map<TypeId, List<Relationship>> fromMap = new HashMap<>();
        Map<TypeId, List<Relationship>> toMap = new HashMap<>();

        for (Relationship rel : this.relationships) {
            fromMap.computeIfAbsent(rel.source(), k -> new ArrayList<>()).add(rel);
            toMap.computeIfAbsent(rel.target(), k -> new ArrayList<>()).add(rel);
        }

        // Make indexes immutable
        fromMap.replaceAll((k, v) -> List.copyOf(v));
        toMap.replaceAll((k, v) -> List.copyOf(v));

        this.fromIndex = Collections.unmodifiableMap(fromMap);
        this.toIndex = Collections.unmodifiableMap(toMap);
    }

    /**
     * A relationship between two types.
     *
     * @param source the source type
     * @param target the target type
     * @param type the relationship type
     * @since 5.0.0
     */
    public record Relationship(TypeId source, TypeId target, RelationType type) {

        /**
         * Creates a new Relationship.
         *
         * @param source the source type, must not be null
         * @param target the target type, must not be null
         * @param type the relationship type, must not be null
         * @throws NullPointerException if any argument is null
         */
        public Relationship {
            Objects.requireNonNull(source, "source must not be null");
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(type, "type must not be null");
        }

        /**
         * Creates a relationship.
         *
         * @param source the source type
         * @param target the target type
         * @param type the relationship type
         * @return a new Relationship
         */
        public static Relationship of(TypeId source, TypeId target, RelationType type) {
            return new Relationship(source, target, type);
        }
    }

    /**
     * Returns all relationships originating from the given type.
     *
     * @param source the source type
     * @return a stream of relationships from this type
     * @throws NullPointerException if source is null
     */
    public Stream<Relationship> from(TypeId source) {
        Objects.requireNonNull(source, "source must not be null");
        return fromIndex.getOrDefault(source, List.of()).stream();
    }

    /**
     * Returns all relationships targeting the given type.
     *
     * @param target the target type
     * @return a stream of relationships to this type
     * @throws NullPointerException if target is null
     */
    public Stream<Relationship> to(TypeId target) {
        Objects.requireNonNull(target, "target must not be null");
        return toIndex.getOrDefault(target, List.of()).stream();
    }

    /**
     * Returns all relationships in the graph.
     *
     * @return a stream of all relationships
     */
    public Stream<Relationship> all() {
        return relationships.stream();
    }

    /**
     * Returns all relationships of the given type.
     *
     * @param type the relationship type to filter by
     * @return a stream of relationships of this type
     * @throws NullPointerException if type is null
     */
    public Stream<Relationship> ofType(RelationType type) {
        Objects.requireNonNull(type, "type must not be null");
        return relationships.stream().filter(r -> r.type() == type);
    }

    /**
     * Checks whether a specific relationship exists.
     *
     * @param source the source type
     * @param target the target type
     * @param type the relationship type
     * @return true if the relationship exists
     * @throws NullPointerException if any argument is null
     */
    public boolean hasRelation(TypeId source, TypeId target, RelationType type) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(type, "type must not be null");

        return from(source).anyMatch(r -> r.target().equals(target) && r.type() == type);
    }

    /**
     * Checks whether any relationship exists between source and target.
     *
     * @param source the source type
     * @param target the target type
     * @return true if any relationship exists
     * @throws NullPointerException if any argument is null
     */
    public boolean hasAnyRelation(TypeId source, TypeId target) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");

        return from(source).anyMatch(r -> r.target().equals(target));
    }

    /**
     * Returns all types that this type is directly related to (as source).
     *
     * @param source the source type
     * @return a set of related target types
     * @throws NullPointerException if source is null
     */
    public Set<TypeId> relatedTo(TypeId source) {
        Objects.requireNonNull(source, "source must not be null");
        return from(source).map(Relationship::target).collect(Collectors.toSet());
    }

    /**
     * Returns all types that are directly related to this type (as target).
     *
     * @param target the target type
     * @return a set of related source types
     * @throws NullPointerException if target is null
     */
    public Set<TypeId> relatedFrom(TypeId target) {
        Objects.requireNonNull(target, "target must not be null");
        return to(target).map(Relationship::source).collect(Collectors.toSet());
    }

    /**
     * Returns the total number of relationships in the graph.
     *
     * @return the relationship count
     */
    public int size() {
        return relationships.size();
    }

    /**
     * Returns whether the graph is empty.
     *
     * @return true if the graph has no relationships
     */
    public boolean isEmpty() {
        return relationships.isEmpty();
    }

    /**
     * Returns all unique type IDs in the graph.
     *
     * @return a set of all type IDs (both sources and targets)
     */
    public Set<TypeId> allTypes() {
        return Stream.concat(fromIndex.keySet().stream(), toIndex.keySet().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Creates an empty graph.
     *
     * @return an empty RelationshipGraph
     */
    public static RelationshipGraph empty() {
        return new RelationshipGraph(List.of());
    }

    /**
     * Creates a builder for constructing a RelationshipGraph.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RelationshipGraph} instances.
     *
     * @since 5.0.0
     */
    public static final class Builder {
        private final List<Relationship> relationships = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a relationship to the graph.
         *
         * @param source the source type
         * @param target the target type
         * @param type the relationship type
         * @return this builder
         * @throws NullPointerException if any argument is null
         */
        public Builder add(TypeId source, TypeId target, RelationType type) {
            relationships.add(new Relationship(source, target, type));
            return this;
        }

        /**
         * Adds a relationship to the graph.
         *
         * @param relationship the relationship to add
         * @return this builder
         * @throws NullPointerException if relationship is null
         */
        public Builder add(Relationship relationship) {
            Objects.requireNonNull(relationship, "relationship must not be null");
            relationships.add(relationship);
            return this;
        }

        /**
         * Adds all relationships from another graph.
         *
         * @param graph the graph to add relationships from
         * @return this builder
         * @throws NullPointerException if graph is null
         */
        public Builder addAll(RelationshipGraph graph) {
            Objects.requireNonNull(graph, "graph must not be null");
            relationships.addAll(graph.relationships);
            return this;
        }

        /**
         * Builds the RelationshipGraph.
         *
         * @return a new RelationshipGraph containing all added relationships
         */
        public RelationshipGraph build() {
            return new RelationshipGraph(relationships);
        }
    }
}
