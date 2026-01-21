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
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a domain event in the domain model.
 *
 * <p>A domain event represents something that happened in the domain that domain
 * experts care about. Events are named in past tense (e.g., {@code OrderCreated},
 * {@code PaymentReceived}) and are typically immutable records of what occurred.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Past tense naming - represents something that already happened</li>
 *   <li>Immutable - events are facts that cannot be changed</li>
 *   <li>Contains relevant data - all information needed to describe what happened</li>
 * </ul>
 *
 * <h2>Event Metadata (since 5.0.0)</h2>
 * <p>Domain events commonly include metadata fields that are detected automatically:</p>
 * <ul>
 *   <li>{@link #aggregateIdField()} - Field containing the aggregate identifier
 *       (fields containing "aggregateId" or ending with "Id")</li>
 *   <li>{@link #timestampField()} - Field containing the event timestamp
 *       (fields named "timestamp", "occurredAt", "createdAt", "eventTime")</li>
 *   <li>{@link #sourceAggregate()} - Reference to the aggregate that emitted this event</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DomainEvent event = DomainEvent.of(
 *     TypeId.of("com.example.OrderCreated"),
 *     structure,
 *     trace
 * );
 *
 * // Access aggregate id field if present (since 5.0.0)
 * event.aggregateIdField().ifPresent(field ->
 *     System.out.println("Aggregate ID field: " + field.name())
 * );
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @param aggregateIdField the field containing the aggregate identifier (optional)
 * @param timestampField the field containing the event timestamp (optional)
 * @param sourceAggregate reference to the aggregate that emitted this event (optional)
 * @since 4.1.0
 * @since 5.0.0 added aggregateIdField, timestampField, and sourceAggregate
 */
public record DomainEvent(
        TypeId id,
        TypeStructure structure,
        ClassificationTrace classification,
        Optional<Field> aggregateIdField,
        Optional<Field> timestampField,
        Optional<TypeRef> sourceAggregate)
        implements DomainType {

    /**
     * Creates a new DomainEvent.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @param aggregateIdField the aggregate id field, must not be null (may be empty)
     * @param timestampField the timestamp field, must not be null (may be empty)
     * @param sourceAggregate the source aggregate reference, must not be null (may be empty)
     * @throws NullPointerException if any argument is null
     */
    public DomainEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(aggregateIdField, "aggregateIdField must not be null");
        Objects.requireNonNull(timestampField, "timestampField must not be null");
        Objects.requireNonNull(sourceAggregate, "sourceAggregate must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DOMAIN_EVENT;
    }

    /**
     * Creates a DomainEvent with the given parameters.
     *
     * <p>This factory method creates an event without metadata fields, for backward
     * compatibility with code that doesn't need the enhanced metadata.</p>
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new DomainEvent
     * @throws NullPointerException if any argument is null
     */
    public static DomainEvent of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new DomainEvent(id, structure, classification, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Creates a DomainEvent with all parameters including metadata fields.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param aggregateIdField the aggregate id field
     * @param timestampField the timestamp field
     * @param sourceAggregate the source aggregate reference
     * @return a new DomainEvent
     * @throws NullPointerException if any argument is null
     * @since 5.0.0
     */
    public static DomainEvent of(
            TypeId id,
            TypeStructure structure,
            ClassificationTrace classification,
            Optional<Field> aggregateIdField,
            Optional<Field> timestampField,
            Optional<TypeRef> sourceAggregate) {
        return new DomainEvent(id, structure, classification, aggregateIdField, timestampField, sourceAggregate);
    }

    /**
     * Returns whether this event has an aggregate id field.
     *
     * @return true if the event has an aggregate id field
     * @since 5.0.0
     */
    public boolean hasAggregateIdField() {
        return aggregateIdField.isPresent();
    }

    /**
     * Returns whether this event has a timestamp field.
     *
     * @return true if the event has a timestamp field
     * @since 5.0.0
     */
    public boolean hasTimestampField() {
        return timestampField.isPresent();
    }

    /**
     * Returns whether this event has a known source aggregate.
     *
     * @return true if the source aggregate is known
     * @since 5.0.0
     */
    public boolean hasSourceAggregate() {
        return sourceAggregate.isPresent();
    }
}
