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
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the Dependency Inversion Principle in hexagonal architecture.
 *
 * <p>Dependency Inversion Principle (SOLID): High-level modules should not depend on
 * low-level modules. Both should depend on abstractions (interfaces). Abstractions should
 * not depend on details. Details should depend on abstractions.
 *
 * <p>This validator ensures that:
 * <ul>
 *   <li>Application layer components depend only on abstractions (interfaces) in infrastructure</li>
 *   <li>No direct dependencies on concrete infrastructure implementations</li>
 *   <li>Domain layer isolation is maintained (already enforced by DependencyDirectionValidator)</li>
 * </ul>
 *
 * <p><strong>Constraint:</strong> hexagonal:dependency-inversion<br>
 * <strong>Severity:</strong> CRITICAL<br>
 * <strong>Rationale:</strong> Depending on concrete infrastructure classes violates the
 * Dependency Inversion Principle, creates tight coupling, and makes the application
 * difficult to test and modify. All infrastructure dependencies should be through interfaces.
 *
 * @since 1.0.0
 */
public class DependencyInversionValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("hexagonal:dependency-inversion");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates the dependency inversion constraint using v5 ArchType API.
     *
     * <p>Checks that application layer types only depend on port interfaces
     * (abstractions) when referencing infrastructure layer types. Uses
     * {@code model.typeRegistry()} and {@code model.portIndex()} for type access.
     *
     * @param model the architectural model containing v5 type registry and port index
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

        // Get application layer types
        List<ArchType> applicationTypes =
                registry.all(ArchType.class).filter(t -> t.kind().isApplication()).toList();

        // Get all ports (interfaces that application should depend on)
        Set<String> portQualifiedNames = Set.of();
        if (model.portIndex().isPresent()) {
            var portIndex = model.portIndex().get();
            portQualifiedNames = portIndex.drivingPorts()
                    .map(p -> p.id().qualifiedName())
                    .collect(Collectors.toSet());
            Set<String> drivenPortNames = portIndex.drivenPorts()
                    .map(p -> p.id().qualifiedName())
                    .collect(Collectors.toSet());
            portQualifiedNames = new java.util.HashSet<>(portQualifiedNames);
            portQualifiedNames.addAll(drivenPortNames);
        }

        // Create type lookup map (qualified name -> ArchType)
        Map<String, ArchType> typeMap = registry.all(ArchType.class)
                .collect(Collectors.toMap(t -> t.id().qualifiedName(), t -> t));

        // Check each application layer type
        for (ArchType appType : applicationTypes) {
            violations.addAll(validateDependencyInversion(codebase, appType, portQualifiedNames, typeMap));
        }

        return violations;
    }

    /**
     * Validates that a specific application unit only depends on abstractions
     * when referencing infrastructure.
     *
     * @param codebase the codebase to analyze
     * @param appType the application layer type to check
     * @param portQualifiedNames the qualified names of all port interfaces
     * @param typeMap the map of qualified names to ArchTypes
     * @return list of violations found
     */
    private List<Violation> validateDependencyInversion(
            Codebase codebase, ArchType appType, Set<String> portQualifiedNames, Map<String, ArchType> typeMap) {
        List<Violation> violations = new ArrayList<>();

        String qualifiedName = appType.id().qualifiedName();
        Set<String> deps = codebase.dependencies().getOrDefault(qualifiedName, Set.of());

        for (String depName : deps) {
            // Check if dependency is to a type we know about
            ArchType depType = typeMap.get(depName);
            if (depType == null) {
                continue; // External dependency, not our concern
            }

            // Skip if dependency is a port (abstraction)
            if (portQualifiedNames.contains(depName)) {
                continue;
            }

            // Check if dependency is to infrastructure layer
            // Infrastructure is anything that's not domain, not application, not port, and not unclassified
            boolean isInfrastructure = !depType.kind().isDomain()
                    && !depType.kind().isApplication()
                    && !depType.kind().isPort()
                    && depType.kind() != ArchKind.UNCLASSIFIED;

            if (isInfrastructure) {
                // Check if dependency is to a concrete class (not an interface)
                if (!depType.structure().isInterface()) {
                    violations.add(Violation.builder(CONSTRAINT_ID)
                            .severity(Severity.CRITICAL)
                            .message(("Application type '%s' depends on concrete Infrastructure type '%s' "
                                            + "(should depend on abstraction/interface)")
                                    .formatted(appType.id().simpleName(), depType.id().simpleName()))
                            .affectedType(qualifiedName)
                            .location(SourceLocation.of(qualifiedName, 1, 1))
                            .evidence(DependencyEvidence.of(
                                    ("Dependency Inversion Principle violated: depends on concrete %s instead of interface")
                                            .formatted(depType.structure().nature().name()),
                                    qualifiedName,
                                    depName))
                            .build());
                }
            }
        }

        return violations;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }
}
