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

import io.hexaglue.syntax.AnnotationSyntax;
import io.hexaglue.syntax.AnnotationValue;
import io.hexaglue.syntax.FieldSyntax;
import io.hexaglue.syntax.TypeRef;
import io.hexaglue.syntax.TypeSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Extracts JPA metadata from TypeSyntax annotations.
 *
 * <p>This utility class extracts identity, relation, and property information
 * from JPA annotations on domain type fields, enabling direct generation from
 * the v4 ArchitecturalModel without the legacy IrSnapshot.</p>
 *
 * <h2>Supported Annotations</h2>
 * <ul>
 *   <li>Identity: {@code @Id}, {@code @EmbeddedId}, {@code @GeneratedValue}</li>
 *   <li>Relations: {@code @OneToMany}, {@code @ManyToOne}, {@code @OneToOne},
 *       {@code @ManyToMany}, {@code @Embedded}, {@code @ElementCollection}</li>
 *   <li>Properties: {@code @Column}</li>
 * </ul>
 *
 * @since 4.0.0
 */
public final class JpaAnnotationExtractor {

    // JPA annotation qualified names
    private static final String JPA_ID = "jakarta.persistence.Id";
    private static final String JPA_EMBEDDED_ID = "jakarta.persistence.EmbeddedId";
    private static final String JPA_GENERATED_VALUE = "jakarta.persistence.GeneratedValue";
    private static final String JPA_ONE_TO_MANY = "jakarta.persistence.OneToMany";
    private static final String JPA_MANY_TO_ONE = "jakarta.persistence.ManyToOne";
    private static final String JPA_ONE_TO_ONE = "jakarta.persistence.OneToOne";
    private static final String JPA_MANY_TO_MANY = "jakarta.persistence.ManyToMany";
    private static final String JPA_EMBEDDED = "jakarta.persistence.Embedded";
    private static final String JPA_ELEMENT_COLLECTION = "jakarta.persistence.ElementCollection";
    private static final String JPA_COLUMN = "jakarta.persistence.Column";

    // Legacy javax.persistence support
    private static final String JAVAX_ID = "javax.persistence.Id";
    private static final String JAVAX_EMBEDDED_ID = "javax.persistence.EmbeddedId";
    private static final String JAVAX_GENERATED_VALUE = "javax.persistence.GeneratedValue";
    private static final String JAVAX_ONE_TO_MANY = "javax.persistence.OneToMany";
    private static final String JAVAX_MANY_TO_ONE = "javax.persistence.ManyToOne";
    private static final String JAVAX_ONE_TO_ONE = "javax.persistence.OneToOne";
    private static final String JAVAX_MANY_TO_MANY = "javax.persistence.ManyToMany";
    private static final String JAVAX_EMBEDDED = "javax.persistence.Embedded";
    private static final String JAVAX_ELEMENT_COLLECTION = "javax.persistence.ElementCollection";
    private static final String JAVAX_COLUMN = "javax.persistence.Column";

    private JpaAnnotationExtractor() {
        // Utility class
    }

    // ===== Identity Extraction =====

    /**
     * Extracts identity information from a type's fields.
     *
     * @param type the type syntax to analyze
     * @return an Optional containing the identity info, or empty if no @Id found
     */
    public static Optional<IdentityInfo> extractIdentity(TypeSyntax type) {
        Objects.requireNonNull(type, "type must not be null");

        for (FieldSyntax field : type.fields()) {
            // Check for @EmbeddedId first
            if (hasAnyAnnotation(field, JPA_EMBEDDED_ID, JAVAX_EMBEDDED_ID)) {
                return Optional.of(IdentityInfo.embeddedId(field.name(), field.type()));
            }

            // Check for @Id
            if (hasAnyAnnotation(field, JPA_ID, JAVAX_ID)) {
                IdentityInfo.GenerationStrategy strategy = extractGenerationStrategy(field);
                return Optional.of(new IdentityInfo(field.name(), field.type(), strategy, false, null));
            }
        }

        return Optional.empty();
    }

