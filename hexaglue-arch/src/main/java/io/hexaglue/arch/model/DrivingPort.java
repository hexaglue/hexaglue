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
 * Represents a driving (primary/inbound) port in hexagonal architecture.
 *
 * <p>A driving port is an interface that the outside world uses to interact
 * with the application. These ports are "driven" by external actors (users,
 * other systems, scheduled tasks) and define the use cases the application
 * provides.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Inbound - defines what the application can do</li>
 *   <li>Interface - typically an interface implemented by application services</li>
 *   <li>Use case oriented - methods represent business operations</li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <p>Common driving port patterns:</p>
 * <ul>
 *   <li>OrderService - creating and managing orders</li>
 *   <li>QueryOrderUseCase - querying order information</li>
 *   <li>PlaceOrderCommand - command handler for placing orders</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DrivingPort port = DrivingPort.of(
 *     TypeId.of("com.example.OrderService"),
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
public record DrivingPort(TypeId id, TypeStructure structure, ClassificationTrace classification) implements PortType {

    /**
     * Creates a new DrivingPort.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @throws NullPointerException if any argument is null
     */
    public DrivingPort {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DRIVING_PORT;
    }

    /**
     * Creates a DrivingPort with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new DrivingPort
     * @throws NullPointerException if any argument is null
     */
    public static DrivingPort of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new DrivingPort(id, structure, classification);
    }
}
