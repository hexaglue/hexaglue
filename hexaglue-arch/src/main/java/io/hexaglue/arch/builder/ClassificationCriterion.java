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

package io.hexaglue.arch.builder;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.syntax.TypeSyntax;
import java.util.Optional;

/**
 * A classification criterion that can be applied to a type.
 *
 * <p>Each criterion evaluates a type and returns a match result if
 * the criterion's conditions are met. Criteria have a priority that
 * determines which one wins when multiple criteria match.</p>
 *
 * <h2>Priority Guidelines</h2>
 * <ul>
 *   <li>100 - Explicit annotations (e.g., @AggregateRoot)</li>
 *   <li>90 - Explicit interfaces (e.g., implements AggregateRoot)</li>
 *   <li>80 - Strong heuristics (e.g., repository primary type)</li>
 *   <li>70 - Medium heuristics (e.g., record with single ID field)</li>
 *   <li>60 - Weak heuristics (e.g., naming conventions)</li>
 * </ul>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class ExplicitAggregateRootCriterion implements ClassificationCriterion {
 *     @Override
 *     public String name() {
 *         return "explicit-aggregate-root";
 *     }
 *
 *     @Override
 *     public int priority() {
 *         return 100;
 *     }
 *
 *     @Override
 *     public ElementKind targetKind() {
 *         return ElementKind.AGGREGATE_ROOT;
 *     }
 *
 *     @Override
 *     public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
 *         return type.annotations().stream()
 *             .filter(a -> a.qualifiedName().endsWith("AggregateRoot"))
 *             .findFirst()
 *             .map(a -> CriterionMatch.of(
 *                 "Type has @AggregateRoot annotation",
 *                 ConfidenceLevel.HIGH,
 *                 Evidence.of(EvidenceType.ANNOTATION, "@AggregateRoot annotation found")
 *             ));
 *     }
 * }
 * }</pre>
 *
 * @since 4.0.0
 */
public interface ClassificationCriterion {

    /**
     * Returns the unique name of this criterion.
     *
     * <p>Used for traceability and debugging.</p>
     *
     * @return the criterion name (e.g., "explicit-aggregate-root")
     */
    String name();

    /**
     * Returns the priority of this criterion.
     *
     * <p>Higher values indicate higher priority. When multiple criteria
     * match, the one with the highest priority wins.</p>
     *
     * @return the priority value (typically 60-100)
     */
    int priority();

    /**
     * Returns the element kind this criterion suggests when matched.
     *
     * @return the target element kind
     */
    ElementKind targetKind();

    /**
     * Evaluates this criterion against a type.
     *
     * @param type the type to evaluate
     * @param context the classification context providing additional information
     * @return a match result if the criterion matched, empty otherwise
     */
    Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context);
}
