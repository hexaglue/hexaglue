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

package io.hexaglue.arch.builder.criteria;

import io.hexaglue.arch.ElementKind;
import java.util.Set;

/**
 * Criterion for explicit @DrivingPort or @PrimaryPort annotation.
 *
 * <p>Matches interfaces annotated with:</p>
 * <ul>
 *   <li>{@code @DrivingPort} (HexaGlue)</li>
 *   <li>{@code @PrimaryPort} (alternative naming)</li>
 *   <li>{@code @InboundPort} (alternative naming)</li>
 * </ul>
 *
 * @since 4.0.0
 */
public final class ExplicitDrivingPortCriterion extends ExplicitAnnotationCriterion {

    private static final Set<String> DRIVING_PORT_ANNOTATIONS =
            Set.of("DrivingPort", "PrimaryPort", "InboundPort");

    /**
     * Creates a new ExplicitDrivingPortCriterion.
     */
    public ExplicitDrivingPortCriterion() {
        super(
                "explicit-driving-port",
                ElementKind.DRIVING_PORT,
                DRIVING_PORT_ANNOTATIONS,
                "Interface has @DrivingPort annotation");
    }
}
