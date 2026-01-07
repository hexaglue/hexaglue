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
 * Nested builder for Mermaid class definitions.
 *
 * <p>This builder is created from {@link ClassDiagramBuilder#addClass(String)} and allows
 * defining class members (stereotypes, fields, methods) before returning to the parent
 * builder with {@link #end()}.
 *
 * <p>Generates Mermaid class definitions like:
 * <pre>
 * class Order {
 *     &lt;&lt;Aggregate Root&gt;&gt;
 *     +UUID id
 *     +List~LineItem~ items
 *     +addItem(LineItem)
 * }
 * </pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * new ClassDiagramBuilder()
 *     .addClass("Order")
 *         .stereotype("Aggregate Root")
 *         .field("UUID", "id")
 *         .field("List~LineItem~", "items")
 *         .method("addItem(LineItem)")
 *         .method("removeItem(UUID)")
 *         .end()
 *     .build();
 * }</pre>
 *
 * @since 0.1.0
 */
public final class ClassBuilder {

    private final ClassDiagramBuilder parent;
    private final StringBuilder content;
    private final String className;

    /**
     * Creates a new class builder.
     *
     * @param parent the parent ClassDiagramBuilder to return to
     * @param className the name of the class being defined
     */
    ClassBuilder(ClassDiagramBuilder parent, String className) {
        this.parent = parent;
        this.content = parent.getContent();
        this.className = MermaidBuilder.sanitizeId(className);

        // Start class definition
        content.append("    class ").append(this.className).append(" {\n");
    }

    /**
     * Adds a stereotype annotation to the class.
     *
     * <p>Stereotypes appear as {@code <<stereotype>>} in the class diagram.
     * Common examples include:
     * <ul>
     *   <li>"Aggregate Root"</li>
     *   <li>"Entity"</li>
     *   <li>"Value Object"</li>
     *   <li>"Service"</li>
     *   <li>"Interface"</li>
     * </ul>
     *
     * @param stereotypeName the stereotype name
     * @return this builder for method chaining
     */
    public ClassBuilder stereotype(String stereotypeName) {
        content.append("        <<").append(stereotypeName).append(">>\n");
        return this;
    }

    /**
     * Adds a field to the class.
     *
     * <p>Fields are shown with public visibility (+) by default.
     * For generic types, use Mermaid's tilde notation: {@code List~T~}
     *
     * @param type the field type (e.g., "String", "List~Item~")
     * @param name the field name
     * @return this builder for method chaining
     */
    public ClassBuilder field(String type, String name) {
        content.append("        +").append(type).append(" ").append(name).append("\n");
        return this;
    }

    /**
     * Adds a field with custom visibility.
     *
     * @param visibility the visibility modifier (+, -, #, ~)
     * @param type the field type
     * @param name the field name
     * @return this builder for method chaining
     */
    public ClassBuilder field(String visibility, String type, String name) {
        content.append("        ")
                .append(visibility)
                .append(type)
                .append(" ")
                .append(name)
                .append("\n");
        return this;
    }

    /**
     * Adds a method to the class.
     *
     * <p>Methods are shown with public visibility (+) by default.
     *
     * @param signature the method signature (e.g., "addItem(LineItem)", "getTotal() Money")
     * @return this builder for method chaining
     */
    public ClassBuilder method(String signature) {
        content.append("        +").append(signature).append("\n");
        return this;
    }

    /**
     * Adds a method with custom visibility.
     *
     * @param visibility the visibility modifier (+, -, #, ~)
     * @param signature the method signature
     * @return this builder for method chaining
     */
    public ClassBuilder method(String visibility, String signature) {
        content.append("        ").append(visibility).append(signature).append("\n");
        return this;
    }

    /**
     * Adds an ellipsis (...) to indicate omitted members.
     *
     * <p>Useful when showing a subset of class members to avoid cluttering the diagram.
     *
     * @return this builder for method chaining
     */
    public ClassBuilder ellipsis() {
        content.append("        ...\n");
        return this;
    }

    /**
     * Completes the class definition and returns to the parent ClassDiagramBuilder.
     *
     * @return the parent ClassDiagramBuilder
     */
    public ClassDiagramBuilder end() {
        content.append("    }\n");
        return parent;
    }
}
