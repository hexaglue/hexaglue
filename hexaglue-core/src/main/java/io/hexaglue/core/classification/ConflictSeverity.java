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

package io.hexaglue.core.classification;

/**
 * Severity level for classification conflicts.
 *
 * <p>Conflicts occur when multiple criteria match for different kinds.
 * The severity indicates whether the conflict represents an actual problem
 * or just an informational warning.
 *
 * <ul>
 *   <li>{@link #ERROR}: Incompatible conflict - the competing kinds cannot coexist
 *       (e.g., ENTITY vs VALUE_OBJECT)</li>
 *   <li>{@link #WARNING}: Compatible conflict - the competing kinds can coexist
 *       but one was chosen over the other (e.g., AGGREGATE_ROOT vs ENTITY)</li>
 * </ul>
 *
 * @see Conflict
 * @see io.hexaglue.core.classification.engine.CompatibilityPolicy
 */
public enum ConflictSeverity {

    /**
     * Incompatible conflict requiring attention.
     *
     * <p>The competing kinds are fundamentally incompatible and cannot coexist.
     * This indicates a potential issue with the classification criteria or
     * the code being analyzed.
     *
     * <p>Examples:
     * <ul>
     *   <li>ENTITY vs VALUE_OBJECT (identity vs no identity)</li>
     *   <li>AGGREGATE_ROOT vs VALUE_OBJECT (aggregate vs embedded)</li>
     * </ul>
     */
    ERROR,

    /**
     * Compatible conflict for informational purposes.
     *
     * <p>The competing kinds are compatible and can coexist conceptually.
     * The conflict is recorded for traceability but does not indicate
     * a problem with the classification.
     *
     * <p>Examples:
     * <ul>
     *   <li>AGGREGATE_ROOT vs ENTITY (aggregate is a specialized entity)</li>
     * </ul>
     */
    WARNING
}
