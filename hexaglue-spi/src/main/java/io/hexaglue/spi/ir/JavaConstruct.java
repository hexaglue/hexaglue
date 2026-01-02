package io.hexaglue.spi.ir;

/**
 * The Java construct used to define a type.
 *
 * <p>This is distinct from {@link DomainKind} - a VALUE_OBJECT can be
 * implemented as a CLASS or a RECORD.
 */
public enum JavaConstruct {

    /**
     * A regular Java class.
     */
    CLASS,

    /**
     * A Java record (immutable data carrier).
     */
    RECORD,

    /**
     * A Java enum.
     */
    ENUM,

    /**
     * A Java interface.
     */
    INTERFACE
}
