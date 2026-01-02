package io.hexaglue.spi.ir;

/**
 * Describes how an identity field is wrapped.
 *
 * <p>In DDD, identity types are often wrapped in value object records or classes
 * to provide type safety and domain meaning. This enum describes the wrapping style.
 *
 * <p>Examples:
 * <ul>
 *   <li>RECORD: {@code record OrderId(UUID value) {}}</li>
 *   <li>CLASS: {@code class OrderId { private final UUID value; }}</li>
 *   <li>NONE: {@code UUID id;} (unwrapped primitive/wrapper type)</li>
 * </ul>
 */
public enum IdentityWrapperKind {

    /**
     * Identity is wrapped in a record.
     * Example: {@code record OrderId(UUID value) {}}
     */
    RECORD,

    /**
     * Identity is wrapped in a class.
     * Example: {@code class OrderId { private final UUID value; }}
     */
    CLASS,

    /**
     * Identity is not wrapped (uses primitive/wrapper type directly).
     * Example: {@code private UUID id;}
     */
    NONE
}
