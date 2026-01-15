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

import io.hexaglue.arch.builder.criteria.ExplicitAggregateRootCriterion;
import io.hexaglue.arch.builder.criteria.ExplicitDomainEventCriterion;
import io.hexaglue.arch.builder.criteria.ExplicitEntityCriterion;
import io.hexaglue.arch.builder.criteria.ExplicitIdentifierCriterion;
import io.hexaglue.arch.builder.criteria.ExplicitValueObjectCriterion;
import io.hexaglue.arch.builder.criteria.RecordValueObjectCriterion;
import io.hexaglue.arch.builder.criteria.RepositoryDominantTypeCriterion;
import java.util.List;

/**
 * Factory for creating DomainClassifier instances with standard criteria.
 *
 * <h2>Standard Criteria</h2>
 * <p>The default configuration includes:</p>
 * <ul>
 *   <li>Explicit annotation criteria (priority 100)</li>
 *   <li>Repository dominant type criterion (priority 80)</li>
 *   <li>Record value object criterion (priority 70)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Use default criteria
 * DomainClassifier classifier = DomainClassifiers.standard();
 *
 * // Or customize
 * DomainClassifier custom = new DomainClassifier(List.of(
 *     new ExplicitAggregateRootCriterion(),
 *     new CustomCriterion()
 * ));
 * }</pre>
 *
 * @since 4.0.0
 */
public final class DomainClassifiers {

    private DomainClassifiers() {}

    /**
     * Returns the standard set of domain classification criteria.
     *
     * @return list of standard criteria
     */
    public static List<ClassificationCriterion> standardCriteria() {
        return List.of(
                // Explicit annotations (priority 100)
                new ExplicitAggregateRootCriterion(),
                new ExplicitEntityCriterion(),
                new ExplicitValueObjectCriterion(),
                new ExplicitIdentifierCriterion(),
                new ExplicitDomainEventCriterion(),
                // Strong heuristics (priority 80)
                new RepositoryDominantTypeCriterion(),
                // Medium heuristics (priority 70)
                new RecordValueObjectCriterion());
    }

    /**
     * Creates a DomainClassifier with standard criteria.
     *
     * @return a new DomainClassifier
     */
    public static DomainClassifier standard() {
        return new DomainClassifier(standardCriteria());
    }

    /**
     * Creates a DomainClassifier with only explicit annotation criteria.
     *
     * <p>Use this when you only want to classify types that have explicit
     * DDD annotations, without heuristics.</p>
     *
     * @return a new DomainClassifier with explicit-only criteria
     */
    public static DomainClassifier explicitOnly() {
        return new DomainClassifier(List.of(
                new ExplicitAggregateRootCriterion(),
                new ExplicitEntityCriterion(),
                new ExplicitValueObjectCriterion(),
                new ExplicitIdentifierCriterion(),
                new ExplicitDomainEventCriterion()));
    }
}
