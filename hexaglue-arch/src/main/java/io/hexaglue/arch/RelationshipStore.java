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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Store of relationships between elements with O(1) lookup.
 *
 * <h2>Why this exists (v2.1)</h2>
 * <p>Without this store, queries like "which repository manages this aggregate?"
 * would require O(n) scans of all DrivenPorts on each call. With this store,
 * it's a direct Map lookup.</p>
 *
 * <h2>Pre-indexed relations</h2>
 * <ul>
 *   <li>IMPLEMENTS: type → implemented interfaces</li>
 *   <li>EXTENDS: type → supertype</li>
 *   <li>MANAGES: repository/service → managed types</li>
 *   <li>USES: type → used types (dependencies)</li>
 *   <li>CONTAINS: aggregate → internal entities/VOs</li>
 *   <li>IMPLEMENTED_BY: port → implementing adapter</li>
 *   <li>PUBLISHES: type → published events</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * RelationshipStore store = RelationshipStore.builder()
 *     .addManages(repositoryId, aggregateId)
 *     .addImplements(classId, interfaceId)
 *     .build();
 *
 * // O(1) lookup
 * Optional<ElementId> repo = store.repositoryFor(aggregateId);
 * }</pre>
 *
 * @since 4.0.0
 */
public final class RelationshipStore {

    private final Map<RelationshipKey, Set<ElementId>> outgoing;
    private final Map<RelationshipKey, Set<ElementId>> incoming;

    private RelationshipStore(
            Map<RelationshipKey, Set<ElementId>> outgoing, Map<RelationshipKey, Set<ElementId>> incoming) {
        this.outgoing = copyDeep(outgoing);
        this.incoming = copyDeep(incoming);
    }

    private static Map<RelationshipKey, Set<ElementId>> copyDeep(Map<RelationshipKey, Set<ElementId>> map) {
        Map<RelationshipKey, Set<ElementId>> copy = new HashMap<>();
        map.forEach((k, v) -> copy.put(k, Set.copyOf(v)));
        return Map.copyOf(copy);
    }

    // ===== Outgoing queries (from → to) =====

    /**
     * Returns the targets of relationships from this element with the given type.
     *
     * @param from the source element
     * @param type the relationship type
     * @return set of target element IDs (empty if none)
     */
    public Set<ElementId> outgoing(ElementId from, RelationType type) {
        return outgoing.getOrDefault(new RelationshipKey(from, type), Set.of());
    }

    /**
     * Returns the interfaces implemented by this type.
     *
     * <p>Shortcut for {@code outgoing(type, IMPLEMENTS)}.</p>
     *
     * @param type the implementing type
     * @return set of implemented interface IDs
     */
    public Set<ElementId> implementedBy(ElementId type) {
        return outgoing(type, RelationType.IMPLEMENTS);
    }

    /**
     * Returns the types used by this type (dependencies).
     *
     * <p>Shortcut for {@code outgoing(type, USES)}.</p>
     *
     * @param type the dependent type
     * @return set of dependency IDs
     */
    public Set<ElementId> usedBy(ElementId type) {
        return outgoing(type, RelationType.USES);
    }

    // ===== Incoming queries (to ← from) =====

    /**
     * Returns the sources of relationships pointing to this element with the given type.
     *
     * @param to the target element
     * @param type the relationship type
     * @return set of source element IDs (empty if none)
     */
    public Set<ElementId> incoming(ElementId to, RelationType type) {
        return incoming.getOrDefault(new RelationshipKey(to, type), Set.of());
    }

    /**
     * Returns the repository managing this aggregate (O(1) lookup).
     *
     * @param aggregate the aggregate ID
     * @return the repository ID, or empty if no repository manages this aggregate
     */
    public Optional<ElementId> repositoryFor(ElementId aggregate) {
        Set<ElementId> managers = incoming(aggregate, RelationType.MANAGES);
        return managers.stream().findFirst();
    }

    /**
     * Returns the consumers (users) of this port.
     *
     * @param port the port ID
     * @return set of consumer IDs
     */
    public Set<ElementId> consumersOf(ElementId port) {
        return incoming(port, RelationType.USES);
    }

    // ===== Builder =====

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating a RelationshipStore.
     */
    public static final class Builder {
        private final Map<RelationshipKey, Set<ElementId>> outgoing = new HashMap<>();
        private final Map<RelationshipKey, Set<ElementId>> incoming = new HashMap<>();

        private Builder() {}

        /**
         * Adds a relationship from one element to another.
         *
         * @param from the source element
         * @param type the relationship type
         * @param to the target element
         * @return this builder
         */
        public Builder addRelation(ElementId from, RelationType type, ElementId to) {
            outgoing.computeIfAbsent(new RelationshipKey(from, type), k -> new HashSet<>())
                    .add(to);
            incoming.computeIfAbsent(new RelationshipKey(to, type), k -> new HashSet<>())
                    .add(from);
            return this;
        }

        /**
         * Adds an IMPLEMENTS relationship.
         *
         * @param type the implementing type
         * @param iface the implemented interface
         * @return this builder
         */
        public Builder addImplements(ElementId type, ElementId iface) {
            return addRelation(type, RelationType.IMPLEMENTS, iface);
        }

        /**
         * Adds a MANAGES relationship.
         *
         * @param repository the repository or service
         * @param aggregate the managed aggregate
         * @return this builder
         */
        public Builder addManages(ElementId repository, ElementId aggregate) {
            return addRelation(repository, RelationType.MANAGES, aggregate);
        }

        /**
         * Adds a USES relationship.
         *
         * @param from the dependent type
         * @param to the dependency
         * @return this builder
         */
        public Builder addUses(ElementId from, ElementId to) {
            return addRelation(from, RelationType.USES, to);
        }

        /**
         * Adds a CONTAINS relationship.
         *
         * @param aggregate the containing aggregate
         * @param contained the contained entity or value object
         * @return this builder
         */
        public Builder addContains(ElementId aggregate, ElementId contained) {
            return addRelation(aggregate, RelationType.CONTAINS, contained);
        }

        /**
         * Adds an IMPLEMENTED_BY relationship.
         *
         * @param port the driven port
         * @param adapter the implementing adapter
         * @return this builder
         */
        public Builder addImplementedBy(ElementId port, ElementId adapter) {
            return addRelation(port, RelationType.IMPLEMENTED_BY, adapter);
        }

        /**
         * Adds a PUBLISHES relationship.
         *
         * @param publisher the publishing type
         * @param event the published event
         * @return this builder
         */
        public Builder addPublishes(ElementId publisher, ElementId event) {
            return addRelation(publisher, RelationType.PUBLISHES, event);
        }

        /**
         * Builds the relationship store.
         *
         * @return an immutable RelationshipStore
         */
        public RelationshipStore build() {
            return new RelationshipStore(outgoing, incoming);
        }
    }

    /**
     * Internal key for indexing relationships.
     */
    private record RelationshipKey(ElementId element, RelationType type) {}
}
