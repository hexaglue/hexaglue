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

package io.hexaglue.spi.ir;

/**
 * The kind of relationship between domain types.
 *
 * <p>This enum models JPA-style relationships for code generation.
 */
public enum RelationKind {

    /**
     * One-to-one relationship.
     * Example: Order has one ShippingAddress.
     */
    ONE_TO_ONE,

    /**
     * One-to-many relationship.
     * Example: Order has many LineItems.
     */
    ONE_TO_MANY,

    /**
     * Many-to-one relationship.
     * Example: LineItem belongs to one Order.
     */
    MANY_TO_ONE,

    /**
     * Many-to-many relationship.
     * Example: Product belongs to many Categories, Category has many Products.
     */
    MANY_TO_MANY,

    /**
     * Embedded value object.
     * Example: Order embeds Address (value object).
     */
    EMBEDDED,

    /**
     * Collection of embeddable elements.
     * Example: Order has a collection of Tags (value objects).
     */
    ELEMENT_COLLECTION
}
