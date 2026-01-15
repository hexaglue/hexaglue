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
 * Criterion for explicit @DrivenPort or @SecondaryPort annotation.
 *
 * <p>Matches interfaces annotated with:</p>
 * <ul>
 *   <li>{@code @DrivenPort} (HexaGlue)</li>
 *   <li>{@code @SecondaryPort} (alternative naming)</li>
 *   <li>{@code @OutboundPort} (alternative naming)</li>
 * </ul>
 *
 * @since 4.0.0
 */
public final class ExplicitDrivenPortCriterion extends ExplicitAnnotationCriterion {

    private static final Set<String> DRIVEN_PORT_ANNOTATIONS = Set.of("DrivenPort", "SecondaryPort", "OutboundPort");

    /**
     * Creates a new ExplicitDrivenPortCriterion.
     */
    public ExplicitDrivenPortCriterion() {
        super(
                "explicit-driven-port",
                ElementKind.DRIVEN_PORT,
                DRIVEN_PORT_ANNOTATIONS,
                "Interface has @DrivenPort annotation");
    }
}
