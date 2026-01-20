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

package io.hexaglue.core.builder;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Builds {@link Entity} instances from classification results.
 *
 * <p>This builder transforms a classified {@link TypeNode} into an {@link Entity}
 * by constructing the type structure, classification trace, and detecting the identity field.</p>
 *
 * <p>The identity field is detected using the {@link FieldRoleDetector} - it looks for
 * fields with the {@link FieldRole#IDENTITY} role (annotated with @Id or named "id").</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * EntityBuilder builder = new EntityBuilder(structureBuilder, traceConverter, fieldRoleDetector);
 * Entity entity = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class EntityBuilder {

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;
    private final FieldRoleDetector fieldRoleDetector;

    /**
     * Creates a new EntityBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @param fieldRoleDetector the detector for field roles
     * @throws NullPointerException if any argument is null
     */
    public EntityBuilder(
            TypeStructureBuilder structureBuilder,
            ClassificationTraceConverter traceConverter,
            FieldRoleDetector fieldRoleDetector) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
        this.fieldRoleDetector = Objects.requireNonNull(fieldRoleDetector, "fieldRoleDetector must not be null");
    }

    /**
     * Builds an Entity from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built Entity
     * @throws NullPointerException if any argument is null
     */
    public Entity build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);
        Optional<Field> identityField = findIdentityField(typeNode, structure, context);

        return Entity.of(id, structure, trace, identityField.orElse(null));
    }

    /**
     * Finds the identity field in the type structure.
     *
     * <p>First attempts to find a field with the IDENTITY role in the built structure.
     * If not found, falls back to using the FieldRoleDetector on the original FieldNodes.</p>
     *
     * @param typeNode the type node
     * @param structure the built type structure
     * @param context the builder context
     * @return the identity field if found
     */
    private Optional<Field> findIdentityField(TypeNode typeNode, TypeStructure structure, BuilderContext context) {
        // First try to find from the built structure
        Optional<Field> fromStructure = structure.fields().stream()
                .filter(f -> f.hasRole(FieldRole.IDENTITY))
                .findFirst();

        if (fromStructure.isPresent()) {
            return fromStructure;
        }

        // Fallback: check field nodes directly
        List<FieldNode> fieldNodes = context.graphQuery().fieldsOf(typeNode);
        for (FieldNode fieldNode : fieldNodes) {
            Set<FieldRole> roles = fieldRoleDetector.detect(fieldNode, context);
            if (roles.contains(FieldRole.IDENTITY)) {
                // Find the corresponding Field in the structure
                return structure.fields().stream()
                        .filter(f -> f.name().equals(fieldNode.simpleName()))
                        .findFirst();
            }
        }

        return Optional.empty();
    }
}
