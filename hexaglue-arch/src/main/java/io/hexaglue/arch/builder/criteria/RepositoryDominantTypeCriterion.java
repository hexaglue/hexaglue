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

/**
 * Criterion for types used as the primary type in a repository interface.
 *
 * <p>If a type is the dominant type in a repository (e.g., {@code OrderRepository<Order, OrderId>}),
 * it is likely an aggregate root, as repositories are typically used for aggregate roots in DDD.</p>
 *
 * <p>Priority: 80 (strong heuristic, but not explicit annotation)</p>
 *
 * @since 4.0.0
 */
public final class RepositoryDominantTypeCriterion implements ClassificationCriterion {

    private static final int HEURISTIC_PRIORITY = 80;

    @Override
    public String name() {
        return "repository-dominant-type";
    }

    @Override
    public int priority() {
        return HEURISTIC_PRIORITY;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.AGGREGATE_ROOT;
    }

    @Override
    public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
        if (context.isRepositoryDominantType(type.qualifiedName())) {
            Evidence evidence =
                    Evidence.of(EvidenceType.RELATIONSHIP, "Type is the primary type in a repository interface");
            return Optional.of(CriterionMatch.medium(
                    "Type is used as primary type in a repository (indicates aggregate root)", evidence));
        }
        return Optional.empty();
    }
}
