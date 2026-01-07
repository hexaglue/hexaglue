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

package io.hexaglue.plugin.livingdoc.util;

/**
 * Utility methods for Markdown formatting.
 *
 * <p>Provides shared functionality for:
 * <ul>
 *   <li>Creating Markdown anchor links</li>
 *   <li>Formatting text (code, bold)</li>
 * </ul>
 *
 * <p>Note: For Mermaid-specific sanitization, use {@link io.hexaglue.plugin.livingdoc.mermaid.MermaidBuilder#sanitizeId(String)}.
 */
public final class MarkdownUtil {

    private MarkdownUtil() {
        // Utility class
    }

    /**
     * Converts text to a Markdown anchor identifier.
     *
     * <p>GitHub Flavored Markdown anchors are lowercase and use hyphens
     * instead of spaces. This method follows that convention.
     *
     * @param text the text to convert to an anchor
     * @return anchor identifier for use in Markdown links
     */
    public static String toAnchor(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }

    /**
     * Wraps text in Markdown inline code formatting.
     *
     * @param text the text to format as code
     * @return text wrapped in backticks
     */
    public static String code(String text) {
        if (text == null || text.isEmpty()) {
            return "`null`";
        }
        return "`" + text + "`";
    }

    /**
     * Wraps text in Markdown bold formatting.
     *
     * @param text the text to format as bold
     * @return text wrapped in double asterisks
     */
    public static String bold(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return "**" + text + "**";
    }
}
