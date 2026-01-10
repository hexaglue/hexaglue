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
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates proper layer isolation in hexagonal architecture.
 *
 * <p>Hexagonal Architecture Principle: Layers should have clear boundaries and
 * dependencies should flow according to the architecture rules:
 * <ul>
 *   <li>DOMAIN → nothing external (completely isolated)</li>
 *   <li>APPLICATION → DOMAIN only (orchestrates domain logic)</li>
 *   <li>INFRASTRUCTURE → DOMAIN, APPLICATION (implements adapters)</li>
 * </ul>
 *
 * <p>This validator ensures these rules are followed, maintaining clean architecture.
 *
 * <p><strong>Constraint:</strong> hexagonal:layer-isolation<br>
 * <strong>Severity:</strong> MAJOR<br>
 * <strong>Rationale:</strong> Proper layer isolation ensures maintainability,
 * testability, and flexibility to change infrastructure without affecting business logic.
 *
 * @since 1.0.0
 */
public class LayerIsolationValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("hexagonal:layer-isolation");

    /**
     * Defines allowed dependencies for each layer.
     * Each layer can depend on itself and the layers listed in the set.
     */
    private static final Map<LayerClassification, Set<LayerClassification>> ALLOWED_DEPENDENCIES = Map.of(
            // Domain can only depend on itself
            LayerClassification.DOMAIN, EnumSet.of(LayerClassification.DOMAIN),
            // Application can depend on itself and Domain
            LayerClassification.APPLICATION, EnumSet.of(LayerClassification.APPLICATION, LayerClassification.DOMAIN),
            // Infrastructure can depend on all layers
            LayerClassification.INFRASTRUCTURE,
                    EnumSet.of(
                            LayerClassification.INFRASTRUCTURE,
                            LayerClassification.APPLICATION,
                            LayerClassification.DOMAIN));

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Check each layer's dependencies
        for (LayerClassification layer : ALLOWED_DEPENDENCIES.keySet()) {
            violations.addAll(validateLayerIsolation(codebase, layer));
        }

        return violations;
    }

    /**
     * Validates that a specific layer only depends on allowed layers.
     *
     * @param codebase the codebase to analyze
     * @param layer the layer to check
     * @return list of violations found
     */
    private List<Violation> validateLayerIsolation(Codebase codebase, LayerClassification layer) {
        List<Violation> violations = new ArrayList<>();
        Set<LayerClassification> allowedLayers = ALLOWED_DEPENDENCIES.get(layer);

        for (CodeUnit unit : codebase.unitsInLayer(layer)) {
            Set<String> deps = codebase.dependencies().getOrDefault(unit.qualifiedName(), Set.of());

            for (String depName : deps) {
                // Find the dependency in the codebase
                codebase.units().stream()
                        .filter(u -> u.qualifiedName().equals(depName))
                        .filter(u -> !allowedLayers.contains(u.layer()))
                        .findFirst()
                        .ifPresent(illegalDep -> {
                            violations.add(Violation.builder(CONSTRAINT_ID)
                                    .severity(Severity.MAJOR)
                                    .message("%s type '%s' depends on %s type '%s' (not allowed)"
                                            .formatted(
                                                    layer.name(),
                                                    unit.simpleName(),
                                                    illegalDep.layer().name(),
                                                    illegalDep.simpleName()))
                                    .affectedType(unit.qualifiedName())
                                    .location(SourceLocation.of(unit.qualifiedName(), 1, 1))
                                    .evidence(DependencyEvidence.of(
                                            "Layer %s can only depend on: %s".formatted(layer.name(), allowedLayers),
                                            unit.qualifiedName(),
                                            illegalDep.qualifiedName()))
                                    .build());
                        });
            }
        }

        return violations;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
