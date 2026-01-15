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
 * Criterion for use case interfaces as driving ports.
 *
 * <p>Matches interfaces that:</p>
 * <ul>
 *   <li>Have @UseCase annotation</li>
 *   <li>Name ends with "UseCase" or "Command" or "Query"</li>
 * </ul>
 *
 * <p>Use cases represent driving ports - the entry points to the application
 * from the outside world.</p>
 *
 * @since 4.0.0
 */
public final class UseCaseInterfaceCriterion implements ClassificationCriterion {

    private static final int EXPLICIT_PRIORITY = 100;
    private static final Set<String> USE_CASE_ANNOTATIONS = Set.of("UseCase", "Command", "Query");
    private static final Set<String> USE_CASE_SUFFIXES = Set.of("UseCase", "Command", "Query");

    @Override
    public String name() {
        return "use-case-interface";
    }

    @Override
    public int priority() {
        return EXPLICIT_PRIORITY;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.DRIVING_PORT;
    }

    @Override
    public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
        // Check for @UseCase/@Command/@Query annotation
        Optional<CriterionMatch> annotationMatch = type.annotations().stream()
                .filter(ann -> USE_CASE_ANNOTATIONS.contains(ann.simpleName()))
                .findFirst()
                .map(ann -> {
                    Evidence evidence = Evidence.at(
                            EvidenceType.ANNOTATION,
                            "@" + ann.simpleName() + " annotation found",
                            type.sourceLocation());
                    return CriterionMatch.high("Interface has @" + ann.simpleName() + " annotation", evidence);
                });

        if (annotationMatch.isPresent()) {
            return annotationMatch;
        }

        // Check naming convention
        for (String suffix : USE_CASE_SUFFIXES) {
            if (type.simpleName().endsWith(suffix)) {
                Evidence evidence = Evidence.at(
                        EvidenceType.NAMING, "Interface name ends with '" + suffix + "'", type.sourceLocation());
                return Optional.of(CriterionMatch.medium("Interface name suggests use case pattern", evidence));
            }
        }

        return Optional.empty();
    }
}
