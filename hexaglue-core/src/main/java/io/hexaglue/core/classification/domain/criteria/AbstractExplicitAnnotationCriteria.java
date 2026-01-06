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
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;

/**
 * Base class for criteria that match explicit jMolecules annotations.
 *
 * <p>All explicit annotation criteria have priority 100 (highest)
 * and confidence EXPLICIT.
 */
public abstract class AbstractExplicitAnnotationCriteria
        implements ClassificationCriteria<DomainKind>, IdentifiedCriteria {

    private final String annotationSimpleName;
    private final String annotationQualifiedName;
    private final DomainKind targetKind;

    protected AbstractExplicitAnnotationCriteria(
            String annotationSimpleName, String annotationQualifiedName, DomainKind targetKind) {
        this.annotationSimpleName = annotationSimpleName;
        this.annotationQualifiedName = annotationQualifiedName;
        this.targetKind = targetKind;
    }

    @Override
    public String id() {
        return "domain.explicit." + toCamelCase(targetKind.name());
    }

    @Override
    public String name() {
        return "explicit-" + targetKind.name().toLowerCase().replace('_', '-');
    }

    private static String toCamelCase(String enumName) {
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(Character.toUpperCase(parts[i].charAt(0)));
            result.append(parts[i].substring(1));
        }
        return result.toString();
    }

    @Override
    public int priority() {
        return 100; // Highest priority for explicit annotations
    }

    @Override
    public DomainKind targetKind() {
        return targetKind;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        for (AnnotationRef annotation : node.annotations()) {
            if (matchesAnnotation(annotation)) {
                return MatchResult.explicitAnnotation(annotationSimpleName, node.id());
            }
        }
        return MatchResult.noMatch();
    }

    private boolean matchesAnnotation(AnnotationRef annotation) {
        // Match by qualified name or simple name (for flexibility)
        return annotation.qualifiedName().equals(annotationQualifiedName)
                || annotation.simpleName().equals(annotationSimpleName);
    }

    protected String annotationSimpleName() {
        return annotationSimpleName;
    }

    protected String annotationQualifiedName() {
        return annotationQualifiedName;
    }
}
