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

package io.hexaglue.arch.domain;

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Objects;

/**
 * A Domain Event in Domain-Driven Design.
 *
 * <p>Domain events represent something significant that happened in the domain.
 * They are immutable and named in the past tense.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Immutable - capture a moment in time</li>
 *   <li>Named in past tense (OrderPlaced, PaymentReceived)</li>
 *   <li>Contain relevant data about what happened</li>
 *   <li>Published by aggregates, consumed by event handlers</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In domain model:
 * public record OrderPlaced(OrderId orderId, Instant occurredAt) implements DomainEvent {}
 *
 * // As ArchElement:
 * DomainEvent event = new DomainEvent(
 *     ElementId.of("com.example.OrderPlaced"),
 *     "com.example.Order",
 *     List.of("orderId", "occurredAt"),
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier
 * @param publishedBy the qualified name of the aggregate that publishes this event (if known)
 * @param eventFields the names of fields in the event
 * @param syntax the syntax information from source analysis (nullable for synthetic types)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record DomainEvent(
        ElementId id,
        String publishedBy,
        List<String> eventFields,
        TypeSyntax syntax,
        ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new DomainEvent instance.
     *
     * @param id the identifier, must not be null
     * @param publishedBy the publisher (can be null if unknown)
     * @param eventFields the event fields, must not be null
     * @param syntax the syntax (can be null for synthetic types)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if id, eventFields, or classificationTrace is null
     */
    public DomainEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(eventFields, "eventFields must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        eventFields = List.copyOf(eventFields);
    }

    @Override
    public ElementKind kind() {
        return ElementKind.DOMAIN_EVENT;
    }

    /**
     * Returns whether the publisher is known.
     *
     * @return true if the publishing aggregate is known
     */
    public boolean hasPublisher() {
        return publishedBy != null && !publishedBy.isBlank();
    }

    /**
     * Creates a simple DomainEvent for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new DomainEvent
     */
    public static DomainEvent of(String qualifiedName, ClassificationTrace trace) {
        return new DomainEvent(ElementId.of(qualifiedName), null, List.of(), null, trace);
    }
}
