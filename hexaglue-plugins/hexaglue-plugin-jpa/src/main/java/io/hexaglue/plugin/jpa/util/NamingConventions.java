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

package io.hexaglue.plugin.jpa.util;

/**
 * Naming convention utilities for JPA code generation.
 *
 * <p>This utility class provides consistent naming transformations across all
 * generated JPA code. It ensures that database names follow SQL conventions
 * (snake_case) while Java code follows Java conventions (camelCase, PascalCase).
 *
 * <h3>Key Capabilities:</h3>
 * <ul>
 *   <li>Case conversion (camelCase ↔ snake_case)</li>
 *   <li>Capitalization (capitalize, decapitalize)</li>
 *   <li>Table name generation with optional prefixes</li>
 *   <li>Column name generation</li>
 *   <li>Foreign key column name generation</li>
 * </ul>
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li>Table names: snake_case with optional prefix (e.g., {@code tbl_order})</li>
 *   <li>Column names: snake_case (e.g., {@code first_name})</li>
 *   <li>Foreign keys: snake_case with {@code _id} suffix (e.g., {@code order_id})</li>
 *   <li>Join tables: snake_case with both entity names (e.g., {@code order_line_item})</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class NamingConventions {

    /**
     * Default foreign key suffix.
     */
    private static final String FK_SUFFIX = "_id";

    /**
     * SQL reserved words that should be pluralized when used as table names.
     *
     * <p>These words are reserved in at least one major SQL database
     * (PostgreSQL, MySQL, Oracle, SQL Server, H2) and would cause syntax errors
     * if used as unquoted table names.
     *
     * <p>C3 fix: pluralizing is preferred over quoting for portability.
     *
     * @since 2.0.0
     */
    private static final java.util.Set<String> SQL_RESERVED_WORDS = java.util.Set.of(
            // Common reserved words that are likely to be entity names
            "order", // ORDER BY
            "user", // Reserved in PostgreSQL, MySQL, SQL Server
            "group", // GROUP BY
            "table", // DDL keyword
            "index", // DDL keyword
            "key", // DDL keyword
            "select", // DML keyword
            "from", // DML keyword
            "where", // DML keyword
            "join", // DML keyword
            "limit", // DML keyword
            "offset", // DML keyword
            "constraint", // DDL keyword
            "check", // DDL keyword
            "default", // DDL keyword
            "column", // DDL keyword
            "value", // Reserved in some DBs
            "values", // DML keyword
            "transaction", // Reserved
            "session", // Reserved in some DBs
            "role", // Reserved in PostgreSQL
            "grant", // DCL keyword
            "revoke" // DCL keyword
            );

    private NamingConventions() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a camelCase string to snake_case.
     *
     * <p>This is the standard SQL naming convention for database identifiers.
     * The conversion handles:
     * <ul>
     *   <li>Single uppercase letters: {@code firstName} → {@code first_name}</li>
     *   <li>Sequences of uppercase letters: {@code XMLParser} → {@code xml_parser}</li>
     *   <li>Numbers: {@code address2} → {@code address2}</li>
     *   <li>Already lowercase: {@code id} → {@code id}</li>
     * </ul>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code firstName} → {@code first_name}</li>
     *   <li>{@code totalAmount} → {@code total_amount}</li>
     *   <li>{@code id} → {@code id}</li>
     *   <li>{@code XMLParser} → {@code xml_parser}</li>
     *   <li>{@code orderLineItem} → {@code order_line_item}</li>
     * </ul>
     *
     * @param camelCase the camelCase string to convert
     * @return the snake_case string
     * @throws IllegalArgumentException if camelCase is null or empty
     */
    public static String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty");
        }

        // Handle sequences of uppercase letters correctly
        // First, insert underscore before uppercase letters that follow lowercase
        // Then, insert underscore before uppercase letters that are followed by lowercase
        return camelCase
                .replaceAll("([a-z])([A-Z])", "$1_$2") // camelCase → camel_Case
                .replaceAll("([A-Z])([A-Z][a-z])", "$1_$2") // XMLParser → XML_Parser
                .toLowerCase();
    }

    /**
     * Capitalizes the first letter of a string (PascalCase).
     *
     * <p>Useful for generating class names from field names or other identifiers.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code order} → {@code Order}</li>
     *   <li>{@code firstName} → {@code FirstName}</li>
     *   <li>{@code id} → {@code Id}</li>
     *   <li>{@code ""} → {@code ""} (empty string unchanged)</li>
     *   <li>{@code "a"} → {@code "A"}</li>
     * </ul>
     *
     * @param str the string to capitalize
     * @return the capitalized string
     * @throws IllegalArgumentException if str is null
     */
    public static String capitalize(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }

        if (str.isEmpty()) {
            return str;
        }

        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Decapitalizes the first letter of a string (camelCase).
     *
     * <p>Useful for generating field names from class names or method names.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code Order} → {@code order}</li>
     *   <li>{@code FirstName} → {@code firstName}</li>
     *   <li>{@code ID} → {@code iD}</li>
     *   <li>{@code ""} → {@code ""} (empty string unchanged)</li>
     *   <li>{@code "A"} → {@code "a"}</li>
     * </ul>
     *
     * @param str the string to decapitalize
     * @return the decapitalized string
     * @throws IllegalArgumentException if str is null
     */
    public static String decapitalize(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }

        if (str.isEmpty()) {
            return str;
        }

        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Generates a database table name from an entity class name.
     *
     * <p>The generated table name follows SQL conventions:
     * <ul>
     *   <li>Converts the entity name to snake_case</li>
     *   <li>Pluralizes SQL reserved words to avoid syntax errors (C3 fix)</li>
     *   <li>Optionally adds a prefix (e.g., {@code "tbl_"}, {@code "t_"})</li>
     *   <li>Result is all lowercase</li>
     * </ul>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code toTableName("Order", "")} → {@code "orders"} (reserved word pluralized)</li>
     *   <li>{@code toTableName("User", "")} → {@code "users"} (reserved word pluralized)</li>
     *   <li>{@code toTableName("Product", "")} → {@code "product"} (not reserved)</li>
     *   <li>{@code toTableName("Order", "tbl_")} → {@code "tbl_orders"}</li>
     *   <li>{@code toTableName("LineItem", "t_")} → {@code "t_line_item"}</li>
     *   <li>{@code toTableName("CustomerAddress", "")} → {@code "customer_address"}</li>
     * </ul>
     *
     * @param entityName the entity class simple name (e.g., {@code "Order"}, {@code "LineItem"})
     * @param prefix the optional table prefix (can be empty string, never null)
     * @return the database table name in snake_case with optional prefix
     * @throws IllegalArgumentException if entityName is null or empty, or if prefix is null
     */
    public static String toTableName(String entityName, String prefix) {
        if (entityName == null || entityName.isEmpty()) {
            throw new IllegalArgumentException("Entity name cannot be null or empty");
        }
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null (use empty string for no prefix)");
        }

        String tableName = toSnakeCase(entityName);

        // C3 fix: Pluralize SQL reserved words to avoid syntax errors
        // Only pluralize if not already plural (doesn't end with 's')
        if (SQL_RESERVED_WORDS.contains(tableName) && !tableName.endsWith("s")) {
            tableName = tableName + "s";
        }

        return prefix.isEmpty() ? tableName : prefix + tableName;
    }

    /**
     * Generates a database column name from a field name.
     *
     * <p>Simply converts the field name to snake_case following SQL conventions.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code firstName} → {@code first_name}</li>
     *   <li>{@code id} → {@code id}</li>
     *   <li>{@code totalAmount} → {@code total_amount}</li>
     *   <li>{@code createdAt} → {@code created_at}</li>
     * </ul>
     *
     * @param fieldName the Java field name in camelCase
     * @return the database column name in snake_case
     * @throws IllegalArgumentException if fieldName is null or empty
     */
    public static String toColumnName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }

        return toSnakeCase(fieldName);
    }

    /**
     * Generates a foreign key column name from a field name.
     *
     * <p>Foreign key columns follow the convention: {@code <field_name>_id}.
     * If the field name already ends with "Id", only one "_id" suffix is added.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code order} → {@code order_id}</li>
     *   <li>{@code customer} → {@code customer_id}</li>
     *   <li>{@code orderId} → {@code order_id} (not {@code order_id_id})</li>
     *   <li>{@code parentCategory} → {@code parent_category_id}</li>
     * </ul>
     *
     * @param fieldName the Java field name in camelCase
     * @return the foreign key column name in snake_case with {@code _id} suffix
     * @throws IllegalArgumentException if fieldName is null or empty
     */
    public static String toForeignKeyColumnName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }

        String snakeCase = toSnakeCase(fieldName);

        // Avoid double _id suffix if the field name already ends with "id"
        if (snakeCase.endsWith(FK_SUFFIX)) {
            return snakeCase;
        }

        return snakeCase + FK_SUFFIX;
    }

    /**
     * Generates a join table name for many-to-many relationships.
     *
     * <p>Join table names combine both entity names in snake_case, typically
     * in alphabetical order to ensure consistency regardless of relationship direction.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code toJoinTableName("Order", "Product")} → {@code "order_product"}</li>
     *   <li>{@code toJoinTableName("Product", "Order")} → {@code "order_product"} (alphabetical)</li>
     *   <li>{@code toJoinTableName("Student", "Course")} → {@code "course_student"} (alphabetical)</li>
     * </ul>
     *
     * @param entityName1 the first entity name
     * @param entityName2 the second entity name
     * @return the join table name in snake_case
     * @throws IllegalArgumentException if either entity name is null or empty
     */
    public static String toJoinTableName(String entityName1, String entityName2) {
        if (entityName1 == null || entityName1.isEmpty()) {
            throw new IllegalArgumentException("First entity name cannot be null or empty");
        }
        if (entityName2 == null || entityName2.isEmpty()) {
            throw new IllegalArgumentException("Second entity name cannot be null or empty");
        }

        String table1 = toSnakeCase(entityName1);
        String table2 = toSnakeCase(entityName2);

        // Alphabetical order for consistency
        if (table1.compareTo(table2) <= 0) {
            return table1 + "_" + table2;
        } else {
            return table2 + "_" + table1;
        }
    }
}
