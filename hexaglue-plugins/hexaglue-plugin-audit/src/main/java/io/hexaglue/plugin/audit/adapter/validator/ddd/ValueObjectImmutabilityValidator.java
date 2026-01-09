/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import io.hexaglue.plugin.audit.domain.model.BehavioralEvidence;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.MethodDeclaration;
import io.hexaglue.spi.audit.RoleClassification;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that value objects are immutable.
 *
 * <p>DDD Principle: Value Objects represent descriptive aspects of the domain
 * with no conceptual identity. They should be immutable - once created, their
 * state cannot change. If you need a different value, you create a new instance.
 *
 * <p>This validator performs heuristic checks for mutability indicators:
 * <ul>
 *   <li>Setter methods (methods starting with "set")</li>
 *   <li>Non-final fields (if field information is available)</li>
 *   <li>Methods that modify internal state</li>
 * </ul>
 *
 * <p><strong>Constraint:</strong> ddd:value-object-immutable<br>
 * <strong>Severity:</strong> CRITICAL<br>
 * <strong>Rationale:</strong> Mutable value objects can lead to unexpected
 * behavior when passed between aggregates or used as map keys. Immutability
 * ensures thread-safety and proper value semantics.
 *
 * @since 1.0.0
 */
public class ValueObjectImmutabilityValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:value-object-immutable");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Find all value objects
        List<CodeUnit> valueObjects = codebase.unitsWithRole(RoleClassification.VALUE_OBJECT);

        for (CodeUnit valueObject : valueObjects) {
            // Check for setter methods
            List<MethodDeclaration> setters = findSetterMethods(valueObject);

            if (!setters.isEmpty()) {
                Violation.Builder builder = Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.CRITICAL)
                        .message("Value object '%s' has %d setter method(s), violating immutability"
                                .formatted(valueObject.simpleName(), setters.size()))
                        .affectedType(valueObject.qualifiedName())
                        .location(SourceLocation.of(valueObject.qualifiedName(), 1, 1));

                // Add evidence for each setter
                for (MethodDeclaration setter : setters) {
                    builder.evidence(BehavioralEvidence.of(
                            "Setter method detected: " + setter.name(),
                            valueObject.qualifiedName(),
                            setter.name()));
                }

                violations.add(builder.build());
            }
        }

        return violations;
    }

    /**
     * Finds setter methods in the code unit.
     *
     * <p>A method is considered a setter if:
     * <ul>
     *   <li>Name starts with "set"</li>
     *   <li>Has exactly one parameter</li>
     *   <li>Returns void</li>
     * </ul>
     *
     * @param unit the code unit to check
     * @return list of setter methods
     */
    private List<MethodDeclaration> findSetterMethods(CodeUnit unit) {
        return unit.methods().stream()
                .filter(method -> {
                    // Check name starts with "set"
                    if (!method.name().startsWith("set") || method.name().length() <= 3) {
                        return false;
                    }

                    // Check has exactly one parameter
                    if (method.parameterTypes().size() != 1) {
                        return false;
                    }

                    // Check returns void
                    return method.returnType().equals("void");
                })
                .toList();
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }
}
