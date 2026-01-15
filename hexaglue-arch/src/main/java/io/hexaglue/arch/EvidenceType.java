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

package io.hexaglue.arch;

/**
 * Types of evidence supporting a classification decision.
 *
 * <p>Used in {@link Evidence} to categorize what kind of evidence
 * was found during classification.</p>
 *
 * @since 4.0.0
 */
public enum EvidenceType {

    /**
     * Evidence from annotations.
     *
     * <p>Examples: @AggregateRoot, @Repository, @Entity</p>
     */
    ANNOTATION,

    /**
     * Evidence from naming conventions.
     *
     * <p>Examples: OrderRepository, CreateOrderUseCase, OrderId</p>
     */
    NAMING,

    /**
     * Evidence from type structure.
     *
     * <p>Examples: has identity field, is immutable record, implements interface</p>
     */
    STRUCTURE,

    /**
     * Evidence from relationships with other types.
     *
     * <p>Examples: used by CoreAppClass, implements DrivingPort, managed by repository</p>
     */
    RELATIONSHIP,

    /**
     * Evidence from package location.
     *
     * <p>Examples: in ports.in, adapters.rest, domain package</p>
     */
    PACKAGE,

    /**
     * Evidence from type behavior (methods).
     *
     * <p>Examples: has save/find methods, publishes events</p>
     */
    BEHAVIOR
}
