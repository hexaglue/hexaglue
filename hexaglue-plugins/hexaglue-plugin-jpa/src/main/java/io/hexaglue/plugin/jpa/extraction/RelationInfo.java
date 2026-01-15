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

package io.hexaglue.plugin.jpa.extraction;

import io.hexaglue.syntax.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA relation information extracted from field annotations.
 *
 * <p>Captures all JPA relationship metadata including cascade, fetch, and mapping details.</p>
 *
 * @param fieldName the field name
 * @param relationKind the kind of relation (ONE_TO_MANY, etc.)
 * @param targetType the target type reference
 * @param cascade the cascade type (defaults to NONE)
 * @param fetch the fetch type (defaults based on relation)
 * @param mappedBy the mapped-by field for bidirectional relations (null if owning side)
 * @param orphanRemoval whether orphan removal is enabled
 * @since 4.0.0
 */
public record RelationInfo(
        String fieldName,
        RelationKind relationKind,
        TypeRef targetType,
        CascadeType cascade,
        FetchType fetch,
        String mappedBy,
        boolean orphanRemoval) {

    /**
     * Kinds of JPA relationships.
     */
    public enum RelationKind {
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_ONE,
        MANY_TO_MANY,
        EMBEDDED,
        ELEMENT_COLLECTION
    }

    /**
     * JPA cascade types.
     */
    public enum CascadeType {
        NONE,
        PERSIST,
        MERGE,
        REMOVE,
        REFRESH,
        DETACH,
        ALL
    }

    /**
     * JPA fetch types.
     */
    public enum FetchType {
        LAZY,
        EAGER
    }

    /**
     * Compact constructor with validation.
     */
    public RelationInfo {
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(relationKind, "relationKind must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        if (cascade == null) {
            cascade = CascadeType.NONE;
        }
        if (fetch == null) {
            fetch = defaultFetchFor(relationKind);
        }
    }

    /**
     * Returns the mapped-by value as an Optional.
     *
     * @return the mapped-by field name, or empty if owning side
     */
    public Optional<String> mappedByOpt() {
        return Optional.ofNullable(mappedBy);
    }

    /**
     * Returns whether this is the owning side of the relationship.
     *
     * @return true if owning side (no mappedBy)
     */
    public boolean isOwning() {
        return mappedBy == null || mappedBy.isEmpty();
    }

    /**
     * Returns whether this is a collection relationship.
     *
     * @return true for ONE_TO_MANY, MANY_TO_MANY, or ELEMENT_COLLECTION
     */
    public boolean isCollection() {
        return relationKind == RelationKind.ONE_TO_MANY
                || relationKind == RelationKind.MANY_TO_MANY
                || relationKind == RelationKind.ELEMENT_COLLECTION;
    }

    /**
     * Returns whether this is an embedded relationship.
     *
     * @return true for EMBEDDED or ELEMENT_COLLECTION
     */
    public boolean isEmbedded() {
        return relationKind == RelationKind.EMBEDDED || relationKind == RelationKind.ELEMENT_COLLECTION;
    }

    /**
     * Returns the default fetch type for a relation kind.
     *
     * @param kind the relation kind
     * @return LAZY for collections, EAGER for single relations
     */
    private static FetchType defaultFetchFor(RelationKind kind) {
        return switch (kind) {
            case ONE_TO_MANY, MANY_TO_MANY, ELEMENT_COLLECTION -> FetchType.LAZY;
            case ONE_TO_ONE, MANY_TO_ONE, EMBEDDED -> FetchType.EAGER;
        };
    }

    // ===== Factory methods =====

    /**
     * Creates a simple embedded relation.
     *
     * @param fieldName the field name
     * @param targetType the embedded type
     * @return a new RelationInfo
     */
    public static RelationInfo embedded(String fieldName, TypeRef targetType) {
        return new RelationInfo(
                fieldName, RelationKind.EMBEDDED, targetType, CascadeType.ALL, FetchType.EAGER, null, false);
    }

    /**
     * Creates a one-to-many relation with defaults.
     *
     * @param fieldName the field name
     * @param elementType the element type
     * @return a new RelationInfo
     */
    public static RelationInfo oneToMany(String fieldName, TypeRef elementType) {
        return new RelationInfo(
                fieldName, RelationKind.ONE_TO_MANY, elementType, CascadeType.ALL, FetchType.LAZY, null, true);
    }

    /**
     * Creates a many-to-one relation with defaults.
     *
     * @param fieldName the field name
     * @param targetType the target entity type
     * @return a new RelationInfo
     */
    public static RelationInfo manyToOne(String fieldName, TypeRef targetType) {
        return new RelationInfo(
                fieldName, RelationKind.MANY_TO_ONE, targetType, CascadeType.NONE, FetchType.LAZY, null, false);
    }

    /**
     * Creates an element collection relation.
     *
     * @param fieldName the field name
     * @param elementType the element type
     * @return a new RelationInfo
     */
    public static RelationInfo elementCollection(String fieldName, TypeRef elementType) {
        return new RelationInfo(
                fieldName, RelationKind.ELEMENT_COLLECTION, elementType, CascadeType.ALL, FetchType.LAZY, null, true);
    }
}
