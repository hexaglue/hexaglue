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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NamingConventions}.
 *
 * <p>Tests validate the behavior of naming convention utilities, ensuring correct
 * transformation between camelCase and snake_case, and proper generation of database names.
 */
class NamingConventionsTest {

    // =====================================================================
    // toSnakeCase tests
    // =====================================================================

    @Test
    void toSnakeCase_shouldConvertSimpleCamelCase() {
        String result = NamingConventions.toSnakeCase("firstName");

        assertThat(result).isEqualTo("first_name");
    }

    @Test
    void toSnakeCase_shouldConvertMultipleWords() {
        String result = NamingConventions.toSnakeCase("totalAmount");

        assertThat(result).isEqualTo("total_amount");
    }

    @Test
    void toSnakeCase_shouldHandleSingleLowercase() {
        String result = NamingConventions.toSnakeCase("id");

        assertThat(result).isEqualTo("id");
    }

    @Test
    void toSnakeCase_shouldHandleConsecutiveUppercase() {
        String result = NamingConventions.toSnakeCase("XMLParser");

        assertThat(result).isEqualTo("xml_parser");
    }

    @Test
    void toSnakeCase_shouldHandleUppercaseAcronyms() {
        String result = NamingConventions.toSnakeCase("HTTPSConnection");

        assertThat(result).isEqualTo("https_connection");
    }

    @Test
    void toSnakeCase_shouldHandleComplexCamelCase() {
        String result = NamingConventions.toSnakeCase("orderLineItem");

        assertThat(result).isEqualTo("order_line_item");
    }

    @Test
    void toSnakeCase_shouldHandleNumbersInName() {
        String result = NamingConventions.toSnakeCase("address2");

        assertThat(result).isEqualTo("address2");
    }

    @Test
    void toSnakeCase_shouldHandlePascalCase() {
        String result = NamingConventions.toSnakeCase("Order");

        assertThat(result).isEqualTo("order");
    }

    @Test
    void toSnakeCase_shouldHandleAllUppercase() {
        String result = NamingConventions.toSnakeCase("UUID");

        assertThat(result).isEqualTo("uuid");
    }

