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

package io.hexaglue.core.frontend;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A Java type (class, record, interface, enum, or annotation type).
 */
public interface JavaType extends JavaNamed, JavaAnnotated, JavaSourced {

    /**
     * Returns the syntactic form of this type.
     */
    JavaForm form();

    /**
     * Returns the modifiers on this type.
     */
    Set<JavaModifier> modifiers();

    /**
     * Returns the supertype, if any (empty for Object, interfaces, enums).
     */
    Optional<TypeRef> superType();

    /**
     * Returns the interfaces implemented/extended by this type.
     */
    List<TypeRef> interfaces();

    /**
     * Returns all members (fields, methods, constructors).
     */
    List<JavaMember> members();

    /**
     * Returns all fields.
     */
    default List<JavaField> fields() {
        return members().stream()
                .filter(m -> m instanceof JavaField)
                .map(m -> (JavaField) m)
                .toList();
    }

    /**
     * Returns all methods (excluding constructors).
     */
    default List<JavaMethod> methods() {
        return members().stream()
                .filter(m -> m instanceof JavaMethod)
                .map(m -> (JavaMethod) m)
                .toList();
    }

    /**
     * Returns all constructors.
     */
    default List<JavaConstructor> constructors() {
        return members().stream()
                .filter(m -> m instanceof JavaConstructor)
                .map(m -> (JavaConstructor) m)
                .toList();
    }

    /**
     * Returns a field by name, if present.
     */
    default Optional<JavaField> field(String name) {
        return fields().stream().filter(f -> f.simpleName().equals(name)).findFirst();
    }

    /**
     * Returns true if this type has a field with the given name.
     */
    default boolean hasField(String name) {
        return field(name).isPresent();
    }

    /**
     * Returns true if this type is a record.
     */
    default boolean isRecord() {
        return form() == JavaForm.RECORD;
    }

    /**
     * Returns true if this type is an interface.
     */
    default boolean isInterface() {
        return form() == JavaForm.INTERFACE;
    }

    /**
     * Returns true if this type is an enum.
     */
    default boolean isEnum() {
        return form() == JavaForm.ENUM;
    }

    /**
     * Returns true if this type is public.
     */
    default boolean isPublic() {
        return modifiers().contains(JavaModifier.PUBLIC);
    }

    /**
     * Returns true if this type is abstract.
     */
    default boolean isAbstract() {
        return modifiers().contains(JavaModifier.ABSTRACT);
    }

    /**
     * Returns true if this type is final.
     */
    default boolean isFinal() {
        return modifiers().contains(JavaModifier.FINAL);
    }

    /**
     * Returns true if this type appears to be immutable.
     *
     * <p>A type is considered immutable if:
     * <ul>
     *   <li>It's a record, OR</li>
     *   <li>It's final with all fields being final</li>
     * </ul>
     */
    default boolean isImmutable() {
        if (isRecord()) {
            return true;
        }
        if (!isFinal()) {
            return false;
        }
        return fields().stream().allMatch(JavaField::isFinal);
    }

    /**
     * Returns true if this type has an "id" field (identity candidate).
     */
    default boolean hasIdField() {
        return fields().stream()
                .anyMatch(f -> f.simpleName().equals("id") || f.simpleName().endsWith("Id"));
    }

    /**
     * Returns the identity field candidate, if any.
     */
    default Optional<JavaField> identityField() {
        // First try exact match "id"
        Optional<JavaField> idField = field("id");
        if (idField.isPresent()) {
            return idField;
        }
        // Then try pattern *Id
        return fields().stream().filter(f -> f.simpleName().endsWith("Id")).findFirst();
    }
}
