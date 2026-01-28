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

package io.hexaglue.arch.model.index;

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.graph.RelationType;
import io.hexaglue.arch.model.graph.RelationshipGraph;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Index for querying composition relationships between types.
 *
 * <p>The CompositionIndex provides convenient methods to navigate compositional
 * relationships (OWNS, CONTAINS, REFERENCES) in the architectural model. It enables
 * cross-package relationship discovery, which is essential for generating accurate
 * domain model diagrams.</p>
 *
 * <h2>Relationship Types</h2>
 * <ul>
 *   <li><strong>OWNS</strong> - Identity ownership (e.g., Order OWNS OrderId)</li>
 *   <li><strong>CONTAINS</strong> - Embedded composition (e.g., Order CONTAINS Money)</li>
 *   <li><strong>REFERENCES</strong> - Cross-aggregate reference (e.g., Order REFERENCES Customer)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * CompositionIndex compositions = model.compositionIndex().orElseThrow();
 *
 * // Get all types embedded by Order
 * compositions.embeddedBy(orderId).forEach(typeId ->
 *     System.out.println("Order embeds: " + typeId.simpleName()));
 *
 * // Get cross-aggregate references from Order
 * compositions.referencesFrom(orderId).forEach(ref ->
 *     System.out.println("Order references " + ref.aggregateType().simpleName()
 *         + " via " + ref.identifierType().simpleName()));
 *
 * // Find which aggregate owns an identifier
 * compositions.aggregateOf(orderIdType).ifPresent(aggId ->
 *     System.out.println("OrderId belongs to: " + aggId.simpleName()));
 * }</pre>
 *
 * @since 5.0.0
 */
public final class CompositionIndex {

    private final RelationshipGraph graph;
    private final TypeRegistry registry;
    private final Map<TypeId, TypeId> identifierToAggregate;

