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
 * The type of relationship between domain types in a composition graph.
 *
 * <p>This enum represents the different ways a domain type can reference or contain
 * another domain type. Understanding these relationship types is crucial for determining
 * aggregate boundaries and detecting architectural anomalies.
 *
 * <p>Relationship types follow DDD best practices:
 * <ul>
 *   <li><b>COMPOSITION</b>: Strong ownership, lifecycle coupling (aggregate boundary)</li>
 *   <li><b>REFERENCE_BY_ID</b>: Weak reference via identifier (crosses aggregate boundary)</li>
 *   <li><b>DIRECT_REFERENCE</b>: Direct object reference (potential design smell)</li>
 * </ul>
 *
 * @since 3.0.0
 */
public enum RelationType {

    /**
     * The source owns the target with full lifecycle coupling.
     *
     * <p>This represents composition in the DDD sense - the target is part of the source's
     * aggregate boundary. When the source is deleted, the target should be deleted too.
     *
     * <p>Typical patterns:
     * <ul>
     *   <li>Value object fields: {@code Money price;}</li>
     *   <li>Embedded entities: {@code List<OrderLine> lines;}</li>
     *   <li>Record components: {@code record Address(String street, String city)}</li>
     * </ul>
     *
     * <p>This relationship type indicates the target is <b>inside</b> the aggregate boundary.
     */
    COMPOSITION,

    /**
     * The source references the target via its identifier only.
     *
     * <p>This represents a reference across aggregate boundaries in DDD. The source holds
     * only the target's ID, not a direct object reference, ensuring proper aggregate isolation.
     *
     * <p>Typical patterns:
     * <ul>
     *   <li>ID wrapper fields: {@code CustomerId customerId;}</li>
     *   <li>Primitive IDs: {@code UUID productId;}</li>
     *   <li>Collections of IDs: {@code Set<OrderId> relatedOrders;}</li>
     * </ul>
     *
     * <p>This relationship type indicates the target is <b>outside</b> the aggregate boundary.
     * This is the <b>recommended</b> way to reference other aggregates in DDD.
     */
    REFERENCE_BY_ID,

    /**
     * The source holds a direct reference to another aggregate root.
     *
     * <p>This represents a direct object reference to an entity or aggregate root,
     * which is generally considered a <b>design smell</b> in DDD. Direct references
     * between aggregates violate aggregate isolation and can lead to issues with
     * transactions, consistency boundaries, and distributed systems.
     *
     * <p>Example (anti-pattern):
     * <pre>{@code
     * class Order {
     *     Customer customer; // Should be: CustomerId customerId;
     * }
     * }</pre>
     *
     * <p>This relationship type triggers an <b>anomaly warning</b> during analysis.
     * Consider refactoring to use {@link #REFERENCE_BY_ID} instead.
     */
    DIRECT_REFERENCE;

    /**
     * Returns true if this relationship crosses aggregate boundaries.
     *
     * <p>References by ID and direct references both point outside the aggregate boundary,
     * while composition stays within it.
     *
     * @return true if the relationship is REFERENCE_BY_ID or DIRECT_REFERENCE
     */
    public boolean crossesAggregateBoundary() {
        return this == REFERENCE_BY_ID || this == DIRECT_REFERENCE;
    }

    /**
     * Returns true if this relationship is a design smell.
     *
     * <p>Only DIRECT_REFERENCE is considered a smell, as it violates DDD aggregate isolation.
     *
     * @return true if the relationship is DIRECT_REFERENCE
     */
    public boolean isSmell() {
        return this == DIRECT_REFERENCE;
    }

    /**
     * Returns true if this relationship indicates strong ownership.
     *
     * <p>Only COMPOSITION represents true ownership where lifecycles are coupled.
     *
     * @return true if the relationship is COMPOSITION
     */
    public boolean isOwnership() {
        return this == COMPOSITION;
    }
}
