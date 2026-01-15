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

package io.hexaglue.spi.ir;

import java.util.Optional;

/**
 * Information about a relationship between domain types.
 *
 * <p>This captures the metadata needed for JPA relationship mapping.
 *
 * @param kind the relationship kind (ONE_TO_ONE, ONE_TO_MANY, etc.)
 * @param targetType the fully qualified name of the target type
 * @param mappedBy the name of the field on the inverse side (for bidirectional relationships)
 * @param owning true if this is the owning side of the relationship
 * @param cascade the cascade type for this relationship (computed by Core)
 * @param fetch the fetch type for this relationship (computed by Core)
 * @param targetKind the domain kind of the target type (AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, etc.)
 */
public record RelationInfo(
        RelationKind kind,
        String targetType,
        String mappedBy,
        boolean owning,
        CascadeType cascade,
        FetchType fetch,
        DomainKind targetKind) {

    /**
     * Creates a relation info for a unidirectional relationship (owning by default).
     *
     * @deprecated Use {@link #unidirectional(RelationKind, String, CascadeType, FetchType, DomainKind)} instead.
     */
    @Deprecated
    public static RelationInfo unidirectional(RelationKind kind, String targetType) {
        return new RelationInfo(kind, targetType, null, true, CascadeType.NONE, FetchType.LAZY, null);
    }

    /**
     * Creates a relation info for a unidirectional relationship with full metadata.
     *
     * @param kind the relationship kind
     * @param targetType the target type qualified name
     * @param cascade the cascade type
     * @param fetch the fetch type
     * @param targetKind the domain kind of the target type
     * @return a new RelationInfo
     * @since 3.0.0
     */
    public static RelationInfo unidirectional(
            RelationKind kind, String targetType, CascadeType cascade, FetchType fetch, DomainKind targetKind) {
        return new RelationInfo(kind, targetType, null, true, cascade, fetch, targetKind);
    }

    /**
     * Creates a relation info for the owning side of a bidirectional relationship.
     *
     * @deprecated Use {@link #owning(RelationKind, String, CascadeType, FetchType, DomainKind)} instead.
     */
    @Deprecated
    public static RelationInfo owning(RelationKind kind, String targetType) {
        return new RelationInfo(kind, targetType, null, true, CascadeType.NONE, FetchType.LAZY, null);
    }

    /**
     * Creates a relation info for the owning side with full metadata.
     *
     * @param kind the relationship kind
     * @param targetType the target type qualified name
     * @param cascade the cascade type
     * @param fetch the fetch type
     * @param targetKind the domain kind of the target type
     * @return a new RelationInfo
     * @since 3.0.0
     */
    public static RelationInfo owning(
            RelationKind kind, String targetType, CascadeType cascade, FetchType fetch, DomainKind targetKind) {
        return new RelationInfo(kind, targetType, null, true, cascade, fetch, targetKind);
    }

    /**
     * Creates a relation info for the inverse side of a bidirectional relationship.
     *
     * @deprecated Use {@link #inverse(RelationKind, String, String, CascadeType, FetchType, DomainKind)} instead.
     */
    @Deprecated
    public static RelationInfo inverse(RelationKind kind, String targetType, String mappedBy) {
        return new RelationInfo(kind, targetType, mappedBy, false, CascadeType.NONE, FetchType.LAZY, null);
    }

    /**
     * Creates a relation info for the inverse side with full metadata.
     *
     * @param kind the relationship kind
     * @param targetType the target type qualified name
     * @param mappedBy the field name on the owning side
     * @param cascade the cascade type
     * @param fetch the fetch type
     * @param targetKind the domain kind of the target type
     * @return a new RelationInfo
     * @since 3.0.0
     */
    public static RelationInfo inverse(
            RelationKind kind,
            String targetType,
            String mappedBy,
            CascadeType cascade,
            FetchType fetch,
            DomainKind targetKind) {
        return new RelationInfo(kind, targetType, mappedBy, false, cascade, fetch, targetKind);
    }

    /**
     * Returns the mappedBy field name wrapped in an Optional.
     *
     * <p>Note: The 'Opt' suffix is used because Java records auto-generate an accessor
     * method {@code mappedBy()} returning the raw {@link String} (nullable).
     * This method provides a null-safe alternative.
     *
     * @return the mappedBy field wrapped in Optional, or empty if unidirectional
     * @since 2.0.0
     */
    public Optional<String> mappedByOpt() {
        return Optional.ofNullable(mappedBy);
    }

    /**
     * Returns true if this is a bidirectional relationship.
     */
    public boolean isBidirectional() {
        return mappedBy != null;
    }

    /**
     * Returns true if this relationship is a collection (ONE_TO_MANY or MANY_TO_MANY).
     */
    public boolean isCollection() {
        return kind == RelationKind.ONE_TO_MANY
                || kind == RelationKind.MANY_TO_MANY
                || kind == RelationKind.ELEMENT_COLLECTION;
    }

    /**
     * Returns true if this is an embedded relationship.
     */
    public boolean isEmbedded() {
        return kind == RelationKind.EMBEDDED || kind == RelationKind.ELEMENT_COLLECTION;
    }
}
