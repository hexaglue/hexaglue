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

import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ir.IdentityStrategy;
import io.hexaglue.arch.model.ir.IdentityWrapperKind;

/**
 * Intermediate representation of an identity field for JPA entity generation.
 *
 * <p>This record captures the identity metadata needed to generate JPA {@code @Id}
 * and {@code @GeneratedValue} annotations, as well as handle wrapped identity types
 * (e.g., {@code record OrderId(UUID value) {}}).
 *
 * <p>Design decision: Identity fields require special handling because they:
 * <ul>
 *   <li>May be wrapped in custom types for type safety</li>
 *   <li>Require ID generation strategy annotations</li>
 *   <li>Need AttributeConverter for wrapped types in JPA</li>
 * </ul>
 *
 * @param fieldName the name of the identity field in the generated entity
 * @param javaType the JavaPoet type of the declared identity (possibly wrapped)
 * @param unwrappedType the JavaPoet type of the underlying primitive/wrapper
 * @param strategy the identity generation strategy
 * @param wrapperKind the kind of wrapper (RECORD, CLASS, or NONE)
 * @since 2.0.0
 */
public record IdFieldSpec(
        String fieldName,
        TypeName javaType,
        TypeName unwrappedType,
        IdentityStrategy strategy,
        IdentityWrapperKind wrapperKind) {

    /**
     * Creates an IdFieldSpec from a v5 model Field with IDENTITY role.
     *
     * <p>This factory method converts the v5 arch.model.Field to the JavaPoet-based
     * generation model. It handles both wrapped and unwrapped identity types using
     * the Field's wrappedType information.
     *
     * <p>Examples:
     * <ul>
     *   <li>Wrapped: {@code OrderId orderId} → javaType=OrderId, unwrappedType=UUID</li>
     *   <li>Unwrapped: {@code UUID id} → javaType=UUID, unwrappedType=UUID</li>
     * </ul>
     *
     * @param field the identity field from arch.model (should have IDENTITY role)
     * @param identityTypeStructure the TypeStructure of the identity type (for wrapper detection), may be null
     * @return an IdFieldSpec ready for code generation
     * @since 5.0.0
     */
    public static IdFieldSpec from(Field field, TypeStructure identityTypeStructure) {
        TypeName javaType = JpaModelUtils.resolveTypeName(field.type().qualifiedName());
        TypeName unwrappedType = field.wrappedType()
                .map(t -> JpaModelUtils.resolveTypeName(t.qualifiedName()))
                .orElse(javaType);

        IdentityStrategy strategy = detectStrategyFromField(field);
        IdentityWrapperKind wrapperKind = detectWrapperKindFromField(field, identityTypeStructure);

        return new IdFieldSpec(field.name(), javaType, unwrappedType, strategy, wrapperKind);
    }

    /**
     * Creates an IdFieldSpec from a v5 model Field with IDENTITY role.
     *
     * <p>Convenience method that calls {@link #from(Field, TypeStructure)} with null
     * for identityTypeStructure.
     *
     * @param field the identity field from arch.model (should have IDENTITY role)
     * @return an IdFieldSpec ready for code generation
     * @since 5.0.0
     */
    public static IdFieldSpec from(Field field) {
        return from(field, null);
    }

    /**
     * Returns true if the identity is wrapped in a custom type.
     *
     * <p>Wrapped identities require AttributeConverter generation for JPA mapping.
     *
     * @return true if wrapperKind is RECORD or CLASS
     */
    public boolean isWrapped() {
        return wrapperKind != IdentityWrapperKind.NONE;
    }

    /**
     * Returns true if this identity requires {@code @GeneratedValue} annotation.
     *
     * <p>Generated value strategies include AUTO, IDENTITY, SEQUENCE, TABLE, and UUID.
     * Natural and assigned identities do not require this annotation.
     *
     * @return true if the strategy requires generated value annotation
     */
    public boolean requiresGeneratedValue() {
        return strategy.requiresGeneratedValue();
    }

    /**
     * Returns the JPA GenerationType string if applicable.
     *
     * <p>Used for generating the {@code @GeneratedValue(strategy = GenerationType.XXX)} annotation.
     *
     * @return the GenerationType name, or null if not applicable
     */
    public String jpaGenerationType() {
        return strategy.toJpaGenerationType();
    }

    /**
     * Returns true if the identity uses a database-generated strategy.
     *
     * <p>Database-generated strategies include IDENTITY, SEQUENCE, TABLE, AUTO.
     *
     * @return true if the database generates the ID
     */
    public boolean isDatabaseGenerated() {
        return strategy.isGenerated() && strategy != IdentityStrategy.UUID;
    }

    /**
     * Returns true if the identity uses UUID generation.
     *
     * <p>UUID generation is typically handled by application code or JPA 3.1+ UUID generator.
     *
     * @return true if the strategy is UUID
     */
    public boolean isUuidGenerated() {
        return strategy == IdentityStrategy.UUID;
    }

    /**
     * Detects identity generation strategy from v5 Field annotations.
     *
     * @param field the identity field
     * @return the detected strategy, or ASSIGNED if no @GeneratedValue found
     * @since 5.0.0
     */
    private static IdentityStrategy detectStrategyFromField(Field field) {
        // Check for @GeneratedValue annotation
        var genValueOpt = field.annotations().stream()
                .filter(ann -> ann.qualifiedName().equals("jakarta.persistence.GeneratedValue")
                        || ann.qualifiedName().equals("javax.persistence.GeneratedValue"))
                .findFirst();

        if (genValueOpt.isEmpty()) {
            return IdentityStrategy.ASSIGNED;
        }

        var genValue = genValueOpt.get();
        var strategyValueOpt = genValue.getTypedValue("strategy");

        if (strategyValueOpt.isEmpty()) {
            return IdentityStrategy.AUTO;
        }

        var av = strategyValueOpt.get();
        if (av instanceof io.hexaglue.arch.model.AnnotationValue.EnumVal enumVal) {
            return switch (enumVal.enumConstant()) {
                case "IDENTITY" -> IdentityStrategy.IDENTITY;
                case "SEQUENCE" -> IdentityStrategy.SEQUENCE;
                case "TABLE" -> IdentityStrategy.TABLE;
                case "UUID" -> IdentityStrategy.UUID;
                default -> IdentityStrategy.AUTO;
            };
        }

        return IdentityStrategy.AUTO;
    }

    /**
     * Detects wrapper kind from v5 Field and its type structure.
     *
     * @param field the identity field
     * @param identityTypeStructure the structure of the identity type (may be null)
     * @return the wrapper kind
     * @since 5.0.0
     */
    private static IdentityWrapperKind detectWrapperKindFromField(Field field, TypeStructure identityTypeStructure) {
        // Check for @EmbeddedId
        boolean isEmbeddedId = field.annotations().stream()
                .anyMatch(ann -> ann.qualifiedName().equals("jakarta.persistence.EmbeddedId")
                        || ann.qualifiedName().equals("javax.persistence.EmbeddedId"));

        if (isEmbeddedId) {
            return IdentityWrapperKind.NONE;
        }

        // If no wrapped type, it's not wrapped
        if (field.wrappedType().isEmpty()) {
            return IdentityWrapperKind.NONE;
        }

        // Use type structure to determine if it's a record or class
        if (identityTypeStructure != null && identityTypeStructure.nature() == TypeNature.RECORD) {
            return IdentityWrapperKind.RECORD;
        }

        return IdentityWrapperKind.CLASS;
    }
}
