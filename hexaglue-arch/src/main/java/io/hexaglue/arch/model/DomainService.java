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

/**
 * Represents a domain service in the domain model.
 *
 * <p>A domain service is a stateless operation that performs domain logic that
 * doesn't naturally fit within an entity or value object. Domain services
 * encapsulate domain logic that involves multiple entities or requires
 * coordination between domain objects.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Stateless - no instance state, only behavior</li>
 *   <li>Domain logic - contains business rules and policies</li>
 *   <li>Named as verb phrases - describes what the service does</li>
 * </ul>
 *
 * <h2>Injected Ports (since 5.0.0)</h2>
 * <p>Domain services often depend on driven ports for accessing external resources.
 * The {@link #injectedPorts()} field contains the types of ports that are injected
 * via constructor or field injection.</p>
 *
 * <h2>Operations (since 5.0.0)</h2>
 * <p>The {@link #operations()} field contains the business methods exposed by this
 * service, excluding getters, setters, and object methods like equals/hashCode.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Basic domain service
 * DomainService service = DomainService.of(
 *     TypeId.of("com.example.PricingService"),
 *     structure,
 *     trace
 * );
 *
 * // Domain service with ports and operations (since 5.0.0)
 * DomainService serviceWithDetails = DomainService.of(
 *     TypeId.of("com.example.PricingService"),
 *     structure,
 *     trace,
 *     List.of(inventoryPort, pricingPort),
 *     List.of(calculatePriceMethod, applyDiscountMethod)
 * );
 *
 * // Check injected ports
 * service.injectedPorts().forEach(port ->
 *     System.out.println("Depends on: " + port.qualifiedName())
 * );
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @param injectedPorts the ports injected via constructor or fields (never null)
 * @param operations the business methods exposed by this service (never null)
 * @since 4.1.0
 * @since 5.0.0 added injectedPorts and operations
 */
public record DomainService(
        TypeId id,
        TypeStructure structure,
        ClassificationTrace classification,
        List<TypeRef> injectedPorts,
        List<Method> operations)
        implements DomainType {

    /**
     * Creates a new DomainService.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @param injectedPorts the injected ports, must not be null
     * @param operations the business operations, must not be null
     * @throws NullPointerException if any argument is null
     */
    public DomainService {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(injectedPorts, "injectedPorts must not be null");
        Objects.requireNonNull(operations, "operations must not be null");
        injectedPorts = List.copyOf(injectedPorts);
        operations = List.copyOf(operations);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DOMAIN_SERVICE;
    }

    /**
     * Returns whether this service has any injected ports.
     *
     * @return true if ports are injected
     * @since 5.0.0
     */
    public boolean hasInjectedPorts() {
        return !injectedPorts.isEmpty();
    }

    /**
     * Returns whether this service has any business operations.
     *
     * @return true if operations are defined
     * @since 5.0.0
     */
    public boolean hasOperations() {
        return !operations.isEmpty();
    }

    /**
     * Creates a DomainService with the given parameters (no ports or operations).
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new DomainService with empty ports and operations
     * @throws NullPointerException if any argument is null
     */
    public static DomainService of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new DomainService(id, structure, classification, List.of(), List.of());
    }

    /**
     * Creates a DomainService with all parameters including ports and operations.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param injectedPorts the ports injected via constructor or fields
     * @param operations the business methods exposed by this service
     * @return a new DomainService
     * @throws NullPointerException if any argument is null
     * @since 5.0.0
     */
    public static DomainService of(
            TypeId id,
            TypeStructure structure,
            ClassificationTrace classification,
            List<TypeRef> injectedPorts,
            List<Method> operations) {
        return new DomainService(id, structure, classification, injectedPorts, operations);
    }
}
