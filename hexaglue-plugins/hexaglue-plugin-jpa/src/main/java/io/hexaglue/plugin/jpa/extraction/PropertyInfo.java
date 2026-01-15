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

import io.hexaglue.syntax.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA property information extracted from field annotations.
 *
 * <p>Captures column metadata including nullability, column name, and type hints.</p>
 *
 * @param fieldName the field name in the domain class
 * @param fieldType the field type reference
 * @param columnName the database column name (null to use default)
 * @param nullable whether the column allows nulls
 * @param unique whether the column has a unique constraint
 * @param length the column length for string types (null for default)
 * @param precision the decimal precision (null for default)
 * @param scale the decimal scale (null for default)
 * @since 4.0.0
 */
public record PropertyInfo(
        String fieldName,
        TypeRef fieldType,
        String columnName,
        boolean nullable,
        boolean unique,
        Integer length,
        Integer precision,
        Integer scale) {

    /**
     * Compact constructor with validation.
     */
    public PropertyInfo {
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(fieldType, "fieldType must not be null");
    }

    /**
     * Returns the column name as an Optional.
     *
     * @return the column name, or empty to use default naming
     */
    public Optional<String> columnNameOpt() {
        return Optional.ofNullable(columnName);
    }

    /**
     * Returns the effective column name.
     *
     * @return the column name if specified, otherwise the field name
     */
    public String effectiveColumnName() {
        return columnName != null ? columnName : fieldName;
    }

    /**
     * Returns the length as an Optional.
     *
     * @return the column length, or empty for default
     */
    public Optional<Integer> lengthOpt() {
        return Optional.ofNullable(length);
    }

    /**
     * Returns the precision as an Optional.
     *
     * @return the decimal precision, or empty for default
     */
    public Optional<Integer> precisionOpt() {
        return Optional.ofNullable(precision);
    }

    /**
     * Returns the scale as an Optional.
     *
     * @return the decimal scale, or empty for default
     */
    public Optional<Integer> scaleOpt() {
        return Optional.ofNullable(scale);
    }

    /**
     * Returns whether this property has custom column definition.
     *
     * @return true if any column attribute is customized
     */
    public boolean hasColumnCustomization() {
        return columnName != null || !nullable || unique || length != null || precision != null || scale != null;
    }

    /**
     * Returns whether this is a string type property.
     *
     * @return true if the field type is String
     */
    public boolean isStringType() {
        return "java.lang.String".equals(fieldType.qualifiedName()) || "String".equals(fieldType.simpleName());
    }

    /**
     * Returns whether this is a numeric type property.
     *
     * @return true if the field type is a numeric type
     */
    public boolean isNumericType() {
        String qn = fieldType.qualifiedName();
        return qn.equals("java.math.BigDecimal")
                || qn.equals("java.math.BigInteger")
                || qn.equals("java.lang.Integer")
                || qn.equals("java.lang.Long")
                || qn.equals("java.lang.Double")
                || qn.equals("java.lang.Float")
                || qn.equals("int")
                || qn.equals("long")
                || qn.equals("double")
                || qn.equals("float");
    }

    // ===== Factory methods =====

    /**
     * Creates a simple property with defaults.
     *
     * @param fieldName the field name
     * @param fieldType the field type
     * @return a new PropertyInfo with default settings
     */
    public static PropertyInfo simple(String fieldName, TypeRef fieldType) {
        return new PropertyInfo(fieldName, fieldType, null, true, false, null, null, null);
    }

    /**
     * Creates a required (non-nullable) property.
     *
     * @param fieldName the field name
     * @param fieldType the field type
     * @return a new PropertyInfo marked as non-nullable
     */
    public static PropertyInfo required(String fieldName, TypeRef fieldType) {
        return new PropertyInfo(fieldName, fieldType, null, false, false, null, null, null);
    }

    /**
     * Creates a string property with length constraint.
     *
     * @param fieldName the field name
     * @param fieldType the field type
     * @param length the maximum length
     * @return a new PropertyInfo with length constraint
     */
    public static PropertyInfo stringWithLength(String fieldName, TypeRef fieldType, int length) {
        return new PropertyInfo(fieldName, fieldType, null, true, false, length, null, null);
    }

    /**
     * Creates a decimal property with precision and scale.
     *
     * @param fieldName the field name
     * @param fieldType the field type
     * @param precision the decimal precision
     * @param scale the decimal scale
     * @return a new PropertyInfo with decimal constraints
     */
    public static PropertyInfo decimal(String fieldName, TypeRef fieldType, int precision, int scale) {
        return new PropertyInfo(fieldName, fieldType, null, true, false, null, precision, scale);
    }

    /**
     * Creates a unique property.
     *
     * @param fieldName the field name
     * @param fieldType the field type
     * @return a new PropertyInfo marked as unique
     */
    public static PropertyInfo unique(String fieldName, TypeRef fieldType) {
        return new PropertyInfo(fieldName, fieldType, null, true, true, null, null, null);
    }
}
