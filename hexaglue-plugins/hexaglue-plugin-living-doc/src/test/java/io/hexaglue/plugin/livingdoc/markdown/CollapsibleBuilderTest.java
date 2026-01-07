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

class CollapsibleBuilderTest {

    @Test
    void shouldGenerateCollapsibleWithContent() {
        String result = new MarkdownBuilder()
                .collapsible("Click to expand")
                .content(builder -> builder.paragraph("Hidden content"))
                .end()
                .build();

        assertThat(result)
                .isEqualTo("<details>\n"
                        + "<summary>Click to expand</summary>\n\n"
                        + "Hidden content\n\n"
                        + "\n</details>\n\n");
    }

    @Test
    void shouldGenerateCollapsibleWithMultipleContentElements() {
        String result = new MarkdownBuilder()
                .collapsible("Details")
                .content(builder ->
                        builder.h4("Section").paragraph("First paragraph").paragraph("Second paragraph"))
                .end()
                .build();

        assertThat(result)
                .contains("<details>\n")
                .contains("<summary>Details</summary>\n\n")
                .contains("#### Section\n\n")
                .contains("First paragraph\n\n")
                .contains("Second paragraph\n\n")
                .contains("\n</details>\n\n");
    }

    @Test
    void shouldGenerateCollapsibleWithTable() {
        String result = new MarkdownBuilder()
                .collapsible("Debug Info")
                .content(builder -> builder.table("Property", "Value")
                        .row("Name", "Test")
                        .row("Type", "Entity")
                        .end())
                .end()
                .build();

        assertThat(result)
                .contains("<summary>Debug Info</summary>\n\n")
                .contains("| Property | Value |\n")
                .contains("| Name | Test |\n")
                .contains("| Type | Entity |\n");
    }

    @Test
    void shouldGenerateCollapsibleWithRawContent() {
        String result = new MarkdownBuilder()
                .collapsible("Raw Section")
                .rawContent("Custom HTML or Markdown\n")
                .end()
                .build();

        assertThat(result)
                .contains("<summary>Raw Section</summary>\n\n")
                .contains("Custom HTML or Markdown\n")
                .contains("\n</details>\n\n");
    }

    @Test
    void shouldAllowChainingAfterCollapsible() {
        String result = new MarkdownBuilder()
                .h2("Documentation")
                .collapsible("Advanced")
                .content(builder -> builder.paragraph("Advanced content"))
                .end()
                .paragraph("After collapsible")
                .build();

        assertThat(result)
                .contains("## Documentation\n\n")
                .contains("<details>\n")
                .contains("Advanced content\n\n")
                .contains("</details>\n\n")
                .contains("After collapsible\n\n");
    }

    @Test
    void shouldGenerateNestedCollapsibles() {
        String result = new MarkdownBuilder()
                .collapsible("Outer")
                .content(outer -> outer.paragraph("Outer content")
                        .collapsible("Inner")
                        .content(inner -> inner.paragraph("Inner content"))
                        .end())
                .end()
                .build();

        assertThat(result)
                .contains("<summary>Outer</summary>")
                .contains("Outer content")
                .contains("<summary>Inner</summary>")
                .contains("Inner content");
    }

    @Test
    void shouldHandleEmptyContent() {
        String result = new MarkdownBuilder()
                .collapsible("Empty")
                .content(builder -> {
                    // Empty content
                })
                .end()
                .build();

        assertThat(result).isEqualTo("<details>\n" + "<summary>Empty</summary>\n\n" + "\n</details>\n\n");
    }

    @Test
    void shouldGenerateMultipleCollapsibles() {
        String result = new MarkdownBuilder()
                .collapsible("First")
                .content(builder -> builder.paragraph("First content"))
                .end()
                .collapsible("Second")
                .content(builder -> builder.paragraph("Second content"))
                .end()
                .build();

        assertThat(result)
                .contains("<summary>First</summary>")
                .contains("First content")
                .contains("<summary>Second</summary>")
                .contains("Second content");
    }
}
