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

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.RoleClassification;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that entities have an identity field.
 *
 * <p>DDD Principle: Entities are defined by their identity, not their attributes.
 * Every entity (including aggregate roots) must have a stable identity that
 * distinguishes it from other instances.
 *
 * <p>This validator checks that entities have at least one field that
 * serves as identity (typically named "id" or annotated as an identifier).
 *
 * <p><strong>Constraint:</strong> ddd:entity-identity<br>
 * <strong>Severity:</strong> CRITICAL<br>
 * <strong>Rationale:</strong> Without identity, an entity cannot be properly
 * distinguished from other instances, breaking a core DDD concept.
 *
 * @since 1.0.0
 */
public class EntityIdentityValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:entity-identity");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Find all entities and aggregates
        List<CodeUnit> entities = new ArrayList<>();
        entities.addAll(codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT));
        entities.addAll(codebase.unitsWithRole(RoleClassification.ENTITY));

        for (CodeUnit entity : entities) {
            // Check if entity has identity field
            boolean hasIdentity = hasIdentityField(entity);

            if (!hasIdentity) {
                violations.add(Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.CRITICAL)
                        .message("Entity '%s' has no identity field".formatted(entity.simpleName()))
                        .affectedType(entity.qualifiedName())
                        .location(SourceLocation.of(entity.qualifiedName(), 1, 1))
                        .evidence(StructuralEvidence.of(
                                "Entities must have an identity to distinguish instances", entity.qualifiedName()))
                        .build());
            }
        }

        return violations;
    }

    /**
     * Checks if the code unit has an identity field.
     *
     * <p>This is a heuristic check that looks for:
     * <ul>
     *   <li>Fields named "id" (case-insensitive)</li>
     *   <li>Fields with common identity annotations (@Id, @Identifier, etc.)</li>
     * </ul>
     *
     * @param unit the code unit to check
     * @return true if an identity field is found
     */
    private boolean hasIdentityField(CodeUnit unit) {
        return unit.fields().stream().anyMatch(field -> {
            // Check for common identity field names
            String fieldName = field.name().toLowerCase();
            if (fieldName.equals("id") || fieldName.endsWith("id")) {
                return true;
            }

            // Check for identity annotations
            return field.annotations().stream()
                    .anyMatch(annotation -> annotation.contains("Id")
                            || annotation.contains("Identifier")
                            || annotation.contains("Key"));
        });
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }
}
