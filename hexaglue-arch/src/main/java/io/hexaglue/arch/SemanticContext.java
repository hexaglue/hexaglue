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

package io.hexaglue.arch;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Semantic context used during classification.
 *
 * <p>Captures contextual information about how a type relates to other
 * types in the architecture, which influences classification decisions.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * SemanticContext context = new SemanticContext(
 *     Optional.of("DOMAIN_ANCHOR"),
 *     Optional.of("PIVOT"),
 *     List.of("Serializable", "Comparable"),
 *     List.of("OrderRepository")
 * );
 * }</pre>
 *
 * @param anchorKind the role as anchor (DOMAIN_ANCHOR, INFRA_ANCHOR), if any
 * @param coreAppClassRole the role in core application (PIVOT, INBOUND_ONLY, OUTBOUND_ONLY), if any
 * @param implementedInterfaces list of interfaces implemented by the type
 * @param usedInterfaces list of interfaces used (depended on) by the type
 * @since 4.0.0
 */
public record SemanticContext(
        Optional<String> anchorKind,
        Optional<String> coreAppClassRole,
        List<String> implementedInterfaces,
        List<String> usedInterfaces) {

    /**
     * Creates a new SemanticContext instance.
     *
     * @param anchorKind the anchor kind, must not be null
     * @param coreAppClassRole the core app class role, must not be null
     * @param implementedInterfaces the implemented interfaces, must not be null
     * @param usedInterfaces the used interfaces, must not be null
     * @throws NullPointerException if any field is null
     */
    public SemanticContext {
        Objects.requireNonNull(anchorKind, "anchorKind must not be null");
        Objects.requireNonNull(coreAppClassRole, "coreAppClassRole must not be null");
        Objects.requireNonNull(implementedInterfaces, "implementedInterfaces must not be null");
        Objects.requireNonNull(usedInterfaces, "usedInterfaces must not be null");
        implementedInterfaces = List.copyOf(implementedInterfaces);
        usedInterfaces = List.copyOf(usedInterfaces);
    }

    /**
     * Creates an empty semantic context with no contextual information.
     *
     * @return an empty SemanticContext
     */
    public static SemanticContext empty() {
        return new SemanticContext(Optional.empty(), Optional.empty(), List.of(), List.of());
    }

    /**
     * Creates a semantic context for a domain anchor.
     *
     * @param implementedInterfaces the interfaces implemented
     * @return a new SemanticContext
     */
    public static SemanticContext domainAnchor(List<String> implementedInterfaces) {
        return new SemanticContext(Optional.of("DOMAIN_ANCHOR"), Optional.empty(), implementedInterfaces, List.of());
    }

    /**
     * Creates a semantic context for an infrastructure anchor.
     *
     * @param usedInterfaces the interfaces used
     * @return a new SemanticContext
     */
    public static SemanticContext infraAnchor(List<String> usedInterfaces) {
        return new SemanticContext(Optional.of("INFRA_ANCHOR"), Optional.empty(), List.of(), usedInterfaces);
    }
}
