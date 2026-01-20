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

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an aggregate root in the domain model.
 *
 * <p>An aggregate root is the entry point to an aggregate - a cluster of domain objects
 * that are treated as a unit for data changes. The aggregate root guarantees the
 * consistency of changes within the aggregate and is the only object that external
 * objects can hold references to.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Entry point - the only way to access objects within the aggregate</li>
 *   <li>Consistency boundary - ensures all invariants are satisfied</li>
 *   <li>Transaction boundary - changes are committed atomically</li>
 *   <li>Required identity - must have an identity field (never null)</li>
 * </ul>
 *
 * <h2>Components</h2>
 * <p>An aggregate root may contain:</p>
 * <ul>
 *   <li>Entities - internal entities that are part of this aggregate</li>
 *   <li>Value objects - immutable objects describing aspects of the aggregate</li>
 *   <li>Domain events - events that this aggregate can emit</li>
 *   <li>Invariants - business rules that must always be true</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AggregateRoot order = AggregateRoot.builder(
 *         TypeId.of("com.example.Order"),
 *         structure,
 *         trace,
 *         idField)
 *     .entities(List.of(TypeRef.of("com.example.OrderLine")))
 *     .valueObjects(List.of(TypeRef.of("com.example.Money")))
 *     .domainEvents(List.of(TypeRef.of("com.example.OrderCreated")))
 *     .drivenPort(TypeRef.of("com.example.OrderRepository"))
 *     .invariants(List.of(Invariant.of("orderMustHaveItems", "An order must have items")))
 *     .build();
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @param identityField the field that identifies this aggregate (REQUIRED - never null)
 * @param effectiveIdentityType the actual type used for identity (may differ from field type for wrapped IDs)
 * @param entities the internal entities that are part of this aggregate (immutable)
 * @param valueObjects the value objects that are part of this aggregate (immutable)
 * @param domainEvents the domain events this aggregate can emit (immutable)
 * @param drivenPort the repository/driven port for this aggregate (if any)
 * @param invariants the business invariants for this aggregate (immutable)
 * @since 4.1.0
 */
public record AggregateRoot(
        TypeId id,
        TypeStructure structure,
        ClassificationTrace classification,
        Field identityField,
        TypeRef effectiveIdentityType,
        List<TypeRef> entities,
        List<TypeRef> valueObjects,
        List<TypeRef> domainEvents,
        Optional<TypeRef> drivenPort,
        List<Invariant> invariants)
        implements DomainType {

    /**
     * Creates a new AggregateRoot.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @param identityField the identity field, must not be null (REQUIRED)
     * @param effectiveIdentityType the effective identity type, must not be null
     * @param entities the internal entities, must not be null
     * @param valueObjects the value objects, must not be null
     * @param domainEvents the domain events, must not be null
     * @param drivenPort the driven port, must not be null (use Optional.empty() for none)
     * @param invariants the invariants, must not be null
     * @throws NullPointerException if any argument is null
     */
    public AggregateRoot {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(identityField, "identityField must not be null");
        Objects.requireNonNull(effectiveIdentityType, "effectiveIdentityType must not be null");
        Objects.requireNonNull(entities, "entities must not be null");
        Objects.requireNonNull(valueObjects, "valueObjects must not be null");
        Objects.requireNonNull(domainEvents, "domainEvents must not be null");
        Objects.requireNonNull(drivenPort, "drivenPort must not be null");
        Objects.requireNonNull(invariants, "invariants must not be null");
        entities = List.copyOf(entities);
        valueObjects = List.copyOf(valueObjects);
        domainEvents = List.copyOf(domainEvents);
        invariants = List.copyOf(invariants);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.AGGREGATE_ROOT;
    }

    /**
     * Returns whether this aggregate has an associated driven port (repository).
     *
     * @return true if a driven port is present
     */
    public boolean hasDrivenPort() {
        return drivenPort.isPresent();
    }

    /**
     * Returns whether this aggregate has any invariants defined.
     *
     * @return true if invariants are present
     */
    public boolean hasInvariants() {
        return !invariants.isEmpty();
    }

    /**
     * Creates a builder for constructing an AggregateRoot.
     *
     * <p>The builder requires the type id, structure, classification trace, and
     * identity field. These are mandatory and cannot be null.</p>
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param identityField the identity field (REQUIRED - cannot be null)
     * @return a new builder
     * @throws NullPointerException if any argument is null
     */
    public static Builder builder(
            TypeId id, TypeStructure structure, ClassificationTrace classification, Field identityField) {
        return new Builder(id, structure, classification, identityField);
    }

    /**
     * Builder for constructing {@link AggregateRoot} instances.
     *
     * @since 4.1.0
     */
    public static final class Builder {
        private final TypeId id;
        private final TypeStructure structure;
        private final ClassificationTrace classification;
        private final Field identityField;
        private TypeRef effectiveIdentityType;
        private List<TypeRef> entities = List.of();
        private List<TypeRef> valueObjects = List.of();
        private List<TypeRef> domainEvents = List.of();
        private Optional<TypeRef> drivenPort = Optional.empty();
        private List<Invariant> invariants = List.of();

        private Builder(TypeId id, TypeStructure structure, ClassificationTrace classification, Field identityField) {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(structure, "structure must not be null");
            Objects.requireNonNull(classification, "classification must not be null");
            Objects.requireNonNull(identityField, "identityField must not be null");
            this.id = id;
            this.structure = structure;
            this.classification = classification;
            this.identityField = identityField;
            this.effectiveIdentityType = identityField.type(); // Default to field type
        }

        /**
         * Sets the effective identity type.
         *
         * <p>The effective identity type may differ from the identity field type
         * for wrapped identifiers (e.g., OrderId wrapping UUID).</p>
         *
         * @param effectiveIdentityType the effective identity type
         * @return this builder
         */
        public Builder effectiveIdentityType(TypeRef effectiveIdentityType) {
            this.effectiveIdentityType = effectiveIdentityType;
            return this;
        }

        /**
         * Sets the internal entities.
         *
         * @param entities the entities that are part of this aggregate
         * @return this builder
         */
        public Builder entities(List<TypeRef> entities) {
            this.entities = entities;
            return this;
        }

        /**
         * Sets the value objects.
         *
         * @param valueObjects the value objects that are part of this aggregate
         * @return this builder
         */
        public Builder valueObjects(List<TypeRef> valueObjects) {
            this.valueObjects = valueObjects;
            return this;
        }

        /**
         * Sets the domain events.
         *
         * @param domainEvents the domain events this aggregate can emit
         * @return this builder
         */
        public Builder domainEvents(List<TypeRef> domainEvents) {
            this.domainEvents = domainEvents;
            return this;
        }

        /**
         * Sets the driven port (repository).
         *
         * @param drivenPort the driven port for this aggregate
         * @return this builder
         */
        public Builder drivenPort(TypeRef drivenPort) {
            this.drivenPort = Optional.ofNullable(drivenPort);
            return this;
        }

        /**
         * Sets the invariants.
         *
         * @param invariants the business invariants for this aggregate
         * @return this builder
         */
        public Builder invariants(List<Invariant> invariants) {
            this.invariants = invariants;
            return this;
        }

        /**
         * Builds the AggregateRoot.
         *
         * @return a new AggregateRoot
         */
        public AggregateRoot build() {
            return new AggregateRoot(
                    id,
                    structure,
                    classification,
                    identityField,
                    effectiveIdentityType,
                    entities,
                    valueObjects,
                    domainEvents,
                    drivenPort,
                    invariants);
        }
    }
}