    /**
     * Extracts identity information with wrapped type detection.
     *
     * <p>This method detects value object identities (e.g., OrderId wrapping Long)
     * by examining the identity type's fields.</p>
     *
     * @param type the type syntax to analyze
     * @param identityTypeSyntax the syntax of the identity type (for wrapped detection)
     * @return an Optional containing the identity info with wrapped type, or empty if no @Id found
     */
    public static Optional<IdentityInfo> extractIdentityWithWrapped(TypeSyntax type, TypeSyntax identityTypeSyntax) {
        Optional<IdentityInfo> basicIdentity = extractIdentity(type);

        if (basicIdentity.isEmpty() || identityTypeSyntax == null) {
            return basicIdentity;
        }

        IdentityInfo identity = basicIdentity.get();

        // Check if identity type wraps a primitive (single field, not embedded)
        if (!identity.embedded() && identityTypeSyntax.fields().size() == 1) {
            FieldSyntax wrappedField = identityTypeSyntax.fields().get(0);
            TypeRef wrappedType = wrappedField.type();

            // Common wrapped types
            if (isWrappableType(wrappedType)) {
                return Optional.of(IdentityInfo.wrappedId(
                        identity.fieldName(), identity.idType(), wrappedType, identity.strategy()));
            }
        }

        return basicIdentity;
    }

    private static IdentityInfo.GenerationStrategy extractGenerationStrategy(FieldSyntax field) {
        Optional<AnnotationSyntax> genValue = getAnyAnnotation(field, JPA_GENERATED_VALUE, JAVAX_GENERATED_VALUE);

        if (genValue.isEmpty()) {
            return null;
        }

        AnnotationSyntax ann = genValue.get();
        Optional<AnnotationValue> strategyValue = ann.getValue("strategy");

        if (strategyValue.isEmpty()) {
            return IdentityInfo.GenerationStrategy.AUTO;
        }

        if (strategyValue.get() instanceof AnnotationValue.EnumValue enumValue) {
            return switch (enumValue.constantName()) {
                case "IDENTITY" -> IdentityInfo.GenerationStrategy.IDENTITY;
                case "SEQUENCE" -> IdentityInfo.GenerationStrategy.SEQUENCE;
                case "TABLE" -> IdentityInfo.GenerationStrategy.TABLE;
                case "UUID" -> IdentityInfo.GenerationStrategy.UUID;
                default -> IdentityInfo.GenerationStrategy.AUTO;
            };
        }

        return IdentityInfo.GenerationStrategy.AUTO;
    }

    private static boolean isWrappableType(TypeRef type) {
        String qn = type.qualifiedName();
        return qn.equals("java.lang.Long")
                || qn.equals("java.lang.Integer")
                || qn.equals("java.lang.String")
                || qn.equals("java.util.UUID")
                || qn.equals("long")
                || qn.equals("int");
    }

    // ===== Relation Extraction =====

    /**
     * Extracts all relation information from a type's fields.
     *
     * @param type the type syntax to analyze
     * @return a list of relation infos (may be empty)
     */
    public static List<RelationInfo> extractRelations(TypeSyntax type) {
        Objects.requireNonNull(type, "type must not be null");

        List<RelationInfo> relations = new ArrayList<>();

        for (FieldSyntax field : type.fields()) {
            extractRelation(field).ifPresent(relations::add);
        }

        return List.copyOf(relations);
    }

    /**
     * Extracts relation information from a single field.
     *
     * @param field the field syntax to analyze
     * @return an Optional containing the relation info, or empty if not a relation
     */
    public static Optional<RelationInfo> extractRelation(FieldSyntax field) {
        Objects.requireNonNull(field, "field must not be null");

        // Check each relation type
        Optional<AnnotationSyntax> ann;

        // @OneToMany
        ann = getAnyAnnotation(field, JPA_ONE_TO_MANY, JAVAX_ONE_TO_MANY);
        if (ann.isPresent()) {
            return Optional.of(buildRelation(field, RelationInfo.RelationKind.ONE_TO_MANY, ann.get()));
        }

        // @ManyToOne
        ann = getAnyAnnotation(field, JPA_MANY_TO_ONE, JAVAX_MANY_TO_ONE);
        if (ann.isPresent()) {
            return Optional.of(buildRelation(field, RelationInfo.RelationKind.MANY_TO_ONE, ann.get()));
        }

        // @OneToOne
        ann = getAnyAnnotation(field, JPA_ONE_TO_ONE, JAVAX_ONE_TO_ONE);
        if (ann.isPresent()) {
            return Optional.of(buildRelation(field, RelationInfo.RelationKind.ONE_TO_ONE, ann.get()));
        }

        // @ManyToMany
        ann = getAnyAnnotation(field, JPA_MANY_TO_MANY, JAVAX_MANY_TO_MANY);
        if (ann.isPresent()) {
            return Optional.of(buildRelation(field, RelationInfo.RelationKind.MANY_TO_MANY, ann.get()));
        }

        // @Embedded
        ann = getAnyAnnotation(field, JPA_EMBEDDED, JAVAX_EMBEDDED);
        if (ann.isPresent()) {
            return Optional.of(RelationInfo.embedded(field.name(), field.type()));
        }

        // @ElementCollection
        ann = getAnyAnnotation(field, JPA_ELEMENT_COLLECTION, JAVAX_ELEMENT_COLLECTION);
        if (ann.isPresent()) {
            TypeRef elementType = extractCollectionElementType(field.type());
            return Optional.of(RelationInfo.elementCollection(field.name(), elementType));
        }

        return Optional.empty();
    }

