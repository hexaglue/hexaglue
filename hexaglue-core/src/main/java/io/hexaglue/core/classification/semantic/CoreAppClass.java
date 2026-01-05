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
import java.util.Set;

/**
 * Represents a "Core Application Class" - a domain anchor class that
 * implements or depends on user-code interfaces.
 *
 * <p>CoreAppClass is the key concept for semantic port classification.
 * It serves as the "pivot" that links:
 * <ul>
 *   <li><b>Implemented interfaces</b> → DRIVING ports (the application provides these)</li>
 *   <li><b>Depended interfaces</b> → DRIVEN ports (the application consumes these)</li>
 * </ul>
 *
 * <p>Classification rules:
 * <ul>
 *   <li><b>APPLICATION_SERVICE (pivot)</b>: implements DRIVING + depends on DRIVEN</li>
 *   <li><b>INBOUND_ONLY</b>: implements DRIVING, no DRIVEN dependencies</li>
 *   <li><b>OUTBOUND_ONLY</b>: depends on DRIVEN, no DRIVING implementation</li>
 * </ul>
 *
 * @param classId the node id of the class
 * @param implementedInterfaces interfaces this class implements (potential DRIVING ports)
 * @param dependedInterfaces interfaces this class depends on via fields (potential DRIVEN ports)
 */
public record CoreAppClass(
        NodeId classId, Set<NodeId> implementedInterfaces, Set<NodeId> dependedInterfaces) {

    public CoreAppClass {
        Objects.requireNonNull(classId, "classId cannot be null");
        implementedInterfaces =
                implementedInterfaces == null ? Set.of() : Set.copyOf(implementedInterfaces);
        dependedInterfaces = dependedInterfaces == null ? Set.of() : Set.copyOf(dependedInterfaces);
    }

    /**
     * Returns true if this class implements at least one interface.
     */
    public boolean implementsAny() {
        return !implementedInterfaces.isEmpty();
    }

    /**
     * Returns true if this class depends on at least one interface.
     */
    public boolean dependsOnAny() {
        return !dependedInterfaces.isEmpty();
    }

    /**
     * Returns true if this class is a "pivot" - implements AND depends on interfaces.
     *
     * <p>A pivot class is typically an Application Service that:
     * <ul>
     *   <li>Implements a use case interface (DRIVING port)</li>
     *   <li>Depends on repository/gateway interfaces (DRIVEN ports)</li>
     * </ul>
     */
    public boolean isPivot() {
        return implementsAny() && dependsOnAny();
    }

    /**
     * Returns true if this class is inbound-only - implements but doesn't depend.
     *
     * <p>An inbound-only class provides an entry point but doesn't delegate
     * to any driven ports.
     */
    public boolean isInboundOnly() {
        return implementsAny() && !dependsOnAny();
    }

    /**
     * Returns true if this class is outbound-only - depends but doesn't implement.
     *
     * <p>An outbound-only class orchestrates driven ports without exposing
     * a driving port interface.
     */
    public boolean isOutboundOnly() {
        return !implementsAny() && dependsOnAny();
    }

    /**
     * Returns the total number of interfaces this class relates to.
     */
    public int interfaceCount() {
        return implementedInterfaces.size() + dependedInterfaces.size();
    }
}
