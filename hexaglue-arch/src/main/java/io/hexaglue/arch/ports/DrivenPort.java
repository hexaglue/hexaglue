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

package io.hexaglue.arch.ports;

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.arch.ElementRegistry;
import io.hexaglue.arch.domain.Aggregate;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A Driven Port (Secondary/Outbound) in Hexagonal Architecture.
 *
 * <p>Driven ports define the SPI (Service Provider Interface) that the application
 * needs from infrastructure. They are called by the application core and implemented
 * by driven adapters.</p>
 *
 * <h2>Classifications</h2>
 * <ul>
 *   <li>{@link PortClassification#REPOSITORY} - Aggregate persistence</li>
 *   <li>{@link PortClassification#GATEWAY} - External system integration</li>
 *   <li>{@link PortClassification#EVENT_PUBLISHER} - Domain event publishing</li>
 *   <li>{@link PortClassification#NOTIFICATION} - Notification service</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In application:
 * public interface OrderRepository {
 *     Optional<Order> findById(OrderId id);
 *     void save(Order order);
 * }
 *
 * // As ArchElement:
 * DrivenPort port = new DrivenPort(
 *     ElementId.of("com.example.OrderRepository"),
 *     PortClassification.REPOSITORY,
 *     List.of(findByIdOp, saveOp),
 *     Optional.of(orderAggRef),
 *     List.of(),
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier
 * @param classification the specific port classification
 * @param operations the operations defined by this port
 * @param primaryManagedType the primary aggregate managed (for repositories)
 * @param managedTypes additional types managed by this port
 * @param syntax the syntax information (nullable)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record DrivenPort(
        ElementId id,
        PortClassification classification,
        List<PortOperation> operations,
        Optional<ElementRef<Aggregate>> primaryManagedType,
        List<ElementRef<? extends ArchElement>> managedTypes,
        TypeSyntax syntax,
        ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new DrivenPort instance.
     *
     * @param id the identifier, must not be null
     * @param classification the classification, must not be null
     * @param operations the operations, must not be null
     * @param primaryManagedType the primary managed type
     * @param managedTypes additional managed types, must not be null
     * @param syntax the syntax (can be null)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if required fields are null
     */
    public DrivenPort {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(operations, "operations must not be null");
        Objects.requireNonNull(primaryManagedType, "primaryManagedType must not be null (use Optional.empty())");
        Objects.requireNonNull(managedTypes, "managedTypes must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        operations = List.copyOf(operations);
        managedTypes = List.copyOf(managedTypes);
    }

    @Override
    public ElementKind kind() {
        return ElementKind.DRIVEN_PORT;
    }

    /**
     * Returns whether this is a repository port.
     *
     * @return true if REPOSITORY classification
     */
    public boolean isRepository() {
        return classification == PortClassification.REPOSITORY;
    }

    /**
     * Returns whether this is a gateway port.
     *
     * @return true if GATEWAY classification
     */
    public boolean isGateway() {
        return classification == PortClassification.GATEWAY;
    }

    /**
     * Returns whether this is an event publisher port.
     *
     * @return true if EVENT_PUBLISHER classification
     */
    public boolean isEventPublisher() {
        return classification == PortClassification.EVENT_PUBLISHER;
    }

    /**
     * Resolves the primary managed aggregate (for repositories).
     *
     * @param registry the element registry
     * @return the resolved aggregate, if any
     */
    public Optional<Aggregate> resolvePrimaryAggregate(ElementRegistry registry) {
        return primaryManagedType.flatMap(ref -> ref.resolveOpt(registry));
    }

    /**
     * Creates a simple DrivenPort for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new DrivenPort
     */
    public static DrivenPort of(String qualifiedName, ClassificationTrace trace) {
        return new DrivenPort(
                ElementId.of(qualifiedName),
                PortClassification.REPOSITORY,
                List.of(),
                Optional.empty(),
                List.of(),
                null,
                trace);
    }

    /**
     * Creates a repository port for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param aggregateRef reference to the managed aggregate
     * @param trace the classification trace
     * @return a new DrivenPort
     */
    public static DrivenPort repository(
            String qualifiedName, ElementRef<Aggregate> aggregateRef, ClassificationTrace trace) {
        return new DrivenPort(
                ElementId.of(qualifiedName),
                PortClassification.REPOSITORY,
                List.of(),
                Optional.of(aggregateRef),
                List.of(),
                null,
                trace);
    }
}
