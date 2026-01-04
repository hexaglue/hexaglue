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

package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Matches types that are explicitly marked as identifiers.
 *
 * <p>Detection methods (in order of preference):
 * <ul>
 *   <li>Implements {@code org.jmolecules.ddd.types.Identifier}</li>
 *   <li>Annotated with custom @Identifier annotation</li>
 * </ul>
 *
 * <p>Priority: 100 (explicit declaration)
 * <p>Confidence: EXPLICIT
 */
public final class ExplicitIdentifierCriteria implements ClassificationCriteria<DomainKind> {

    public static final String INTERFACE_QUALIFIED_NAME = "org.jmolecules.ddd.types.Identifier";
    public static final String ANNOTATION_SIMPLE_NAME = "Identifier";

    @Override
    public String name() {
        return "explicit-identifier";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.IDENTIFIER;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Check if implements Identifier interface
        boolean implementsIdentifier = node.interfaces().stream()
                .anyMatch(iface -> iface.rawQualifiedName().equals(INTERFACE_QUALIFIED_NAME)
                        || iface.simpleName().equals("Identifier"));

        if (implementsIdentifier) {
            return MatchResult.match(
                    ConfidenceLevel.EXPLICIT,
                    "Implements Identifier interface",
                    List.of(Evidence.fromStructure("Implements " + INTERFACE_QUALIFIED_NAME, List.of(node.id()))));
        }

        // Check for @Identifier annotation
        boolean hasAnnotation =
                node.annotations().stream().anyMatch(ann -> ann.simpleName().equals(ANNOTATION_SIMPLE_NAME));

        if (hasAnnotation) {
            return MatchResult.match(
                    ConfidenceLevel.EXPLICIT,
                    "@Identifier annotation present",
                    List.of(Evidence.fromAnnotation(ANNOTATION_SIMPLE_NAME, node.id())));
        }

        return MatchResult.noMatch();
    }
}
