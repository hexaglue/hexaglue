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
import io.hexaglue.arch.Evidence;
import io.hexaglue.arch.EvidenceType;
import io.hexaglue.arch.builder.ClassificationContext;
import io.hexaglue.arch.builder.ClassificationCriterion;
import io.hexaglue.arch.builder.CriterionMatch;
import io.hexaglue.syntax.TypeSyntax;
import java.util.Optional;
import java.util.Set;

/**
 * Criterion for explicit @Entity annotation (DDD Entity).
 *
 * <p>Matches types annotated with:</p>
 * <ul>
 *   <li>{@code @io.hexaglue.ddd.Entity} (HexaGlue DDD annotation)</li>
 *   <li>{@code @org.jmolecules.ddd.annotation.Entity}</li>
 * </ul>
 *
 * <p><b>Note:</b> This does NOT match JPA's {@code @jakarta.persistence.Entity}
 * as that indicates a persistence mapping, not a DDD entity classification.</p>
 *
 * @since 4.0.0
 */
public final class ExplicitEntityCriterion implements ClassificationCriterion {

    private static final int EXPLICIT_PRIORITY = 100;
    private static final Set<String> DDD_ENTITY_ANNOTATIONS =
            Set.of("io.hexaglue.ddd.Entity", "org.jmolecules.ddd.annotation.Entity");

    @Override
    public String name() {
        return "explicit-entity";
    }

    @Override
    public int priority() {
        return EXPLICIT_PRIORITY;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.ENTITY;
    }

    @Override
    public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
        return type.annotations().stream()
                .filter(ann -> isDddEntityAnnotation(ann.qualifiedName()))
                .findFirst()
                .map(ann -> {
                    Evidence evidence = Evidence.at(
                            EvidenceType.ANNOTATION, "@Entity (DDD) annotation found", type.sourceLocation());
                    return CriterionMatch.high("Type has @Entity (DDD) annotation", evidence);
                });
    }

    private boolean isDddEntityAnnotation(String qualifiedName) {
        return DDD_ENTITY_ANNOTATIONS.contains(qualifiedName);
    }
}
