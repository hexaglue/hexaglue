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
 * A relationship between domain types.
 *
 * <p>This captures all the metadata needed for JPA relationship mapping.
 *
 * @param propertyName the name of the property on this type (e.g., "items")
 * @param kind the relationship kind (ONE_TO_ONE, ONE_TO_MANY, etc.)
 * @param targetTypeFqn the fully qualified name of the target type
 * @param targetKind the DDD classification of the target type
 * @param mappedBy the field name on the inverse side (null for unidirectional or owning side)
 * @param cascade the cascade operation type
 * @param fetch the fetch strategy
 * @param orphanRemoval true if orphaned children should be removed
 */
public record DomainRelation(
        String propertyName,
        RelationKind kind,
        String targetTypeFqn,
        DomainKind targetKind,
        String mappedBy,
        CascadeType cascade,
        FetchType fetch,
        boolean orphanRemoval) {

    /**
     * Creates a relation for an embedded value object.
     */
    public static DomainRelation embedded(String propertyName, String targetTypeFqn) {
        return new DomainRelation(
                propertyName,
                RelationKind.EMBEDDED,
                targetTypeFqn,
                DomainKind.VALUE_OBJECT,
                null,
                CascadeType.NONE,
                FetchType.EAGER,
                false);
    }

    /**
     * Creates a relation for a collection of entities (one-to-many).
     */
    public static DomainRelation oneToMany(String propertyName, String targetTypeFqn, DomainKind targetKind) {
        return new DomainRelation(
                propertyName,
                RelationKind.ONE_TO_MANY,
                targetTypeFqn,
                targetKind,
                null,
                CascadeType.ALL,
                FetchType.LAZY,
                true);
    }

    /**
     * Creates a relation for a reference to another aggregate (many-to-one).
     */
    public static DomainRelation manyToOne(String propertyName, String targetTypeFqn) {
        return new DomainRelation(
                propertyName,
                RelationKind.MANY_TO_ONE,
                targetTypeFqn,
                DomainKind.AGGREGATE_ROOT,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false);
    }

    /**
     * Returns the mappedBy field name as an Optional.
     */
    public Optional<String> mappedByOpt() {
        return Optional.ofNullable(mappedBy);
    }

    /**
     * Returns true if this is the owning side of the relationship.
     */
    public boolean isOwning() {
        return mappedBy == null;
    }

    /**
     * Returns true if this is a bidirectional relationship.
     */
    public boolean isBidirectional() {
        return mappedBy != null;
    }

    /**
     * Returns true if this relationship targets an entity.
     */
    public boolean targetsEntity() {
        return targetKind == DomainKind.ENTITY || targetKind == DomainKind.AGGREGATE_ROOT;
    }

    /**
     * Returns true if this relationship targets a value object.
     */
    public boolean targetsValueObject() {
        return targetKind == DomainKind.VALUE_OBJECT;
    }

    /**
     * Returns the simple name of the target type.
     */
    public String targetSimpleName() {
        int lastDot = targetTypeFqn.lastIndexOf('.');
        return lastDot < 0 ? targetTypeFqn : targetTypeFqn.substring(lastDot + 1);
    }
}
