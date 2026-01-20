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

import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Index for port types providing convenient access methods.
 *
 * <p>The PortIndex provides typed access to driving and driven ports,
 * with specialized methods for repositories and gateways.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * PortIndex ports = model.portIndex();
 *
 * // Iterate over driving ports (inbound)
 * ports.drivingPorts().forEach(port -> {
 *     System.out.println("Use Case: " + port.simpleName());
 * });
 *
 * // Find repository for an aggregate
 * ports.repositoryFor(TypeId.of("com.example.Order"))
 *     .ifPresent(repo -> System.out.println("Repository: " + repo.simpleName()));
 *
 * // List all gateways
 * ports.gateways().forEach(gateway ->
 *     System.out.println("Gateway: " + gateway.simpleName()));
 * }</pre>
 *
 * @since 4.1.0
 */
public final class PortIndex {

    private final TypeRegistry registry;

    private PortIndex(TypeRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * Creates a PortIndex from a TypeRegistry.
     *
     * @param registry the type registry
     * @return a new PortIndex
     * @throws NullPointerException if registry is null
     */
    public static PortIndex from(TypeRegistry registry) {
        return new PortIndex(registry);
    }

    /**
     * Returns a stream of all driving ports.
     *
     * <p>Driving ports are inbound/primary ports that define the
     * interfaces exposed to the outside world (use cases).</p>
     *
     * @return stream of driving ports
     */
    public Stream<DrivingPort> drivingPorts() {
        return registry.all(DrivingPort.class);
    }

    /**
     * Returns a stream of all driven ports.
     *
     * <p>Driven ports are outbound/secondary ports that define
     * interfaces for external dependencies (repositories, gateways).</p>
     *
     * @return stream of driven ports
     */
    public Stream<DrivenPort> drivenPorts() {
        return registry.all(DrivenPort.class);
    }

    /**
     * Returns a stream of all repository ports.
     *
     * @return stream of repository ports
     */
    public Stream<DrivenPort> repositories() {
        return drivenPorts().filter(p -> p.portType() == DrivenPortType.REPOSITORY);
    }

    /**
     * Returns a stream of all gateway ports.
     *
     * @return stream of gateway ports
     */
    public Stream<DrivenPort> gateways() {
        return drivenPorts().filter(p -> p.portType() == DrivenPortType.GATEWAY);
    }

    /**
     * Returns the repository that manages the given aggregate.
     *
     * <p>Looks for a driven port with type REPOSITORY that has the
     * aggregate as its managed aggregate.</p>
     *
     * @param aggregateId the aggregate type id
     * @return an optional containing the repository, or empty if not found
     */
    public Optional<DrivenPort> repositoryFor(TypeId aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return repositories()
                .filter(repo -> repo.managedAggregate()
                        .map(ref -> ref.qualifiedName().equals(aggregateId.qualifiedName()))
                        .orElse(false))
                .findFirst();
    }

    /**
     * Returns the driving ports that are implemented by the given service type.
     *
     * <p>This searches for driving ports where the service type appears
     * in the port's interface implementations.</p>
     *
     * @param serviceId the service type id
     * @return list of driving ports implemented by the service
     */
    public List<DrivingPort> portsImplementedBy(TypeId serviceId) {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        return drivingPorts().filter(port -> implementsService(port, serviceId)).toList();
    }

    private boolean implementsService(DrivingPort port, TypeId serviceId) {
        return port.structure().interfaces().stream()
                .map(TypeRef::qualifiedName)
                .anyMatch(qn -> qn.equals(serviceId.qualifiedName()));
    }
}
