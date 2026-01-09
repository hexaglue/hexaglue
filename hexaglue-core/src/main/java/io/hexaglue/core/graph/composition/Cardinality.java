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

package io.hexaglue.core.graph.composition;

/**
 * The cardinality of a relationship between domain types.
 *
 * <p>Cardinality describes whether a relationship represents a single reference
 * or a collection (one-to-many). This information is essential for:
 * <ul>
 *   <li>JPA mapping generation (OneToOne vs OneToMany)</li>
 *   <li>Understanding aggregate complexity</li>
 *   <li>Cascade operation semantics</li>
 * </ul>
 *
 * @since 3.0.0
 */
public enum Cardinality {

    /**
     * Single reference (one-to-one relationship).
     *
     * <p>The source contains exactly one instance of the target type.
     *
     * <p>Examples:
     * <pre>{@code
     * Money price;              // ONE
     * Address shippingAddress;  // ONE
     * CustomerId customerId;    // ONE
     * }</pre>
     */
    ONE,

    /**
     * Collection reference (one-to-many relationship).
     *
     * <p>The source contains multiple instances of the target type,
     * typically via Collection, List, Set, or array.
     *
     * <p>Examples:
     * <pre>{@code
     * List<OrderLine> lines;        // MANY
     * Set<String> tags;             // MANY
     * OrderItem[] items;            // MANY
     * Map<String, Money> prices;    // MANY (values)
     * }</pre>
     */
    MANY;

    /**
     * Returns true if this cardinality represents a single element.
     *
     * @return true if this is ONE
     */
    public boolean isSingle() {
        return this == ONE;
    }

    /**
     * Returns true if this cardinality represents multiple elements.
     *
     * @return true if this is MANY
     */
    public boolean isMultiple() {
        return this == MANY;
    }
}
