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
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;

/**
 * Builds {@link Identifier} instances from classification results.
 *
 * <p>This builder transforms a classified {@link TypeNode} into an {@link Identifier}
 * by constructing the type structure, classification trace, and detecting the wrapped type.</p>
 *
 * <p>The wrapped type is detected from the first field of the identifier type.
 * For example, if {@code OrderId} has a field {@code UUID value}, the wrapped type
 * will be {@code java.util.UUID}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * IdentifierBuilder builder = new IdentifierBuilder(structureBuilder, traceConverter);
 * Identifier identifier = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class IdentifierBuilder {

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;

    /**
     * Creates a new IdentifierBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @throws NullPointerException if any argument is null
     */
    public IdentifierBuilder(TypeStructureBuilder structureBuilder, ClassificationTraceConverter traceConverter) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
    }

    /**
     * Builds an Identifier from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built Identifier
     * @throws NullPointerException if any argument is null
     */
    public Identifier build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);
        TypeRef wrappedType = detectWrappedType(typeNode, context);

        return Identifier.of(id, structure, trace, wrappedType);
    }

    /**
     * Detects the wrapped type from the identifier's fields.
     *
     * <p>The wrapped type is typically the type of the first field in the identifier.
     * For example, {@code record OrderId(UUID value)} wraps {@code java.util.UUID}.</p>
     *
     * @param typeNode the type node
     * @param context the builder context
     * @return the wrapped type, or a fallback type if no fields exist
     */
    private TypeRef detectWrappedType(TypeNode typeNode, BuilderContext context) {
        List<FieldNode> fields = context.graphQuery().fieldsOf(typeNode);

        if (fields.isEmpty()) {
            // Fallback to Object when no fields are found
            return TypeRef.of("java.lang.Object");
        }

        // Use the first field's type as the wrapped type
        FieldNode firstField = fields.get(0);
        return mapTypeRef(firstField.type());
    }

    /**
     * Maps a core TypeRef to a syntax TypeRef.
     *
     * @param coreTypeRef the core type reference
     * @return the syntax type reference
     */
    private TypeRef mapTypeRef(io.hexaglue.core.frontend.TypeRef coreTypeRef) {
        return TypeRef.of(coreTypeRef.rawQualifiedName());
    }
}
