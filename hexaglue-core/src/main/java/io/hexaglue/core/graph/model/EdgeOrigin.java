package io.hexaglue.core.graph.model;

/**
 * Origin of an edge in the application graph.
 */
public enum EdgeOrigin {

    /**
     * Edge is extracted directly from the AST.
     * No proof is required for RAW edges.
     */
    RAW,

    /**
     * Edge is computed/derived from other edges.
     * A proof is required explaining the derivation.
     */
    DERIVED
}
