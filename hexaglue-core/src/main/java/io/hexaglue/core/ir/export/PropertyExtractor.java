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
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.Nullability;
import java.util.List;
import java.util.Set;

/**
 * Extracts domain properties from type nodes.
 *
 * <p>This class handles property extraction including:
 * <ul>
 *   <li>Filtering out static and transient fields</li>
 *   <li>Determining nullability from annotations</li>
 *   <li>Identifying identity fields</li>
 * </ul>
 */
final class PropertyExtractor {

    private static final Set<String> PRIMITIVE_TYPES =
            Set.of("boolean", "byte", "char", "short", "int", "long", "float", "double");

    private final TypeConverter typeConverter;

    PropertyExtractor(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    /**
     * Extracts domain properties from a type node.
     *
     * @param graph the application graph
     * @param node the type node
     * @param identityFieldName the name of the identity field (to mark it appropriately)
     * @return list of domain properties
     */
    List<DomainProperty> extractProperties(ApplicationGraph graph, TypeNode node, String identityFieldName) {
        List<FieldNode> fields = graph.fieldsOf(node);

        return fields.stream()
                .filter(this::shouldIncludeField)
                .map(field -> toDomainProperty(field, identityFieldName))
                .toList();
    }

    /**
     * Determines if a field should be included in the domain properties.
     * Static and transient fields are excluded as they don't represent domain state.
     */
    private boolean shouldIncludeField(FieldNode field) {
        return !field.isStatic() && !field.isTransient();
    }

    private DomainProperty toDomainProperty(FieldNode field, String identityFieldName) {
        TypeRef typeRef = field.type();

        // Only mark as identity if this is the actual identity field, not just any field ending with "Id"
        // Fields like "customerId" are inter-aggregate references, not identity fields
        boolean isIdentity = identityFieldName != null && field.simpleName().equals(identityFieldName);

        return new DomainProperty(
                field.simpleName(),
                typeConverter.toSpiTypeRef(typeRef),
                typeConverter.extractCardinality(typeRef),
                extractNullability(field),
                isIdentity);
    }

    /**
     * Extracts nullability information from field annotations.
     *
     * @param field the field node
     * @return the determined nullability
     */
    private Nullability extractNullability(FieldNode field) {
        // Check for @Nullable or @NonNull annotations
        boolean hasNullable = field.annotations().stream()
                .anyMatch(
                        a -> a.simpleName().equals("Nullable") || a.simpleName().equals("CheckForNull"));
        if (hasNullable) {
            return Nullability.NULLABLE;
        }

        boolean hasNonNull = field.annotations().stream()
                .anyMatch(a -> a.simpleName().equals("NonNull")
                        || a.simpleName().equals("NotNull")
                        || a.simpleName().equals("Nonnull"));
        if (hasNonNull) {
            return Nullability.NON_NULL;
        }

        // Optionals are nullable by design
        if (field.type().isOptionalLike()) {
            return Nullability.NULLABLE;
        }

        // Primitives are never null
        if (PRIMITIVE_TYPES.contains(field.type().rawQualifiedName())) {
            return Nullability.NON_NULL;
        }

        return Nullability.UNKNOWN;
    }
}
