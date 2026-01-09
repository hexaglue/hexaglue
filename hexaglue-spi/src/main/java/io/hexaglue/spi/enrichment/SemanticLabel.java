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

package io.hexaglue.spi.enrichment;

/**
 * Semantic labels applied to types and methods during enrichment.
 *
 * <p>Labels provide additional semantic information beyond structural classification.
 * They are detected by enrichment plugins and used by generators and auditors to
 * make more informed decisions.
 *
 * <p>Labels are organized into two categories:
 * <ul>
 *   <li><b>Method-level labels</b>: Applied to individual methods</li>
 *   <li><b>Type-level labels</b>: Applied to entire types</li>
 * </ul>
 *
 * @since 3.0.0
 */
public enum SemanticLabel {

    // === Method-level labels ===

    /**
     * Factory method: Static method that returns an instance of the declaring type.
     *
     * <p>Examples:
     * <pre>{@code
     * static Order createOrder(Customer customer) { ... }
     * static Money of(BigDecimal amount, Currency currency) { ... }
     * }</pre>
     */
    FACTORY_METHOD,

    /**
     * Invariant validator: Method that validates domain invariants.
     *
     * <p>Typically contains conditional logic that throws domain exceptions or returns
     * validation results. Often named {@code validate*}, {@code check*}, {@code ensure*}.
     *
     * <p>Examples:
     * <pre>{@code
     * void validateQuantity(int quantity) {
     *     if (quantity <= 0) throw new InvalidQuantityException();
     * }
     * }</pre>
     */
    INVARIANT_VALIDATOR,

    /**
     * Collection manager: Method that modifies entity collections.
     *
     * <p>Methods that add/remove items from collections while enforcing invariants.
     * Often found in aggregate roots managing entity collections.
     *
     * <p>Examples:
     * <pre>{@code
     * void addLineItem(OrderLine line) { ... }
     * void removeLineItem(LineItemId id) { ... }
     * }</pre>
     */
    COLLECTION_MANAGER,

    /**
     * Lifecycle method: Controls entity lifecycle transitions.
     *
     * <p>Methods that change entity state or status, often implementing state machines.
     *
     * <p>Examples:
     * <pre>{@code
     * void activate() { this.status = ACTIVE; }
     * void cancel() { this.status = CANCELLED; }
     * }</pre>
     */
    LIFECYCLE_METHOD,

    /**
     * Domain operation: Complex business logic method.
     *
     * <p>Methods implementing core business rules that don't fit other categories.
     */
    DOMAIN_OPERATION,

    /**
     * Getter: Simple accessor method returning a field value.
     */
    GETTER,

    /**
     * Setter: Simple mutator method setting a field value.
     */
    SETTER,

    /**
     * Delegation method: Forwards calls to another object.
     *
     * <p>Methods that simply delegate to a field or dependency.
     */
    DELEGATION_METHOD,

    /**
     * Event handler: Processes domain events.
     *
     * <p>Methods annotated with event handling annotations or following naming conventions.
     */
    EVENT_HANDLER,

    /**
     * Command handler: Processes commands in CQRS architectures.
     *
     * <p>Methods that handle command objects and coordinate domain logic.
     */
    COMMAND_HANDLER,

    // === Type-level labels ===

    /**
     * Immutable type: All fields are final and the type is thread-safe.
     *
     * <p>Often used for value objects and DTOs.
     */
    IMMUTABLE_TYPE,

    /**
     * Side-effect free: Type has no mutable state.
     *
     * <p>All methods return new instances rather than modifying state.
     */
    SIDE_EFFECT_FREE,

    /**
     * Event publisher: Type publishes domain events.
     *
     * <p>Contains logic for collecting and publishing domain events.
     */
    EVENT_PUBLISHER,

    /**
     * Aggregate boundary: Marks the aggregate root boundary.
     *
     * <p>Applied to aggregate roots that manage entity clusters.
     */
    AGGREGATE_BOUNDARY,

    /**
     * Anti-corruption layer: Translates between different domain models.
     *
     * <p>Adapters that prevent external models from polluting the domain.
     */
    ANTI_CORRUPTION_LAYER
}
