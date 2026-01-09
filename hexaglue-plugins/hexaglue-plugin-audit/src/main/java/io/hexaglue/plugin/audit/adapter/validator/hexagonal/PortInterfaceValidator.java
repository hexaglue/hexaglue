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
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.RoleClassification;
import io.hexaglue.spi.core.SourceLocation;
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

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Find all ports (excluding repositories which have their own validation)
        List<CodeUnit> ports = codebase.unitsWithRole(RoleClassification.PORT);

        for (CodeUnit port : ports) {
            // Check if port is an interface
            if (port.kind() != CodeUnitKind.INTERFACE) {
                violations.add(Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.CRITICAL)
                        .message("Port '%s' is not an interface (found: %s)"
                                .formatted(port.simpleName(), port.kind()))
                        .affectedType(port.qualifiedName())
                        .location(SourceLocation.of(port.qualifiedName(), 1, 1))
                        .evidence(StructuralEvidence.of(
                                "Ports must be interfaces to support the Dependency Inversion Principle",
                                port.qualifiedName()))
                        .build());
            }
        }

        return violations;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }
}
