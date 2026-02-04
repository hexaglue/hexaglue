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
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.arch.model.graph.RelationType;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.DependencyEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates that port direction (DRIVING/DRIVEN) is consistent with actual usage.
 *
 * <p>Hexagonal Architecture Principle: Ports should be used according to their direction:
 * <ul>
 *   <li><b>DRIVING ports</b> (inbound): Implemented by application services, called by adapters</li>
 *   <li><b>DRIVEN ports</b> (outbound): Used/called by application services, implemented by adapters</li>
 * </ul>
 *
 * <p>This validator uses the v5 PortIndex to access classified driving and driven ports
 * directly from the architectural model, ensuring consistency with the classification
 * performed by the core engine.
 *
 * <p><strong>Constraint:</strong> hexagonal:port-direction<br>
 * <strong>Severity:</strong> MAJOR<br>
 * <strong>Rationale:</strong> Incorrect port direction usage violates hexagonal architecture
 * principles and leads to inverted dependencies.
 *
 * @since 1.0.0
 */
public class PortDirectionValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("hexagonal:port-direction");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates the port direction constraint using v5 ArchType API.
     *
     * <p>Checks that driven ports are used by application services and that
     * driving ports are implemented by application services. Uses
     * {@code model.portIndex()} for direct port access.
     *
     * @param model the architectural model containing v5 port index
     * @param codebase the codebase for dependency graph access
     * @param query architecture query (not used in this validator)
     * @return list of violations found
     * @since 5.0.0
     */
    @Override
    public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        if (model.portIndex().isEmpty() || model.typeRegistry().isEmpty()) {
            // No v5 model available - cannot validate
            return violations;
        }

        var portIndex = model.portIndex().get();
        var registry = model.typeRegistry().get();

        // Get all application service qualified names
        Set<String> applicationServiceQNames = registry.all(ArchType.class)
                .filter(t -> t.kind().isApplication())
                .map(t -> t.id().qualifiedName())
                .collect(Collectors.toSet());

        // When no application services are in the registry AND no other discovery
        // mechanism is available, port direction validation cannot produce meaningful results
        boolean hasCompositionIndex = model.compositionIndex().isPresent();
        if (applicationServiceQNames.isEmpty() && query == null && !hasCompositionIndex) {
            return violations;
        }

        // Validate driven ports (should be used by application services)
        portIndex.drivenPorts().forEach(drivenPort -> {
            violations.addAll(validateDrivenPort(drivenPort, applicationServiceQNames, codebase, model, query));
        });

        // Validate driving ports (should be referenced by application services)
        portIndex.drivingPorts().forEach(drivingPort -> {
            violations.addAll(validateDrivingPort(drivingPort, applicationServiceQNames, codebase, model, query));
        });

        return violations;
    }

    /**
     * Validates that a DRIVEN port is used by at least one application service.
     *
     * <p>Uses three strategies to detect usage:
     * <ol>
     *   <li>Codebase dependency graph (application service depends on port)</li>
     *   <li>CompositionIndex IMPLEMENTS relationships (application service implements port)</li>
     *   <li>ArchitectureQuery findImplementors (covers excluded packages)</li>
     * </ol>
     *
     * @param port the driven port to validate
     * @param applicationServiceQNames the qualified names of application services
     * @param codebase the codebase
     * @param model the architectural model for CompositionIndex access
     * @param query the architecture query for full graph access (may be null)
     * @return list of violations (empty if valid)
     * @since 5.0.0
     */
    private List<Violation> validateDrivenPort(
            DrivenPort port,
            Set<String> applicationServiceQNames,
            Codebase codebase,
            ArchitecturalModel model,
            ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        String portQualifiedName = port.id().qualifiedName();

        if (isUsedOrImplemented(port.id(), portQualifiedName, applicationServiceQNames, codebase, model, query)) {
            return violations;
        }

        violations.add(Violation.builder(CONSTRAINT_ID)
                .severity(Severity.MAJOR)
                .message("DRIVEN port '%s' is not used by any application service"
                        .formatted(port.id().simpleName()))
                .affectedType(portQualifiedName)
                .location(SourceLocation.of(portQualifiedName, 1, 1))
                .evidence(DependencyEvidence.of(
                        "DRIVEN ports should be called by application services to interact with external systems",
                        portQualifiedName,
                        "APPLICATION_SERVICES"))
                .build());

        return violations;
    }

    /**
     * Validates that a DRIVING port is implemented by or referenced by at least one application service.
     *
     * <p>Uses three strategies to detect implementation:
     * <ol>
     *   <li>Codebase dependency graph (application service depends on port)</li>
     *   <li>CompositionIndex IMPLEMENTS relationships (application service implements port)</li>
     *   <li>ArchitectureQuery findImplementors (covers excluded packages)</li>
     * </ol>
     *
     * @param port the driving port to validate
     * @param applicationServiceQNames the qualified names of application services
     * @param codebase the codebase
     * @param model the architectural model for CompositionIndex access
     * @param query the architecture query for full graph access (may be null)
     * @return list of violations (empty if valid)
     * @since 5.0.0
     */
    private List<Violation> validateDrivingPort(
            DrivingPort port,
            Set<String> applicationServiceQNames,
            Codebase codebase,
            ArchitecturalModel model,
            ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        String portQualifiedName = port.id().qualifiedName();

        // Also check if the port itself is an application service
        boolean isApplicationService = applicationServiceQNames.contains(portQualifiedName);

        if (isApplicationService
                || isUsedOrImplemented(
                        port.id(), portQualifiedName, applicationServiceQNames, codebase, model, query)) {
            return violations;
        }

        violations.add(Violation.builder(CONSTRAINT_ID)
                .severity(Severity.MAJOR)
                .message("DRIVING port '%s' is not implemented by any application service"
                        .formatted(port.id().simpleName()))
                .affectedType(portQualifiedName)
                .location(SourceLocation.of(portQualifiedName, 1, 1))
                .evidence(DependencyEvidence.of(
                        "DRIVING ports should be implemented by application services to provide use cases",
                        portQualifiedName,
                        "APPLICATION_SERVICES"))
                .build());

        return violations;
    }

    /**
     * Checks if a port is used or implemented using three strategies.
     *
     * <p>Strategy 1: Codebase dependency graph — checks if any application service
     * depends on the port.
     * <p>Strategy 2: CompositionIndex — checks for IMPLEMENTS relationships
     * targeting the port (covers types classified as OUT_OF_SCOPE or generated adapters).
     * <p>Strategy 3: ArchitectureQuery — uses the full application graph to find
     * implementors (covers adapters in excluded packages).
     *
     * @param portId the port's type identifier
     * @param portQualifiedName the port's qualified name
     * @param applicationServiceQNames the qualified names of application services
     * @param codebase the codebase containing dependency information
     * @param model the architectural model for CompositionIndex access
     * @param query the architecture query for full graph access (may be null)
     * @return true if the port is used or implemented by at least one type
     * @since 5.0.0
     */
    private boolean isUsedOrImplemented(
            TypeId portId,
            String portQualifiedName,
            Set<String> applicationServiceQNames,
            Codebase codebase,
            ArchitecturalModel model,
            ArchitectureQuery query) {

        // Strategy 1: Check codebase dependencies
        boolean foundViaDependencies = applicationServiceQNames.stream().anyMatch(appServiceQName -> {
            Set<String> deps = codebase.dependencies().getOrDefault(appServiceQName, Set.of());
            return deps.contains(portQualifiedName);
        });
        if (foundViaDependencies) {
            return true;
        }

        // Strategy 2: Check CompositionIndex for IMPLEMENTS relationships
        boolean foundViaCompositionIndex = model.compositionIndex()
                .map(ci -> ci.graph().to(portId).anyMatch(r -> r.type() == RelationType.IMPLEMENTS))
                .orElse(false);
        if (foundViaCompositionIndex) {
            return true;
        }

        // Strategy 3: Check full ApplicationGraph via ArchitectureQuery (handles excluded packages)
        if (query != null) {
            return !query.findImplementors(portQualifiedName).isEmpty();
        }

        return false;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
