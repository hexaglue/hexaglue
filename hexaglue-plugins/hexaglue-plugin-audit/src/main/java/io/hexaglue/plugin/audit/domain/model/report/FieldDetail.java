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
 * Detail of a field for rendering in class diagrams.
 *
 * <p>Captures all information needed to render a field in Mermaid class diagrams,
 * with the type already converted to Mermaid syntax (using tildes for generics).
 *
 * <h2>Mermaid Format</h2>
 * Fields are rendered as: {@code visibility + name [+ "$" if static] + ": " + type}
 *
 * <h3>Examples</h3>
 * <ul>
 *   <li>{@code -id: OrderId} (private field)</li>
 *   <li>{@code -items: List~OrderLine~} (private generic collection)</li>
 *   <li>{@code +INSTANCE$: Singleton} (public static field)</li>
 * </ul>
 *
 * @param name the field name
 * @param typeMermaid the type in Mermaid syntax (e.g., "List~Order~")
 * @param visibility the Mermaid visibility symbol (+, -, #, ~)
 * @param isStatic whether the field is static
 * @since 5.0.0
 */
public record FieldDetail(String name, String typeMermaid, String visibility, boolean isStatic) {

    /**
     * Creates a field detail with validation.
     */
    public FieldDetail {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(typeMermaid, "typeMermaid is required");
        Objects.requireNonNull(visibility, "visibility is required");
    }

    /**
     * Renders this field detail as a Mermaid class diagram member line.
     *
     * <p>Format: {@code visibility + name [+ "$" if static] + ": " + type}
     *
     * <h3>Examples</h3>
     * <ul>
     *   <li>{@code -id: OrderId}</li>
     *   <li>{@code -items: List~OrderLine~}</li>
     *   <li>{@code +INSTANCE$: Singleton}</li>
     * </ul>
     *
     * @return the Mermaid representation
     */
    public String toMermaid() {
        String staticMark = isStatic ? "$" : "";
        return visibility + name + staticMark + ": " + typeMermaid;
    }

    /**
     * Creates a field detail.
     *
     * @param name the field name
     * @param typeMermaid the type in Mermaid syntax
     * @param visibility the visibility symbol
     * @param isStatic whether static
     * @return the field detail
     */
    public static FieldDetail of(String name, String typeMermaid, String visibility, boolean isStatic) {
        return new FieldDetail(name, typeMermaid, visibility, isStatic);
    }

    /**
     * Creates a non-static field detail.
     *
     * @param name the field name
     * @param typeMermaid the type in Mermaid syntax
     * @param visibility the visibility symbol
     * @return the field detail
     */
    public static FieldDetail of(String name, String typeMermaid, String visibility) {
        return new FieldDetail(name, typeMermaid, visibility, false);
    }
}
