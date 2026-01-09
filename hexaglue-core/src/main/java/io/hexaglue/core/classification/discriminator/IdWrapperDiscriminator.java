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

package io.hexaglue.core.classification.discriminator;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtRecordComponent;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Discriminator for detecting ID wrapper types.
 *
 * <p>An ID wrapper is a type that encapsulates a single primitive identity value,
 * following the type-safe ID pattern commonly used in DDD. These types are typically:
 * <ul>
 *   <li>Named with "Id" or "ID" suffix (e.g., OrderId, CustomerID)</li>
 *   <li>Contain exactly one field/component of a primitive ID type</li>
 *   <li>Often implemented as records for immutability</li>
 * </ul>
 *
 * <p>ID wrappers are important to detect because they:
 * <ul>
 *   <li>Should be classified as IDENTIFIER domain kind</li>
 *   <li>Indicate REFERENCE_BY_ID relationships when used as fields</li>
 *   <li>Provide type safety for aggregate references</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class IdWrapperDiscriminator {

    /**
     * Primitive types commonly used for identifiers.
     */
    private static final Set<String> PRIMITIVE_ID_TYPES =
            Set.of("java.util.UUID", "java.lang.String", "java.lang.Long", "long", "java.lang.Integer", "int");

    /**
     * Checks if the given type is an ID wrapper.
     *
     * <p>A type is considered an ID wrapper if:
     * <ol>
     *   <li>Its simple name ends with "Id" or "ID"</li>
     *   <li>It has exactly one field/component</li>
     *   <li>That field/component is of a primitive ID type</li>
     * </ol>
     *
     * @param type the type to check (can be class or record)
     * @return true if the type is an ID wrapper
     */
    public boolean isIdWrapper(CtType<?> type) {
        Objects.requireNonNull(type, "type required");

        // Check naming convention
        if (!hasIdNaming(type)) {
            return false;
        }

        // Check structure based on type kind
        if (type instanceof CtRecord record) {
            return isRecordIdWrapper(record);
        } else if (type instanceof CtClass<?> clazz) {
            return isClassIdWrapper(clazz);
        }

        return false;
    }

    /**
     * Checks if the type name follows ID wrapper naming convention.
     *
     * @param type the type to check
     * @return true if name ends with "Id" or "ID"
     */
    private boolean hasIdNaming(CtType<?> type) {
        String simpleName = type.getSimpleName();
        return simpleName.endsWith("Id") || simpleName.endsWith("ID");
    }

    /**
     * Checks if a record is an ID wrapper.
     *
     * @param record the record to check
     * @return true if the record has one component of primitive ID type
     */
    private boolean isRecordIdWrapper(CtRecord record) {
        Set<CtRecordComponent> components = record.getRecordComponents();

        // Must have exactly one component
        if (components.size() != 1) {
            return false;
        }

        CtRecordComponent component = components.iterator().next();
        return isPrimitiveIdType(component.getType());
    }

    /**
     * Checks if a class is an ID wrapper.
     *
     * @param clazz the class to check
     * @return true if the class has one non-static field of primitive ID type
     */
    private boolean isClassIdWrapper(CtClass<?> clazz) {
        List<CtField<?>> fields =
                clazz.getFields().stream().filter(f -> !f.isStatic()).toList();

        // Must have exactly one non-static field
        if (fields.size() != 1) {
            return false;
        }

        CtField<?> field = fields.get(0);
        return isPrimitiveIdType(field.getType());
    }

    /**
     * Checks if a type reference is a primitive ID type.
     *
     * @param typeRef the type reference to check
     * @return true if the type is a known primitive ID type
     */
    private boolean isPrimitiveIdType(CtTypeReference<?> typeRef) {
        if (typeRef == null) {
            return false;
        }

        String qualifiedName = typeRef.getQualifiedName();
        return PRIMITIVE_ID_TYPES.contains(qualifiedName);
    }

    /**
     * Returns the set of primitive ID types recognized by this discriminator.
     *
     * @return unmodifiable set of qualified type names
     */
    public Set<String> getPrimitiveIdTypes() {
        return PRIMITIVE_ID_TYPES;
    }

    /**
     * Extracts the wrapped ID type from an ID wrapper.
     *
     * <p>Returns the qualified name of the wrapped primitive type.
     *
     * @param type the ID wrapper type
     * @return the qualified name of the wrapped type, or null if not an ID wrapper
     */
    public String getWrappedIdType(CtType<?> type) {
        Objects.requireNonNull(type, "type required");

        if (!isIdWrapper(type)) {
            return null;
        }

        if (type instanceof CtRecord record) {
            return record.getRecordComponents().iterator().next().getType().getQualifiedName();
        } else if (type instanceof CtClass<?> clazz) {
            return clazz.getFields().stream()
                    .filter(f -> !f.isStatic())
                    .findFirst()
                    .map(f -> f.getType().getQualifiedName())
                    .orElse(null);
        }

        return null;
    }
}
