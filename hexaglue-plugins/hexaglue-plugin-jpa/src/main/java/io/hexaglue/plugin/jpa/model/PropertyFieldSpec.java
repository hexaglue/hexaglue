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
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.plugin.jpa.extraction.PropertyInfo;
import io.hexaglue.plugin.jpa.util.TypeMappings;
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.JavaConstruct;
import io.hexaglue.spi.ir.Nullability;
import io.hexaglue.syntax.TypeForm;
import java.util.List;
import java.util.Map;

/**
 * Intermediate representation of a simple property field for JPA entity generation.
 *
 * <p>This record bridges the gap between the SPI's {@link DomainProperty} and
 * JavaPoet's code generation model. It contains only the information needed to
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
        String wrapperAccessorMethod) {

    /**
     * Creates a PropertyFieldSpec from a SPI DomainProperty.
     *
     * <p>This factory method performs the conversion from the SPI domain model
     * to the JavaPoet-based generation model. It handles type conversion and
     * column name transformation.
     *
     * <p>Note: This method cannot detect Value Objects or enums since it doesn't have
     * access to all domain types. Use {@link #from(DomainProperty, List)} for
     * Value Object and enum detection.
     *
     * @param property the domain property from the SPI
     * @return a PropertyFieldSpec ready for code generation
     * @throws IllegalArgumentException if the property has a relationship (use RelationFieldSpec instead)
     */
    public static PropertyFieldSpec from(DomainProperty property) {
        return from(property, List.of());
    }

    /**
     * Creates a PropertyFieldSpec from a SPI DomainProperty with Value Object and enum detection.
     *
     * <p>This factory method performs the conversion from the SPI domain model
     * to the JavaPoet-based generation model. It handles type conversion,
     * column name transformation, Value Object detection, and enum detection.
     *
     * <p>A property is considered a Value Object if its type is classified as
     * {@link ElementKind#VALUE_OBJECT} in the provided list of all domain types.
     *
     * <p>A property is considered an enum if its type has {@link JavaConstruct#ENUM}
     * in the provided list of all domain types. Enums require special JPA handling
     * with {@code @Enumerated(EnumType.STRING)}.
     *
     * <p>A property is considered a wrapped foreign key if its type is a VALUE_OBJECT
     * record with exactly one property, and the type name ends with "Id". These
     * inter-aggregate references should be unwrapped for JPA persistence.
     *
     * @param property the domain property from the SPI
     * @param allTypes all domain types from the IR snapshot for Value Object and enum detection
     * @return a PropertyFieldSpec ready for code generation
     * @throws IllegalArgumentException if the property has a relationship (use RelationFieldSpec instead)
     */
    public static PropertyFieldSpec from(DomainProperty property, List<DomainType> allTypes) {
        return from(property, allTypes, Map.of(), null);
    }

    /**
     * Creates a PropertyFieldSpec with embeddable type substitution support.
     *
     * <p>This factory method extends {@link #from(DomainProperty, List)} with support
     * for substituting complex VALUE_OBJECT types with their generated embeddable types.
     *
     * <p>When a property's type is found in the embeddableMapping, the javaType is
     * substituted with the corresponding embeddable type. This is used when generating
     * embeddable classes that contain nested VALUE_OBJECTs.
     *
     * @param property the domain property from the SPI
     * @param allTypes all domain types from the IR snapshot
     * @param embeddableMapping map from domain FQN to embeddable FQN
     * @param infrastructurePackage the infrastructure package for embeddable types
     * @return a PropertyFieldSpec ready for code generation
     * @throws IllegalArgumentException if the property has a relationship
     */
    public static PropertyFieldSpec from(
            DomainProperty property,
            List<DomainType> allTypes,
            Map<String, String> embeddableMapping,
            String infrastructurePackage) {
        if (property.hasRelation()) {
            throw new IllegalArgumentException(
                    "Property " + property.name() + " has a relation. Use RelationFieldSpec.from() instead.");
        }

        if (property.isIdentity()) {
            throw new IllegalArgumentException(
                    "Property " + property.name() + " is an identity. Use IdFieldSpec.from() instead.");
        }

        TypeName javaType = TypeMappings.toJpaType(property.type());
        String columnName = JpaModelUtils.toSnakeCase(property.name());
        String typeQualifiedName = property.type().qualifiedName();

        // Check if this type should be substituted with an embeddable type
        // This is used for complex VALUE_OBJECTs that have corresponding generated embeddables
        if (embeddableMapping != null && embeddableMapping.containsKey(typeQualifiedName)) {
            String embeddableFqn = embeddableMapping.get(typeQualifiedName);
            javaType = ClassName.bestGuess(embeddableFqn);
            // For embeddable substitution, return a spec with the substituted type
            // No need for unwrapping since the embeddable handles the mapping
            return new PropertyFieldSpec(
                    property.name(),
                    javaType,
                    property.nullability(),
                    columnName,
                    true, // isEmbedded - treated as embedded since it's an embeddable type
                    true, // isValueObject
                    false, // isEnum
                    embeddableFqn,
                    false, // isWrappedForeignKey
                    null, // unwrappedType - not applicable
                    null); // wrapperAccessorMethod - not applicable
        }

        // Find the matching domain type to check its properties
        DomainType matchingType = allTypes.stream()
                .filter(type -> type.qualifiedName().equals(typeQualifiedName))
                .findFirst()
                .orElse(null);

        // Detect if the property's type is a Value Object
        boolean isValueObject = matchingType != null && matchingType.isValueObject();

        // Detect if the property's type is an enum
        boolean isEnum = matchingType != null && matchingType.construct() == JavaConstruct.ENUM;

        // Detect if the property is a simple wrapper type (single-property VALUE_OBJECT or IDENTIFIER)
        // This includes:
        // - Foreign keys like CustomerId (single property, name ends with "Id")
        // - Simple value wrappers like Money, Quantity (single property)
        // These should be unwrapped to their primitive types for JPA persistence
        boolean isSimpleWrapper = isSimpleWrapperType(matchingType);
        boolean isWrappedForeignKey = isSimpleWrapper
                && matchingType != null
                && matchingType.simpleName().endsWith("Id");
        TypeName unwrappedType = null;
        String wrapperAccessorMethod = null;

        if (isSimpleWrapper && matchingType != null) {
            // Get the single property from the wrapper type
            DomainProperty wrappedProperty = matchingType.properties().get(0);
            unwrappedType = TypeMappings.toJpaType(wrappedProperty.type());
            // For records, the accessor is the property name (e.g., "value")
            wrapperAccessorMethod =
                    matchingType.isRecord() ? wrappedProperty.name() : "get" + capitalize(wrappedProperty.name());
        }

        return new PropertyFieldSpec(
                property.name(),
                javaType,
                property.nullability(),
                columnName,
                property.isEmbedded(),
                isValueObject,
                isEnum,
                typeQualifiedName,
                isWrappedForeignKey,
                unwrappedType,
                wrapperAccessorMethod);
    }

    /**
     * Determines if a domain type is a simple wrapper type.
     *
     * <p>A simple wrapper type is identified by the following criteria:
     * <ul>
     *   <li>It is a VALUE_OBJECT or IDENTIFIER (typed wrapper)</li>
     *   <li>It is a Java record (immutable wrapper pattern)</li>
     *   <li>It has exactly one property (single-field wrapper)</li>
     * </ul>
     *
     * <p>This includes both:
     * <ul>
     *   <li>Foreign keys: {@code CustomerId(UUID value)}, {@code ProductId(Long value)}</li>
     *   <li>Value wrappers: {@code Money(BigDecimal amount)}, {@code Quantity(int value)}</li>
     * </ul>
     *
     * <p>These wrapper types should be unwrapped to their primitive types for JPA persistence
     * in embeddable classes, avoiding the need for complex embedded relationships.
     *
     * @param type the domain type to check
     * @return true if the type is a simple wrapper
     */
    private static boolean isSimpleWrapperType(DomainType type) {
        if (type == null) {
            return false;
        }
        // Accept both VALUE_OBJECT and IDENTIFIER kinds
        // Both are single-property wrappers that should be unwrapped
        boolean isWrapper = type.isValueObject() || type.kind() == ElementKind.IDENTIFIER;
        return isWrapper
                && type.construct() == JavaConstruct.RECORD
                && type.properties().size() == 1;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Creates a PropertyFieldSpec from v4 PropertyInfo extracted from annotations.
     *
     * <p>This factory method converts the v4 extraction model to the JavaPoet-based
     * generation model. It uses the ArchitecturalModel for type resolution.
     *
     * @param info the property info from JpaAnnotationExtractor
     * @param model the architectural model for type resolution
     * @return a PropertyFieldSpec ready for code generation
     * @since 4.0.0
     * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
     */
    public static PropertyFieldSpec from(PropertyInfo info, ArchitecturalModel model) {
        TypeName javaType = TypeMappings.toJpaType(info.fieldType().qualifiedName());
        String columnName = info.columnNameOpt().orElseGet(() -> JpaModelUtils.toSnakeCase(info.fieldName()));
        String typeQualifiedName = info.fieldType().qualifiedName();

        // v4.1.0: Use registry for type detection
        var registry = model.registry();

        // Detect value object using registry
        boolean isValueObject = registry.all(ValueObject.class)
                .anyMatch(vo -> vo.id().qualifiedName().equals(typeQualifiedName));

        // Find matching ValueObject and check if enum
        ValueObject matchingVo = registry.all(ValueObject.class)
                .filter(vo -> vo.id().qualifiedName().equals(typeQualifiedName))
                .findFirst()
                .orElse(null);

        boolean isEnum = matchingVo != null
                && matchingVo.syntax() != null
                && matchingVo.syntax().form() == TypeForm.ENUM;

        // Check for simple wrapper type
        boolean isSimpleWrapper = isSimpleWrapperV4(matchingVo);
        boolean isWrappedForeignKey = isSimpleWrapper && typeQualifiedName.endsWith("Id");

        TypeName unwrappedType = null;
        String wrapperAccessorMethod = null;

        if (isSimpleWrapper && matchingVo != null && matchingVo.syntax() != null) {
            // Get the single field from the wrapper type
            var wrappedField = matchingVo.syntax().fields().get(0);
            unwrappedType = TypeMappings.toJpaType(wrappedField.type().qualifiedName());
            // For records, the accessor is the field name
            boolean isRecord = matchingVo.syntax().form() == TypeForm.RECORD;
            wrapperAccessorMethod = isRecord ? wrappedField.name() : "get" + capitalize(wrappedField.name());
        }

        Nullability nullability = info.nullable() ? Nullability.NULLABLE : Nullability.NON_NULL;

        return new PropertyFieldSpec(
                info.fieldName(),
                javaType,
                nullability,
                columnName,
                false, // isEmbedded - derived from relation, not property
                isValueObject,
                isEnum,
                typeQualifiedName,
                isWrappedForeignKey,
                unwrappedType,
                wrapperAccessorMethod);
    }

    /**
     * Determines if a v4 value object is a simple wrapper type.
     */
    private static boolean isSimpleWrapperV4(ValueObject vo) {
        if (vo == null || vo.syntax() == null) {
            return false;
        }
        return (vo.syntax().form() == TypeForm.RECORD) && vo.componentFields().size() == 1;
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
