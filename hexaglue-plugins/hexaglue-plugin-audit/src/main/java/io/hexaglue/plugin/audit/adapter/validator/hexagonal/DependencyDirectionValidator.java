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

package io.hexaglue.plugin.audit.adapter.validator.hexagonal;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.DependencyEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.SourceLocation;
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

    /**
     * Validates the dependency direction constraint using v5 ArchType API.
     *
     * <p>Checks that domain and application layer types do not depend on
     * infrastructure layer types. Uses {@code model.typeRegistry()} to access
     * all classified types and {@code ArchKind} to determine layer membership.
     *
     * @param model the architectural model containing v5 type registry
     * @param codebase the codebase for dependency graph access
     * @param query architecture query (not used in this validator)
     * @return list of violations found
     * @since 5.0.0
     */
    @Override
    public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        if (model.typeRegistry().isEmpty()) {
            // No v5 model available - cannot validate
            return violations;
        }

        var registry = model.typeRegistry().get();

        // Get all types and classify by layer
        List<ArchType> domainTypes =
                registry.all(ArchType.class).filter(t -> t.kind().isDomain()).toList();

        List<ArchType> applicationTypes = registry.all(ArchType.class)
                .filter(t -> t.kind().isApplication())
                .toList();

        List<ArchType> infrastructureTypes = registry.all(ArchType.class)
                .filter(t -> !t.kind().isDomain()
                        && !t.kind().isApplication()
                        && !t.kind().isPort()
                        && t.kind() != ArchKind.UNCLASSIFIED)
                .toList();

        // Create a quick lookup for infrastructure types
        Set<String> infraQualifiedNames = infrastructureTypes.stream()
                .map(t -> t.id().qualifiedName())
                .collect(java.util.stream.Collectors.toSet());

        // Check domain layer dependencies
        violations.addAll(validateLayerDependencies(codebase, domainTypes, infraQualifiedNames, "DOMAIN"));

        // Check application layer dependencies
        violations.addAll(validateLayerDependencies(codebase, applicationTypes, infraQualifiedNames, "APPLICATION"));

        return violations;
    }

    /**
     * Validates that a specific layer doesn't depend on infrastructure.
     *
     * @param codebase the codebase to analyze
     * @param layerTypes the types in the layer to check
     * @param infraQualifiedNames the qualified names of infrastructure types
     * @param layerName the name of the layer being checked (for error messages)
     * @return list of violations found
     */
    private List<Violation> validateLayerDependencies(
            Codebase codebase, List<ArchType> layerTypes, Set<String> infraQualifiedNames, String layerName) {
        List<Violation> violations = new ArrayList<>();

        for (ArchType type : layerTypes) {
            String qualifiedName = type.id().qualifiedName();
            Set<String> deps = codebase.dependencies().getOrDefault(qualifiedName, Set.of());

            for (String depName : deps) {
                if (infraQualifiedNames.contains(depName)) {
                    violations.add(Violation.builder(CONSTRAINT_ID)
                            .severity(Severity.BLOCKER)
                            .message("%s type '%s' depends on Infrastructure type '%s'"
                                    .formatted(layerName, type.id().simpleName(), extractSimpleName(depName)))
                            .affectedType(qualifiedName)
                            .location(SourceLocation.of(qualifiedName, 1, 1))
                            .evidence(DependencyEvidence.of(
                                    "Illegal dependency from %s to Infrastructure".formatted(layerName),
                                    qualifiedName,
                                    depName))
                            .build());
                }
            }
        }

        return violations;
    }

    /**
     * Extracts simple name from qualified name.
     *
     * @param qualifiedName the qualified name
     * @return the simple name
     */
    private String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }
}
