package io.hexaglue.core.graph.model;

/**
 * Kind of edge in the application graph.
 *
 * <p>Edges are categorized by their origin:
 * <ul>
 *   <li><b>RAW</b> edges are extracted directly from the AST</li>
 *   <li><b>DERIVED</b> edges are computed from RAW edges</li>
 * </ul>
 */
public enum EdgeKind {

    // === Hierarchy edges (RAW) ===

    /**
     * Type extends another type (class → superclass).
     */
    EXTENDS,

    /**
     * Type implements an interface (class → interface).
     */
    IMPLEMENTS,

    // === Containment edges (RAW) ===

    /**
     * Type declares a member (type → field/method/constructor).
     */
    DECLARES,

    /**
     * Method/constructor has a parameter (method → parameter type).
     */
    HAS_PARAMETER,

    // === Type reference edges (RAW) ===

    /**
     * Field has a type (field → type).
     */
    FIELD_TYPE,

    /**
     * Method returns a type (method → type).
     */
    RETURN_TYPE,

    /**
     * Parameter has a type (method → type, via parameter).
     */
    PARAMETER_TYPE,

    /**
     * Method/constructor throws an exception (method → exception type).
     */
    THROWS,

    /**
     * Type argument in a generic type (e.g., List&lt;Order&gt; → Order).
     */
    TYPE_ARGUMENT,

    // === Annotation edges (RAW) ===

    /**
     * Element is annotated by an annotation (element → annotation type).
     */
    ANNOTATED_BY,

    // === Derived edges ===

    /**
     * Type is used in the signature of an interface method.
     * Derived from RETURN_TYPE and PARAMETER_TYPE edges on interface methods.
     */
    USES_IN_SIGNATURE,

    /**
     * Type is used as the element type of a collection.
     * Derived from TYPE_ARGUMENT edges on collection types.
     */
    USES_AS_COLLECTION_ELEMENT,

    /**
     * Type is referenced by another type (any usage).
     * Derived from all type reference edges.
     */
    REFERENCES;

    /**
     * Returns true if this is a RAW edge kind (directly from AST).
     */
    public boolean isRaw() {
        return this != USES_IN_SIGNATURE && this != USES_AS_COLLECTION_ELEMENT && this != REFERENCES;
    }

    /**
     * Returns true if this is a DERIVED edge kind (computed).
     */
    public boolean isDerived() {
        return !isRaw();
    }
}
