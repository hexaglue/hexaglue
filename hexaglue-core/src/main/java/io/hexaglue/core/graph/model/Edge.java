package io.hexaglue.core.graph.model;

import java.util.Objects;

/**
 * An edge in the application graph connecting two nodes.
 *
 * <p>Edges can be RAW (extracted directly from AST) or DERIVED (computed).
 * DERIVED edges must include a proof explaining the derivation.
 *
 * @param from the source node id
 * @param to the target node id
 * @param kind the kind of edge
 * @param origin whether this edge is RAW or DERIVED
 * @param proof the derivation proof (required for DERIVED, null for RAW)
 */
public record Edge(NodeId from, NodeId to, EdgeKind kind, EdgeOrigin origin, EdgeProof proof) {

    public Edge {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(origin, "origin cannot be null");

        // Validate: DERIVED edges must have proof
        if (origin == EdgeOrigin.DERIVED && proof == null) {
            throw new IllegalArgumentException("DERIVED edges must have a proof");
        }
        // Validate: RAW edges should not have proof
        if (origin == EdgeOrigin.RAW && proof != null) {
            throw new IllegalArgumentException("RAW edges should not have a proof");
        }
    }

    /**
     * Creates a RAW edge (no proof required).
     */
    public static Edge raw(NodeId from, NodeId to, EdgeKind kind) {
        return new Edge(from, to, kind, EdgeOrigin.RAW, null);
    }

    /**
     * Creates a DERIVED edge with proof.
     */
    public static Edge derived(NodeId from, NodeId to, EdgeKind kind, EdgeProof proof) {
        return new Edge(from, to, kind, EdgeOrigin.DERIVED, proof);
    }

    // === Factory methods for common RAW edges ===

    /**
     * Creates an EXTENDS edge (type → supertype).
     */
    public static Edge extends_(NodeId subtype, NodeId supertype) {
        return raw(subtype, supertype, EdgeKind.EXTENDS);
    }

    /**
     * Creates an IMPLEMENTS edge (type → interface).
     */
    public static Edge implements_(NodeId type, NodeId interfaceType) {
        return raw(type, interfaceType, EdgeKind.IMPLEMENTS);
    }

    /**
     * Creates a DECLARES edge (type → member).
     */
    public static Edge declares(NodeId type, NodeId member) {
        return raw(type, member, EdgeKind.DECLARES);
    }

    /**
     * Creates a FIELD_TYPE edge (field → type).
     */
    public static Edge fieldType(NodeId field, NodeId type) {
        return raw(field, type, EdgeKind.FIELD_TYPE);
    }

    /**
     * Creates a RETURN_TYPE edge (method → type).
     */
    public static Edge returnType(NodeId method, NodeId type) {
        return raw(method, type, EdgeKind.RETURN_TYPE);
    }

    /**
     * Creates a PARAMETER_TYPE edge (method → type).
     */
    public static Edge parameterType(NodeId method, NodeId type) {
        return raw(method, type, EdgeKind.PARAMETER_TYPE);
    }

    /**
     * Creates a THROWS edge (method → exception type).
     */
    public static Edge throws_(NodeId method, NodeId exceptionType) {
        return raw(method, exceptionType, EdgeKind.THROWS);
    }

    /**
     * Creates a TYPE_ARGUMENT edge (parameterized reference → type argument).
     */
    public static Edge typeArgument(NodeId source, NodeId typeArg) {
        return raw(source, typeArg, EdgeKind.TYPE_ARGUMENT);
    }

    /**
     * Creates an ANNOTATED_BY edge (element → annotation type).
     */
    public static Edge annotatedBy(NodeId element, NodeId annotationType) {
        return raw(element, annotationType, EdgeKind.ANNOTATED_BY);
    }

    // === Factory methods for common DERIVED edges ===

    /**
     * Creates a USES_IN_SIGNATURE edge.
     */
    public static Edge usesInSignature(NodeId interfaceType, NodeId usedType, EdgeProof proof) {
        return derived(interfaceType, usedType, EdgeKind.USES_IN_SIGNATURE, proof);
    }

    /**
     * Creates a USES_AS_COLLECTION_ELEMENT edge.
     */
    public static Edge usesAsCollectionElement(NodeId source, NodeId elementType, EdgeProof proof) {
        return derived(source, elementType, EdgeKind.USES_AS_COLLECTION_ELEMENT, proof);
    }

    /**
     * Creates a REFERENCES edge.
     */
    public static Edge references(NodeId source, NodeId target, EdgeProof proof) {
        return derived(source, target, EdgeKind.REFERENCES, proof);
    }

    // === Convenience methods ===

    /**
     * Returns true if this is a RAW edge.
     */
    public boolean isRaw() {
        return origin == EdgeOrigin.RAW;
    }

    /**
     * Returns true if this is a DERIVED edge.
     */
    public boolean isDerived() {
        return origin == EdgeOrigin.DERIVED;
    }

    /**
     * Returns true if both endpoints are type nodes.
     */
    public boolean connectsTypes() {
        return from.isType() && to.isType();
    }

    /**
     * Returns true if this edge goes from a type to a member.
     */
    public boolean isTypeToMember() {
        return from.isType() && to.isMember();
    }

    /**
     * Returns true if this edge goes from a member to a type.
     */
    public boolean isMemberToType() {
        return from.isMember() && to.isType();
    }
}
