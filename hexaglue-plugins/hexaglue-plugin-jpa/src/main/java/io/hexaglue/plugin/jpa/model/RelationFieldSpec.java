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
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.spi.ir.CascadeType;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.FetchType;
import io.hexaglue.spi.ir.RelationKind;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * @param attributeOverrides list of attribute overrides for embedded fields with column name conflicts
 * @param isElementTypeEnum true if the element type is an enum (for ELEMENT_COLLECTION)
 * @since 2.0.0
 */
public record RelationFieldSpec(
        String fieldName,
        TypeName targetType,
        RelationKind kind,
        ElementKind targetKind,
        String mappedBy,
        CascadeType cascade,
        FetchType fetch,
        boolean orphanRemoval,
        List<AttributeOverride> attributeOverrides,
        boolean isElementTypeEnum) {

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
        attributeOverrides = attributeOverrides == null ? List.of() : List.copyOf(attributeOverrides);
    }

    /**
     * Convenience constructor without attribute overrides and isElementTypeEnum (backward compatibility).
     */
    public RelationFieldSpec(
            String fieldName,
            TypeName targetType,
            RelationKind kind,
            ElementKind targetKind,
            String mappedBy,
            CascadeType cascade,
            FetchType fetch,
            boolean orphanRemoval) {
        this(fieldName, targetType, kind, targetKind, mappedBy, cascade, fetch, orphanRemoval, List.of(), false);
    }

    /**
     * Convenience constructor without isElementTypeEnum (backward compatibility).
     */
    public RelationFieldSpec(
            String fieldName,
            TypeName targetType,
            RelationKind kind,
            ElementKind targetKind,
            String mappedBy,
            CascadeType cascade,
            FetchType fetch,
            boolean orphanRemoval,
            List<AttributeOverride> attributeOverrides) {
        this(fieldName, targetType, kind, targetKind, mappedBy, cascade, fetch, orphanRemoval, attributeOverrides, false);
    }

    /**
     * Returns true if this relation has attribute overrides.
     *
     * @return true if attributeOverrides is not empty
     */
    public boolean hasAttributeOverrides() {
        return !attributeOverrides.isEmpty();
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
        return from(relation, java.util.Map.of());
    }

    /**
     * Creates a RelationFieldSpec from a SPI DomainRelation with embeddable type mapping.
     *
     * <p>This factory method performs the conversion from the SPI domain model
     * to the JavaPoet-based generation model. For relationships targeting VALUE_OBJECTs,
     * the embeddableMapping is used to replace domain types with JPA embeddable types.
     *
     * @param relation the domain relation from the SPI
     * @param embeddableMapping map from domain type FQN to embeddable type FQN
     * @return a RelationFieldSpec ready for code generation
     */
    public static RelationFieldSpec from(DomainRelation relation, java.util.Map<String, String> embeddableMapping) {
        // For EMBEDDED and ELEMENT_COLLECTION, use embeddable type if available
        String targetFqn = relation.targetTypeFqn();
        if ((relation.kind() == RelationKind.EMBEDDED || relation.kind() == RelationKind.ELEMENT_COLLECTION)
                && embeddableMapping.containsKey(targetFqn)) {
            targetFqn = embeddableMapping.get(targetFqn);
        }

        TypeName targetType = resolveTargetType(relation.kind(), targetFqn);

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
     * Creates a new RelationFieldSpec with the specified mappedBy value.
     *
     * <p>This is used for BUG-003 fix: automatically setting mappedBy on the inverse
     * side of bidirectional relationships detected by {@link io.hexaglue.plugin.jpa.util.BidirectionalDetector}.
     *
     * @param newMappedBy the mappedBy field name
     * @return a new RelationFieldSpec with mappedBy set
     * @since 5.0.0
     */
    public RelationFieldSpec withMappedBy(String newMappedBy) {
        return new RelationFieldSpec(
                fieldName,
                targetType,
                kind,
                targetKind,
                newMappedBy,
                cascade,
                fetch,
                orphanRemoval,
                attributeOverrides,
                isElementTypeEnum);
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
        return targetKind == ElementKind.AGGREGATE_ROOT || targetKind == ElementKind.ENTITY;
    }

    /**
     * Returns true if this relationship targets a value object.
     *
     * @return true if targetKind is VALUE_OBJECT
     */
    public boolean targetsValueObject() {
        return targetKind == ElementKind.VALUE_OBJECT;
    }

    /**
     * Creates a RelationFieldSpec from a v5 model Field with REFERENCE or COLLECTION role.
     *
     * <p>This factory method converts the v5 arch.model.Field to the JavaPoet-based
     * generation model. It detects relation kind from field roles and annotations,
     * and properly handles Value Object collections as ELEMENT_COLLECTION.
     *
     * @param field the field from arch.model (should have REFERENCE or COLLECTION role)
     * @param model the architectural model for type resolution
     * @param embeddableMapping map from domain FQN to embeddable FQN
     * @return a RelationFieldSpec ready for code generation
     * @since 5.0.0
     */
    public static RelationFieldSpec fromV5(
            Field field, ArchitecturalModel model, Map<String, String> embeddableMapping) {
        // Delegate to the new method with empty entityMapping for backward compatibility
        return fromV5(field, model, embeddableMapping, Map.of());
    }

    /**
     * Creates a RelationFieldSpec from a v5 model Field with REFERENCE or COLLECTION role.
     *
     * <p>This factory method converts the v5 arch.model.Field to the JavaPoet-based
     * generation model. It detects relation kind from field roles and annotations,
     * and properly handles:
     * <ul>
     *   <li>Value Object collections as ELEMENT_COLLECTION with embeddable mapping</li>
     *   <li>Entity relationships with entity mapping (BUG-008 fix)</li>
     * </ul>
     *
     * @param field the field from arch.model (should have REFERENCE or COLLECTION role)
     * @param model the architectural model for type resolution
     * @param embeddableMapping map from domain VALUE_OBJECT FQN to embeddable FQN
     * @param entityMapping map from domain AGGREGATE_ROOT/ENTITY FQN to entity FQN
     * @return a RelationFieldSpec ready for code generation
     * @since 2.0.0
     */
    public static RelationFieldSpec fromV5(
            Field field, ArchitecturalModel model, Map<String, String> embeddableMapping,
            Map<String, String> entityMapping) {

        String targetFqn = field.elementType()
                .map(t -> t.qualifiedName())
                .orElse(field.type().qualifiedName());

        // Determine target kind first - needed to detect ELEMENT_COLLECTION vs ONE_TO_MANY
        ElementKind targetKind = findElementKindV5(model, targetFqn);

        // Detect relation kind from field annotations, roles, and target kind
        RelationKind kind = detectRelationKindV5(field, targetKind);

        // Detect if element type is an enum (for ELEMENT_COLLECTION with @Enumerated)
        boolean isElementTypeEnum = kind == RelationKind.ELEMENT_COLLECTION
                && isEnumTypeV5(model, targetFqn);

        // For EMBEDDED and ELEMENT_COLLECTION, use embeddable type if available
        // Note: Enums don't use embeddable mapping
        if ((kind == RelationKind.EMBEDDED || kind == RelationKind.ELEMENT_COLLECTION)
                && !isElementTypeEnum
                && embeddableMapping.containsKey(targetFqn)) {
            targetFqn = embeddableMapping.get(targetFqn);
        }

        // BUG-008 fix: For entity relationships, use entity type instead of domain type
        // This is critical for ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY, ONE_TO_ONE
        if (isEntityRelation(kind) && entityMapping.containsKey(targetFqn)) {
            targetFqn = entityMapping.get(targetFqn);
        }

        TypeName targetType = resolveTargetType(kind, targetFqn);

        // Extract cascade, fetch, orphanRemoval from annotations
        CascadeType cascade = detectCascadeTypeV5(field);
        FetchType fetch = detectFetchTypeV5(field);
        boolean orphanRemoval = detectOrphanRemovalV5(field);
        String mappedBy = detectMappedByV5(field);

        return new RelationFieldSpec(
                field.name(), targetType, kind, targetKind, mappedBy, cascade, fetch, orphanRemoval,
                List.of(), isElementTypeEnum);
    }

    /**
     * Returns true if the relation kind represents an entity-to-entity relationship.
     *
     * <p>These relationships require the target type to be a JPA @Entity, not a domain type.
     *
     * @param kind the relation kind
     * @return true for ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY, ONE_TO_ONE
     * @since 2.0.0
     */
    private static boolean isEntityRelation(RelationKind kind) {
        return kind == RelationKind.ONE_TO_MANY
                || kind == RelationKind.MANY_TO_ONE
                || kind == RelationKind.MANY_TO_MANY
                || kind == RelationKind.ONE_TO_ONE;
    }

    /**
     * Detects relation kind from v5 Field annotations, roles, and target type.
     *
     * <p>For collections of Value Objects (targetKind == VALUE_OBJECT), uses
     * ELEMENT_COLLECTION instead of ONE_TO_MANY. This is critical for JPA
     * as @ElementCollection is required for embedded collections.
     *
     * @param field the field to analyze
     * @param targetKind the ElementKind of the target type
     * @return the appropriate RelationKind
     * @since 5.0.0
     */
    private static RelationKind detectRelationKindV5(Field field, ElementKind targetKind) {
        // Check for explicit JPA annotations first - they take precedence
        if (field.hasAnnotation("jakarta.persistence.OneToMany")
                || field.hasAnnotation("javax.persistence.OneToMany")) {
            return RelationKind.ONE_TO_MANY;
        }
        if (field.hasAnnotation("jakarta.persistence.ManyToOne")
                || field.hasAnnotation("javax.persistence.ManyToOne")) {
            return RelationKind.MANY_TO_ONE;
        }
        if (field.hasAnnotation("jakarta.persistence.OneToOne") || field.hasAnnotation("javax.persistence.OneToOne")) {
            return RelationKind.ONE_TO_ONE;
        }
        if (field.hasAnnotation("jakarta.persistence.ManyToMany")
                || field.hasAnnotation("javax.persistence.ManyToMany")) {
            return RelationKind.MANY_TO_MANY;
        }
        if (field.hasAnnotation("jakarta.persistence.ElementCollection")
                || field.hasAnnotation("javax.persistence.ElementCollection")) {
            return RelationKind.ELEMENT_COLLECTION;
        }
        if (field.hasAnnotation("jakarta.persistence.Embedded") || field.hasAnnotation("javax.persistence.Embedded")) {
            return RelationKind.EMBEDDED;
        }

        // Infer from roles and target kind
        if (field.hasRole(FieldRole.EMBEDDED)) {
            return RelationKind.EMBEDDED;
        }
        if (field.hasRole(FieldRole.COLLECTION)) {
            // Collections of VALUE_OBJECTs use @ElementCollection
            // Collections of entities use @OneToMany
            if (targetKind == ElementKind.VALUE_OBJECT || targetKind == ElementKind.IDENTIFIER) {
                return RelationKind.ELEMENT_COLLECTION;
            }
            return RelationKind.ONE_TO_MANY;
        }
        if (field.hasRole(FieldRole.AGGREGATE_REFERENCE)) {
            return RelationKind.MANY_TO_ONE; // Default for single references
        }

        // Fallback: if target is a VALUE_OBJECT, use EMBEDDED
        if (targetKind == ElementKind.VALUE_OBJECT || targetKind == ElementKind.IDENTIFIER) {
            return RelationKind.EMBEDDED;
        }

        return RelationKind.EMBEDDED; // Fallback
    }

    /**
     * Detects cascade type from v5 Field annotations.
     *
     * @since 5.0.0
     */
    private static CascadeType detectCascadeTypeV5(Field field) {
        var cascadeOpt = extractRelationAnnotationValue(field, "cascade");
        if (cascadeOpt.isEmpty()) {
            return CascadeType.NONE;
        }

        Object cascadeValue = cascadeOpt.get();
        if (cascadeValue instanceof String str) {
            if (str.contains("ALL")) return CascadeType.ALL;
            if (str.contains("PERSIST")) return CascadeType.PERSIST;
            if (str.contains("MERGE")) return CascadeType.MERGE;
            if (str.contains("REMOVE")) return CascadeType.REMOVE;
        }
        return CascadeType.NONE;
    }

    /**
     * Detects fetch type from v5 Field annotations.
     *
     * @since 5.0.0
     */
    private static FetchType detectFetchTypeV5(Field field) {
        var fetchOpt = extractRelationAnnotationValue(field, "fetch");
        if (fetchOpt.isEmpty()) {
            return FetchType.LAZY;
        }

        Object fetchValue = fetchOpt.get();
        if (fetchValue instanceof String str && str.contains("EAGER")) {
            return FetchType.EAGER;
        }
        return FetchType.LAZY;
    }

    /**
     * Detects orphan removal from v5 Field annotations.
     *
     * @since 5.0.0
     */
    private static boolean detectOrphanRemovalV5(Field field) {
        var orphanOpt = extractRelationAnnotationValue(field, "orphanRemoval");
        if (orphanOpt.isEmpty()) {
            return false;
        }

        Object value = orphanOpt.get();
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    /**
     * Detects mappedBy from v5 Field annotations.
     *
     * @since 5.0.0
     */
    private static String detectMappedByV5(Field field) {
        var mappedByOpt = extractRelationAnnotationValue(field, "mappedBy");
        if (mappedByOpt.isPresent()) {
            Object value = mappedByOpt.get();
            String str = String.valueOf(value);
            if (!str.isBlank() && !str.equals("null")) {
                return str;
            }
        }
        return null;
    }

    /**
     * Extracts an attribute value from a JPA relation annotation.
     *
     * @since 5.0.0
     */
    private static Optional<Object> extractRelationAnnotationValue(Field field, String attributeName) {
        var relationAnnotations = List.of(
                "jakarta.persistence.OneToMany", "javax.persistence.OneToMany",
                "jakarta.persistence.ManyToOne", "javax.persistence.ManyToOne",
                "jakarta.persistence.OneToOne", "javax.persistence.OneToOne",
                "jakarta.persistence.ManyToMany", "javax.persistence.ManyToMany",
                "jakarta.persistence.ElementCollection", "javax.persistence.ElementCollection");

        for (var ann : field.annotations()) {
            if (relationAnnotations.contains(ann.qualifiedName())) {
                return ann.getValue(attributeName);
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the ElementKind for a type using v5 domainIndex.
     *
     * @since 5.0.0
     */
    private static ElementKind findElementKindV5(ArchitecturalModel model, String qualifiedName) {
        var domainIndexOpt = model.domainIndex();
        if (domainIndexOpt.isPresent()) {
            var domainIndex = domainIndexOpt.get();

            // Check aggregate roots
            if (domainIndex
                    .aggregateRoots()
                    .anyMatch(agg -> agg.id().qualifiedName().equals(qualifiedName))) {
                return ElementKind.AGGREGATE_ROOT;
            }
            // Check entities
            if (domainIndex.entities().anyMatch(e -> e.id().qualifiedName().equals(qualifiedName))) {
                return ElementKind.ENTITY;
            }
            // Check value objects
            if (domainIndex
                    .valueObjects()
                    .anyMatch(vo -> vo.id().qualifiedName().equals(qualifiedName))) {
                return ElementKind.VALUE_OBJECT;
            }
            // Check identifiers - important for cross-aggregate references like CustomerId
            if (domainIndex
                    .identifiers()
                    .anyMatch(id -> id.id().qualifiedName().equals(qualifiedName))) {
                return ElementKind.IDENTIFIER;
            }
        }

        // Fallback to legacy method
        return findElementKindV4(model, qualifiedName);
    }

    /**
     * Finds the ElementKind for a type using the v5 model type registry.
     *
     * <p>Uses the new arch.model types (AggregateRoot, Entity, ValueObject) via
     * typeRegistry() when available, with a fallback to the legacy registry.
     *
     * @since 4.0.0
     * @since 5.0.0 - Uses typeRegistry() with new model types
     */
    private static ElementKind findElementKindV4(ArchitecturalModel model, String qualifiedName) {
        // Try v5 typeRegistry first
        var typeRegistryOpt = model.typeRegistry();
        if (typeRegistryOpt.isPresent()) {
            var typeRegistry = typeRegistryOpt.get();

            // Check if it's an aggregate root
            if (typeRegistry
                    .all(AggregateRoot.class)
                    .anyMatch(agg -> agg.id().qualifiedName().equals(qualifiedName))) {
                return ElementKind.AGGREGATE_ROOT;
            }
            // Check if it's an entity
            if (typeRegistry.all(Entity.class).anyMatch(e -> e.id().qualifiedName()
                    .equals(qualifiedName))) {
                return ElementKind.ENTITY;
            }
            // Check if it's a value object
            if (typeRegistry
                    .all(ValueObject.class)
                    .anyMatch(vo -> vo.id().qualifiedName().equals(qualifiedName))) {
                return ElementKind.VALUE_OBJECT;
            }
        }

        // Default to VALUE_OBJECT for unknown types
        return ElementKind.VALUE_OBJECT;
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
     * @param kind the relation kind
     * @param targetTypeFqn the fully qualified target type name
     * @return the resolved JavaPoet TypeName (possibly wrapped in collection)
     */
    private static TypeName resolveTargetType(RelationKind kind, String targetTypeFqn) {
        ClassName targetClass = ClassName.bestGuess(targetTypeFqn);

        return switch (kind) {
            case ONE_TO_MANY, ELEMENT_COLLECTION -> ParameterizedTypeName.get(ClassName.get(List.class), targetClass);
            case MANY_TO_MANY -> ParameterizedTypeName.get(ClassName.get(Set.class), targetClass);
            case MANY_TO_ONE, ONE_TO_ONE, EMBEDDED -> targetClass;
        };
    }

    /**
     * Checks if a type is an enum in the domain model.
     *
     * <p>Enum types classified as VALUE_OBJECT with TypeNature.ENUM need
     * {@code @Enumerated(EnumType.STRING)} annotation when used in collections.
     *
     * @param model the architectural model
     * @param qualifiedName the fully qualified type name
     * @return true if the type is a VALUE_OBJECT with TypeNature.ENUM
     * @since 5.0.0
     */
    private static boolean isEnumTypeV5(ArchitecturalModel model, String qualifiedName) {
        var domainIndexOpt = model.domainIndex();
        if (domainIndexOpt.isEmpty()) {
            return false;
        }
        var domainIndex = domainIndexOpt.get();
        return domainIndex.valueObjects()
                .filter(vo -> vo.id().qualifiedName().equals(qualifiedName))
                .anyMatch(vo -> vo.structure() != null
                        && vo.structure().nature() == TypeNature.ENUM);
    }
}
