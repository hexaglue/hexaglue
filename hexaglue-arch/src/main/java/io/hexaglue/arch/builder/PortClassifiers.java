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

import io.hexaglue.arch.builder.criteria.ExplicitDrivenPortCriterion;
import io.hexaglue.arch.builder.criteria.ExplicitDrivingPortCriterion;
import io.hexaglue.arch.builder.criteria.RepositoryInterfaceCriterion;
import io.hexaglue.arch.builder.criteria.UseCaseInterfaceCriterion;
import java.util.List;

/**
 * Factory for creating PortClassifier instances with standard criteria.
 *
 * <h2>Standard Criteria</h2>
 * <p>The default configuration includes:</p>
 * <ul>
 *   <li>Explicit port annotations (priority 100)</li>
 *   <li>Repository interface criterion (priority 100/75)</li>
 *   <li>Use case interface criterion (priority 100/75)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Use default criteria
 * PortClassifier classifier = PortClassifiers.standard();
 *
 * // Only classify interfaces with explicit annotations
 * PortClassifier explicit = PortClassifiers.explicitOnly();
 * }</pre>
 *
 * @since 4.0.0
 */
public final class PortClassifiers {

    private PortClassifiers() {}

    /**
     * Returns the standard set of port classification criteria.
     *
     * @return list of standard criteria
     */
    public static List<ClassificationCriterion> standardCriteria() {
        return List.of(
                // Explicit annotations (priority 100)
                new ExplicitDrivingPortCriterion(),
                new ExplicitDrivenPortCriterion(),
                // Pattern-based criteria (priority 100 for annotations, 75 for naming)
                new RepositoryInterfaceCriterion(),
                new UseCaseInterfaceCriterion());
    }

    /**
     * Creates a PortClassifier with standard criteria.
     *
     * @return a new PortClassifier
     */
    public static PortClassifier standard() {
        return new PortClassifier(standardCriteria());
    }

    /**
     * Creates a PortClassifier with only explicit annotation criteria.
     *
     * <p>Use this when you only want to classify interfaces that have explicit
     * port annotations, without pattern-based heuristics.</p>
     *
     * @return a new PortClassifier with explicit-only criteria
     */
    public static PortClassifier explicitOnly() {
        return new PortClassifier(List.of(new ExplicitDrivingPortCriterion(), new ExplicitDrivenPortCriterion()));
    }
}