    @Test
    void toSnakeCase_shouldThrowExceptionForNullInput() {
        assertThatThrownBy(() -> NamingConventions.toSnakeCase(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Input string cannot be null or empty");
    }

    @Test
    void toSnakeCase_shouldThrowExceptionForEmptyInput() {
        assertThatThrownBy(() -> NamingConventions.toSnakeCase(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Input string cannot be null or empty");
    }

    // =====================================================================
    // capitalize tests
    // =====================================================================

    @Test
    void capitalize_shouldCapitalizeLowercaseWord() {
        String result = NamingConventions.capitalize("order");

        assertThat(result).isEqualTo("Order");
    }

    @Test
    void capitalize_shouldCapitalizeCamelCase() {
        String result = NamingConventions.capitalize("firstName");

        assertThat(result).isEqualTo("FirstName");
    }

    @Test
    void capitalize_shouldHandleSingleCharacter() {
        String result = NamingConventions.capitalize("a");

        assertThat(result).isEqualTo("A");
    }

    @Test
    void capitalize_shouldHandleAlreadyCapitalized() {
        String result = NamingConventions.capitalize("Order");

        assertThat(result).isEqualTo("Order");
    }

    @Test
    void capitalize_shouldReturnEmptyStringForEmptyInput() {
        String result = NamingConventions.capitalize("");

        assertThat(result).isEmpty();
    }

    @Test
    void capitalize_shouldThrowExceptionForNullInput() {
        assertThatThrownBy(() -> NamingConventions.capitalize(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Input string cannot be null");
    }

    // =====================================================================
    // decapitalize tests
    // =====================================================================

    @Test
    void decapitalize_shouldDecapitalizePascalCase() {
        String result = NamingConventions.decapitalize("Order");

        assertThat(result).isEqualTo("order");
    }

    @Test
    void decapitalize_shouldDecapitalizeMultipleWords() {
        String result = NamingConventions.decapitalize("FirstName");

        assertThat(result).isEqualTo("firstName");
    }

    @Test
    void decapitalize_shouldHandleSingleCharacter() {
        String result = NamingConventions.decapitalize("A");

        assertThat(result).isEqualTo("a");
    }

    @Test
    void decapitalize_shouldHandleAlreadyDecapitalized() {
        String result = NamingConventions.decapitalize("order");

        assertThat(result).isEqualTo("order");
    }

    @Test
    void decapitalize_shouldHandleAllUppercase() {
        String result = NamingConventions.decapitalize("ID");

        assertThat(result).isEqualTo("iD");
    }

    @Test
    void decapitalize_shouldReturnEmptyStringForEmptyInput() {
        String result = NamingConventions.decapitalize("");

        assertThat(result).isEmpty();
    }

    @Test
    void decapitalize_shouldThrowExceptionForNullInput() {
        assertThatThrownBy(() -> NamingConventions.decapitalize(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Input string cannot be null");
    }

    // =====================================================================
    // toTableName tests
    // =====================================================================

    @Test
    void toTableName_shouldConvertWithoutPrefix() {
        String result = NamingConventions.toTableName("Product", "");

        assertThat(result).isEqualTo("product");
    }

    @Test
    void toTableName_C3_shouldPluralizeReservedWordOrder() {
        // C3 BUG: "order" is a SQL reserved word - generates invalid SQL
        // SELECT * FROM order → SQL syntax error
        // SELECT * FROM orders → OK
        String result = NamingConventions.toTableName("Order", "");

        assertThat(result)
                .as("SQL reserved word 'order' should be pluralized to 'orders'")
                .isEqualTo("orders");
    }

    @Test
    void toTableName_shouldPluralizeReservedWordUser() {
        // "user" is reserved in PostgreSQL, MySQL, and SQL Server
        String result = NamingConventions.toTableName("User", "");

        assertThat(result)
                .as("SQL reserved word 'user' should be pluralized to 'users'")
                .isEqualTo("users");
    }

    @Test
    void toTableName_shouldPluralizeReservedWordGroup() {
        // "group" is reserved in SQL
        String result = NamingConventions.toTableName("Group", "");

        assertThat(result)
                .as("SQL reserved word 'group' should be pluralized to 'groups'")
                .isEqualTo("groups");
    }

    @Test
    void toTableName_shouldNotPluralizeNonReservedWords() {
        // "customer" is not a reserved word
        String result = NamingConventions.toTableName("Customer", "");

        assertThat(result)
                .as("Non-reserved word should not be pluralized")
                .isEqualTo("customer");
    }

    @Test
    void toTableName_shouldNotDoublePluralize() {
        // If entity is already plural, don't add extra 's'
        String result = NamingConventions.toTableName("Orders", "");

        assertThat(result).isEqualTo("orders");
    }

    @Test
    void toTableName_shouldConvertWithPrefix() {
        String result = NamingConventions.toTableName("Product", "tbl_");

        assertThat(result).isEqualTo("tbl_product");
    }

    @Test
    void toTableName_shouldConvertReservedWordWithPrefix() {
        // Reserved word "order" should be pluralized even with prefix
        String result = NamingConventions.toTableName("Order", "tbl_");

        assertThat(result).isEqualTo("tbl_orders");
    }

    @Test
    void toTableName_shouldConvertCamelCaseEntityWithPrefix() {
        String result = NamingConventions.toTableName("LineItem", "t_");

        assertThat(result).isEqualTo("t_line_item");
    }

    @Test
    void toTableName_shouldConvertComplexEntityNameWithoutPrefix() {
        String result = NamingConventions.toTableName("CustomerAddress", "");

        assertThat(result).isEqualTo("customer_address");
    }

    @Test
    void toTableName_shouldHandleSingleWordEntity() {
        String result = NamingConventions.toTableName("Product", "tbl_");

        assertThat(result).isEqualTo("tbl_product");
    }

    @Test
    void toTableName_shouldThrowExceptionForNullEntityName() {
        assertThatThrownBy(() -> NamingConventions.toTableName(null, "tbl_"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Entity name cannot be null or empty");
    }

    @Test
    void toTableName_shouldThrowExceptionForEmptyEntityName() {
        assertThatThrownBy(() -> NamingConventions.toTableName("", "tbl_"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Entity name cannot be null or empty");
    }

    @Test
    void toTableName_shouldThrowExceptionForNullPrefix() {
        assertThatThrownBy(() -> NamingConventions.toTableName("Order", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prefix cannot be null (use empty string for no prefix)");
    }

    // =====================================================================
    // toColumnName tests
    // =====================================================================

    @Test
    void toColumnName_shouldConvertCamelCaseFieldName() {
        String result = NamingConventions.toColumnName("firstName");

        assertThat(result).isEqualTo("first_name");
    }

    @Test
    void toColumnName_shouldConvertSimpleFieldName() {
        String result = NamingConventions.toColumnName("id");

        assertThat(result).isEqualTo("id");
    }

    @Test
    void toColumnName_shouldConvertMultiWordFieldName() {
        String result = NamingConventions.toColumnName("totalAmount");

        assertThat(result).isEqualTo("total_amount");
    }

    @Test
    void toColumnName_shouldConvertTimestampField() {
        String result = NamingConventions.toColumnName("createdAt");

        assertThat(result).isEqualTo("created_at");
    }

    @Test
    void toColumnName_shouldThrowExceptionForNullFieldName() {
        assertThatThrownBy(() -> NamingConventions.toColumnName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field name cannot be null or empty");
    }

    @Test
    void toColumnName_shouldThrowExceptionForEmptyFieldName() {
        assertThatThrownBy(() -> NamingConventions.toColumnName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field name cannot be null or empty");
    }

    @Test
    void toColumnName_shouldSuffixReservedWordValue() {
        // "value" is a SQL reserved word - generates invalid SQL if used as column name
        String result = NamingConventions.toColumnName("value");

        assertThat(result)
                .as("SQL reserved word 'value' should be suffixed with '_col'")
                .isEqualTo("value_col");
    }

    @Test
    void toColumnName_shouldSuffixReservedWordKey() {
        // "key" is a SQL reserved word
        String result = NamingConventions.toColumnName("key");

        assertThat(result)
                .as("SQL reserved word 'key' should be suffixed with '_col'")
                .isEqualTo("key_col");
    }

    @Test
    void toColumnName_shouldSuffixReservedWordOrder() {
        // "order" is a SQL reserved word
        String result = NamingConventions.toColumnName("order");

        assertThat(result)
                .as("SQL reserved word 'order' should be suffixed with '_col'")
                .isEqualTo("order_col");
    }

    @Test
    void toColumnName_shouldNotSuffixNonReservedWords() {
        // "email" is not a reserved word
        String result = NamingConventions.toColumnName("email");

        assertThat(result)
                .as("Non-reserved word should not be suffixed")
                .isEqualTo("email");
    }

    // =====================================================================
    // toForeignKeyColumnName tests
    // =====================================================================

    @Test
    void toForeignKeyColumnName_shouldAddIdSuffix() {
        String result = NamingConventions.toForeignKeyColumnName("order");

        assertThat(result).isEqualTo("order_id");
    }

    @Test
    void toForeignKeyColumnName_shouldAddIdSuffixToCamelCase() {
        String result = NamingConventions.toForeignKeyColumnName("customer");

        assertThat(result).isEqualTo("customer_id");
    }

    @Test
    void toForeignKeyColumnName_shouldNotDuplicateIdSuffix() {
        String result = NamingConventions.toForeignKeyColumnName("orderId");

        assertThat(result).isEqualTo("order_id");
    }

    @Test
    void toForeignKeyColumnName_shouldHandleComplexFieldNames() {
        String result = NamingConventions.toForeignKeyColumnName("parentCategory");

        assertThat(result).isEqualTo("parent_category_id");
    }

    @Test
    void toForeignKeyColumnName_shouldHandleFieldAlreadyEndingWithId() {
        String result = NamingConventions.toForeignKeyColumnName("customerId");

        assertThat(result).isEqualTo("customer_id");
    }

    @Test
    void toForeignKeyColumnName_shouldHandleLowercaseIdField() {
        String result = NamingConventions.toForeignKeyColumnName("id");

        // When field is already "id", it gets "_id" suffix to become "id_id"
        // This is the expected behavior per the implementation
        assertThat(result).isEqualTo("id_id");
    }

    @Test
    void toForeignKeyColumnName_shouldThrowExceptionForNullFieldName() {
        assertThatThrownBy(() -> NamingConventions.toForeignKeyColumnName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field name cannot be null or empty");
    }

    @Test
    void toForeignKeyColumnName_shouldThrowExceptionForEmptyFieldName() {
        assertThatThrownBy(() -> NamingConventions.toForeignKeyColumnName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field name cannot be null or empty");
    }

    // =====================================================================
    // toJoinTableName tests
    // =====================================================================

    @Test
    void toJoinTableName_shouldCombineInAlphabeticalOrder() {
        String result = NamingConventions.toJoinTableName("Order", "Product");

        assertThat(result).isEqualTo("order_product");
    }

    @Test
    void toJoinTableName_shouldCombineInAlphabeticalOrderRegardlessOfInputOrder() {
        String result = NamingConventions.toJoinTableName("Product", "Order");

        assertThat(result).isEqualTo("order_product");
    }

    @Test
    void toJoinTableName_shouldHandleCamelCaseEntityNames() {
        String result = NamingConventions.toJoinTableName("Student", "Course");

        assertThat(result).isEqualTo("course_student");
    }

    @Test
    void toJoinTableName_shouldHandleComplexEntityNames() {
        String result = NamingConventions.toJoinTableName("OrderLineItem", "Product");

        assertThat(result).isEqualTo("order_line_item_product");
    }

    @Test
    void toJoinTableName_shouldHandleIdenticalNames() {
        String result = NamingConventions.toJoinTableName("Order", "Order");

        assertThat(result).isEqualTo("order_order");
    }

    @Test
    void toJoinTableName_shouldThrowExceptionForNullFirstEntityName() {
        assertThatThrownBy(() -> NamingConventions.toJoinTableName(null, "Product"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("First entity name cannot be null or empty");
    }

    @Test
    void toJoinTableName_shouldThrowExceptionForEmptyFirstEntityName() {
        assertThatThrownBy(() -> NamingConventions.toJoinTableName("", "Product"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("First entity name cannot be null or empty");
    }

    @Test
    void toJoinTableName_shouldThrowExceptionForNullSecondEntityName() {
        assertThatThrownBy(() -> NamingConventions.toJoinTableName("Order", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Second entity name cannot be null or empty");
    }

    @Test
    void toJoinTableName_shouldThrowExceptionForEmptySecondEntityName() {
        assertThatThrownBy(() -> NamingConventions.toJoinTableName("Order", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Second entity name cannot be null or empty");
    }
}
