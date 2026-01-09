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
import java.util.Objects;

/**
 * Analysis results for a field containing metadata and modifiers.
 *
 * <p>Field analysis extracts information about:
 * <ul>
 *   <li>Field type and generics</li>
 *   <li>Modifiers (public, private, final, static, etc.)</li>
 *   <li>Annotations</li>
 *   <li>Initialization expressions</li>
 * </ul>
 *
 * <p>This record caches field metadata to avoid repeated AST traversal.
 * It is immutable and thread-safe.
 *
 * <p>Example usage:
 * <pre>{@code
 * FieldAnalysis analysis = CachedSpoonAnalyzer.analyzeField(field);
 *
 * // Check if field is an identifier
 * if (analysis.hasIdAnnotation() || analysis.typeName().endsWith("Id")) {
 *     return ClassificationResult.ENTITY;
 * }
 *
 * // Check if field is a collection
 * if (analysis.isCollection()) {
 *     String elementType = analysis.collectionElementType().orElse("Object");
 * }
 * }</pre>
 *
 * @param typeName the qualified name of the field type
 * @param isCollection true if the type is a collection (List, Set, Map, etc.)
 * @param collectionElementType the element type if this is a collection, null otherwise
 * @param modifiers the field modifiers (public, private, final, static, etc.)
 * @param annotations the annotations applied to this field
 * @param hasInitializer true if the field has an initialization expression
 * @since 3.0.0
 */
public record FieldAnalysis(
        String typeName,
        boolean isCollection,
        String collectionElementType,
        List<String> modifiers,
        List<String> annotations,
        boolean hasInitializer) {

    public FieldAnalysis {
        Objects.requireNonNull(typeName, "typeName cannot be null");
        Objects.requireNonNull(modifiers, "modifiers cannot be null");
        Objects.requireNonNull(annotations, "annotations cannot be null");

        // Make defensive copies for immutability
        modifiers = List.copyOf(modifiers);
        annotations = List.copyOf(annotations);
    }

    /**
     * Returns true if the field has the @Id annotation (JPA entity identifier).
     *
     * @return true if annotated with @Id
     */
    public boolean hasIdAnnotation() {
        return annotations.stream()
                .anyMatch(anno -> anno.equals("javax.persistence.Id") || anno.equals("jakarta.persistence.Id"));
    }

    /**
     * Returns true if the field is final.
     *
     * @return true if final
     */
    public boolean isFinal() {
        return modifiers.contains("final");
    }

    /**
     * Returns true if the field is static.
     *
     * @return true if static
     */
    public boolean isStatic() {
        return modifiers.contains("static");
    }

    /**
     * Returns true if the field is public.
     *
     * @return true if public
     */
    public boolean isPublic() {
        return modifiers.contains("public");
    }

    /**
     * Returns true if the field is private.
     *
     * @return true if private
     */
    public boolean isPrivate() {
        return modifiers.contains("private");
    }

    /**
     * Returns true if the field is protected.
     *
     * @return true if protected
     */
    public boolean isProtected() {
        return modifiers.contains("protected");
    }

    /**
     * Returns the collection element type if this is a collection field, wrapped in Optional.
     *
     * @return the element type, or empty if not a collection
     */
    public java.util.Optional<String> optionalCollectionElementType() {
        return java.util.Optional.ofNullable(collectionElementType);
    }

    /**
     * Returns true if the field type name ends with "Id" (likely an identifier).
     *
     * @return true if the type name suggests an identifier
     */
    public boolean isLikelyIdentifier() {
        return typeName.endsWith("Id") || hasIdAnnotation();
    }

    /**
     * Returns true if the field is an immutable value (final and has initializer).
     *
     * @return true if immutable
     */
    public boolean isImmutableValue() {
        return isFinal() && hasInitializer;
    }
}
