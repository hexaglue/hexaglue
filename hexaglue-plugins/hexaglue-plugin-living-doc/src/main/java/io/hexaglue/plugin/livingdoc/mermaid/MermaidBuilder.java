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

package io.hexaglue.plugin.livingdoc.mermaid;

/**
 * Base utilities for Mermaid diagram generation.
 *
 * <p>Provides common functionality shared across different Mermaid diagram types:
 * <ul>
 *   <li>Identifier sanitization for valid Mermaid syntax</li>
 *   <li>Text escaping for labels and descriptions</li>
 * </ul>
 *
 * <p>For specific diagram types, use:
 * <ul>
 *   <li>{@link ClassDiagramBuilder} for UML class diagrams</li>
 *   <li>{@link GraphBuilder} for graph diagrams</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class MermaidBuilder {

    private MermaidBuilder() {
        // Utility class
    }

    /**
     * Sanitizes an identifier for use in Mermaid diagrams.
     *
     * <p>Mermaid diagram identifiers can only contain alphanumeric characters
     * and underscores. This method replaces all other characters with underscores
     * to ensure valid Mermaid syntax.
     *
     * <p>Examples:
     * <ul>
     *   <li>"OrderService" → "OrderService"</li>
     *   <li>"Order-Repository" → "Order_Repository"</li>
     *   <li>"User.Profile" → "User_Profile"</li>
     * </ul>
     *
     * @param name the identifier to sanitize
     * @return sanitized identifier safe for Mermaid diagrams
     */
    public static String sanitizeId(String name) {
        if (name == null || name.isEmpty()) {
            return "_";
        }
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Escapes special characters in text for use in Mermaid labels.
     *
     * <p>Quotes in labels need to be escaped to prevent syntax errors.
     * This method ensures text can be safely used within quoted strings.
     *
     * @param text the text to escape
     * @return escaped text safe for Mermaid labels
     */
    public static String escapeLabel(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\"", "\\\"");
    }
}
