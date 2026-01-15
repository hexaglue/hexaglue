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
 * Criterion for repository interfaces as driven ports.
 *
 * <p>Matches interfaces that:</p>
 * <ul>
 *   <li>Have @Repository annotation (HexaGlue, Spring, or JMolecules)</li>
 *   <li>Name ends with "Repository"</li>
 * </ul>
 *
 * <p>Repositories are driven ports because they are called by the domain
 * to persist or retrieve aggregates.</p>
 *
 * @since 4.0.0
 */
public final class RepositoryInterfaceCriterion implements ClassificationCriterion {

    private static final int EXPLICIT_PRIORITY = 100;
    private static final int NAMING_PRIORITY = 75;
    private static final Set<String> REPOSITORY_ANNOTATIONS = Set.of("Repository");

    @Override
    public String name() {
        return "repository-interface";
    }

    @Override
    public int priority() {
        return EXPLICIT_PRIORITY;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.DRIVEN_PORT;
    }

    @Override
    public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
        // Check for @Repository annotation
        Optional<CriterionMatch> annotationMatch = type.annotations().stream()
                .filter(ann -> REPOSITORY_ANNOTATIONS.contains(ann.simpleName()))
                .findFirst()
                .map(ann -> {
                    Evidence evidence =
                            Evidence.at(EvidenceType.ANNOTATION, "@Repository annotation found", type.sourceLocation());
                    return CriterionMatch.high("Interface has @Repository annotation", evidence);
                });

        if (annotationMatch.isPresent()) {
            return annotationMatch;
        }

        // Check naming convention
        if (type.simpleName().endsWith("Repository")) {
            Evidence evidence =
                    Evidence.at(EvidenceType.NAMING, "Interface name ends with 'Repository'", type.sourceLocation());
            return Optional.of(CriterionMatch.medium("Interface name suggests repository pattern", evidence));
        }

        return Optional.empty();
    }
}
