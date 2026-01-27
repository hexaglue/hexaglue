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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that entities have an identity field.
 *
 * <p>DDD Principle: Entities are defined by their identity, not their attributes.
 * Every entity (including aggregate roots) must have a stable identity that
 * distinguishes it from other instances.
 *
 * <p>This validator uses the v5 ArchType API to check:
 * <ul>
 *   <li>AggregateRoot always has an identityField (non-null by design)</li>
 *   <li>Entity should have an identityField (optional but recommended)</li>
 * </ul>
 *
 * <p><strong>Constraint:</strong> ddd:entity-identity<br>
 * <strong>Severity:</strong> CRITICAL<br>
 * <strong>Rationale:</strong> Without identity, an entity cannot be properly
 * distinguished from other instances, breaking a core DDD concept.
 *
 * @since 1.0.0
 * @since 5.0.0 Migrated to v5 ArchType API using DomainIndex
 */
public class EntityIdentityValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:entity-identity");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        DomainIndex domain = model.domainIndex().orElse(null);
        if (domain == null) {
            return violations;
        }

        // AggregateRoot always has identityField (non-null by design in v5)
        // So we only need to check Entity types

        domain.entities().forEach(entity -> {
            if (!hasIdentity(entity)) {
                violations.add(Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.CRITICAL)
                        .message("Entity '%s' has no identity field"
                                .formatted(entity.id().simpleName()))
                        .affectedType(entity.id().qualifiedName())
                        .location(SourceLocation.of(entity.id().qualifiedName(), 1, 1))
                        .evidence(StructuralEvidence.of(
                                "Entities must have an identity to distinguish instances",
                                entity.id().qualifiedName()))
                        .build());
            }
        });

        return violations;
    }

    /**
     * Checks if the entity has an identity field using v5 API.
     *
     * <p>Uses the v5 Entity API which provides:
     * <ul>
     *   <li>{@code entity.identityField()} - Optional field with IDENTITY role</li>
     *   <li>{@code entity.hasIdentity()} - Convenience method</li>
     * </ul>
     *
     * @param entity the entity to check
     * @return true if an identity field is found
     */
    private boolean hasIdentity(Entity entity) {
        // Use v5 API: Entity.hasIdentity() or check for IDENTITY role in structure
        if (entity.hasIdentity()) {
            return true;
        }

        // Fallback: check structure for IDENTITY role fields
        return !entity.structure().getFieldsWithRole(FieldRole.IDENTITY).isEmpty();
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }
}
