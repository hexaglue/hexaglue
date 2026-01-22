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
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.DependencyEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
     * Layer classification for architectural types.
     */
    private enum Layer {
        DOMAIN,
        APPLICATION,
        PORT,
        INFRASTRUCTURE,
        UNCLASSIFIED
    }

    /**
     * Defines allowed dependencies for each layer.
     * Each layer can depend on itself and the layers listed in the set.
     */
    private static final Map<Layer, Set<Layer>> ALLOWED_DEPENDENCIES = Map.of(
            // Domain can only depend on itself
            Layer.DOMAIN, EnumSet.of(Layer.DOMAIN),
            // Application can depend on itself, Domain, and Ports
            Layer.APPLICATION, EnumSet.of(Layer.APPLICATION, Layer.DOMAIN, Layer.PORT),
            // Infrastructure can depend on all layers
            Layer.INFRASTRUCTURE, EnumSet.allOf(Layer.class),
            // Ports can depend on Domain (for return types, parameters)
            Layer.PORT, EnumSet.of(Layer.PORT, Layer.DOMAIN));

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates the layer isolation constraint using v5 ArchType API.
     *
     * <p>Checks that types in each layer only depend on allowed layers.
     * Uses {@code model.typeRegistry()} to access all classified types and
     * {@code ArchKind} to determine layer membership.
     *
     * @param model the architectural model containing v5 type registry
     * @param codebase the codebase for dependency graph access
     * @param query architecture query (not used in this validator)
     * @return list of violations found
     * @since 5.0.0
     */
    @Override
    public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        if (model.typeRegistry().isEmpty()) {
            // No v5 model available - cannot validate
            return violations;
        }

        var registry = model.typeRegistry().get();

        // Create type lookup map (qualified name -> ArchType)
        Map<String, ArchType> typeMap =
                registry.all(ArchType.class).collect(Collectors.toMap(t -> t.id().qualifiedName(), t -> t));

        // Check each layer's dependencies
        for (Layer layer : ALLOWED_DEPENDENCIES.keySet()) {
            violations.addAll(validateLayerIsolation(codebase, layer, typeMap));
        }

        return violations;
    }

    /**
     * Validates that a specific layer only depends on allowed layers.
     *
     * @param codebase the codebase to analyze
     * @param layer the layer to check
     * @param typeMap the map of qualified names to ArchTypes
     * @return list of violations found
     */
    private List<Violation> validateLayerIsolation(Codebase codebase, Layer layer, Map<String, ArchType> typeMap) {
        List<Violation> violations = new ArrayList<>();
        Set<Layer> allowedLayers = ALLOWED_DEPENDENCIES.get(layer);

        // Get types in this layer
        List<ArchType> layerTypes = typeMap.values().stream()
                .filter(t -> getLayer(t.kind()) == layer)
                .toList();

        for (ArchType type : layerTypes) {
            String qualifiedName = type.id().qualifiedName();
            Set<String> deps = codebase.dependencies().getOrDefault(qualifiedName, Set.of());

            for (String depName : deps) {
                // Find the dependency in the type map
                ArchType depType = typeMap.get(depName);
                if (depType == null) {
                    continue; // External dependency, not our concern
                }

                Layer depLayer = getLayer(depType.kind());
                if (!allowedLayers.contains(depLayer)) {
                    violations.add(Violation.builder(CONSTRAINT_ID)
                            .severity(Severity.MAJOR)
                            .message("%s type '%s' depends on %s type '%s' (not allowed)"
                                    .formatted(
                                            layer.name(),
                                            type.id().simpleName(),
                                            depLayer.name(),
                                            depType.id().simpleName()))
                            .affectedType(qualifiedName)
                            .location(SourceLocation.of(qualifiedName, 1, 1))
                            .evidence(DependencyEvidence.of(
                                    "Layer %s can only depend on: %s".formatted(layer.name(), allowedLayers),
                                    qualifiedName,
                                    depName))
                            .build());
                }
            }
        }

        return violations;
    }

    /**
     * Determines the layer for an ArchKind.
     *
     * @param kind the ArchKind
     * @return the corresponding Layer
     */
    private Layer getLayer(ArchKind kind) {
        if (kind.isDomain()) {
            return Layer.DOMAIN;
        }
        if (kind.isApplication()) {
            return Layer.APPLICATION;
        }
        if (kind.isPort()) {
            return Layer.PORT;
        }
        if (kind == ArchKind.UNCLASSIFIED) {
            return Layer.UNCLASSIFIED;
        }
        // Anything else is considered infrastructure
        return Layer.INFRASTRUCTURE;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
