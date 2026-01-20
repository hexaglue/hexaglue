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
 * Represents a driven (secondary/outbound) port in hexagonal architecture.
 *
 * <p>A driven port is an interface that the application uses to interact with
 * external systems. The application "drives" these ports to perform operations
 * like persistence, messaging, or external service calls.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Outbound - defines what the application needs from outside</li>
 *   <li>Interface - implemented by adapters (infrastructure code)</li>
 *   <li>Dependency inversion - application depends on abstraction, not implementation</li>
 * </ul>
 *
 * <h2>Port Types</h2>
 * <p>Driven ports are categorized by their purpose:</p>
 * <ul>
 *   <li>REPOSITORY - persistence for aggregates</li>
 *   <li>GATEWAY - external service abstraction</li>
 *   <li>EVENT_PUBLISHER - domain event publishing</li>
 *   <li>NOTIFICATION - sending notifications</li>
 *   <li>OTHER - generic driven port</li>
 * </ul>
 *
 * <h2>Managed Aggregate</h2>
 * <p>Repository ports typically manage a specific aggregate root. The
 * {@link #managedAggregate()} method returns the aggregate type if detected.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Generic driven port
 * DrivenPort gateway = DrivenPort.of(
 *     TypeId.of("com.example.PaymentGateway"),
 *     structure,
 *     trace,
 *     DrivenPortType.GATEWAY
 * );
 *
 * // Repository with managed aggregate
 * DrivenPort repo = DrivenPort.repository(
 *     TypeId.of("com.example.OrderRepository"),
 *     structure,
 *     trace,
 *     TypeRef.of("com.example.Order")
 * );
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @param portType the type of driven port (REPOSITORY, GATEWAY, etc.)
 * @param managedAggregate the aggregate root managed by this port (for repositories)
 * @since 4.1.0
 */
public record DrivenPort(
        TypeId id,
        TypeStructure structure,
        ClassificationTrace classification,
        DrivenPortType portType,
        Optional<TypeRef> managedAggregate)
        implements PortType {

    /**
     * Creates a new DrivenPort.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @param portType the port type, must not be null
     * @param managedAggregate the managed aggregate, must not be null (use Optional.empty() for none)
     * @throws NullPointerException if any argument is null
     */
    public DrivenPort {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(portType, "portType must not be null");
        Objects.requireNonNull(managedAggregate, "managedAggregate must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DRIVEN_PORT;
    }

    /**
     * Returns whether this port is a repository.
     *
     * @return true if this is a REPOSITORY port
     */
    public boolean isRepository() {
        return portType == DrivenPortType.REPOSITORY;
    }

    /**
     * Returns whether this port is a gateway.
     *
     * @return true if this is a GATEWAY port
     */
    public boolean isGateway() {
        return portType == DrivenPortType.GATEWAY;
    }

    /**
     * Returns whether this port is an event publisher.
     *
     * @return true if this is an EVENT_PUBLISHER port
     */
    public boolean isEventPublisher() {
        return portType == DrivenPortType.EVENT_PUBLISHER;
    }

    /**
     * Returns whether this port has a managed aggregate.
     *
     * @return true if a managed aggregate is present
     */
    public boolean hasAggregate() {
        return managedAggregate.isPresent();
    }

    /**
     * Creates a DrivenPort with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param portType the port type
     * @return a new DrivenPort with no managed aggregate
     * @throws NullPointerException if any argument is null
     */
    public static DrivenPort of(
            TypeId id, TypeStructure structure, ClassificationTrace classification, DrivenPortType portType) {
        return new DrivenPort(id, structure, classification, portType, Optional.empty());
    }

    /**
     * Creates a repository DrivenPort with a managed aggregate.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param aggregate the aggregate root managed by this repository
     * @return a new DrivenPort with REPOSITORY type and managed aggregate
     * @throws NullPointerException if any argument is null
     */
    public static DrivenPort repository(
            TypeId id, TypeStructure structure, ClassificationTrace classification, TypeRef aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        return new DrivenPort(id, structure, classification, DrivenPortType.REPOSITORY, Optional.of(aggregate));
    }
}
