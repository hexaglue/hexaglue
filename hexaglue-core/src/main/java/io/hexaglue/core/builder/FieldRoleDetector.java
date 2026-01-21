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

package io.hexaglue.core.builder;

import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.FieldNode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Detects the semantic roles of fields during ArchType construction.
 *
 * <p>This detector analyzes field annotations, naming patterns, and type information
 * to determine what role(s) a field plays within the domain model. A field may have
 * multiple roles.</p>
 *
 * <h2>Detected Roles</h2>
 * <ul>
 *   <li>{@link FieldRole#IDENTITY} - Primary identifier (@Id, @Identity, or naming pattern)</li>
 *   <li>{@link FieldRole#COLLECTION} - Collection types (List, Set, Collection)</li>
 *   <li>{@link FieldRole#AGGREGATE_REFERENCE} - Reference to an aggregate root</li>
 *   <li>{@link FieldRole#EMBEDDED} - Embedded value object or identifier</li>
 *   <li>{@link FieldRole#AUDIT} - Audit fields (createdAt, updatedBy, etc.)</li>
 *   <li>{@link FieldRole#TECHNICAL} - Technical fields (version, serialVersionUID, etc.)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * FieldRoleDetector detector = new FieldRoleDetector();
 * Set<FieldRole> roles = detector.detect(fieldNode, context);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class FieldRoleDetector {

    private static final Set<String> ID_ANNOTATIONS = Set.of(
            "javax.persistence.Id",
            "jakarta.persistence.Id",
            "org.jmolecules.ddd.annotation.Identity",
            "org.springframework.data.annotation.Id");

    private static final Set<String> VERSION_ANNOTATIONS = Set.of(
            "javax.persistence.Version", "jakarta.persistence.Version", "org.springframework.data.annotation.Version");

    private static final Set<String> AUDIT_FIELD_PATTERNS = Set.of(
            "createdat",
            "updatedat",
            "createdby",
            "updatedby",
            "createdon",
            "updatedon",
            "modifiedat",
            "modifiedon",
            "modifiedby");

    private static final Set<String> TECHNICAL_FIELD_PATTERNS =
            Set.of("version", "tenant", "tenantid", "serialversionuid");

    /**
     * Creates a new FieldRoleDetector.
     */
    public FieldRoleDetector() {
        // Stateless detector
    }

    /**
     * Detects the semantic roles of a field.
     *
     * @param field the field to analyze
     * @param context the builder context for lookups
     * @return the set of detected roles (may be empty)
     */
    public Set<FieldRole> detect(FieldNode field, BuilderContext context) {
        Set<FieldRole> roles = EnumSet.noneOf(FieldRole.class);

        // IDENTITY: @Id annotation or naming convention (but not for collections/maps)
        if (isIdentityField(field, context)) {
            roles.add(FieldRole.IDENTITY);
        }

        // COLLECTION: List, Set, Collection
        if (field.isCollectionType()) {
            roles.add(FieldRole.COLLECTION);
        }

        // AGGREGATE_REFERENCE: type classified as AGGREGATE_ROOT
        String typeName = field.type().rawQualifiedName();
        if (isClassifiedAs(typeName, "AGGREGATE_ROOT", context)) {
            roles.add(FieldRole.AGGREGATE_REFERENCE);
        }

        // EMBEDDED: type classified as VALUE_OBJECT or IDENTIFIER
        if (isClassifiedAs(typeName, "VALUE_OBJECT", context) || isClassifiedAs(typeName, "IDENTIFIER", context)) {
            roles.add(FieldRole.EMBEDDED);
        }

        // AUDIT: audit fields
        if (isAuditField(field)) {
            roles.add(FieldRole.AUDIT);
        }

        // TECHNICAL: technical fields
        if (isTechnicalField(field)) {
            roles.add(FieldRole.TECHNICAL);
        }

        return roles.isEmpty() ? Set.of() : Set.copyOf(roles);
    }

    /**
     * Checks if a field is an identity field.
     *
     * <p>Identity is detected by:
     * <ul>
     *   <li>@Id annotations (JPA, Spring Data, jMolecules)</li>
     *   <li>Name is exactly {@code id}</li>
     *   <li>Name is {@code <typeName>Id} where typeName is the declaring type's simple name
     *       (e.g., {@code orderId} for {@code Order})</li>
     * </ul>
     *
     * <p>Fields like {@code productId} in {@code OrderLine} are NOT identity fields
     * because they are foreign key references to other aggregates.
     */
    private boolean isIdentityField(FieldNode field, BuilderContext context) {
        // Collections and maps are not identity fields
        if (field.isCollectionType() || field.type().isMapLike()) {
            return false;
        }

        // Check annotations - definitive
        if (hasAnyAnnotation(field.annotations(), ID_ANNOTATIONS)) {
            return true;
        }

        // Check naming pattern: "id" exactly
        String fieldName = field.simpleName();
        if (fieldName.equals("id")) {
            return true;
        }

        // Try to get declaring type from graph for precise heuristic
        var graph = context.graphQuery().graph();
        if (graph != null) {
            var declaringTypeOpt = graph.indexes().declaringTypeOf(field.id());
            if (declaringTypeOpt.isPresent()) {
                var declaringType = context.graphQuery().type(declaringTypeOpt.get());
                if (declaringType.isPresent()) {
                    String typeName = declaringType.get().simpleName();
                    String expectedIdFieldName =
                            Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1) + "Id";
                    if (fieldName.equals(expectedIdFieldName)) {
                        return true;
                    }
                    // If we have graph access and the field doesn't match the expected pattern,
                    // it's NOT an identity (e.g., productId in OrderLine)
                    return false;
                }
            }
        }

        // Fallback for tests without graph: use FieldNode.declaringTypeName
        String declaringTypeName = field.declaringTypeName();
        if (declaringTypeName != null && !declaringTypeName.isEmpty()) {
            String simpleTypeName = declaringTypeName.contains(".")
                    ? declaringTypeName.substring(declaringTypeName.lastIndexOf('.') + 1)
                    : declaringTypeName;
            String expectedIdFieldName =
                    Character.toLowerCase(simpleTypeName.charAt(0)) + simpleTypeName.substring(1) + "Id";
            return fieldName.equals(expectedIdFieldName);
        }

        return false;
    }

    private boolean isAuditField(FieldNode field) {
        String lowerName = field.simpleName().toLowerCase();
        return AUDIT_FIELD_PATTERNS.contains(lowerName);
    }

    private boolean isTechnicalField(FieldNode field) {
        String lowerName = field.simpleName().toLowerCase();

        // Check naming pattern
        if (TECHNICAL_FIELD_PATTERNS.contains(lowerName)) {
            return true;
        }

        // Check version annotation
        if (hasAnyAnnotation(field.annotations(), VERSION_ANNOTATIONS)) {
            return true;
        }

        return false;
    }

    private boolean hasAnyAnnotation(List<AnnotationRef> annotations, Set<String> annotationNames) {
        return annotations.stream().anyMatch(a -> annotationNames.contains(a.qualifiedName()));
    }

    private boolean isClassifiedAs(String typeName, String kind, BuilderContext context) {
        return context.isClassifiedAs(typeName, kind);
    }
}
