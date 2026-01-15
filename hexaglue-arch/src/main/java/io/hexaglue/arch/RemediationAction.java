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
 * Types of corrective actions to resolve classification ambiguity.
 *
 * <p>Used in {@link RemediationHint} to specify what action the user
 * can take to make the classification explicit.</p>
 *
 * @since 4.0.0
 */
public enum RemediationAction {

    /**
     * Add a DDD/jMolecules annotation.
     *
     * <p>Example: Add @AggregateRoot to Order class</p>
     */
    ADD_ANNOTATION,

    /**
     * Configure explicitly in hexaglue.yaml.
     *
     * <p>Example: classification.explicit.Order: AGGREGATE_ROOT</p>
     */
    CONFIGURE_EXPLICIT,

    /**
     * Rename the type to follow conventions.
     *
     * <p>Example: Rename Order to OrderAggregate</p>
     */
    RENAME,

    /**
     * Move the type to a different package.
     *
     * <p>Example: Move to domain.model package</p>
     */
    MOVE_PACKAGE,

    /**
     * Implement a marker interface.
     *
     * <p>Example: Implement AggregateRoot interface</p>
     */
    IMPLEMENT_INTERFACE,

    /**
     * Exclude from analysis.
     *
     * <p>Example: Add to exclusion list in hexaglue.yaml</p>
     */
    EXCLUDE
}
