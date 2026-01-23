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

/**
 * Represents a JPA {@code @AttributeOverride} specification for embedded fields.
 *
 * <p>When an entity has multiple embedded fields of the same type, JPA requires
 * {@code @AttributeOverride} annotations to specify distinct column names for each
 * embedded field's attributes. This record captures the mapping from the embeddable's
 * attribute name to the overridden column name.
 *
 * <h3>Example:</h3>
 * For an entity with two Money fields (price and discount), the Money embeddable
 * has attributes "amount" and "currency". Without overrides, both fields would
 * map to the same columns, causing a conflict.
 *
 * <pre>{@code
 * // Domain entity
 * public record Order(Money price, Money discount) {}
 *
 * // Generated JPA entity (without overrides - causes conflict!)
 * @Entity
 * public class OrderEntity {
 *     @Embedded
 *     private MoneyEmbeddable price;   // amount, currency columns
 *     @Embedded
 *     private MoneyEmbeddable discount; // amount, currency columns (CONFLICT!)
 * }
 *
 * // Generated JPA entity (with overrides - correct)
 * @Entity
 * public class OrderEntity {
 *     @Embedded
 *     @AttributeOverrides({
 *         @AttributeOverride(name = "amount", column = @Column(name = "price_amount")),
 *         @AttributeOverride(name = "currency", column = @Column(name = "price_currency"))
 *     })
 *     private MoneyEmbeddable price;
 *
 *     @Embedded
 *     @AttributeOverrides({
 *         @AttributeOverride(name = "amount", column = @Column(name = "discount_amount")),
 *         @AttributeOverride(name = "currency", column = @Column(name = "discount_currency"))
 *     })
 *     private MoneyEmbeddable discount;
 * }
 * }</pre>
 *
 * @param attributeName the name of the attribute in the embeddable class (e.g., "amount")
 * @param columnName the overridden column name (e.g., "price_amount")
 * @since 2.0.0
 */
public record AttributeOverride(String attributeName, String columnName) {

    /**
     * Creates an AttributeOverride with validation.
     *
     * @param attributeName the attribute name (must not be null or empty)
     * @param columnName the column name (must not be null or empty)
     * @throws IllegalArgumentException if attributeName or columnName is null or empty
     */
    public AttributeOverride {
        if (attributeName == null || attributeName.isEmpty()) {
            throw new IllegalArgumentException("attributeName cannot be null or empty");
        }
        if (columnName == null || columnName.isEmpty()) {
            throw new IllegalArgumentException("columnName cannot be null or empty");
        }
    }
}