    private static RelationInfo buildRelation(
            FieldSyntax field, RelationInfo.RelationKind kind, AnnotationSyntax annotation) {

        TypeRef targetType = extractTargetType(field, annotation);
        RelationInfo.CascadeType cascade = extractCascade(annotation);
        RelationInfo.FetchType fetch = extractFetch(annotation);
        String mappedBy = annotation.getString("mappedBy").orElse(null);
        boolean orphanRemoval = annotation.getBoolean("orphanRemoval").orElse(false);

        return new RelationInfo(field.name(), kind, targetType, cascade, fetch, mappedBy, orphanRemoval);
    }

    private static TypeRef extractTargetType(FieldSyntax field, AnnotationSyntax annotation) {
        // First check targetEntity attribute
        Optional<TypeRef> targetEntity = annotation.getClass("targetEntity");
        if (targetEntity.isPresent()
                && !targetEntity.get().qualifiedName().equals("void")
                && !targetEntity.get().qualifiedName().equals("java.lang.Void")) {
            return targetEntity.get();
        }

        // Otherwise, extract from field type
        return extractCollectionElementType(field.type());
    }

    private static TypeRef extractCollectionElementType(TypeRef type) {
        // For collections, the type argument is the element type
        if (!type.typeArguments().isEmpty()) {
            return type.typeArguments().get(0);
        }
        // For non-collections, return the type itself
        return type;
    }

    private static RelationInfo.CascadeType extractCascade(AnnotationSyntax annotation) {
        Optional<AnnotationValue> cascadeValue = annotation.getValue("cascade");

        if (cascadeValue.isEmpty()) {
            return RelationInfo.CascadeType.NONE;
        }

        AnnotationValue value = cascadeValue.get();

        // Single enum value
        if (value instanceof AnnotationValue.EnumValue enumValue) {
            return mapCascadeType(enumValue.constantName());
        }

        // Array of enum values
        if (value instanceof AnnotationValue.ArrayValue arrayValue) {
            // If ALL is present, return ALL
            for (AnnotationValue v : arrayValue.values()) {
                if (v instanceof AnnotationValue.EnumValue ev && "ALL".equals(ev.constantName())) {
                    return RelationInfo.CascadeType.ALL;
                }
            }
            // Otherwise return the first one (simplified)
            if (!arrayValue.values().isEmpty()
                    && arrayValue.values().get(0) instanceof AnnotationValue.EnumValue firstEnum) {
                return mapCascadeType(firstEnum.constantName());
            }
        }

        return RelationInfo.CascadeType.NONE;
    }

    private static RelationInfo.CascadeType mapCascadeType(String name) {
        return switch (name) {
            case "ALL" -> RelationInfo.CascadeType.ALL;
            case "PERSIST" -> RelationInfo.CascadeType.PERSIST;
            case "MERGE" -> RelationInfo.CascadeType.MERGE;
            case "REMOVE" -> RelationInfo.CascadeType.REMOVE;
            case "REFRESH" -> RelationInfo.CascadeType.REFRESH;
            case "DETACH" -> RelationInfo.CascadeType.DETACH;
            default -> RelationInfo.CascadeType.NONE;
        };
    }

    private static RelationInfo.FetchType extractFetch(AnnotationSyntax annotation) {
        Optional<AnnotationValue> fetchValue = annotation.getValue("fetch");

        if (fetchValue.isEmpty()) {
            return null; // Let RelationInfo use default based on relation kind
        }

        if (fetchValue.get() instanceof AnnotationValue.EnumValue enumValue) {
            return switch (enumValue.constantName()) {
                case "LAZY" -> RelationInfo.FetchType.LAZY;
                case "EAGER" -> RelationInfo.FetchType.EAGER;
                default -> null;
            };
        }

        return null;
    }

    // ===== Property Extraction =====

