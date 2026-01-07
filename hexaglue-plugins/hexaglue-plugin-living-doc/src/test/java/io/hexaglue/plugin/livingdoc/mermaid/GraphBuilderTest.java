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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GraphBuilderTest {

    @Test
    void shouldGenerateEmptyGraphWithLRDirection() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT).build();

        assertThat(result).isEqualTo("```mermaid\ngraph LR\n```\n\n");
    }

    @Test
    void shouldGenerateEmptyGraphWithTBDirection() {
        String result = new GraphBuilder(GraphBuilder.Direction.TOP_TO_BOTTOM).build();

        assertThat(result).isEqualTo("```mermaid\ngraph TB\n```\n\n");
    }

    @Test
    void shouldGenerateNode() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .node("A", "[Node A]")
                .build();

        assertThat(result).contains("    A[Node A]");
    }

    @Test
    void shouldGenerateMultipleNodes() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .node("A", "[Node A]")
                .node("B", "[Node B]")
                .node("C", "[Node C]")
                .build();

        assertThat(result).contains("    A[Node A]").contains("    B[Node B]").contains("    C[Node C]");
    }

    @Test
    void shouldGenerateArrowConnection() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .node("A", "[Node A]")
                .node("B", "[Node B]")
                .arrow("A", "B")
                .build();

        assertThat(result).contains("    A --> B");
    }

    @Test
    void shouldGenerateArrowWithLabel() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .node("A", "[Node A]")
                .node("B", "[Node B]")
                .arrow("A", "B", "connects to")
                .build();

        assertThat(result).contains("    A -->|connects to| B");
    }

    @Test
    void shouldGenerateSubgraph() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .startSubgraph("External", "External Actors")
                .node("UI", "[UI/API]")
                .endSubgraph()
                .build();

        assertThat(result)
                .contains("    subgraph External[\"External Actors\"]")
                .contains("        UI[UI/API]")
                .contains("    end");
    }

    @Test
    void shouldGenerateNestedSubgraphs() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .startSubgraph("Application", "Application")
                .startSubgraph("Domain", "Domain")
                .node("Order", "[Order]")
                .endSubgraph()
                .endSubgraph()
                .build();

        assertThat(result)
                .contains("    subgraph Application[\"Application\"]")
                .contains("        subgraph Domain[\"Domain\"]")
                .contains("            Order[Order]")
                .contains("        end")
                .contains("    end");
    }

    @Test
    void shouldSanitizeNodeIds() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .node("Order-Service", "[Order Service]")
                .node("Line.Item", "[Line Item]")
                .arrow("Order-Service", "Line.Item")
                .build();

        assertThat(result)
                .contains("Order_Service[Order Service]")
                .contains("Line_Item[Line Item]")
                .contains("Order_Service --> Line_Item");
    }

    @Test
    void shouldEscapeLabels() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .startSubgraph("test", "Label \"with\" quotes")
                .endSubgraph()
                .build();

        assertThat(result).contains("subgraph test[\"Label \\\"with\\\" quotes\"]");
    }

    @Test
    void shouldAllowRawMermaidSyntax() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .raw("A[Custom Node]")
                .raw("B{Decision}")
                .raw("A --> B")
                .build();

        assertThat(result)
                .contains("    A[Custom Node]")
                .contains("    B{Decision}")
                .contains("    A --> B");
    }

    @Test
    void shouldGenerateComplexGraph() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT)
                .startSubgraph("External", "External")
                .node("User", "([User])")
                .endSubgraph()
                .startSubgraph("Application", "Application")
                .node("Service", "[Service]")
                .endSubgraph()
                .startSubgraph("Infrastructure", "Infrastructure")
                .node("DB", "[(Database)]")
                .endSubgraph()
                .arrow("User", "Application")
                .arrow("Application", "Infrastructure")
                .build();

        assertThat(result)
                .contains("subgraph External[\"External\"]")
                .contains("User([User])")
                .contains("subgraph Application[\"Application\"]")
                .contains("Service[Service]")
                .contains("subgraph Infrastructure[\"Infrastructure\"]")
                .contains("DB[(Database)]")
                .contains("User --> Application")
                .contains("Application --> Infrastructure");
    }

    @Test
    void shouldWrapGraphInCodeFence() {
        String result = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT).build();

        assertThat(result).startsWith("```mermaid\n").endsWith("```\n\n");
    }
}
