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
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.ir.Nullability;
import io.hexaglue.plugin.jpa.util.TypeMappings;
import java.util.List;
import java.util.Map;

/**
 * Intermediate representation of a simple property field for JPA entity generation.
 *
 * <p>This record bridges the domain model and JavaPoet's code generation model.
 * It contains only the information needed to
 * generate a JPA field with appropriate annotations.
 *
 * <p>Design decision: This spec focuses on simple properties only. Relationships
 * are handled separately by {@link RelationFieldSpec}.
 *
 * @param fieldName the name of the field in the generated entity
 * @param javaType the JavaPoet type representation for the field
 * @param nullability whether the field can be null (from SPI)
 * @param columnName the database column name (typically snake_case)
 * @param isEmbedded true if this property represents an embedded value object
 * @param isValueObject true if this property's type is a Value Object (from ElementKind)
 * @param isEnum true if this property's type is a Java enum
 * @param typeQualifiedName the fully qualified name of the property's type
 * @param isWrappedForeignKey true if this property is a foreign key wrapper (e.g., CustomerId)
 * @param unwrappedType the JavaPoet type representation for the unwrapped value (null if not wrapped)
 * @param wrapperAccessorMethod the method to access the unwrapped value (e.g., "value" for records)
 * @param attributeOverrides list of attribute overrides for embedded fields with column name conflicts
 * @since 2.0.0
 */