    private CompositionIndex(RelationshipGraph graph, TypeRegistry registry) {
        this.graph = Objects.requireNonNull(graph, "graph must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.identifierToAggregate = buildIdentifierIndex(registry);
    }

    /**
     * Creates a CompositionIndex from a RelationshipGraph and TypeRegistry.
     *
     * @param graph the relationship graph
     * @param registry the type registry
     * @return a new CompositionIndex
     * @throws NullPointerException if any argument is null
     */
    public static CompositionIndex from(RelationshipGraph graph, TypeRegistry registry) {
        return new CompositionIndex(graph, registry);
    }

    /**
     * Returns all types that are embedded by (contained in) the given owner type.
     *
     * <p>This includes types related via {@link RelationType#CONTAINS} or
     * {@link RelationType#OWNS}. These represent types that are part of the
     * owner's aggregate boundary.</p>
     *
     * @param ownerId the owner type ID
     * @return stream of embedded type IDs
     * @throws NullPointerException if ownerId is null
     */
    public Stream<TypeId> embeddedBy(TypeId ownerId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        return graph.from(ownerId)
                .filter(r -> r.type() == RelationType.CONTAINS || r.type() == RelationType.OWNS)
                .map(RelationshipGraph.Relationship::target);
    }

    /**
     * Returns all types that embed (contain) the given type.
     *
     * <p>This finds all owners that have a CONTAINS or OWNS relationship
     * targeting the given type.</p>
     *
     * @param embeddedId the embedded type ID
     * @return stream of owner type IDs
     * @throws NullPointerException if embeddedId is null
     */
    public Stream<TypeId> embeddedIn(TypeId embeddedId) {
        Objects.requireNonNull(embeddedId, "embeddedId must not be null");
        return graph.to(embeddedId)
                .filter(r -> r.type() == RelationType.CONTAINS || r.type() == RelationType.OWNS)
                .map(RelationshipGraph.Relationship::source);
    }

    /**
     * Returns all cross-aggregate references originating from the given type.
     *
     * <p>This returns {@link AggregateReference} records that contain the source type,
     * the identifier type used for the reference, and the target aggregate type.</p>
     *
     * @param sourceId the source type ID
     * @return stream of aggregate references
     * @throws NullPointerException if sourceId is null
     */
    public Stream<AggregateReference> referencesFrom(TypeId sourceId) {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        return graph.from(sourceId)
                .filter(r -> r.type() == RelationType.REFERENCES)
                .map(r -> {
                    TypeId aggregateId = r.target();
                    // Try to find the identifier type for this aggregate
                    TypeId identifierId = identifierOf(aggregateId).orElse(null);
                    return new AggregateReference(sourceId, identifierId, aggregateId);
                });
    }

    /**
     * Returns all types that reference the given aggregate.
     *
     * @param aggregateId the aggregate type ID
     * @return stream of referencing type IDs
     * @throws NullPointerException if aggregateId is null
     */
    public Stream<TypeId> referencedBy(TypeId aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return graph.to(aggregateId)
                .filter(r -> r.type() == RelationType.REFERENCES)
                .map(RelationshipGraph.Relationship::source);
    }

    /**
     * Returns the identifier type for the given aggregate.
     *
     * <p>Looks for a type that is OWNED by the aggregate, which typically
     * represents the aggregate's identity type.</p>
     *
     * @param aggregateId the aggregate type ID
     * @return the identifier type ID if found
     * @throws NullPointerException if aggregateId is null
     */
    public Optional<TypeId> identifierOf(TypeId aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return graph.from(aggregateId)
                .filter(r -> r.type() == RelationType.OWNS)
                .map(RelationshipGraph.Relationship::target)
                .findFirst();
    }

    /**
     * Returns the aggregate that owns the given identifier type.
     *
     * @param identifierId the identifier type ID
     * @return the aggregate type ID if found
     * @throws NullPointerException if identifierId is null
     */
    public Optional<TypeId> aggregateOf(TypeId identifierId) {
        Objects.requireNonNull(identifierId, "identifierId must not be null");
        return Optional.ofNullable(identifierToAggregate.get(identifierId));
    }

    /**
     * Returns all compositional relationships (OWNS, CONTAINS) from the given type.
     *
     * @param sourceId the source type ID
     * @return stream of relationships
     * @throws NullPointerException if sourceId is null
     */
    public Stream<RelationshipGraph.Relationship> compositionsFrom(TypeId sourceId) {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        return graph.from(sourceId).filter(r -> r.type().isCompositional());
    }

    /**
     * Returns whether the given type has any compositional relationships.
     *
     * @param typeId the type ID
     * @return true if the type has OWNS or CONTAINS relationships
     * @throws NullPointerException if typeId is null
     */
    public boolean hasCompositions(TypeId typeId) {
        Objects.requireNonNull(typeId, "typeId must not be null");
        return graph.from(typeId).anyMatch(r -> r.type().isCompositional());
    }

    /**
     * Returns the underlying relationship graph.
     *
     * @return the relationship graph
     */
    public RelationshipGraph graph() {
        return graph;
    }

    private Map<TypeId, TypeId> buildIdentifierIndex(TypeRegistry registry) {
        Map<TypeId, TypeId> index = new HashMap<>();
        registry.all(AggregateRoot.class).forEach(agg -> {
            TypeId identifierTypeId = TypeId.of(agg.identityField().type().qualifiedName());
            index.put(identifierTypeId, agg.id());
        });
        return index;
    }

    /**
     * Represents a cross-aggregate reference.
     *
     * <p>An aggregate reference captures the relationship where one type references
     * another aggregate through an identifier. For example, an Order might reference
     * a Customer via a CustomerId field.</p>
     *
     * @param sourceType the type that holds the reference (e.g., Order)
     * @param identifierType the identifier type used for the reference (e.g., CustomerId), may be null
     * @param aggregateType the referenced aggregate type (e.g., Customer)
     * @since 5.0.0
     */
    public record AggregateReference(TypeId sourceType, TypeId identifierType, TypeId aggregateType) {

        /**
         * Creates a new AggregateReference.
         *
         * @param sourceType the source type, must not be null
         * @param identifierType the identifier type, may be null if not determined
         * @param aggregateType the aggregate type, must not be null
         * @throws NullPointerException if sourceType or aggregateType is null
         */
        public AggregateReference {
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        }

        /**
         * Returns whether this reference has a known identifier type.
         *
         * @return true if identifierType is present
         */
        public boolean hasIdentifierType() {
            return identifierType != null;
        }
    }
}
