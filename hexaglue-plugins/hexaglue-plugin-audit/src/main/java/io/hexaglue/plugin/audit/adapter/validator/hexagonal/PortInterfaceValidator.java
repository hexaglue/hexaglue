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
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that ports are defined as interfaces.
 *
 * <p>Hexagonal Architecture Principle: Ports define the boundaries between the
 * application core and external adapters. They should be interfaces to allow
 * multiple implementations (adapters) and maintain the Dependency Inversion Principle.
 *
 * <p>This validator ensures that all types classified as ports are Java interfaces,
 * not classes.
 *
 * <p><strong>Constraint:</strong> hexagonal:port-interface<br>
 * <strong>Severity:</strong> CRITICAL<br>
 * <strong>Rationale:</strong> Using classes for ports couples the application to
 * specific implementations, violating the core principle of hexagonal architecture.
 *
 * @since 1.0.0
 */
public class PortInterfaceValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("hexagonal:port-interface");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates the port interface constraint using v5 ArchType API.
     *
     * <p>Checks that all driving and driven ports are Java interfaces.
     * Uses {@code model.portIndex()} for direct port access and
     * {@code structure.isInterface()} to verify the type nature.
     *
     * @param model the architectural model containing v5 port index
     * @param codebase the codebase (not used in this validator)
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

        // Check driving ports
        portIndex.drivingPorts().forEach(port -> {
            violations.addAll(validatePortIsInterface(port));
        });

        // Check driven ports
        portIndex.drivenPorts().forEach(port -> {
            violations.addAll(validatePortIsInterface(port));
        });

        return violations;
    }

    /**
     * Validates that a driving port is an interface.
     *
     * @param port the driving port to validate
     * @return list of violations (empty if valid)
     */
    private List<Violation> validatePortIsInterface(DrivingPort port) {
        List<Violation> violations = new ArrayList<>();

        if (!port.structure().isInterface()) {
            violations.add(Violation.builder(CONSTRAINT_ID)
                    .severity(Severity.CRITICAL)
                    .message("Port '%s' is not an interface (found: %s)"
                            .formatted(
                                    port.id().simpleName(),
                                    port.structure().nature().name()))
                    .affectedType(port.id().qualifiedName())
                    .location(SourceLocation.of(port.id().qualifiedName(), 1, 1))
                    .evidence(StructuralEvidence.of(
                            "Ports must be interfaces to support the Dependency Inversion Principle",
                            port.id().qualifiedName()))
                    .build());
        }

        return violations;
    }

    /**
     * Validates that a driven port is an interface.
     *
     * @param port the driven port to validate
     * @return list of violations (empty if valid)
     */
    private List<Violation> validatePortIsInterface(DrivenPort port) {
        List<Violation> violations = new ArrayList<>();

        if (!port.structure().isInterface()) {
            violations.add(Violation.builder(CONSTRAINT_ID)
                    .severity(Severity.CRITICAL)
                    .message("Port '%s' is not an interface (found: %s)"
                            .formatted(
                                    port.id().simpleName(),
                                    port.structure().nature().name()))
                    .affectedType(port.id().qualifiedName())
                    .location(SourceLocation.of(port.id().qualifiedName(), 1, 1))
                    .evidence(StructuralEvidence.of(
                            "Ports must be interfaces to support the Dependency Inversion Principle",
                            port.id().qualifiedName()))
                    .build());
        }

        return violations;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }
}