    /**
     * Extracts property information from all non-relation, non-identity fields.
     *
     * <p>Collection fields are also skipped as they are treated as implicit relations.</p>
     *
     * @param type the type syntax to analyze
     * @return a list of property infos (may be empty)
     */
    public static List<PropertyInfo> extractProperties(TypeSyntax type) {
        Objects.requireNonNull(type, "type must not be null");

        List<PropertyInfo> properties = new ArrayList<>();

        for (FieldSyntax field : type.fields()) {
            // Skip static fields
            if (field.isStatic()) {
                continue;
            }

            // Skip identity fields
            if (hasAnyAnnotation(field, JPA_ID, JAVAX_ID, JPA_EMBEDDED_ID, JAVAX_EMBEDDED_ID)) {
                continue;
            }

            // Skip relation fields (with explicit JPA annotations)
            if (extractRelation(field).isPresent()) {
                continue;
            }

            // Skip collection fields (they become implicit relations)
            if (isCollectionType(field.type())) {
                continue;
            }

            properties.add(extractProperty(field));
        }

        return List.copyOf(properties);
    }

    /**
     * Extracts property information from a field.
     *
     * @param field the field syntax to analyze
     * @return the property info
     */
    public static PropertyInfo extractProperty(FieldSyntax field) {
        Objects.requireNonNull(field, "field must not be null");

        Optional<AnnotationSyntax> columnAnn = getAnyAnnotation(field, JPA_COLUMN, JAVAX_COLUMN);

        if (columnAnn.isEmpty()) {
            return PropertyInfo.simple(field.name(), field.type());
        }

        AnnotationSyntax ann = columnAnn.get();

        String columnName = ann.getString("name").filter(s -> !s.isEmpty()).orElse(null);
        boolean nullable = ann.getBoolean("nullable").orElse(true);
        boolean unique = ann.getBoolean("unique").orElse(false);
        Integer length = ann.getInt("length").filter(l -> l != 255).orElse(null);
        Integer precision = ann.getInt("precision").filter(p -> p != 0).orElse(null);
        Integer scale = ann.getInt("scale").filter(s -> s != 0).orElse(null);

        return new PropertyInfo(field.name(), field.type(), columnName, nullable, unique, length, precision, scale);
    }

    // ===== Helper Methods =====

    private static boolean hasAnyAnnotation(FieldSyntax field, String... qualifiedNames) {
        for (String qn : qualifiedNames) {
            if (field.hasAnnotation(qn)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<AnnotationSyntax> getAnyAnnotation(FieldSyntax field, String... qualifiedNames) {
        for (String qn : qualifiedNames) {
            Optional<AnnotationSyntax> ann = field.getAnnotation(qn);
            if (ann.isPresent()) {
                return ann;
            }
        }
        return Optional.empty();
    }

    /**
     * Determines if a type is a collection type.
     *
     * <p>Recognized collection types include List, Set, Collection, and their implementations.</p>
     *
     * @param type the type reference to check
     * @return true if the type is a collection
     */
    private static boolean isCollectionType(TypeRef type) {
        String qn = type.qualifiedName();
        return qn.equals("java.util.List")
                || qn.equals("java.util.Set")
                || qn.equals("java.util.Collection")
                || qn.equals("java.util.ArrayList")
                || qn.equals("java.util.LinkedList")
                || qn.equals("java.util.HashSet")
                || qn.equals("java.util.TreeSet")
                || qn.equals("java.util.LinkedHashSet")
                || qn.startsWith("java.util.")
                        && (qn.contains("List") || qn.contains("Set") || qn.contains("Collection"));
    }

    /**
     * Extracts implicit relations from collection fields without JPA annotations.
     *
     * <p>This method detects collections that are not annotated with JPA relationship
     * annotations and creates implicit element-collection relations.</p>
     *
     * <p>Element collection is used by default because the target type may not have
     * a corresponding JPA entity generated (e.g., child entities within aggregates).
     * Using {@code @ElementCollection} allows JPA to persist these as embedded collections.</p>
     *
     * @param type the type syntax to analyze
     * @return a list of implicit relation infos (may be empty)
     */
    public static List<RelationInfo> extractImplicitRelations(TypeSyntax type) {
        Objects.requireNonNull(type, "type must not be null");

        List<RelationInfo> implicitRelations = new ArrayList<>();

        for (FieldSyntax field : type.fields()) {
            // Skip static fields
            if (field.isStatic()) {
                continue;
            }

            // Skip fields with explicit JPA relation annotations
            if (extractRelation(field).isPresent()) {
                continue;
            }

            // Detect implicit relations from collection fields
            if (isCollectionType(field.type())) {
                TypeRef elementType = extractCollectionElementType(field.type());
                // Use element collection since target may not be a JPA entity
                implicitRelations.add(RelationInfo.elementCollection(field.name(), elementType));
            }
        }

        return List.copyOf(implicitRelations);
    }
}
