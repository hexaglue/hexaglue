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
import io.hexaglue.syntax.TypeForm;
import io.hexaglue.syntax.TypeSyntax;
import java.util.Optional;

/**
 * Criterion for Java records as value objects.
 *
 * <p>Java records are immutable by design and defined by their components,
 * making them a natural fit for value objects in DDD. However, this is a
 * medium confidence heuristic as not all records are value objects.</p>
 *
 * <p>Priority: 70 (medium heuristic)</p>
 *
 * @since 4.0.0
 */
public final class RecordValueObjectCriterion implements ClassificationCriterion {

    private static final int HEURISTIC_PRIORITY = 70;

    @Override
    public String name() {
        return "record-value-object";
    }

    @Override
    public int priority() {
        return HEURISTIC_PRIORITY;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.VALUE_OBJECT;
    }

    @Override
    public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
        if (type.form() == TypeForm.RECORD) {
            Evidence evidence = Evidence.at(
                    EvidenceType.STRUCTURE, "Type is a Java record (immutable, value-based)", type.sourceLocation());
            return Optional.of(CriterionMatch.medium(
                    "Java records are typically value objects (immutable, defined by components)", evidence));
        }
        return Optional.empty();
    }
}
