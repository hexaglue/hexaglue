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

package io.hexaglue.plugin.audit.config;

import io.hexaglue.plugin.audit.domain.model.Constraint;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry of available constraints and their validators.
 *
 * <p>This class implements the Registry pattern, providing a centralized location
 * for:
 * <ul>
 *   <li>Registering constraint validators</li>
 *   <li>Discovering available constraints</li>
 *   <li>Retrieving validators by constraint ID</li>
 *   <li>Building validator maps for the constraint engine</li>
 * </ul>
 *
 * <p>The registry is mutable during initialization but should be considered
 * immutable after all constraints are registered.
 *
 * @since 1.0.0
 */
public class ConstraintRegistry {

    private final Map<ConstraintId, ConstraintValidator> validators = new HashMap<>();
    private final List<Constraint> availableConstraints = new ArrayList<>();

    /**
     * Registers a constraint validator.
     *
     * <p>The constraint ID from the validator must match the constraint's ID.
     *
     * @param validator  the validator implementation
     * @param constraint the constraint metadata
     * @throws IllegalArgumentException if IDs don't match
     */
    public void register(ConstraintValidator validator, Constraint constraint) {
        Objects.requireNonNull(validator, "validator required");
        Objects.requireNonNull(constraint, "constraint required");

        if (!validator.constraintId().equals(constraint.id())) {
            throw new IllegalArgumentException("Validator constraint ID (%s) doesn't match constraint ID (%s)"
                    .formatted(validator.constraintId(), constraint.id()));
        }

        validators.put(validator.constraintId(), validator);
        availableConstraints.add(constraint);
    }

    /**
     * Retrieves a validator for the given constraint.
     *
     * @param id the constraint ID
     * @return the validator, or empty if not registered
     */
    public Optional<ConstraintValidator> getValidator(ConstraintId id) {
        return Optional.ofNullable(validators.get(id));
    }

    /**
     * Returns all available constraints.
     *
     * @return immutable copy of the constraint list
     */
    public List<Constraint> availableConstraints() {
        return List.copyOf(availableConstraints);
    }

    /**
     * Returns a map of all validators (for use with ConstraintEngine).
     *
     * @return immutable copy of the validator map
     */
    public Map<ConstraintId, ConstraintValidator> allValidators() {
        return Map.copyOf(validators);
    }

    /**
     * Returns the number of registered constraints.
     *
     * @return the constraint count
     */
    public int size() {
        return validators.size();
    }

    /**
     * Checks if a constraint is registered.
     *
     * @param id the constraint ID
     * @return true if registered
     */
    public boolean isRegistered(ConstraintId id) {
        return validators.containsKey(id);
    }

    /**
     * Creates a registry with default constraints.
     *
     * @return a new registry with defaults registered
     * @see DefaultConstraints
     */
    public static ConstraintRegistry withDefaults() {
        ConstraintRegistry registry = new ConstraintRegistry();
        DefaultConstraints.registerAll(registry);
        return registry;
    }
}
