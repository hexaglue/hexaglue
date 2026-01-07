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
 * Fluent API for building Mermaid graph diagrams.
 *
 * <p>Supports both left-to-right (LR) and top-to-bottom (TB) graph layouts
 * with nodes, subgraphs, and connections.
 *
 * <p>Example usage:
 * <pre>{@code
 * String diagram = new GraphBuilder(Direction.LEFT_TO_RIGHT)
 *     .startSubgraph("External", "External Actors")
 *         .node("UI", "[UI/API]")
 *         .end()
 *     .startSubgraph("Application", "Application")
 *         .node("Domain", "[Domain]")
 *         .end()
 *     .arrow("UI", "Domain")
 *     .build();
 * }</pre>
 *
 * @since 0.1.0
 */
public final class GraphBuilder {

    /**
     * Graph direction options.
     */
    public enum Direction {
        /** Left to right layout */
        LEFT_TO_RIGHT("LR"),
        /** Top to bottom layout */
        TOP_TO_BOTTOM("TB");

        private final String code;

        Direction(String code) {
            this.code = code;
        }

        String getCode() {
            return code;
        }
    }

    private final StringBuilder content;
    private int indentLevel = 1;

    /**
     * Creates a new graph builder with the specified direction.
     *
     * @param direction the layout direction (LR or TB)
     */
    public GraphBuilder(Direction direction) {
        this.content = new StringBuilder();
        content.append("```mermaid\n");
        content.append("graph ").append(direction.getCode()).append("\n");
    }

    /**
     * Starts a new subgraph.
     *
     * @param id the subgraph identifier
     * @param label the display label
     * @return this builder for method chaining
     */
    public GraphBuilder startSubgraph(String id, String label) {
        indent();
        content.append("subgraph ")
                .append(MermaidBuilder.sanitizeId(id))
                .append("[\"")
                .append(MermaidBuilder.escapeLabel(label))
                .append("\"]\n");
        indentLevel++;
        return this;
    }

    /**
     * Ends the current subgraph.
     *
     * @return this builder for method chaining
     */
    public GraphBuilder endSubgraph() {
        indentLevel--;
        indent();
        content.append("end\n");
        return this;
    }

    /**
     * Adds a node with a label.
     *
     * @param id the node identifier
     * @param shape the node shape and label (e.g., "[Label]", "((Label))", "{Label}")
     * @return this builder for method chaining
     */
    public GraphBuilder node(String id, String shape) {
        indent();
        content.append(MermaidBuilder.sanitizeId(id)).append(shape).append("\n");
        return this;
    }

    /**
     * Adds a simple arrow connection between nodes.
     *
     * @param from the source node
     * @param to the target node
     * @return this builder for method chaining
     */
    public GraphBuilder arrow(String from, String to) {
        indent();
        content.append(MermaidBuilder.sanitizeId(from))
                .append(" --> ")
                .append(MermaidBuilder.sanitizeId(to))
                .append("\n");
        return this;
    }

    /**
     * Adds an arrow connection with a label.
     *
     * @param from the source node
     * @param to the target node
     * @param label the connection label
     * @return this builder for method chaining
     */
    public GraphBuilder arrow(String from, String to, String label) {
        indent();
        content.append(MermaidBuilder.sanitizeId(from))
                .append(" -->|")
                .append(label)
                .append("| ")
                .append(MermaidBuilder.sanitizeId(to))
                .append("\n");
        return this;
    }

    /**
     * Appends raw Mermaid syntax directly to the graph.
     *
     * @param rawMermaid the raw Mermaid syntax (without indentation)
     * @return this builder for method chaining
     */
    public GraphBuilder raw(String rawMermaid) {
        indent();
        content.append(rawMermaid).append("\n");
        return this;
    }

    /**
     * Builds and returns the complete Mermaid graph diagram.
     *
     * @return the generated Mermaid diagram as a String
     */
    public String build() {
        return content.append("```\n\n").toString();
    }

    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            content.append("    ");
        }
    }
}
