/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.validator.hexagonal;

import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.DependencyEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that dependencies flow in the correct direction.
 *
 * <p>Hexagonal Architecture Principle: The domain layer (business logic) must
 * not depend on infrastructure concerns. Dependencies should always flow inward
 * toward the domain core.
 *
 * <p>This validator checks that:
 * <ul>
 *   <li>Domain types do NOT depend on Infrastructure types</li>
 *   <li>Application types do NOT depend on Infrastructure types</li>
 * </ul>
 *
 * <p><strong>Constraint:</strong> hexagonal:dependency-direction<br>
 * <strong>Severity:</strong> BLOCKER<br>
 * <strong>Rationale:</strong> Allowing the domain to depend on infrastructure
 * violates the core principle of hexagonal architecture and makes the domain
 * tightly coupled to external concerns.
 *
 * @since 1.0.0
 */
public class DependencyDirectionValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("hexagonal:dependency-direction");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Check domain layer dependencies
        violations.addAll(validateLayerDependencies(codebase, LayerClassification.DOMAIN));

        // Check application layer dependencies (also shouldn't depend on infrastructure)
        violations.addAll(validateLayerDependencies(codebase, LayerClassification.APPLICATION));

        return violations;
    }

    /**
     * Validates that a specific layer doesn't depend on infrastructure.
     *
     * @param codebase the codebase to analyze
     * @param layer the layer to check
     * @return list of violations found
     */
    private List<Violation> validateLayerDependencies(Codebase codebase, LayerClassification layer) {
        List<Violation> violations = new ArrayList<>();

        for (CodeUnit unit : codebase.unitsInLayer(layer)) {
            Set<String> deps = codebase.dependencies().getOrDefault(unit.qualifiedName(), Set.of());

            for (String depName : deps) {
                // Find the dependency in the codebase
                codebase.units().stream()
                        .filter(u -> u.qualifiedName().equals(depName))
                        .filter(u -> u.layer() == LayerClassification.INFRASTRUCTURE)
                        .findFirst()
                        .ifPresent(infraUnit -> {
                            violations.add(Violation.builder(CONSTRAINT_ID)
                                    .severity(Severity.BLOCKER)
                                    .message("%s type '%s' depends on Infrastructure type '%s'"
                                            .formatted(
                                                    layer.name(),
                                                    unit.simpleName(),
                                                    infraUnit.simpleName()))
                                    .affectedType(unit.qualifiedName())
                                    .location(SourceLocation.of(unit.qualifiedName(), 1, 1))
                                    .evidence(DependencyEvidence.of(
                                            "Illegal dependency from %s to Infrastructure"
                                                    .formatted(layer.name()),
                                            unit.qualifiedName(),
                                            infraUnit.qualifiedName()))
                                    .build());
                        });
            }
        }

        return violations;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }
}
