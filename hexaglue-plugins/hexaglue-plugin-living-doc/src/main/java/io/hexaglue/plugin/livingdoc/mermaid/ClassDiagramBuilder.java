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
 * Fluent API for building Mermaid class diagrams.
 *
 * <p>Generates UML class diagrams in Mermaid syntax with support for:
 * <ul>
 *   <li>Class definitions with stereotypes</li>
 *   <li>Fields and methods</li>
 *   <li>Relationships (composition, aggregation, association, inheritance, dependency)</li>
 *   <li>Notes and annotations</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * String diagram = new ClassDiagramBuilder()
 *     .addClass("Order")
 *         .stereotype("Aggregate Root")
 *         .field("UUID", "id")
 *         .field("List~LineItem~", "items")
 *         .method("addItem(LineItem)")
 *         .end()
 *     .addClass("LineItem")
 *         .stereotype("Entity")
 *         .field("String", "product")
 *         .field("int", "quantity")
 *         .end()
 *     .composition("Order", "LineItem")
 *     .build();
 * }</pre>
 *
 * @since 0.1.0
 */
public final class ClassDiagramBuilder {

    private final StringBuilder content;

    /**
     * Creates a new class diagram builder.
     */
    public ClassDiagramBuilder() {
        this.content = new StringBuilder();
        content.append("```mermaid\n");
        content.append("classDiagram\n");
    }

    /**
     * Starts defining a new class in the diagram.
     *
     * <p>Returns a {@link ClassBuilder} that allows adding stereotypes, fields,
     * and methods before returning to this builder with {@link ClassBuilder#end()}.
     *
     * @param className the name of the class (will be sanitized)
     * @return a ClassBuilder for adding class details
     */
    public ClassBuilder addClass(String className) {
        return new ClassBuilder(this, className);
    }

    /**
     * Adds a relationship between two classes.
     *
     * <p>The arrow parameter should be a valid Mermaid relationship syntax such as:
     * <ul>
     *   <li>{@code <|--} for inheritance</li>
     *   <li>{@code *--} for composition</li>
     *   <li>{@code o--} for aggregation</li>
     *   <li>{@code -->} for association</li>
     *   <li>{@code ..>} for dependency</li>
     * </ul>
     *
     * @param from the source class name
     * @param arrow the relationship arrow syntax
     * @param to the target class name
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder relation(String from, String arrow, String to) {
        content.append("    ")
                .append(MermaidBuilder.sanitizeId(from))
                .append(" ")
                .append(arrow)
                .append(" ")
                .append(MermaidBuilder.sanitizeId(to))
                .append("\n");
        return this;
    }

    /**
     * Adds a relationship with a label.
     *
     * @param from the source class name
     * @param arrow the relationship arrow syntax
     * @param to the target class name
     * @param label the relationship label
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder relation(String from, String arrow, String to, String label) {
        content.append("    ")
                .append(MermaidBuilder.sanitizeId(from))
                .append(" ")
                .append(arrow)
                .append(" ")
                .append(MermaidBuilder.sanitizeId(to))
                .append(" : ")
                .append(label)
                .append("\n");
        return this;
    }

    /**
     * Adds a composition relationship (strong ownership).
     *
     * @param owner the owning class
     * @param owned the owned class
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder composition(String owner, String owned) {
        return relation(owner, "*--", owned);
    }

    /**
     * Adds a composition relationship with cardinality.
     *
     * @param owner the owning class
     * @param ownerCardinality the owner cardinality (e.g., "1")
     * @param ownedCardinality the owned cardinality (e.g., "*")
     * @param owned the owned class
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder composition(
            String owner, String ownerCardinality, String ownedCardinality, String owned) {
        return relation(owner, "\"" + ownerCardinality + "\" *-- \"" + ownedCardinality + "\"", owned);
    }

    /**
     * Adds an aggregation relationship (weak ownership).
     *
     * @param container the container class
     * @param contained the contained class
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder aggregation(String container, String contained) {
        return relation(container, "o--", contained);
    }

    /**
     * Adds an aggregation relationship with cardinality.
     *
     * @param container the container class
     * @param containerCardinality the container cardinality (e.g., "1")
     * @param containedCardinality the contained cardinality (e.g., "*")
     * @param contained the contained class
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder aggregation(
            String container, String containerCardinality, String containedCardinality, String contained) {
        return relation(container, "\"" + containerCardinality + "\" o-- \"" + containedCardinality + "\"", contained);
    }

    /**
     * Adds an association relationship.
     *
     * @param from the source class
     * @param to the target class
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder association(String from, String to) {
        return relation(from, "-->", to);
    }

    /**
     * Adds an association relationship with a label.
     *
     * @param from the source class
     * @param to the target class
     * @param label the relationship label
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder association(String from, String to, String label) {
        return relation(from, "-->", to, label);
    }

    /**
     * Adds a dependency relationship.
     *
     * @param from the dependent class
     * @param to the dependency class
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder dependency(String from, String to) {
        return relation(from, "..>", to);
    }

    /**
     * Adds an inheritance relationship.
     *
     * @param subclass the subclass
     * @param superclass the superclass
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder inheritance(String subclass, String superclass) {
        return relation(subclass, "<|--", superclass);
    }

    /**
     * Adds a note to a class.
     *
     * @param className the class to annotate
     * @param noteText the note text
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder note(String className, String noteText) {
        content.append("    note for ")
                .append(MermaidBuilder.sanitizeId(className))
                .append(" \"")
                .append(MermaidBuilder.escapeLabel(noteText))
                .append("\"\n");
        return this;
    }

    /**
     * Appends raw Mermaid syntax directly to the diagram.
     *
     * <p>Use this for advanced Mermaid features not covered by the builder API.
     *
     * @param rawMermaid the raw Mermaid syntax (without indentation)
     * @return this builder for method chaining
     */
    public ClassDiagramBuilder raw(String rawMermaid) {
        content.append("    ").append(rawMermaid).append("\n");
        return this;
    }

    /**
     * Builds and returns the complete Mermaid class diagram.
     *
     * <p>The result includes the opening and closing Mermaid code fence markers.
     *
     * @return the generated Mermaid diagram as a String
     */
    public String build() {
        return content.append("```\n\n").toString();
    }

    /**
     * Returns the underlying StringBuilder for nested builder access.
     *
     * @return the shared StringBuilder
     */
    StringBuilder getContent() {
        return content;
    }
}
