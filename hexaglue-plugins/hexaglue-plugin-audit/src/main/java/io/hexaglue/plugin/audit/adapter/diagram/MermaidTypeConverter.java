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

package io.hexaglue.plugin.audit.adapter.diagram;

import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.Set;

/**
 * Converts Java types to Mermaid class diagram syntax.
 *
 * <p>Mermaid class diagrams use tildes (~) instead of angle brackets (&lt;&gt;)
 * for generic type parameters. This utility handles the conversion.
 *
 * <h2>Type Conversion Examples</h2>
 * <ul>
 *   <li>{@code List<Order>} → {@code List~Order~}</li>
 *   <li>{@code Map<String, Order>} → {@code Map~String, Order~}</li>
 *   <li>{@code Optional<Money>} → {@code Optional~Money~}</li>
 *   <li>{@code Set<List<Item>>} → {@code Set~List~Item~~}</li>
 * </ul>
 *
 * <h2>Visibility Symbols</h2>
 * <ul>
 *   <li>{@code +} public</li>
 *   <li>{@code -} private</li>
 *   <li>{@code #} protected</li>
 *   <li>{@code ~} package-private (default)</li>
 * </ul>
 *
 * @since 5.0.0
 */
public final class MermaidTypeConverter {

    private MermaidTypeConverter() {
        // Utility class
    }

    /**
     * Converts a Java type reference to Mermaid syntax.
     *
     * <p>Handles parameterized types by replacing angle brackets with tildes.
     *
     * @param typeRef the type reference to convert
     * @return the Mermaid-compatible type string
     * @since 5.0.0
     */
    public static String convert(TypeRef typeRef) {
        if (typeRef == null) {
            return "void";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(typeRef.simpleName());

        if (typeRef.isParameterized()) {
            sb.append("~");
            for (int i = 0; i < typeRef.typeArguments().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(convert(typeRef.typeArguments().get(i)));
            }
            sb.append("~");
        }

        if (typeRef.isArray()) {
            for (int i = 0; i < typeRef.arrayDimensions(); i++) {
                sb.append("[]");
            }
        }

        return sb.toString();
    }

    /**
     * Converts a simple type name string to Mermaid syntax.
     *
     * <p>This handles simple string representations that may contain generic syntax.
     * It converts {@code <} to {@code ~} and {@code >} to {@code ~}.
     *
     * @param typeName the type name string (e.g., "List&lt;Order&gt;")
     * @return the Mermaid-compatible type string
     * @since 5.0.0
     */
    public static String convertString(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return "void";
        }
        return typeName.replace("<", "~").replace(">", "~");
    }

    /**
     * Gets the Mermaid visibility symbol for the given modifiers.
     *
     * <p>Visibility symbols in Mermaid class diagrams:
     * <ul>
     *   <li>{@code +} for public</li>
     *   <li>{@code -} for private</li>
     *   <li>{@code #} for protected</li>
     *   <li>{@code ~} for package-private (default visibility)</li>
     * </ul>
     *
     * @param modifiers the set of modifiers
     * @return the visibility symbol
     * @since 5.0.0
     */
    public static String visibilitySymbol(Set<Modifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return "~"; // package-private (default)
        }
        if (modifiers.contains(Modifier.PUBLIC)) {
            return "+";
        }
        if (modifiers.contains(Modifier.PRIVATE)) {
            return "-";
        }
        if (modifiers.contains(Modifier.PROTECTED)) {
            return "#";
        }
        return "~"; // package-private
    }

    /**
     * Checks if the given modifiers indicate a static member.
     *
     * @param modifiers the set of modifiers
     * @return true if static
     * @since 5.0.0
     */
    public static boolean isStatic(Set<Modifier> modifiers) {
        return modifiers != null && modifiers.contains(Modifier.STATIC);
    }
}
