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
