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

package io.hexaglue.spi.ir;

import java.util.List;

/**
 * A port interface in the hexagonal architecture.
 *
 * @param qualifiedName the fully qualified interface name
 * @param simpleName the simple interface name
 * @param packageName the package name
 * @param kind the port classification
 * @param direction driving (inbound) or driven (outbound)
 * @param confidence how confident the classification is
 * @param managedTypes the domain types used in this port's signatures
 * @param primaryManagedType the main aggregate managed by this port (may be null if none or generic port)
 * @param methods the port methods
 * @param annotations the annotation qualified names present on this interface
 * @param sourceRef source location for diagnostics
 * @since 2.0.0
 */
public record Port(
        String qualifiedName,
        String simpleName,
        String packageName,
        PortKind kind,
        PortDirection direction,
        ConfidenceLevel confidence,
        List<String> managedTypes,
        String primaryManagedType,
        List<PortMethod> methods,
        List<String> annotations,
        SourceRef sourceRef) {

    /**
     * Backward-compatible constructor without primaryManagedType.
     *
     * @deprecated Use the full constructor with primaryManagedType
     * @since 2.0.0
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public Port(
            String qualifiedName,
            String simpleName,
            String packageName,
            PortKind kind,
            PortDirection direction,
            ConfidenceLevel confidence,
            List<String> managedTypes,
            List<PortMethod> methods,
            List<String> annotations,
            SourceRef sourceRef) {
        this(
                qualifiedName,
                simpleName,
                packageName,
                kind,
                direction,
                confidence,
                managedTypes,
                null,
                methods,
                annotations,
                sourceRef);
    }

    /**
     * Returns true if this is a repository port.
     */
    public boolean isRepository() {
        return kind == PortKind.REPOSITORY;
    }

    /**
     * Returns true if this is a driving (primary/inbound) port.
     */
    public boolean isDriving() {
        return direction == PortDirection.DRIVING;
    }

    /**
     * Returns true if this is a driven (secondary/outbound) port.
     */
    public boolean isDriven() {
        return direction == PortDirection.DRIVEN;
    }
}
