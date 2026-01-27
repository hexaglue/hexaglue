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
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.MethodRole;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.audit.domain.model.BehavioralEvidence;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that value objects are immutable.
 *
 * <p>DDD Principle: Value Objects represent descriptive aspects of the domain
 * with no conceptual identity. They should be immutable - once created, their
 * state cannot change. If you need a different value, you create a new instance.
 *
 * <p>This validator uses the v5 ArchType API to check:
 * <ul>
 *   <li>Records are immutable by design</li>
 *   <li>Classes are checked for setter methods via {@link MethodRole#SETTER}</li>
 * </ul>
 *
 * <p><strong>Constraint:</strong> ddd:value-object-immutable<br>
 * <strong>Severity:</strong> CRITICAL<br>
 * <strong>Rationale:</strong> Mutable value objects can lead to unexpected
 * behavior when passed between aggregates or used as map keys. Immutability
 * ensures thread-safety and proper value semantics.
 *
 * @since 1.0.0
 * @since 5.0.0 Migrated to v5 ArchType API using DomainIndex and MethodRole
 */
public class ValueObjectImmutabilityValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:value-object-immutable");

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

        domain.valueObjects().forEach(vo -> {
            // Records are immutable by design, skip them
            if (vo.structure().isRecord()) {
                return;
            }

            // For classes, check for setter methods
            List<Method> setters = findSetterMethods(vo);

            if (!setters.isEmpty()) {
                Violation.Builder builder = Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.CRITICAL)
                        .message("Value object '%s' has %d setter method(s), violating immutability"
                                .formatted(vo.id().simpleName(), setters.size()))
                        .affectedType(vo.id().qualifiedName())
                        .location(SourceLocation.of(vo.id().qualifiedName(), 1, 1));

                // Add evidence for each setter
                for (Method setter : setters) {
                    builder.evidence(BehavioralEvidence.of(
                            "Setter method detected: " + setter.name(), vo.id().qualifiedName(), setter.name()));
                }

                violations.add(builder.build());
            }
        });

        return violations;
    }

    /**
     * Finds setter methods in the value object using v5 API.
     *
     * <p>Uses the v5 Method API which provides:
     * <ul>
     *   <li>{@code method.hasRole(MethodRole.SETTER)} - Direct role check</li>
     *   <li>{@code method.isSetter()} - Convenience method</li>
     * </ul>
     *
     * <p>Fallback heuristic if roles not populated:
     * <ul>
     *   <li>Name starts with "set"</li>
     *   <li>Has exactly one parameter</li>
     *   <li>Returns void</li>
     * </ul>
     *
     * @param vo the value object to check
     * @return list of setter methods
     */
    private List<Method> findSetterMethods(ValueObject vo) {
        return vo.structure().methods().stream()
                .filter(method -> {
                    // First check v5 role
                    if (method.hasRole(MethodRole.SETTER)) {
                        return true;
                    }

                    // Fallback heuristic
                    if (!method.name().startsWith("set") || method.name().length() <= 3) {
                        return false;
                    }
                    if (method.parameters().size() != 1) {
                        return false;
                    }
                    return method.returnType().qualifiedName().equals("void");
                })
                .toList();
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }
}
