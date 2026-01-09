/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.core.graph.model.edges;

import io.hexaglue.core.graph.model.Edge;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.EdgeProof;
import io.hexaglue.core.graph.model.NodeId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a method invocation edge with calling context and invocation metadata.
 *
 * <p>Method call edges capture runtime behavioral relationships that are critical for:
 * <ul>
 *   <li>Detecting architectural patterns (use case calling repository)</li>
 *   <li>Identifying service layer boundaries</li>
 *   <li>Understanding control flow and dependencies</li>
 *   <li>Analyzing method coupling and cohesion</li>
 * </ul>
 *
 * <p>This information is extracted from method body AST analysis and is more
 * expensive to compute than signature-based dependencies.
 *
 * <p>Example usage:
 * <pre>{@code
 * MethodCallEdge edge = MethodCallEdge.builder()
 *     .from(NodeId.method("com.example.OrderService", "processOrder", "Order"))
 *     .to(NodeId.method("com.example.OrderRepository", "save", "Order"))
 *     .callingMethod(NodeId.method("com.example.OrderService", "processOrder", "Order"))
 *     .calledMethod(NodeId.method("com.example.OrderRepository", "save", "Order"))
 *     .invocationCount(3)
 *     .isStaticCall(false)
 *     .isConstructorCall(false)
 *     .build();
 * }</pre>
 *
 * @param from the source node (caller)
 * @param to the target node (callee)
 * @param callingMethod the method containing the invocation
 * @param calledMethod the method being invoked
 * @param invocationCount number of invocations in the calling method (1 = single call, >1 = multiple sites)
 * @param isStaticCall true if this is a static method call
 * @param isConstructorCall true if this is a constructor invocation
 * @since 3.0.0
 */
public record MethodCallEdge(
        NodeId from,
        NodeId to,
        NodeId callingMethod,
        NodeId calledMethod,
        int invocationCount,
        boolean isStaticCall,
        boolean isConstructorCall)
        implements TypedEdge {

    /**
     * EdgeKind for method call edges - this is a derived edge.
     */
    private static final EdgeKind METHOD_CALL_KIND = EdgeKind.REFERENCES;

    public MethodCallEdge {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
        Objects.requireNonNull(callingMethod, "callingMethod cannot be null");
        Objects.requireNonNull(calledMethod, "calledMethod cannot be null");

        if (invocationCount < 1) {
            throw new IllegalArgumentException("invocationCount must be >= 1 (got " + invocationCount + ")");
        }
    }

    /**
     * Creates a simple method call edge with count 1.
     *
     * @param callingMethod the calling method
     * @param calledMethod the called method
     * @return the method call edge
     */
    public static MethodCallEdge create(NodeId callingMethod, NodeId calledMethod) {
        return new MethodCallEdge(callingMethod, calledMethod, callingMethod, calledMethod, 1, false, false);
    }

    /**
     * Creates a static method call edge.
     *
     * @param callingMethod the calling method
     * @param calledMethod the called static method
     * @return the method call edge
     */
    public static MethodCallEdge staticCall(NodeId callingMethod, NodeId calledMethod) {
        return new MethodCallEdge(callingMethod, calledMethod, callingMethod, calledMethod, 1, true, false);
    }

    /**
     * Creates a constructor call edge.
     *
     * @param callingMethod the calling method
     * @param constructor the constructor being invoked
     * @return the method call edge
     */
    public static MethodCallEdge constructorCall(NodeId callingMethod, NodeId constructor) {
        return new MethodCallEdge(callingMethod, constructor, callingMethod, constructor, 1, false, true);
    }

    /**
     * Creates a builder for fluent construction.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public EdgeKind kind() {
        return METHOD_CALL_KIND;
    }

    @Override
    public Map<String, Object> metadata() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("edgeType", "METHOD_CALL");
        meta.put("callingMethod", callingMethod.value());
        meta.put("calledMethod", calledMethod.value());
        meta.put("invocationCount", invocationCount);
        meta.put("isStaticCall", isStaticCall);
        meta.put("isConstructorCall", isConstructorCall);
        return Map.copyOf(meta);
    }

    @Override
    public Edge toEdge() {
        String via = String.format(
                "invocation(%s->%s, count=%d, static=%b, ctor=%b)",
                callingMethod.value(), calledMethod.value(), invocationCount, isStaticCall, isConstructorCall);
        EdgeProof proof = new EdgeProof(from, via, "METHOD_CALL");
        return Edge.derived(from, to, kind(), proof);
    }

    /**
     * Returns true if this call is invoked multiple times in the calling method.
     *
     * @return true if invocation count > 1
     */
    public boolean hasMultipleInvocations() {
        return invocationCount > 1;
    }

    /**
     * Returns true if this is a regular instance method call.
     *
     * @return true if not static and not constructor
     */
    public boolean isInstanceCall() {
        return !isStaticCall && !isConstructorCall;
    }

    /**
     * Builder for MethodCallEdge.
     */
    public static final class Builder {
        private NodeId from;
        private NodeId to;
        private NodeId callingMethod;
        private NodeId calledMethod;
        private int invocationCount = 1;
        private boolean isStaticCall = false;
        private boolean isConstructorCall = false;

        private Builder() {}

        public Builder from(NodeId from) {
            this.from = from;
            return this;
        }

        public Builder to(NodeId to) {
            this.to = to;
            return this;
        }

        public Builder callingMethod(NodeId callingMethod) {
            this.callingMethod = callingMethod;
            return this;
        }

        public Builder calledMethod(NodeId calledMethod) {
            this.calledMethod = calledMethod;
            return this;
        }

        public Builder invocationCount(int invocationCount) {
            this.invocationCount = invocationCount;
            return this;
        }

        public Builder isStaticCall(boolean isStaticCall) {
            this.isStaticCall = isStaticCall;
            return this;
        }

        public Builder isConstructorCall(boolean isConstructorCall) {
            this.isConstructorCall = isConstructorCall;
            return this;
        }

        public MethodCallEdge build() {
            return new MethodCallEdge(
                    from, to, callingMethod, calledMethod, invocationCount, isStaticCall, isConstructorCall);
        }
    }
}
