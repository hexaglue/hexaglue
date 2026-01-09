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

package io.hexaglue.core.frontend;

import java.util.List;
import java.util.Objects;

/**
 * Analysis results for a method body containing invocations, field accesses, and complexity metrics.
 *
 * <p>Method body analysis is expensive because it requires:
 * <ul>
 *   <li>AST traversal of all statements and expressions</li>
 *   <li>Type resolution for method invocations</li>
 *   <li>Symbol resolution for field references</li>
 *   <li>Control flow analysis for complexity calculation</li>
 * </ul>
 *
 * <p>This record caches the analysis results to avoid redundant traversal.
 * It is immutable and thread-safe.
 *
 * <p>Example usage:
 * <pre>{@code
 * MethodBodyAnalysis analysis = CachedSpoonAnalyzer.analyzeMethodBody(method);
 *
 * // Check if method calls repository
 * boolean callsRepo = analysis.invocations().stream()
 *     .anyMatch(inv -> inv.targetMethod().contains("Repository"));
 *
 * // Check complexity
 * if (analysis.cyclomaticComplexity() > 10) {
 *     log.warn("Method {} is too complex", method);
 * }
 * }</pre>
 *
 * @param invocations list of method invocations found in the body
 * @param fieldAccesses list of field accesses found in the body
 * @param cyclomaticComplexity McCabe cyclomatic complexity of the method (1 = straight-line code)
 * @since 3.0.0
 */
public record MethodBodyAnalysis(
        List<MethodInvocation> invocations, List<FieldAccess> fieldAccesses, int cyclomaticComplexity) {

    public MethodBodyAnalysis {
        Objects.requireNonNull(invocations, "invocations cannot be null");
        Objects.requireNonNull(fieldAccesses, "fieldAccesses cannot be null");

        if (cyclomaticComplexity < 1) {
            throw new IllegalArgumentException("cyclomaticComplexity must be >= 1 (got " + cyclomaticComplexity + ")");
        }

        // Make defensive copies for immutability
        invocations = List.copyOf(invocations);
        fieldAccesses = List.copyOf(fieldAccesses);
    }

    /**
     * Creates an empty analysis (no invocations, no field accesses, complexity 1).
     *
     * @return an empty analysis
     */
    public static MethodBodyAnalysis empty() {
        return new MethodBodyAnalysis(List.of(), List.of(), 1);
    }

    /**
     * Returns true if the method body is empty (no invocations or field accesses).
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return invocations.isEmpty() && fieldAccesses.isEmpty();
    }

    /**
     * Returns true if the method has high complexity (> 10).
     *
     * @return true if complex
     */
    public boolean isComplex() {
        return cyclomaticComplexity > 10;
    }

    /**
     * Returns the number of statements analyzed.
     *
     * @return the statement count (approximation: invocations + field accesses)
     */
    public int statementCount() {
        return invocations.size() + fieldAccesses.size();
    }

    /**
     * Represents a method invocation found in the body.
     *
     * @param targetMethod the qualified name of the method being called
     * @param isStatic true if this is a static method call
     * @param lineNumber the source line number
     * @since 3.0.0
     */
    public record MethodInvocation(String targetMethod, boolean isStatic, int lineNumber) {

        public MethodInvocation {
            Objects.requireNonNull(targetMethod, "targetMethod cannot be null");
            if (lineNumber < 0) {
                throw new IllegalArgumentException("lineNumber must be >= 0 (got " + lineNumber + ")");
            }
        }
    }

    /**
     * Represents a field access found in the body.
     *
     * @param fieldName the name of the field being accessed
     * @param isWrite true if the field is being written, false if read
     * @param lineNumber the source line number
     * @since 3.0.0
     */
    public record FieldAccess(String fieldName, boolean isWrite, int lineNumber) {

        public FieldAccess {
            Objects.requireNonNull(fieldName, "fieldName cannot be null");
            if (lineNumber < 0) {
                throw new IllegalArgumentException("lineNumber must be >= 0 (got " + lineNumber + ")");
            }
        }

        /**
         * Returns true if this is a read access.
         *
         * @return true if read
         */
        public boolean isRead() {
            return !isWrite;
        }
    }
}
