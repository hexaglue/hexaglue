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

package io.hexaglue.core.classification.engine;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.port.PortKind;
import java.util.Set;

/**
 * Determines whether two classification kinds are compatible.
 *
 * <p>Compatible kinds can coexist without being marked as a conflict.
 * For example, AGGREGATE_ROOT and ENTITY are compatible because an
 * aggregate root is a special kind of entity.
 *
 * <p>This interface is parameterized to support both domain and port
 * classification kinds.
 *
 * @param <K> the kind type (e.g., ElementKind, PortKind)
 */
@FunctionalInterface
public interface CompatibilityPolicy<K> {

    /**
     * Checks if two kinds are compatible.
     *
     * <p>Kinds are always compatible with themselves (a.equals(b) → true).
     *
     * @param a the first kind
     * @param b the second kind
     * @return true if the kinds can coexist without conflict
     */
    boolean areCompatible(K a, K b);

    /**
     * Creates a policy where only identical kinds are compatible.
     *
     * <p>This is the strictest policy: any two different kinds are
     * considered incompatible.
     *
     * @param <K> the kind type
     * @return a policy that only allows identical kinds
     */
    static <K> CompatibilityPolicy<K> noneCompatible() {
        return (a, b) -> a.equals(b);
    }

    /**
     * Creates the default domain compatibility policy.
     *
     * <p>Compatible pairs:
     * <ul>
     *   <li>AGGREGATE_ROOT ↔ ENTITY (an aggregate root is a special entity)</li>
     * </ul>
     *
     * @return the default domain compatibility policy
     */
    static CompatibilityPolicy<ElementKind> domainDefault() {
        return (a, b) -> {
            if (a == b) {
                return true;
            }
            // AGGREGATE_ROOT and ENTITY are compatible
            return Set.of(a, b).equals(Set.of(ElementKind.AGGREGATE_ROOT, ElementKind.ENTITY));
        };
    }

    /**
     * Creates the default port compatibility policy.
     *
     * <p>Port kinds are mutually exclusive - no two different kinds
     * are compatible.
     *
     * @return the default port compatibility policy
     */
    static CompatibilityPolicy<PortKind> portDefault() {
        return noneCompatible();
    }
}
