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
 * Criterion for explicit @ValueObject annotation.
 *
 * <p>Matches types annotated with:</p>
 * <ul>
 *   <li>{@code @ValueObject} (HexaGlue or custom)</li>
 *   <li>{@code @org.jmolecules.ddd.annotation.ValueObject}</li>
 * </ul>
 *
 * @since 4.0.0
 */
public final class ExplicitValueObjectCriterion extends ExplicitAnnotationCriterion {

    private static final Set<String> VALUE_OBJECT_ANNOTATIONS = Set.of("ValueObject");

    /**
     * Creates a new ExplicitValueObjectCriterion.
     */
    public ExplicitValueObjectCriterion() {
        super(
                "explicit-value-object",
                ElementKind.VALUE_OBJECT,
                VALUE_OBJECT_ANNOTATIONS,
                "Type has @ValueObject annotation");
    }
}
