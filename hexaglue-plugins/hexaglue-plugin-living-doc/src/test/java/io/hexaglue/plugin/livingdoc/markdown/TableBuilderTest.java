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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TableBuilderTest {

    @Test
    void shouldGenerateTableWithTwoColumns() {
        String result = new MarkdownBuilder()
                .table("Name", "Value")
                .row("Key1", "Value1")
                .row("Key2", "Value2")
                .end()
                .build();

        assertThat(result)
                .isEqualTo("| Name | Value |\n"
                        + "|--------|--------|\n"
                        + "| Key1 | Value1 |\n"
                        + "| Key2 | Value2 |\n\n");
    }

    @Test
    void shouldGenerateTableWithThreeColumns() {
        String result = new MarkdownBuilder()
                .table("Col1", "Col2", "Col3")
                .row("A", "B", "C")
                .row("D", "E", "F")
                .end()
                .build();

        assertThat(result)
                .isEqualTo("| Col1 | Col2 | Col3 |\n"
                        + "|--------|--------|--------|\n"
                        + "| A | B | C |\n"
                        + "| D | E | F |\n\n");
    }

    @Test
    void shouldHandleEmptyTable() {
        String result = new MarkdownBuilder().table("Header1", "Header2").end().build();

        assertThat(result).isEqualTo("| Header1 | Header2 |\n" + "|--------|--------|\n\n");
    }

    @Test
    void shouldHandleFewerCellsThanColumns() {
        String result = new MarkdownBuilder()
                .table("A", "B", "C")
                .row("1", "2") // Missing third column
                .end()
                .build();

        assertThat(result).isEqualTo("| A | B | C |\n" + "|--------|--------|--------|\n" + "| 1 | 2 |  |\n\n");
    }

    @Test
    void shouldIgnoreExtraCells() {
        String result = new MarkdownBuilder()
                .table("A", "B")
                .row("1", "2", "3", "4") // Extra cells
                .end()
                .build();

        assertThat(result).isEqualTo("| A | B |\n" + "|--------|--------|\n" + "| 1 | 2 |\n\n");
    }

    @Test
    void shouldAllowChainingAfterTable() {
        String result = new MarkdownBuilder()
                .h2("Data")
                .table("Name", "Count")
                .row("Items", "5")
                .end()
                .paragraph("End of table")
                .build();

        assertThat(result)
                .contains("## Data\n\n")
                .contains("| Name | Count |\n")
                .contains("| Items | 5 |\n\n")
                .contains("End of table\n\n");
    }

    @Test
    void shouldHandleSpecialCharactersInCells() {
        String result = new MarkdownBuilder()
                .table("Type", "Example")
                .row("Code", "`String`")
                .row("Bold", "**Important**")
                .row("Link", "[Click](url)")
                .end()
                .build();

        assertThat(result)
                .contains("| Code | `String` |")
                .contains("| Bold | **Important** |")
                .contains("| Link | [Click](url) |");
    }

    @Test
    void shouldGenerateMultipleTables() {
        String result = new MarkdownBuilder()
                .table("A", "B")
                .row("1", "2")
                .end()
                .table("X", "Y")
                .row("3", "4")
                .end()
                .build();

        assertThat(result)
                .contains("| A | B |")
                .contains("| 1 | 2 |")
                .contains("| X | Y |")
                .contains("| 3 | 4 |");
    }
}
