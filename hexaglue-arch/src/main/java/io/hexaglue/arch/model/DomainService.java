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
 * <h2>Usage</h2>
 * <pre>{@code
 * DomainService service = DomainService.of(
 *     TypeId.of("com.example.PricingService"),
 *     structure,
 *     trace
 * );
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @since 4.1.0
 */
public record DomainService(TypeId id, TypeStructure structure, ClassificationTrace classification)
        implements DomainType {

    /**
     * Creates a new DomainService.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @throws NullPointerException if any argument is null
     */
    public DomainService {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DOMAIN_SERVICE;
    }

    /**
     * Creates a DomainService with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new DomainService
     * @throws NullPointerException if any argument is null
     */
    public static DomainService of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new DomainService(id, structure, classification);
    }
}
