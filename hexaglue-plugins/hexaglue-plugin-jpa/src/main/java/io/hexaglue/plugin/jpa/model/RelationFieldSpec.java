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

package io.hexaglue.plugin.jpa.model;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.spi.ir.CascadeType;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.FetchType;
import io.hexaglue.spi.ir.RelationKind;
import java.util.List;
import java.util.Set;

/**
 * Intermediate representation of a relationship field for JPA entity generation.
 *
 * <p>This record captures all metadata needed to generate JPA relationship annotations
 * such as {@code @OneToMany}, {@code @ManyToOne}, {@code @Embedded}, etc.
 *
 * <p>Design decision: Relationships are handled separately from simple properties
 * because they require different JPA annotations and have additional complexity
 * (cascade, fetch strategy, bidirectionality).
 *
 * @param fieldName the name of the field in the generated entity
 * @param targetType the JavaPoet type of the target entity/value object
 * @param kind the relationship kind (ONE_TO_MANY, MANY_TO_ONE, etc.)
 * @param targetKind the DDD classification of the target type
 * @param mappedBy the inverse field name for bidirectional relationships (null if owning side)
 * @param cascade the cascade operation type
 * @param fetch the fetch strategy (LAZY or EAGER)
 * @param orphanRemoval true if orphaned children should be removed
 * @since 2.0.0
 */
public record RelationFieldSpec(
        String fieldName,
        TypeName targetType,
        RelationKind kind,
        DomainKind targetKind,
        String mappedBy,
        CascadeType cascade,
        FetchType fetch,
        boolean orphanRemoval) {

    /**
     * Compact constructor with validation.
     *
     * <p>Ensures that mappedBy is either null (for owning side) or a non-empty string
     * (for inverse side). An empty string is not allowed as it's ambiguous.
     *
     * @throws IllegalArgumentException if mappedBy is an empty string
     */
    public RelationFieldSpec {
        if (mappedBy != null && mappedBy.isEmpty()) {
            throw new IllegalArgumentException("mappedBy cannot be empty (use null for owning side)");
        }
    }

    /**
     * Creates a RelationFieldSpec from a SPI DomainRelation.
     *
     * <p>This factory method performs the conversion from the SPI domain model
     * to the JavaPoet-based generation model. It handles:
     * <ul>
     *   <li>Collection type wrapping (List, Set) for one-to-many relationships</li>
     *   <li>Target type resolution from fully qualified name</li>
     *   <li>Default values for bidirectionality metadata</li>
     * </ul>
     *
     * @param relation the domain relation from the SPI
     * @return a RelationFieldSpec ready for code generation
     */
    public static RelationFieldSpec from(DomainRelation relation) {
        TypeName targetType = resolveTargetType(relation);

        return new RelationFieldSpec(
                relation.propertyName(),
                targetType,
                relation.kind(),
                relation.targetKind(),
                relation.mappedBy(),
                relation.cascade(),
                relation.fetch(),
                relation.orphanRemoval());
    }

    /**
     * Returns true if this relationship is embedded (value object).
     *
     * <p>Embedded relationships use {@code @Embedded} or {@code @ElementCollection}
     * annotations instead of standard JPA relationship annotations.
     *
     * @return true if kind is EMBEDDED or ELEMENT_COLLECTION
     */
    public boolean isEmbedded() {
        return kind == RelationKind.EMBEDDED || kind == RelationKind.ELEMENT_COLLECTION;
    }

    /**
     * Returns true if this is the owning side of the relationship.
     *
     * <p>The owning side is responsible for managing the relationship in the database.
     * A relationship is owning if mappedBy is null.
     *
     * @return true if mappedBy is null
     */
    public boolean isOwning() {
        return mappedBy == null;
    }

    /**
     * Returns true if this is a bidirectional relationship.
     *
     * <p>Bidirectional relationships have a mappedBy value pointing to the inverse field.
     *
     * @return true if mappedBy is not null
     */
    public boolean isBidirectional() {
        return mappedBy != null;
    }

    /**
     * Returns true if this relationship is a collection (one-to-many, many-to-many, element collection).
     *
     * @return true if the field type is a collection
     */
    public boolean isCollection() {
        return kind == RelationKind.ONE_TO_MANY
                || kind == RelationKind.MANY_TO_MANY
                || kind == RelationKind.ELEMENT_COLLECTION;
    }

    /**
     * Returns true if this relationship targets an entity (aggregate root or entity).
     *
     * @return true if targetKind is AGGREGATE_ROOT or ENTITY
     */
    public boolean targetsEntity() {
        return targetKind == DomainKind.AGGREGATE_ROOT || targetKind == DomainKind.ENTITY;
    }

    /**
     * Returns true if this relationship targets a value object.
     *
     * @return true if targetKind is VALUE_OBJECT
     */
    public boolean targetsValueObject() {
        return targetKind == DomainKind.VALUE_OBJECT;
    }

    /**
     * Resolves the target type including collection wrapping if necessary.
     *
     * <p>For collection relationships (one-to-many, element collection), wraps
     * the target type in a List. Uses Set for many-to-many relationships to
     * avoid duplicate entries.
     *
     * <p>Examples:
     * <ul>
     *   <li>ONE_TO_MANY: {@code List<LineItem>}</li>
     *   <li>MANY_TO_MANY: {@code Set<Category>}</li>
     *   <li>MANY_TO_ONE: {@code Order}</li>
     *   <li>EMBEDDED: {@code Address}</li>
     * </ul>
     *
     * @param relation the domain relation
     * @return the resolved JavaPoet TypeName (possibly wrapped in collection)
     */
    private static TypeName resolveTargetType(DomainRelation relation) {
        ClassName targetClass = ClassName.bestGuess(relation.targetTypeFqn());

        return switch (relation.kind()) {
            case ONE_TO_MANY, ELEMENT_COLLECTION -> ParameterizedTypeName.get(ClassName.get(List.class), targetClass);
            case MANY_TO_MANY -> ParameterizedTypeName.get(ClassName.get(Set.class), targetClass);
            case MANY_TO_ONE, ONE_TO_ONE, EMBEDDED -> targetClass;
        };
    }
}
