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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Base criterion for explicit annotation-based classification.
 *
 * <p>Matches types that have specific annotations. The annotation names
 * are checked as simple names (e.g., "AggregateRoot") or qualified names
 * (e.g., "io.hexaglue.ddd.AggregateRoot").</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ClassificationCriterion criterion = new ExplicitAnnotationCriterion(
 *     "explicit-aggregate-root",
 *     ElementKind.AGGREGATE_ROOT,
 *     Set.of("AggregateRoot"),
 *     "Type has @AggregateRoot annotation"
 * );
 * }</pre>
 *
 * @since 4.0.0
 */
public class ExplicitAnnotationCriterion implements ClassificationCriterion {

    private static final int EXPLICIT_PRIORITY = 100;

    private final String criterionName;
    private final ElementKind targetKind;
    private final Set<String> annotationNames;
    private final String justification;

    /**
     * Creates a new explicit annotation criterion.
     *
     * @param criterionName the unique name for this criterion
     * @param targetKind the element kind to classify as
     * @param annotationNames the annotation simple names to match (e.g., "AggregateRoot")
     * @param justification the justification message when matched
     */
    public ExplicitAnnotationCriterion(
            String criterionName, ElementKind targetKind, Set<String> annotationNames, String justification) {
        this.criterionName = Objects.requireNonNull(criterionName, "criterionName must not be null");
        this.targetKind = Objects.requireNonNull(targetKind, "targetKind must not be null");
        this.annotationNames = Set.copyOf(Objects.requireNonNull(annotationNames, "annotationNames must not be null"));
        this.justification = Objects.requireNonNull(justification, "justification must not be null");
    }

    @Override
    public String name() {
        return criterionName;
    }

    @Override
    public int priority() {
        return EXPLICIT_PRIORITY;
    }

    @Override
    public ElementKind targetKind() {
        return targetKind;
    }

    @Override
    public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
        return type.annotations().stream()
                .filter(ann -> matchesAnnotation(ann.simpleName()) || matchesAnnotation(ann.qualifiedName()))
                .findFirst()
                .map(ann -> {
                    Evidence evidence = Evidence.at(
                            EvidenceType.ANNOTATION,
                            "@" + ann.simpleName() + " annotation found",
                            type.sourceLocation());
                    return CriterionMatch.high(justification, evidence);
                });
    }

    private boolean matchesAnnotation(String name) {
        // Check exact match
        if (annotationNames.contains(name)) {
            return true;
        }
        // Check if qualified name ends with any simple name
        for (String annotationName : annotationNames) {
            if (name.endsWith("." + annotationName) || name.endsWith(annotationName)) {
                return true;
            }
        }
        return false;
    }
}
