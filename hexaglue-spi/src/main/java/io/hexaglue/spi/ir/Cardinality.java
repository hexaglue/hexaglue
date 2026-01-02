package io.hexaglue.spi.ir;

/**
 * Cardinality of a property.
 */
public enum Cardinality {

    /**
     * Single required value (e.g., {@code String name}).
     */
    SINGLE,

    /**
     * Optional value (e.g., {@code Optional<String> middleName}).
     */
    OPTIONAL,

    /**
     * Collection of values (e.g., {@code List<LineItem> items}).
     */
    COLLECTION
}
