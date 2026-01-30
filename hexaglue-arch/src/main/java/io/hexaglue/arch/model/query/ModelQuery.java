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

package io.hexaglue.arch.model.query;

import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import java.util.Objects;
import java.util.Optional;

/**
 * Entry point for fluent queries on the architectural model.
 *
 * <p>Provides chainable query builders for navigating and filtering
 * architectural elements. All queries are immutable and can be reused.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create from indexes
 * ModelQuery query = ModelQuery.of(registry, domainIndex, portIndex);
 *
 * // Query aggregates with fluent API
 * List<AggregateRoot> aggregates = query.aggregates()
 *     .withRepository()
 *     .inPackage("com.example.order")
 *     .toList();
 *
 * // Query ports
 * List<DrivingPort> useCasePorts = query.drivingPorts()
 *     .withUseCases()
 *     .toList();
 *
 * // Find specific type
 * Optional<AggregateRoot> order = query.aggregate(TypeId.of("com.example.Order"));
 * }</pre>
 *
 * @since 5.0.0
 */
public final class ModelQuery {

    private final TypeRegistry registry;
    private final DomainIndex domainIndex;
    private final PortIndex portIndex;

    private ModelQuery(TypeRegistry registry, DomainIndex domainIndex, PortIndex portIndex) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.domainIndex = Objects.requireNonNull(domainIndex, "domainIndex must not be null");
        this.portIndex = Objects.requireNonNull(portIndex, "portIndex must not be null");
    }

    /**
     * Creates a new ModelQuery from the given components.
     *
     * @param registry the type registry
     * @param domainIndex the domain index
     * @param portIndex the port index
     * @return a new ModelQuery
     * @throws NullPointerException if any argument is null
     */
    public static ModelQuery of(TypeRegistry registry, DomainIndex domainIndex, PortIndex portIndex) {
        return new ModelQuery(registry, domainIndex, portIndex);
    }

    /**
     * Returns a query for aggregate roots.
     *
     * @return an aggregate query
     */
    public AggregateQuery aggregates() {
        return AggregateQuery.of(domainIndex, portIndex);
    }

    /**
     * Returns a query for driving ports.
     *
     * @return a port query for driving ports
     */
    public PortQuery drivingPorts() {
        return PortQuery.drivingPorts(portIndex);
    }

    /**
     * Returns a query for driven ports.
     *
     * @return a port query for driven ports
     */
    public PortQuery drivenPorts() {
        return PortQuery.drivenPorts(portIndex);
    }

    /**
     * Returns a query for all ports.
     *
     * @return a port query for all ports
     */
    public PortQuery allPorts() {
        return PortQuery.allPorts(portIndex);
    }

    /**
     * Finds an aggregate root by its type ID.
     *
     * @param id the type ID
     * @return the aggregate if found
     */
    public Optional<io.hexaglue.arch.model.AggregateRoot> aggregate(TypeId id) {
        return domainIndex.aggregateRoots().filter(agg -> agg.id().equals(id)).findFirst();
    }

    /**
     * Finds any type by its type ID.
     *
     * @param id the type ID
     * @param <T> the expected type
     * @return the type if found
     */
    // Suppressed: TypeRegistry stores ArchType values; caller specifies T via context, erasure requires cast
    @SuppressWarnings("unchecked")
    public <T extends ArchType> Optional<T> find(TypeId id) {
        return (Optional<T>) registry.get(id);
    }

    /**
     * Returns the underlying type registry.
     *
     * @return the type registry
     */
    public TypeRegistry registry() {
        return registry;
    }

    /**
     * Returns the underlying domain index.
     *
     * @return the domain index
     */
    public DomainIndex domainIndex() {
        return domainIndex;
    }

    /**
     * Returns the underlying port index.
     *
     * @return the port index
     */
    public PortIndex portIndex() {
        return portIndex;
    }
}
