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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.List;

/**
 * Detailed breakdown of all architectural components.
 *
 * <h2>Domain Layer</h2>
 * <ul>
 *   <li>{@link #aggregates()} - Aggregate root components</li>
 *   <li>{@link #entities()} - Entity components</li>
 *   <li>{@link #valueObjects()} - Value object components</li>
 *   <li>{@link #identifiers()} - Identifier components</li>
 *   <li>{@link #domainEvents()} - Domain event components</li>
 *   <li>{@link #domainServices()} - Domain service components</li>
 * </ul>
 *
 * <h2>Application Layer</h2>
 * <ul>
 *   <li>{@link #applicationServices()} - Application service components</li>
 *   <li>{@link #commandHandlers()} - Command handler components</li>
 *   <li>{@link #queryHandlers()} - Query handler components</li>
 * </ul>
 *
 * <h2>Ports Layer</h2>
 * <ul>
 *   <li>{@link #drivingPorts()} - Driving port components</li>
 *   <li>{@link #drivenPorts()} - Driven port components</li>
 * </ul>
 *
 * <h2>Adapters</h2>
 * <ul>
 *   <li>{@link #adapters()} - Adapter components</li>
 * </ul>
 *
 * @param aggregates list of aggregate root components
 * @param entities list of entity components
 * @param valueObjects list of value object components
 * @param identifiers list of identifier components
 * @param domainEvents list of domain event components
 * @param domainServices list of domain service components
 * @param applicationServices list of application service components
 * @param commandHandlers list of command handler components
 * @param queryHandlers list of query handler components
 * @param drivingPorts list of driving port components
 * @param drivenPorts list of driven port components
 * @param adapters list of adapter components
 * @since 5.0.0
 */
public record ComponentDetails(
        // Domain Layer
        List<AggregateComponent> aggregates,
        List<EntityComponent> entities,
        List<ValueObjectComponent> valueObjects,
        List<IdentifierComponent> identifiers,
        List<DomainEventComponent> domainEvents,
        List<DomainServiceComponent> domainServices,
        // Application Layer
        List<ApplicationServiceComponent> applicationServices,
        List<CommandHandlerComponent> commandHandlers,
        List<QueryHandlerComponent> queryHandlers,
        // Ports Layer
        List<PortComponent> drivingPorts,
        List<PortComponent> drivenPorts,
        // Adapters
        List<AdapterComponent> adapters) {

    /**
     * Creates component details with defensive copies.
     */
    public ComponentDetails {
        aggregates = aggregates != null ? List.copyOf(aggregates) : List.of();
        entities = entities != null ? List.copyOf(entities) : List.of();
        valueObjects = valueObjects != null ? List.copyOf(valueObjects) : List.of();
        identifiers = identifiers != null ? List.copyOf(identifiers) : List.of();
        domainEvents = domainEvents != null ? List.copyOf(domainEvents) : List.of();
        domainServices = domainServices != null ? List.copyOf(domainServices) : List.of();
        applicationServices = applicationServices != null ? List.copyOf(applicationServices) : List.of();
        commandHandlers = commandHandlers != null ? List.copyOf(commandHandlers) : List.of();
        queryHandlers = queryHandlers != null ? List.copyOf(queryHandlers) : List.of();
        drivingPorts = drivingPorts != null ? List.copyOf(drivingPorts) : List.of();
        drivenPorts = drivenPorts != null ? List.copyOf(drivenPorts) : List.of();
        adapters = adapters != null ? List.copyOf(adapters) : List.of();
    }

    /**
     * Creates an empty component details instance.
     *
     * @return empty component details
     */
    public static ComponentDetails empty() {
        return new ComponentDetails(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of());
    }

    /**
     * Creates component details with only the original 6 fields (backward compatibility).
     *
     * <p>This factory method maintains compatibility with existing code that only
     * provides aggregates, valueObjects, identifiers, drivingPorts, drivenPorts, and adapters.
     *
     * @param aggregates list of aggregate root components
     * @param valueObjects list of value object components
     * @param identifiers list of identifier components
     * @param drivingPorts list of driving port components
     * @param drivenPorts list of driven port components
     * @param adapters list of adapter components
     * @return component details with empty lists for new fields
     */
    public static ComponentDetails of(
            List<AggregateComponent> aggregates,
            List<ValueObjectComponent> valueObjects,
            List<IdentifierComponent> identifiers,
            List<PortComponent> drivingPorts,
            List<PortComponent> drivenPorts,
            List<AdapterComponent> adapters) {
        return new ComponentDetails(
                aggregates,
                List.of(), // entities
                valueObjects,
                identifiers,
                List.of(), // domainEvents
                List.of(), // domainServices
                List.of(), // applicationServices
                List.of(), // commandHandlers
                List.of(), // queryHandlers
                drivingPorts,
                drivenPorts,
                adapters);
    }

    /**
     * Returns whether the application layer has any components.
     *
     * @return true if there are application services, command handlers, or query handlers
     * @since 5.0.0
     */
    public boolean hasApplicationLayer() {
        return !applicationServices.isEmpty() || !commandHandlers.isEmpty() || !queryHandlers.isEmpty();
    }

    /**
     * Returns whether the ports layer has any components.
     *
     * @return true if there are driving or driven ports
     * @since 5.0.0
     */
    public boolean hasPortsLayer() {
        return !drivingPorts.isEmpty() || !drivenPorts.isEmpty();
    }
}
