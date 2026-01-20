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
 * <h2>Usage</h2>
 * <pre>{@code
 * DomainEvent event = DomainEvent.of(
 *     TypeId.of("com.example.OrderCreated"),
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
public record DomainEvent(TypeId id, TypeStructure structure, ClassificationTrace classification)
        implements DomainType {

    /**
     * Creates a new DomainEvent.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @throws NullPointerException if any argument is null
     */
    public DomainEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DOMAIN_EVENT;
    }

    /**
     * Creates a DomainEvent with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new DomainEvent
     * @throws NullPointerException if any argument is null
     */
    public static DomainEvent of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new DomainEvent(id, structure, classification);
    }
}
