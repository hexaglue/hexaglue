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

package io.hexaglue.plugin.audit.domain.port.driving;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.arch.model.audit.Codebase;
import java.util.List;

/**
 * Driving port for validating architectural constraints.
 *
 * <p>Each implementation validates a single constraint using the Strategy pattern.
 * This allows for:
 * <ul>
 *   <li>Easy addition of new constraints (Open/Closed Principle)</li>
 *   <li>Single Responsibility (one validator = one constraint)</li>
 *   <li>Independent testing of each constraint</li>
 *   <li>Flexible configuration (enable/disable per constraint)</li>
 * </ul>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class AggregateRepositoryValidator implements ConstraintValidator {
 *
 *     @Override
 *     public ConstraintId constraintId() {
 *         return ConstraintId.of("ddd:aggregate-repository");
 *     }
 *
 *     @Override
 *     public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
 *         // Use model.domainIndex() and model.portIndex() for v5 type access
 *         return violations;
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @since 5.0.0 Added ArchitecturalModel parameter to validate() for v5 ArchType access
 */
public interface ConstraintValidator {

    /**
     * Returns the unique identifier of the constraint validated by this validator.
     *
     * @return the constraint ID
     */
    ConstraintId constraintId();

    /**
     * Validates the constraint against the codebase using v5 ArchType API.
     *
     * <p>This method analyzes the architectural model and returns a list of violations found.
     * An empty list indicates the constraint is satisfied.
     *
     * <p>Validators should use the v5 indices for type access:
     * <ul>
     *   <li>{@code model.domainIndex()} for aggregate roots, entities, value objects, etc.</li>
     *   <li>{@code model.portIndex()} for driving and driven ports</li>
     *   <li>{@code model.typeRegistry()} for generic type access</li>
     * </ul>
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for legacy access (to be phased out)
     * @param query architecture query for advanced analysis (may be null)
     * @return list of violations (empty if constraint is satisfied)
     * @since 5.0.0
     */
    List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query);

    /**
     * Returns the default severity for violations of this constraint.
     *
     * <p>This can be overridden in configuration.
     *
     * @return the default severity (defaults to MAJOR)
     */
    default Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
