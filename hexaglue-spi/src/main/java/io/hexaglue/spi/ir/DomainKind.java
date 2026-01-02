package io.hexaglue.spi.ir;

/**
 * Classification of domain types according to DDD tactical patterns.
 */
public enum DomainKind {

    /**
     * An aggregate root - the entry point to an aggregate.
     * Has identity and manages its invariants.
     */
    AGGREGATE_ROOT,

    /**
     * An entity within an aggregate (not the root).
     * Has identity but is accessed through its aggregate root.
     */
    ENTITY,

    /**
     * A value object - immutable, no identity, defined by its attributes.
     */
    VALUE_OBJECT,

    /**
     * An identifier type - wraps a primitive identity value.
     */
    IDENTIFIER,

    /**
     * A domain event - immutable record of something that happened.
     */
    DOMAIN_EVENT,

    /**
     * A domain service - stateless operation that doesn't belong to an entity.
     */
    DOMAIN_SERVICE
}
