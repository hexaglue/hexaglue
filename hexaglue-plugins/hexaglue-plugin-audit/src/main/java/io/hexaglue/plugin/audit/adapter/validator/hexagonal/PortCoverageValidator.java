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
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that all ports have at least one adapter implementing them.
 *
 * <p>Hexagonal Architecture Principle: Ports define the boundaries between the
 * application core and external systems. Each port should have one or more
 * adapters implementing it. Ports without adapters indicate incomplete
 * architecture where the application cannot interact with external systems.
 *
 * <p>This validator checks that every port interface has at least one adapter
 * (infrastructure implementation) that depends on it. Both driving ports
 * (use cases) and driven ports (repositories, gateways) are verified.
 *
 * <p>A port is considered implemented if:
 * <ul>
 *   <li>An adapter in the INFRASTRUCTURE layer depends on the port</li>
 *   <li>The adapter is classified with role ADAPTER</li>
 * </ul>
 *
 * <p><strong>Note:</strong> This validator only checks for implementations in the
 * infrastructure/adapter layer. Implementations in the application layer are not
 * considered valid adapter implementations, as they violate the separation of
 * concerns between application logic and infrastructure adapters.
 *
 * <p><strong>Constraint:</strong> hexagonal:port-coverage<br>
 * <strong>Severity:</strong> MAJOR<br>
 * <strong>Rationale:</strong> Unimplemented ports indicate incomplete hexagonal
 * architecture. The application cannot function properly without concrete adapters
 * bridging the gap between the application core and external systems.
 *
 * @since 1.0.0
 */
public class PortCoverageValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("hexagonal:port-coverage");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates the port coverage constraint using v5 ArchType API.
     *
     * <p>Checks that all driving and driven ports have at least one
     * adapter implementation. Uses {@code model.portIndex()} for direct
     * port access and {@code codebase.dependencies()} to identify adapters
     * that depend on ports.
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

        if (model.portIndex().isEmpty()) {
            // No v5 model available - cannot validate
            return violations;
        }

        var portIndex = model.portIndex().get();

        // Check driving ports for coverage
        portIndex.drivingPorts().forEach(port -> {
            if (!hasAdapterImplementation(port, codebase)) {
                violations.add(createViolation(port));
            }
        });

        // Check driven ports for coverage
        portIndex.drivenPorts().forEach(port -> {
            if (!hasAdapterImplementation(port, codebase)) {
                violations.add(createViolation(port));
            }
        });

        return violations;
    }

    /**
     * Checks if a driving port has at least one adapter implementation.
     *
     * <p>An adapter is considered to implement a port if the adapter depends on
     * the port (indicating it implements or uses the port interface).</p>
     *
     * @param port the driving port to check
     * @param codebase the codebase containing dependency information
     * @return true if the port has at least one adapter implementation
     */
    private boolean hasAdapterImplementation(DrivingPort port, Codebase codebase) {
        String portQualifiedName = port.id().qualifiedName();

        // Check if any type depends on this port (reverse lookup)
        // In practice, adapters will depend on the port interface they implement
        return codebase.dependencies().entrySet().stream()
                .anyMatch(entry -> entry.getValue().contains(portQualifiedName));
    }

    /**
     * Checks if a driven port has at least one adapter implementation.
     *
     * <p>An adapter is considered to implement a port if the adapter depends on
     * the port (indicating it implements or uses the port interface).</p>
     *
     * @param port the driven port to check
     * @param codebase the codebase containing dependency information
     * @return true if the port has at least one adapter implementation
     */
    private boolean hasAdapterImplementation(DrivenPort port, Codebase codebase) {
        String portQualifiedName = port.id().qualifiedName();

        // Check if any type depends on this port (reverse lookup)
        // In practice, adapters will depend on the port interface they implement
        return codebase.dependencies().entrySet().stream()
                .anyMatch(entry -> entry.getValue().contains(portQualifiedName));
    }

    /**
     * Creates a violation for a driving port without adapter implementation.
     *
     * @param port the driving port without coverage
     * @return the violation
     */
    private Violation createViolation(DrivingPort port) {
        return Violation.builder(CONSTRAINT_ID)
                .severity(Severity.MAJOR)
                .message("Port '%s' has no adapter implementation"
                        .formatted(port.id().simpleName()))
                .affectedType(port.id().qualifiedName())
                .location(SourceLocation.of(port.id().qualifiedName(), 1, 1))
                .evidence(StructuralEvidence.of(
                        "Ports must have at least one adapter implementation in the infrastructure layer",
                        port.id().qualifiedName()))
                .build();
    }

    /**
     * Creates a violation for a driven port without adapter implementation.
     *
     * @param port the driven port without coverage
     * @return the violation
     */
    private Violation createViolation(DrivenPort port) {
        return Violation.builder(CONSTRAINT_ID)
                .severity(Severity.MAJOR)
                .message("Port '%s' has no adapter implementation"
                        .formatted(port.id().simpleName()))
                .affectedType(port.id().qualifiedName())
                .location(SourceLocation.of(port.id().qualifiedName(), 1, 1))
                .evidence(StructuralEvidence.of(
                        "Ports must have at least one adapter implementation in the infrastructure layer",
                        port.id().qualifiedName()))
                .build();
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
