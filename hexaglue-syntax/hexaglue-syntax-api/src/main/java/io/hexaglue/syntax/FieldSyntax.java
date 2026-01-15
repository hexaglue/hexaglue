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

package io.hexaglue.syntax;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Syntactic representation of a field.
 *
 * @since 4.0.0
 */
public interface FieldSyntax {

    /**
     * Returns the field name.
     *
     * @return the field name
     */
    String name();

    /**
     * Returns the field type.
     *
     * @return the type reference
     */
    TypeRef type();

    /**
     * Returns the field modifiers.
     *
     * @return an immutable set of modifiers
     */
    Set<Modifier> modifiers();

    /**
     * Returns the annotations on this field.
     *
     * @return an immutable list of annotations
     */
    List<AnnotationSyntax> annotations();

    /**
     * Returns the initializer expression, if present.
     *
     * <p>The expression is returned as source code string.</p>
     *
     * @return an Optional containing the initializer expression
     */
    Optional<String> initializer();

    /**
     * Returns the source location of this field.
     *
     * @return the source location
     */
    SourceLocation sourceLocation();

    // ===== Convenience methods =====

    /**
     * Returns whether this field has a specific annotation.
     *
     * @param annotationQualifiedName the qualified name of the annotation
     * @return true if the field has the annotation
     */
    default boolean hasAnnotation(String annotationQualifiedName) {
        return annotations().stream().anyMatch(a -> a.qualifiedName().equals(annotationQualifiedName));
    }

    /**
     * Returns the annotation with the given name, if present.
     *
     * @param annotationQualifiedName the qualified name of the annotation
     * @return an Optional containing the annotation
     */
    default Optional<AnnotationSyntax> getAnnotation(String annotationQualifiedName) {
        return annotations().stream()
                .filter(a -> a.qualifiedName().equals(annotationQualifiedName))
                .findFirst();
    }

    /**
     * Returns whether this field is static.
     *
     * @return true if static
     */
    default boolean isStatic() {
        return modifiers().contains(Modifier.STATIC);
    }

    /**
     * Returns whether this field is final.
     *
     * @return true if final
     */
    default boolean isFinal() {
        return modifiers().contains(Modifier.FINAL);
    }

    /**
     * Returns whether this field is private.
     *
     * @return true if private
     */
    default boolean isPrivate() {
        return modifiers().contains(Modifier.PRIVATE);
    }
}
