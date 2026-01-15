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
 * Criterion for explicit @Identifier annotation.
 *
 * <p>Matches types annotated with:</p>
 * <ul>
 *   <li>{@code @Identifier} (HexaGlue or custom)</li>
 *   <li>{@code @org.jmolecules.ddd.annotation.Identifier}</li>
 * </ul>
 *
 * @since 4.0.0
 */
public final class ExplicitIdentifierCriterion extends ExplicitAnnotationCriterion {

    private static final Set<String> IDENTIFIER_ANNOTATIONS = Set.of("Identifier");

    /**
     * Creates a new ExplicitIdentifierCriterion.
     */
    public ExplicitIdentifierCriterion() {
        super(
                "explicit-identifier",
                ElementKind.IDENTIFIER,
                IDENTIFIER_ANNOTATIONS,
                "Type has @Identifier annotation");
    }
}
