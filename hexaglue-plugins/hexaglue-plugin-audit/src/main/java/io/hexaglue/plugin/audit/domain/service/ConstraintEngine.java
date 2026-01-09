/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.domain.service;

import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Domain service for executing constraint validators.
 *
 * <p>This service is responsible for:
 * <ul>
 *   <li>Managing the registry of available validators</li>
 *   <li>Executing enabled validators</li>
 *   <li>Collecting violations from all validators</li>
 *   <li>Handling validator execution errors gracefully</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ConstraintEngine {

    private final Map<ConstraintId, ConstraintValidator> validators;

    /**
     * Creates a new constraint engine with the given validators.
     *
     * @param validators the map of validators (constraint ID -> validator)
     */
    public ConstraintEngine(Map<ConstraintId, ConstraintValidator> validators) {
        this.validators = Map.copyOf(Objects.requireNonNull(validators, "validators required"));
    }

    /**
     * Executes the specified constraints against the codebase.
     *
     * <p>This method iterates through the enabled constraints, executes their
     * validators, and collects all violations. If a validator throws an exception,
     * it is logged but doesn't stop execution of other validators.
     *
     * @param codebase           the codebase to validate
     * @param query              architecture query for advanced analysis (may be null)
     * @param enabledConstraints the set of constraint IDs to execute (empty = all)
     * @return list of all violations found
     */
    public List<Violation> executeConstraints(
            Codebase codebase, ArchitectureQuery query, Set<ConstraintId> enabledConstraints) {
        Objects.requireNonNull(codebase, "codebase required");
        Objects.requireNonNull(enabledConstraints, "enabledConstraints required");

        // If empty, enable all constraints
        Set<ConstraintId> toExecute =
                enabledConstraints.isEmpty() ? validators.keySet() : enabledConstraints;

        return toExecute.stream()
                .map(validators::get)
                .filter(Objects::nonNull)
                .flatMap(validator -> {
                    try {
                        return validator.validate(codebase, query).stream();
                    } catch (Exception e) {
                        // Log error but continue with other validators
                        System.err.println(
                                "Error executing validator " + validator.constraintId() + ": " + e.getMessage());
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();
    }

    /**
     * Returns the number of registered validators.
     *
     * @return the validator count
     */
    public int validatorCount() {
        return validators.size();
    }

    /**
     * Checks if a validator is registered for the given constraint.
     *
     * @param constraintId the constraint ID
     * @return true if a validator is registered
     */
    public boolean hasValidator(ConstraintId constraintId) {
        return validators.containsKey(constraintId);
    }
}
