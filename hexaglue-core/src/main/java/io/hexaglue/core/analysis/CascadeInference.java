package io.hexaglue.core.analysis;

import io.hexaglue.core.classification.ClassificationContext;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.spi.ir.CascadeType;
import io.hexaglue.spi.ir.RelationKind;

/**
 * Infers JPA cascade types based on DDD relationship semantics.
 *
 * <p>Cascade rules are based on aggregate boundaries:
 * <ul>
 *   <li>Entities within the same aggregate: CASCADE ALL (owned lifecycle)</li>
 *   <li>References to other aggregates: NO CASCADE (independent lifecycle)</li>
 *   <li>Embedded value objects: handled by JPA (no cascade annotation needed)</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * CascadeInference inference = new CascadeInference();
 *
 * // Order -> LineItem (child entity) = CASCADE ALL
 * CascadeType cascade1 = inference.infer(order, lineItem, ONE_TO_MANY, context);
 *
 * // Order -> Customer (another aggregate) = NONE
 * CascadeType cascade2 = inference.infer(order, customer, MANY_TO_ONE, context);
 * }</pre>
 */
public final class CascadeInference {

    /**
     * Infers the appropriate cascade type for a relationship.
     *
     * @param ownerType the type containing the relationship
     * @param targetType the target type of the relationship
     * @param relationKind the kind of relationship
     * @param context the classification context with domain classifications
     * @return the inferred cascade type
     */
    public CascadeType infer(
            TypeNode ownerType, TypeNode targetType, RelationKind relationKind, ClassificationContext context) {

        // Rule 1: Embedded relationships don't need cascade (JPA handles them)
        if (relationKind == RelationKind.EMBEDDED || relationKind == RelationKind.ELEMENT_COLLECTION) {
            return CascadeType.NONE;
        }

        // Rule 2: Reference to another aggregate = NO cascade
        // Aggregates have independent lifecycles
        if (context.isAggregate(targetType.id())) {
            return CascadeType.NONE;
        }

        // Rule 3: Collection of entities within the aggregate = CASCADE ALL
        // Child entities' lifecycle is managed by the aggregate root
        if (relationKind == RelationKind.ONE_TO_MANY && context.isEntity(targetType.id())) {
            return CascadeType.ALL;
        }

        // Rule 4: One-to-one to an entity within the aggregate
        if (relationKind == RelationKind.ONE_TO_ONE && context.isEntity(targetType.id())) {
            // Owned one-to-one relationships should cascade
            return CascadeType.ALL;
        }

        // Rule 5: Many-to-one relationships typically don't cascade
        // The "many" side doesn't own the "one" side
        if (relationKind == RelationKind.MANY_TO_ONE) {
            // Only cascade if the target is a child entity (not an aggregate)
            if (context.isEntity(targetType.id()) && !context.isAggregate(targetType.id())) {
                // This is an unusual case - many pointing to a child entity
                // Generally, we don't cascade
                return CascadeType.NONE;
            }
            return CascadeType.NONE;
        }

        // Rule 6: Many-to-many relationships typically use PERSIST/MERGE but not REMOVE
        if (relationKind == RelationKind.MANY_TO_MANY) {
            return CascadeType.PERSIST;
        }

        // Default: no cascade
        return CascadeType.NONE;
    }

    /**
     * Determines if orphan removal should be enabled for a relationship.
     *
     * <p>Orphan removal is appropriate when:
     * <ul>
     *   <li>The relationship is ONE_TO_MANY</li>
     *   <li>The target is an entity within the same aggregate</li>
     *   <li>The target has no meaning without the owner (owned lifecycle)</li>
     * </ul>
     *
     * @param relationKind the kind of relationship
     * @param targetType the target type
     * @param context the classification context
     * @return true if orphan removal should be enabled
     */
    public boolean shouldEnableOrphanRemoval(
            RelationKind relationKind, TypeNode targetType, ClassificationContext context) {

        // Only for ONE_TO_MANY relationships
        if (relationKind != RelationKind.ONE_TO_MANY) {
            return false;
        }

        // Target must be an entity (not an aggregate root)
        if (!context.isEntity(targetType.id()) || context.isAggregate(targetType.id())) {
            return false;
        }

        // Child entities within an aggregate should have orphan removal
        return true;
    }
}
