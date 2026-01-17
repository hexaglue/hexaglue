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
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RoleClassification;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Find all ports (including repositories, which are driven ports)
        List<CodeUnit> ports = new ArrayList<>();
        ports.addAll(codebase.unitsWithRole(RoleClassification.PORT));
        ports.addAll(codebase.unitsWithRole(RoleClassification.REPOSITORY));

        // Find all adapters in the infrastructure layer
        List<CodeUnit> adapters = codebase.units().stream()
                .filter(unit -> unit.layer() == LayerClassification.INFRASTRUCTURE)
                .filter(unit -> unit.role() == RoleClassification.ADAPTER)
                .toList();

        // Check each port for adapter implementations
        for (CodeUnit port : ports) {
            boolean hasAdapterImplementation = hasAdapterImplementation(port, adapters, codebase);

            if (!hasAdapterImplementation) {
                violations.add(Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.MAJOR)
                        .message("Port '%s' has no adapter implementation".formatted(port.simpleName()))
                        .affectedType(port.qualifiedName())
                        .location(SourceLocation.of(port.qualifiedName(), 1, 1))
                        .evidence(StructuralEvidence.of(
                                "Ports must have at least one adapter implementation in the infrastructure layer",
                                port.qualifiedName()))
                        .build());
            }
        }

        return violations;
    }

    /**
     * Checks if a port has at least one adapter implementation.
     *
     * <p>An adapter is considered to implement a port if the adapter depends on
     * the port (indicating it implements or uses the port interface).
     *
     * @param port the port to check
     * @param adapters all adapters in the codebase
     * @param codebase the codebase containing dependency information
     * @return true if the port has at least one adapter implementation
     */
    private boolean hasAdapterImplementation(CodeUnit port, List<CodeUnit> adapters, Codebase codebase) {
        String portQualifiedName = port.qualifiedName();

        // Check if any adapter depends on this port
        return adapters.stream().anyMatch(adapter -> {
            Set<String> adapterDeps = codebase.dependencies().getOrDefault(adapter.qualifiedName(), Set.of());
            return adapterDeps.contains(portQualifiedName);
        });
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
