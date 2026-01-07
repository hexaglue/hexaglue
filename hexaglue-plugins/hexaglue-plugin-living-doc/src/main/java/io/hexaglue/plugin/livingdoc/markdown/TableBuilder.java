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

package io.hexaglue.plugin.livingdoc.markdown;

/**
 * Nested builder for Markdown tables.
 *
 * <p>This builder is created from {@link MarkdownBuilder#table(String...)} and allows
 * adding table rows before returning to the parent builder with {@link #end()}.
 *
 * <p>Generates standard Markdown tables with pipe-delimited columns:
 * <pre>
 * | Header 1 | Header 2 |
 * |----------|----------|
 * | Cell 1   | Cell 2   |
 * </pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * new MarkdownBuilder()
 *     .h2("Metrics")
 *     .table("Name", "Type", "Count")
 *         .row("Order", "Aggregate", "1")
 *         .row("LineItem", "Entity", "5")
 *         .end()
 *     .build();
 * }</pre>
 *
 * @since 0.1.0
 */
public final class TableBuilder {

    private final MarkdownBuilder parent;
    private final StringBuilder content;
    private final int columnCount;

    /**
     * Creates a new table builder.
     *
     * @param parent the parent MarkdownBuilder to return to
     * @param headers the column headers
     */
    TableBuilder(MarkdownBuilder parent, String... headers) {
        this.parent = parent;
        this.content = parent.getContent();
        this.columnCount = headers.length;

        // Header row
        content.append("|");
        for (String header : headers) {
            content.append(" ").append(header).append(" |");
        }
        content.append("\n");

        // Separator row
        content.append("|");
        for (int i = 0; i < columnCount; i++) {
            content.append("--------|");
        }
        content.append("\n");
    }

    /**
     * Adds a row to the table.
     *
     * <p>The number of cells should match the number of columns defined
     * in the table headers. If fewer cells are provided, empty cells will
     * be added. If more cells are provided, they will be ignored.
     *
     * @param cells the cell values for this row (varargs)
     * @return this builder for method chaining
     */
    public TableBuilder row(String... cells) {
        content.append("|");
        for (int i = 0; i < columnCount; i++) {
            String cell = i < cells.length ? cells[i] : "";
            content.append(" ").append(cell).append(" |");
        }
        content.append("\n");
        return this;
    }

    /**
     * Completes the table and returns to the parent MarkdownBuilder.
     *
     * @return the parent MarkdownBuilder
     */
    public MarkdownBuilder end() {
        content.append("\n");
        return parent;
    }
}
