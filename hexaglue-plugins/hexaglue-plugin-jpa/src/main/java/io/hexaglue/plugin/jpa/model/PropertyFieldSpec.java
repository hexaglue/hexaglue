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
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.Nullability;

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
 * @since 2.0.0
 */
public record PropertyFieldSpec(
        String fieldName, TypeName javaType, Nullability nullability, String columnName, boolean isEmbedded) {

    /**
     * Creates a PropertyFieldSpec from a SPI DomainProperty.
     *
     * <p>This factory method performs the conversion from the SPI domain model
     * to the JavaPoet-based generation model. It handles type conversion and
     * column name transformation.
     *
     * @param property the domain property from the SPI
     * @return a PropertyFieldSpec ready for code generation
     * @throws IllegalArgumentException if the property has a relationship (use RelationFieldSpec instead)
     */
    public static PropertyFieldSpec from(DomainProperty property) {
        if (property.hasRelation()) {
            throw new IllegalArgumentException(
                    "Property " + property.name() + " has a relation. Use RelationFieldSpec.from() instead.");
        }

        if (property.isIdentity()) {
            throw new IllegalArgumentException(
                    "Property " + property.name() + " is an identity. Use IdFieldSpec.from() instead.");
        }

        TypeName javaType = ClassName.bestGuess(property.type().qualifiedName());
        String columnName = JpaModelUtils.toSnakeCase(property.name());

        return new PropertyFieldSpec(
                property.name(), javaType, property.nullability(), columnName, property.isEmbedded());
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
