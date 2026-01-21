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

        // Validate driven ports (should be used by application services)
        portIndex.drivenPorts().forEach(drivenPort -> {
            violations.addAll(validateDrivenPort(drivenPort, applicationServiceQNames, codebase));
        });

        // Validate driving ports (should be referenced by application services)
        portIndex.drivingPorts().forEach(drivingPort -> {
            violations.addAll(validateDrivingPort(drivingPort, applicationServiceQNames, codebase));
        });

        return violations;
    }

    /**
     * Validates that a DRIVEN port is used by at least one application service.
     *
     * @param port the driven port to validate
     * @param applicationServiceQNames the qualified names of application services
     * @param codebase the codebase
     * @return list of violations (empty if valid)
     */
    private List<Violation> validateDrivenPort(
            DrivenPort port, Set<String> applicationServiceQNames, Codebase codebase) {
        List<Violation> violations = new ArrayList<>();

        String portQualifiedName = port.id().qualifiedName();

        // Check if any application service depends on this port
        boolean usedByApplicationService = applicationServiceQNames.stream().anyMatch(appServiceQName -> {
            Set<String> deps = codebase.dependencies().getOrDefault(appServiceQName, Set.of());
            return deps.contains(portQualifiedName);
        });

        if (!usedByApplicationService) {
            violations.add(Violation.builder(CONSTRAINT_ID)
                    .severity(Severity.MAJOR)
                    .message("DRIVEN port '%s' is not used by any application service".formatted(port.id()
                            .simpleName()))
                    .affectedType(portQualifiedName)
                    .location(SourceLocation.of(portQualifiedName, 1, 1))
                    .evidence(DependencyEvidence.of(
                            "DRIVEN ports should be called by application services to interact with external systems",
                            portQualifiedName,
                            "APPLICATION_SERVICES"))
                    .build());
        }

        return violations;
    }

    /**
     * Validates that a DRIVING port is implemented by or referenced by at least one application service.
     *
     * <p>Since the codebase doesn't track implementations, we check if any application
     * service depends on this port (as it would need to implement or use it).</p>
     *
     * @param port the driving port to validate
     * @param applicationServiceQNames the qualified names of application services
     * @param codebase the codebase
     * @return list of violations (empty if valid)
     */
    private List<Violation> validateDrivingPort(
            DrivingPort port, Set<String> applicationServiceQNames, Codebase codebase) {
        List<Violation> violations = new ArrayList<>();

        String portQualifiedName = port.id().qualifiedName();

        // Check if any application service references this port
        // In hexagonal architecture, application services should implement driving ports
        boolean referencedByApplicationService = applicationServiceQNames.stream().anyMatch(appServiceQName -> {
            Set<String> deps = codebase.dependencies().getOrDefault(appServiceQName, Set.of());
            return deps.contains(portQualifiedName);
        });

        // Also check reverse: is the port itself an application service?
        // Driving ports are often defined in the application layer
        boolean isApplicationService = applicationServiceQNames.contains(portQualifiedName);

        if (!referencedByApplicationService && !isApplicationService) {
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
        }

        return violations;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
