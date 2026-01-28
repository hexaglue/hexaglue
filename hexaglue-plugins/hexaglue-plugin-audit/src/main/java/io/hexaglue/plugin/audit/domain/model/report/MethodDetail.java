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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;

/**
 * Detail of a method for rendering in class diagrams.
 *
 * <p>Captures all information needed to render a method in Mermaid class diagrams,
 * with types already converted to Mermaid syntax (using tildes for generics).
 *
 * <h2>Mermaid Format</h2>
 * Methods are rendered as: {@code visibility + name [+ "$" if static] + signature}
 *
 * <h3>Examples</h3>
 * <ul>
 *   <li>{@code +place(CustomerId, List~OrderLine~): Order} (public method)</li>
 *   <li>{@code +cancel(): void} (public void method)</li>
 *   <li>{@code +getInstance()$: Singleton} (public static factory)</li>
 * </ul>
 *
 * @param name the method name
 * @param signatureMermaid the method signature in Mermaid syntax (e.g., "(CustomerId): Order")
 * @param visibility the Mermaid visibility symbol (+, -, #, ~)
 * @param isStatic whether the method is static
 * @since 5.0.0
 */
public record MethodDetail(String name, String signatureMermaid, String visibility, boolean isStatic) {

    /**
     * Creates a method detail with validation.
     */
    public MethodDetail {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(signatureMermaid, "signatureMermaid is required");
        Objects.requireNonNull(visibility, "visibility is required");
    }

    /**
     * Renders this method detail as a Mermaid class diagram member line.
     *
     * <p>Format: {@code visibility + name [+ "$" if static] + signature}
     *
     * <h3>Examples</h3>
     * <ul>
     *   <li>{@code +place(CustomerId, List~OrderLine~): Order}</li>
     *   <li>{@code +cancel(): void}</li>
     *   <li>{@code +getInstance()$: Singleton}</li>
     * </ul>
     *
     * @return the Mermaid representation
     */
    public String toMermaid() {
        String staticMark = isStatic ? "$" : "";
        return visibility + name + staticMark + signatureMermaid;
    }

    /**
     * Creates a method detail.
     *
     * @param name the method name
     * @param signatureMermaid the signature in Mermaid syntax
     * @param visibility the visibility symbol
     * @param isStatic whether static
     * @return the method detail
     */
    public static MethodDetail of(String name, String signatureMermaid, String visibility, boolean isStatic) {
        return new MethodDetail(name, signatureMermaid, visibility, isStatic);
    }

    /**
     * Creates a non-static method detail.
     *
     * @param name the method name
     * @param signatureMermaid the signature in Mermaid syntax
     * @param visibility the visibility symbol
     * @return the method detail
     */
    public static MethodDetail of(String name, String signatureMermaid, String visibility) {
        return new MethodDetail(name, signatureMermaid, visibility, false);
    }
}