public record PropertyFieldSpec(
        String fieldName,
        TypeName javaType,
        Nullability nullability,
        String columnName,
        boolean isEmbedded,
        boolean isValueObject,
        boolean isEnum,
        String typeQualifiedName,
        boolean isWrappedForeignKey,
        TypeName unwrappedType,
        String wrapperAccessorMethod,
        List<AttributeOverride> attributeOverrides) {

    /**
     * Canonical constructor with validation.
     * Ensures attributeOverrides is never null (uses empty list as default).
     */
    public PropertyFieldSpec {
        attributeOverrides = attributeOverrides == null ? List.of() : List.copyOf(attributeOverrides);
    }

    /**
     * Returns true if this property has attribute overrides.
     *
     * @return true if attributeOverrides is not empty
     */
    public boolean hasAttributeOverrides() {
        return !attributeOverrides.isEmpty();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Creates a PropertyFieldSpec from a v5 model Field.
     *
     * <p>This factory method converts the v5 arch.model.Field to the JavaPoet-based
     * generation model. It uses the ArchitecturalModel's domainIndex to detect value objects.
     *
     * @param field the field from arch.model
     * @param model the architectural model for value object detection
     * @return a PropertyFieldSpec ready for code generation
     * @throws IllegalArgumentException if the field has an IDENTITY role
     * @since 5.0.0
     */
    public static PropertyFieldSpec fromV5(Field field, ArchitecturalModel model) {
        return fromV5(field, model, Map.of(), null);
    }

    /**
     * Creates a PropertyFieldSpec from a v5 model Field with embeddable mapping support.
     *
     * <p>This factory method extends {@link #fromV5(Field, ArchitecturalModel)} with support
     * for substituting complex VALUE_OBJECT types with their generated embeddable types.
     *
     * <p>When a field's type is found in the embeddableMapping, the javaType is
     * substituted with the corresponding embeddable type. This is used when generating
     * embeddable classes that contain nested VALUE_OBJECTs (e.g., OrderLine with Money).
     *
     * <p>Simple wrapper types (single-field VALUE_OBJECTs like Quantity) are still
     * unwrapped to their primitive types regardless of the embeddable mapping.
     *
     * @param field the field from arch.model
     * @param model the architectural model for value object detection
     * @param embeddableMapping map from domain FQN to embeddable FQN
     * @param infrastructurePackage the infrastructure package for embeddable types (unused but for consistency)
     * @return a PropertyFieldSpec ready for code generation
     * @throws IllegalArgumentException if the field has an IDENTITY role
     * @since 5.0.0
     */
    public static PropertyFieldSpec fromV5(
            Field field,
            ArchitecturalModel model,
            Map<String, String> embeddableMapping,
            String infrastructurePackage) {
        // Skip identity fields - they are handled by IdFieldSpec
        if (field.hasRole(FieldRole.IDENTITY)) {
            throw new IllegalArgumentException(
                    "Field " + field.name() + " is an identity. Use IdFieldSpec.from() instead.");
        }

        TypeName javaType = TypeMappings.toJpaType(field.type().qualifiedName());
        String columnName = JpaModelUtils.toSnakeCase(field.name());
        String typeQualifiedName = field.type().qualifiedName();

        // Detect value object using v5 domainIndex (if available)
        io.hexaglue.arch.model.ValueObject matchingVo = null;
        io.hexaglue.arch.model.Identifier matchingId = null;
        var domainIndexOpt = model.domainIndex();
        if (domainIndexOpt.isPresent()) {
            var domainIndex = domainIndexOpt.get();

            // Check if it's a Value Object
            matchingVo = domainIndex
                    .valueObjects()
                    .filter(vo -> vo.id().qualifiedName().equals(typeQualifiedName))
                    .findFirst()
                    .orElse(null);

            // Also check if it's an Identifier (for fields like productId: ProductId)
            if (matchingVo == null) {
                matchingId = domainIndex
                        .identifiers()
                        .filter(id -> id.id().qualifiedName().equals(typeQualifiedName))
                        .findFirst()
                        .orElse(null);
            }
        }

        boolean isValueObject = matchingVo != null;
        boolean isIdentifier = matchingId != null;

        // Detect enum type using structure
        boolean isEnum = matchingVo != null
                && matchingVo.structure() != null
                && matchingVo.structure().nature() == TypeNature.ENUM;

        // Detect simple wrapper type
        boolean isSimpleWrapper = isSimpleWrapperV5(matchingVo);
        boolean isWrappedForeignKey = (isSimpleWrapper || isIdentifier) && typeQualifiedName.endsWith("Id");

        TypeName unwrappedType = null;
        String wrapperAccessorMethod = null;

        // Handle Value Object simple wrappers
        if (isSimpleWrapper && matchingVo != null) {
            var wrappedFieldOpt = matchingVo.wrappedField();
            if (wrappedFieldOpt.isPresent()) {
                var wrappedField = wrappedFieldOpt.get();
                unwrappedType = TypeMappings.toJpaType(wrappedField.type().qualifiedName());
                boolean isRecord = matchingVo.structure().isRecord();
                wrapperAccessorMethod = isRecord ? wrappedField.name() : "get" + capitalize(wrappedField.name());
            }
        }

        // Handle Identifier types (e.g., ProductId wrapping UUID)
        if (matchingId != null) {
            var wrappedTypeRef = matchingId.wrappedType();
            if (wrappedTypeRef != null) {
                unwrappedType = TypeMappings.toJpaType(wrappedTypeRef.qualifiedName());
                // For identifiers, assume record-style accessor with 'value' field
                boolean isRecord = matchingId.structure().isRecord();
                wrapperAccessorMethod = isRecord ? "value" : "getValue";
            }
        }

        // Determine nullability from annotations
        boolean isNullable = !field.hasAnnotation("jakarta.persistence.NotNull")
                && !field.hasAnnotation("javax.validation.constraints.NotNull")
                && !field.hasAnnotation("javax.persistence.NotNull");
        Nullability nullability = isNullable ? Nullability.NULLABLE : Nullability.NON_NULL;

        // Check if embedded
        boolean isEmbedded = field.hasRole(FieldRole.EMBEDDED);

        // Check if this type should be substituted with an embeddable type
        // Only substitute if it's a VALUE_OBJECT that is NOT a simple wrapper
        // Simple wrappers (like Quantity) should be unwrapped to their primitive type
        // Complex VALUE_OBJECTs (like Money with 2 fields) should use @Embedded with the embeddable type
        if (embeddableMapping != null
                && !embeddableMapping.isEmpty()
                && !isSimpleWrapper
                && !isIdentifier
                && embeddableMapping.containsKey(typeQualifiedName)) {
            String embeddableFqn = embeddableMapping.get(typeQualifiedName);
            TypeName embeddableType = ClassName.bestGuess(embeddableFqn);
            return new PropertyFieldSpec(
                    field.name(),
                    embeddableType,
                    nullability,
                    columnName,
                    true, // isEmbedded - treated as embedded since it's an embeddable type
                    true, // isValueObject
                    false, // isEnum - embeddables are not enums
                    embeddableFqn,
                    false, // isWrappedForeignKey
                    null, // unwrappedType - not applicable for complex VOs
                    null, // wrapperAccessorMethod - not applicable
                    List.of());
        }

        return new PropertyFieldSpec(
                field.name(),
                javaType,
                nullability,
                columnName,
                isEmbedded,
                isValueObject,
                isEnum,
                typeQualifiedName,
                isWrappedForeignKey,
                unwrappedType,
                wrapperAccessorMethod,
                List.of());
    }

    /**
     * Determines if a v5 value object is a simple wrapper type.
     *
     * @param vo the v5 model value object
     * @return true if it's a single-value wrapper record
     * @since 5.0.0
     */
    private static boolean isSimpleWrapperV5(io.hexaglue.arch.model.ValueObject vo) {
        if (vo == null) {
            return false;
        }
        return vo.isSingleValue() && vo.structure().isRecord();
    }

    /**
     * Returns true if this property should be treated as embedded.
     *
     * <p>A property is treated as embedded if it is explicitly marked as embedded
     * or if its type is a Value Object. In JPA, embedded properties use the
     * {@code @Embedded} annotation.
     *
     * <p>Note: Enums are never embedded - they use {@code @Enumerated(EnumType.STRING)}
     * instead. Even if an enum is classified as VALUE_OBJECT, it should not be embedded.
     *
     * <p>Note: Simple wrapper types (e.g., CustomerId, Money, Quantity) are never
     * embedded - they should be unwrapped to their primitive type and persisted
     * as a simple column.
     *
     * @return true if the property should be embedded
     */
    public boolean shouldBeEmbedded() {
        // Enums should never be embedded - they use @Enumerated
        if (isEnum) {
            return false;
        }
        // Simple wrappers (including foreign keys and value wrappers like Money)
        // should be unwrapped, not embedded
        if (unwrappedType != null) {
            return false;
        }
        return isEmbedded || isValueObject;
    }

    /**
     * Returns the effective JPA type for this property.
     *
     * <p>For simple wrapper types (e.g., CustomerId, Money, Quantity), this returns
     * the unwrapped type (e.g., UUID, BigDecimal, Integer). For all other properties,
     * this returns the original javaType.
     *
     * <p>This ensures that simple VALUE_OBJECT wrappers are persisted as their
     * primitive types in JPA entities and embeddables.
     *
     * @return the type to use in the JPA entity field
     */
    public TypeName effectiveJpaType() {
        // Unwrap simple wrappers (both foreign keys and value wrappers)
        if (unwrappedType != null) {
            return unwrappedType;
        }
        return javaType;
    }

    /**
     * Returns true if this field is nullable.
     *
     * <p>Convenience method for checking nullability without comparing enum values.
     *
     * @return true if nullability is NULLABLE, false otherwise
     */
    public boolean isNullable() {
        return nullability == Nullability.NULLABLE;
    }

    /**
     * Returns true if this field is required (non-null).
     *
     * <p>Convenience method for checking non-nullability.
     *
     * @return true if nullability is NON_NULL, false otherwise
     */
    public boolean isRequired() {
        return nullability == Nullability.NON_NULL;
    }
}
