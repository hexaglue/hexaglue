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
 * Represents an application service in the application layer.
 *
 * <p>An application service orchestrates use cases by coordinating between
 * the domain layer and external systems through ports. It implements driving
 * ports and uses driven ports to accomplish business operations.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Use case coordinator - orchestrates domain operations</li>
 *   <li>Transaction boundary - typically manages transactions</li>
 *   <li>Thin - contains no business logic, just coordination</li>
 * </ul>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Implement driving port interfaces</li>
 *   <li>Load aggregates from repositories</li>
 *   <li>Invoke domain operations</li>
 *   <li>Persist changes through repositories</li>
 *   <li>Publish domain events</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ApplicationService service = ApplicationService.of(
 *     TypeId.of("com.example.OrderApplicationService"),
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
public record ApplicationService(TypeId id, TypeStructure structure, ClassificationTrace classification)
        implements ApplicationType {

    /**
     * Creates a new ApplicationService.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @throws NullPointerException if any argument is null
     */
    public ApplicationService {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.APPLICATION_SERVICE;
    }

    /**
     * Creates an ApplicationService with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new ApplicationService
     * @throws NullPointerException if any argument is null
     */
    public static ApplicationService of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new ApplicationService(id, structure, classification);
    }
}
