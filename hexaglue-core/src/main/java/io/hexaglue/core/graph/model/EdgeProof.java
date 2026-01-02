package io.hexaglue.core.graph.model;

import java.util.Objects;

/**
 * Proof of derivation for a derived edge.
 *
 * <p>Required for all DERIVED edges to explain how they were computed.
 *
 * @param sourceNode the node that was the source of the derivation
 * @param via how the derivation was made (e.g., "return", "param:0", "typeArg:0")
 * @param derivationRule the rule that produced this edge (e.g., "signature-usage", "collection-unwrap")
 */
public record EdgeProof(NodeId sourceNode, String via, String derivationRule) {

    // Common derivation rules
    public static final String RULE_SIGNATURE_USAGE = "signature-usage";
    public static final String RULE_COLLECTION_UNWRAP = "collection-unwrap";
    public static final String RULE_OPTIONAL_UNWRAP = "optional-unwrap";
    public static final String RULE_TYPE_REFERENCE = "type-reference";

    public EdgeProof {
        Objects.requireNonNull(sourceNode, "sourceNode cannot be null");
        Objects.requireNonNull(via, "via cannot be null");
        Objects.requireNonNull(derivationRule, "derivationRule cannot be null");
    }

    /**
     * Creates a proof for signature usage (method return/parameter type in interface).
     */
    public static EdgeProof signatureUsage(NodeId methodNode, String via) {
        return new EdgeProof(methodNode, via, RULE_SIGNATURE_USAGE);
    }

    /**
     * Creates a proof for collection element unwrap.
     */
    public static EdgeProof collectionUnwrap(NodeId fieldOrMethod, String via) {
        return new EdgeProof(fieldOrMethod, via, RULE_COLLECTION_UNWRAP);
    }

    /**
     * Creates a proof for optional unwrap.
     */
    public static EdgeProof optionalUnwrap(NodeId fieldOrMethod, String via) {
        return new EdgeProof(fieldOrMethod, via, RULE_OPTIONAL_UNWRAP);
    }

    /**
     * Creates a proof for general type reference.
     */
    public static EdgeProof typeReference(NodeId sourceNode, String via) {
        return new EdgeProof(sourceNode, via, RULE_TYPE_REFERENCE);
    }

    /**
     * Creates a "via return" proof.
     */
    public static EdgeProof viaReturn(NodeId methodNode, String rule) {
        return new EdgeProof(methodNode, "return", rule);
    }

    /**
     * Creates a "via parameter" proof.
     */
    public static EdgeProof viaParameter(NodeId methodNode, int paramIndex, String rule) {
        return new EdgeProof(methodNode, "param:" + paramIndex, rule);
    }

    /**
     * Creates a "via type argument" proof.
     */
    public static EdgeProof viaTypeArgument(NodeId sourceNode, int argIndex, String rule) {
        return new EdgeProof(sourceNode, "typeArg:" + argIndex, rule);
    }

    /**
     * Creates a "via field" proof.
     */
    public static EdgeProof viaField(NodeId fieldNode, String rule) {
        return new EdgeProof(fieldNode, "field", rule);
    }
}
