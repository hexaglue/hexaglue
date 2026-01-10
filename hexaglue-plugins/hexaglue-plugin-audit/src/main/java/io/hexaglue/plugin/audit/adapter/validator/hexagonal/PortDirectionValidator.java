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

import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.DependencyEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.RoleClassification;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that port direction (DRIVING/DRIVEN) is consistent with actual usage.
 *
 * <p>Hexagonal Architecture Principle: Ports should be used according to their direction:
 * <ul>
 *   <li><b>DRIVING ports</b> (inbound): Implemented by application services, called by adapters</li>
 *   <li><b>DRIVEN ports</b> (outbound): Used/called by application services, implemented by adapters</li>
 * </ul>
 *
 * <p><strong>Implementation Note:</strong> Due to current SPI limitations, this validator
 * infers port direction from naming conventions:
 * <ul>
 *   <li>Ports ending in "Repository", "Gateway", "Client" → DRIVEN (outbound)</li>
 *   <li>Ports ending in "Service", "UseCase", "Handler" → DRIVING (inbound)</li>
 * </ul>
 *
 * <p>Future enhancement: Access Port.direction() directly from IR snapshot.
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

    /**
     * Suffixes that indicate a driven (outbound) port.
     * These ports should be USED by application services, not implemented by them.
     */
    private static final Set<String> DRIVEN_PORT_SUFFIXES =
            Set.of("Repository", "Gateway", "Client", "Publisher", "Adapter", "Store");

    /**
     * Suffixes that indicate a driving (inbound) port.
     * These ports should be IMPLEMENTED by application services.
     */
    private static final Set<String> DRIVING_PORT_SUFFIXES = Set.of("Service", "UseCase", "Handler", "Facade");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Get all ports
        List<CodeUnit> ports = codebase.unitsWithRole(RoleClassification.PORT);

        for (CodeUnit port : ports) {
            PortDirection inferredDirection = inferPortDirection(port);

            if (inferredDirection == PortDirection.DRIVEN) {
                // DRIVEN ports should be used by application services
                violations.addAll(validateDrivenPort(port, codebase));
            } else if (inferredDirection == PortDirection.DRIVING) {
                // DRIVING ports should be implemented by application services
                violations.addAll(validateDrivingPort(port, codebase));
            }
            // If direction cannot be inferred, skip validation
        }

        return violations;
    }

    /**
     * Validates that a DRIVEN port is used by at least one application service.
     *
     * @param port the driven port to validate
     * @param codebase the codebase
     * @return list of violations (empty if valid)
     */
    private List<Violation> validateDrivenPort(CodeUnit port, Codebase codebase) {
        List<Violation> violations = new ArrayList<>();

        // Find application services that use this port
        boolean usedByApplicationService = codebase.unitsWithRole(RoleClassification.USE_CASE).stream()
                .anyMatch(appService -> {
                    Set<String> deps = codebase.dependencies().getOrDefault(appService.qualifiedName(), Set.of());
                    return deps.contains(port.qualifiedName());
                });

        if (!usedByApplicationService) {
            violations.add(Violation.builder(CONSTRAINT_ID)
                    .severity(Severity.MAJOR)
                    .message("DRIVEN port '%s' is not used by any application service".formatted(port.simpleName()))
                    .affectedType(port.qualifiedName())
                    .location(SourceLocation.of(port.qualifiedName(), 1, 1))
                    .evidence(DependencyEvidence.of(
                            "DRIVEN ports should be called by application services to interact with external systems",
                            port.qualifiedName(),
                            "APPLICATION_SERVICES"))
                    .build());
        }

        return violations;
    }

    /**
     * Validates that a DRIVING port is implemented by at least one application service.
     *
     * <p>Since CodeUnit doesn't track implementations, we check if any application
     * service depends on this port (as it would need to implement or use it).
     *
     * @param port the driving port to validate
     * @param codebase the codebase
     * @return list of violations (empty if valid)
     */
    private List<Violation> validateDrivingPort(CodeUnit port, Codebase codebase) {
        List<Violation> violations = new ArrayList<>();

        // Check if application services reference this port
        // In hexagonal architecture, application services should implement driving ports
        boolean referencedByApplicationService = codebase.unitsWithRole(RoleClassification.USE_CASE).stream()
                .anyMatch(appService -> {
                    Set<String> deps = codebase.dependencies().getOrDefault(appService.qualifiedName(), Set.of());
                    return deps.contains(port.qualifiedName());
                });

        // Also check reverse: is the port itself in the application layer?
        // Driving ports are often defined in the application layer
        boolean inApplicationLayer = codebase.unitsWithRole(RoleClassification.USE_CASE).stream()
                .anyMatch(unit -> unit.qualifiedName().equals(port.qualifiedName()));

        if (!referencedByApplicationService && !inApplicationLayer) {
            violations.add(Violation.builder(CONSTRAINT_ID)
                    .severity(Severity.MAJOR)
                    .message("DRIVING port '%s' is not implemented by any application service"
                            .formatted(port.simpleName()))
                    .affectedType(port.qualifiedName())
                    .location(SourceLocation.of(port.qualifiedName(), 1, 1))
                    .evidence(DependencyEvidence.of(
                            "DRIVING ports should be implemented by application services to provide use cases",
                            port.qualifiedName(),
                            "APPLICATION_SERVICES"))
                    .build());
        }

        return violations;
    }

    /**
     * Infers port direction from naming conventions.
     *
     * @param port the port to analyze
     * @return the inferred direction, or UNKNOWN if cannot be determined
     */
    private PortDirection inferPortDirection(CodeUnit port) {
        String simpleName = port.simpleName();

        // Check if it matches driven port suffixes
        if (DRIVEN_PORT_SUFFIXES.stream().anyMatch(simpleName::endsWith)) {
            return PortDirection.DRIVEN;
        }

        // Check if it matches driving port suffixes
        if (DRIVING_PORT_SUFFIXES.stream().anyMatch(simpleName::endsWith)) {
            return PortDirection.DRIVING;
        }

        // Cannot determine direction from naming
        return PortDirection.UNKNOWN;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    /**
     * Internal enum for port direction inference.
     */
    private enum PortDirection {
        DRIVING,
        DRIVEN,
        UNKNOWN
    }
}
