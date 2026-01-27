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

package io.hexaglue.arch.model.audit;

/**
 * Represents a layer dependency violation.
 *
 * <p>Layer violations occur when a component in an outer layer depends on
 * a component in the same layer or when dependencies flow in the wrong direction.
 *
 * <p>Example violations:
 * <ul>
 *   <li>Domain layer depending on infrastructure layer</li>
 *   <li>Application layer depending on presentation layer</li>
 *   <li>Inner hexagon depending on outer hexagon</li>
 * </ul>
 *
 * @param fromType the type causing the violation
 * @param toType the type being depended upon
 * @param fromLayer the source layer
 * @param toLayer the target layer (invalid dependency)
 * @param description human-readable description of the violation
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record LayerViolation(String fromType, String toType, String fromLayer, String toLayer, String description) {

    /**
     * Creates a layer violation with a default description.
     */
    public LayerViolation(String fromType, String toType, String fromLayer, String toLayer) {
        this(
                fromType,
                toType,
                fromLayer,
                toLayer,
                String.format("%s (%s) depends on %s (%s)", fromType, fromLayer, toType, toLayer));
    }
}
