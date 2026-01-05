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

package io.hexaglue.core.classification.semantic;

import io.hexaglue.core.graph.model.NodeId;
import java.util.Objects;

/**
 * Facts about a user-code interface for semantic port classification.
 *
 * <p>These facts are used to determine if an interface is a DRIVING or DRIVEN port:
 * <ul>
 *   <li><b>DRIVING port</b>: {@code implementedByCore} is true</li>
 *   <li><b>DRIVEN port</b>: {@code usedByCore} is true AND ({@code missingImpl} OR {@code internalImplOnly})
 *       AND {@code hasPortAnnotation} is true (safety check)</li>
 * </ul>
 *
 * @param interfaceId the node id of the interface
 * @param implsProdCount number of implementations in production code
 * @param missingImpl true if there are no implementations in production code
 * @param internalImplOnly true if all implementations are domain anchors (internal/dev impls)
 * @param usedByCore true if at least one CoreAppClass depends on this interface
 * @param implementedByCore true if at least one CoreAppClass implements this interface
 * @param hasPortAnnotation true if the interface has a port-related annotation (jMolecules @Port, @Repository, etc.)
 */
public record InterfaceFacts(
        NodeId interfaceId,
        int implsProdCount,
        boolean missingImpl,
        boolean internalImplOnly,
        boolean usedByCore,
        boolean implementedByCore,
        boolean hasPortAnnotation) {

    public InterfaceFacts {
        Objects.requireNonNull(interfaceId, "interfaceId cannot be null");
        if (implsProdCount < 0) {
            throw new IllegalArgumentException("implsProdCount cannot be negative");
        }
    }

    /**
     * Creates InterfaceFacts indicating a potential DRIVING port.
     */
    public static InterfaceFacts drivingPort(NodeId interfaceId, int implsProdCount, boolean hasPortAnnotation) {
        return new InterfaceFacts(
                interfaceId,
                implsProdCount,
                implsProdCount == 0,
                false,
                false,
                true, // implementedByCore
                hasPortAnnotation);
    }

    /**
     * Creates InterfaceFacts indicating a potential DRIVEN port with missing implementation.
     */
    public static InterfaceFacts drivenPortMissingImpl(NodeId interfaceId, boolean hasPortAnnotation) {
        return new InterfaceFacts(
                interfaceId,
                0,
                true, // missingImpl
                false,
                true, // usedByCore
                false,
                hasPortAnnotation);
    }

    /**
     * Creates InterfaceFacts indicating a potential DRIVEN port with internal-only implementation.
     */
    public static InterfaceFacts drivenPortInternalImpl(
            NodeId interfaceId, int implsProdCount, boolean hasPortAnnotation) {
        return new InterfaceFacts(
                interfaceId,
                implsProdCount,
                false,
                true, // internalImplOnly
                true, // usedByCore
                false,
                hasPortAnnotation);
    }

    /**
     * Returns true if this interface qualifies as a DRIVING port.
     *
     * <p>A DRIVING port is an interface implemented by a CoreAppClass.
     */
    public boolean isDrivingPortCandidate() {
        return implementedByCore;
    }

    /**
     * Returns true if this interface qualifies as a DRIVEN port for generation.
     *
     * <p>A DRIVEN port is an interface that:
     * <ol>
     *   <li>Is used by a CoreAppClass (usedByCore)</li>
     *   <li>Has no production implementation (missingImpl) OR only domain-internal implementations (internalImplOnly)</li>
     *   <li>Has a port annotation (safety check to avoid generating for forgotten interfaces)</li>
     * </ol>
     */
    public boolean isDrivenPortCandidate() {
        return usedByCore && (missingImpl || internalImplOnly) && hasPortAnnotation;
    }

    /**
     * Returns true if this interface qualifies as a DRIVEN port without the annotation check.
     *
     * <p>Use this for less strict classification (e.g., when annotations are optional).
     */
    public boolean isDrivenPortCandidateWithoutAnnotationCheck() {
        return usedByCore && (missingImpl || internalImplOnly);
    }

    /**
     * Returns true if this interface has production implementations.
     */
    public boolean hasProdImpl() {
        return implsProdCount > 0;
    }

    /**
     * Returns true if this interface is internal (has implementations but all are domain anchors).
     */
    public boolean isInternalInterface() {
        return hasProdImpl() && internalImplOnly && !implementedByCore;
    }

    /**
     * Returns true if this interface is not clearly classified as a port.
     */
    public boolean isUndecided() {
        return !isDrivingPortCandidate() && !isDrivenPortCandidate() && !isInternalInterface();
    }
}
