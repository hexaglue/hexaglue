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

package io.hexaglue.arch.query;

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.arch.adapters.DrivenAdapter;
import io.hexaglue.arch.adapters.DrivingAdapter;
import io.hexaglue.arch.domain.DomainService;
import io.hexaglue.arch.ports.ApplicationService;
import java.util.Optional;

/**
 * Main entry point for fluent queries on the architectural model.
 *
 * <p>Provides access to specialized queries for different element types
 * (aggregates, ports, services, adapters) as well as generic element queries.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ModelQuery query = model.query();
 *
 * // Query aggregates
 * query.aggregates()
 *      .withRepository()
 *      .forEach(agg -> process(agg));
 *
 * // Query ports
 * query.drivingPorts()
 *      .useCases()
 *      .toList();
 *
 * // Find specific element
 * Optional<DrivingPort> port = query.find(ElementRef.of(id, DrivingPort.class));
 * }</pre>
 *
 * @since 4.0.0
 */
public interface ModelQuery {

    // === Aggregates ===

    /**
     * Returns a query for all aggregates.
     *
     * @return an aggregate query
     */
    AggregateQuery aggregates();

    // === Ports ===

    /**
     * Returns a query for driving ports (primary/inbound ports).
     *
     * @return a port query for driving ports
     */
    PortQuery drivingPorts();

    /**
     * Returns a query for driven ports (secondary/outbound ports).
     *
     * @return a port query for driven ports
     */
    PortQuery drivenPorts();

    /**
     * Returns a query for all ports (driving and driven).
     *
     * @return a port query for all ports
     */
    PortQuery allPorts();

    // === Services ===

    /**
     * Returns a query for application services.
     *
     * @return a service query for application services
     */
    ServiceQuery<ApplicationService> applicationServices();

    /**
     * Returns a query for domain services.
     *
     * @return a service query for domain services
     */
    ServiceQuery<DomainService> domainServices();

    // === Adapters ===

    /**
     * Returns a query for driving adapters (primary adapters).
     *
     * @return an adapter query for driving adapters
     */
    AdapterQuery<DrivingAdapter> drivingAdapters();

    /**
     * Returns a query for driven adapters (secondary adapters).
     *
     * @return an adapter query for driven adapters
     */
    AdapterQuery<DrivenAdapter> drivenAdapters();

    // === Generic ===

    /**
     * Returns a query for all elements.
     *
     * @return an element query for all elements
     */
    ElementQuery<ArchElement> elements();

    /**
     * Returns a query for elements of a specific type.
     *
     * @param type the element type class
     * @param <T> the element type
     * @return an element query for the specified type
     */
    <T extends ArchElement> ElementQuery<T> elements(Class<T> type);

    // === Direct Lookup ===

    /**
     * Finds an element by reference.
     *
     * @param ref the element reference
     * @param <T> the element type
     * @return the element if found
     */
    <T extends ArchElement> Optional<T> find(ElementRef<T> ref);

    /**
     * Gets an element by reference or throws.
     *
     * @param ref the element reference
     * @param <T> the element type
     * @return the element
     * @throws io.hexaglue.arch.UnresolvedReferenceException if not found
     */
    <T extends ArchElement> T get(ElementRef<T> ref);
}
