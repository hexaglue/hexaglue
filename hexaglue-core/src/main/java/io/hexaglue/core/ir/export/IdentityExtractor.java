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

package io.hexaglue.core.ir.export;

import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IdentityStrategy;
import io.hexaglue.spi.ir.IdentityWrapperKind;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Extracts identity information from domain types.
 *
 * <p>This class handles the detection and extraction of identity fields from
 * aggregate roots and entities, supporting various patterns:
 * <ul>
 *   <li>Explicit @Identity annotation</li>
 *   <li>Field named exactly "id"</li>
 *   <li>Field ending with "Id" suffix (e.g., orderId)</li>
 * </ul>
 *
 * <p>It also handles identity type unwrapping for wrapper types (records/classes
 * wrapping primitives like UUID, Long, etc.).
 */
final class IdentityExtractor {

    private static final Set<String> IDENTITY_TYPES =
            Set.of("java.util.UUID", "java.lang.Long", "java.lang.String", "java.lang.Integer", "long", "int");

    private final TypeConverter typeConverter;

    IdentityExtractor(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    /**
     * Extracts identity from a type node if present.
     *
     * @param graph the application graph
     * @param node the type node to extract identity from
     * @return the extracted identity, or empty if no identity field found
     */
    Optional<Identity> extractIdentity(ApplicationGraph graph, TypeNode node) {
        List<FieldNode> fields = graph.fieldsOf(node);

        // Priority 1: Look for explicit @Identity annotation
        Optional<FieldNode> annotatedId =
                fields.stream().filter(this::hasIdentityAnnotation).findFirst();
        if (annotatedId.isPresent()) {
            return Optional.of(createIdentity(annotatedId.get(), graph));
        }

        // Priority 2: Look for field named exactly "id"
        Optional<FieldNode> exactIdField =
                fields.stream().filter(f -> f.simpleName().equals("id")).findFirst();
        if (exactIdField.isPresent()) {
            return Optional.of(createIdentity(exactIdField.get(), graph));
        }

        // Priority 3: Look for field ending with "Id" (e.g., orderId, customerId)
        Optional<FieldNode> suffixIdField =
                fields.stream().filter(f -> f.simpleName().endsWith("Id")).findFirst();

        return suffixIdField.map(field -> createIdentity(field, graph));
    }

    private boolean hasIdentityAnnotation(FieldNode field) {
        return field.annotations().stream()
                .anyMatch(a -> a.qualifiedName().equals("org.jmolecules.ddd.annotation.Identity")
                        || a.simpleName().equals("Identity")
                        || a.simpleName().equals("Id"));
    }

    private Identity createIdentity(FieldNode field, ApplicationGraph graph) {
        TypeRef coreTypeRef = field.type();
        io.hexaglue.spi.ir.TypeRef spiType = typeConverter.toSpiTypeRef(coreTypeRef);

        UnwrapResult unwrapResult = unwrapIdentityType(coreTypeRef, graph);
        io.hexaglue.spi.ir.TypeRef spiUnwrappedType = typeConverter.toSpiTypeRef(unwrapResult.unwrappedType());

        IdentityStrategy strategy =
                determineStrategy(unwrapResult.unwrappedType().rawQualifiedName());

        return new Identity(field.simpleName(), spiType, spiUnwrappedType, strategy, unwrapResult.wrapperKind());
    }

    /**
     * Result of unwrapping an identity type.
     *
     * @param unwrappedType the inner type (primitive/wrapper after unwrapping)
     * @param wrapperKind the kind of wrapper (NONE, RECORD, or CLASS)
     */
    record UnwrapResult(TypeRef unwrappedType, IdentityWrapperKind wrapperKind) {}

    /**
     * Unwraps identity wrapper types to extract the underlying primitive/simple type.
     *
     * <p>This method handles three cases:
     * <ul>
     *   <li><b>NONE</b>: Type is already a primitive/simple type (UUID, Long, etc.)</li>
     *   <li><b>RECORD</b>: Type is a record with single field wrapping primitive
     *       (e.g., {@code OrderId(UUID value)} → UUID)</li>
     *   <li><b>CLASS</b>: Type is a class with single field wrapping primitive
     *       (e.g., {@code class CustomerId { Long id; }} → Long)</li>
     * </ul>
     *
     * <p>Examples:
     * <pre>
     * unwrapIdentityType(UUID) → (UUID, NONE)
     * unwrapIdentityType(OrderId) → (UUID, RECORD) if OrderId is record OrderId(UUID value)
     * unwrapIdentityType(CustomerId) → (Long, CLASS) if CustomerId has Long id field
     * </pre>
     *
     * @param typeRef the type reference to unwrap
     * @param graph the application graph for type lookup
     * @return unwrap result containing the inner type and wrapper kind
     */
    UnwrapResult unwrapIdentityType(TypeRef typeRef, ApplicationGraph graph) {
        String typeName = typeRef.rawQualifiedName();

        // Check if this is a known primitive/wrapper ID type
        if (IDENTITY_TYPES.contains(typeName)) {
            return new UnwrapResult(typeRef, IdentityWrapperKind.NONE);
        }

        // Check if the type is a record with a single component (likely an ID wrapper)
        Optional<TypeNode> idType = graph.typeNode(typeName);
        if (idType.isPresent()) {
            TypeNode typeNode = idType.get();
            List<FieldNode> fields = graph.fieldsOf(typeNode);

            if (fields.size() == 1) {
                TypeRef innerType = fields.get(0).type();
                if (IDENTITY_TYPES.contains(innerType.rawQualifiedName())) {
                    IdentityWrapperKind wrapperKind =
                            typeNode.isRecord() ? IdentityWrapperKind.RECORD : IdentityWrapperKind.CLASS;
                    return new UnwrapResult(innerType, wrapperKind);
                }
            }
        }

        return new UnwrapResult(typeRef, IdentityWrapperKind.NONE);
    }

    /**
     * Determines the identity generation strategy based on the unwrapped type.
     *
     * @param unwrappedTypeName the fully qualified name of the unwrapped type
     * @return the appropriate identity strategy
     */
    private IdentityStrategy determineStrategy(String unwrappedTypeName) {
        // UUID is typically UUID strategy (application-generated)
        if (unwrappedTypeName.equals("java.util.UUID")) {
            return IdentityStrategy.UUID;
        }
        // Long/Integer often use database sequences or identity columns
        if (unwrappedTypeName.equals("java.lang.Long")
                || unwrappedTypeName.equals("long")
                || unwrappedTypeName.equals("java.lang.Integer")
                || unwrappedTypeName.equals("int")) {
            return IdentityStrategy.AUTO;
        }
        // String is typically assigned (natural key or application-assigned)
        if (unwrappedTypeName.equals("java.lang.String")) {
            return IdentityStrategy.ASSIGNED;
        }
        return IdentityStrategy.UNKNOWN;
    }
}
