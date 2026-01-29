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

package io.hexaglue.plugin.livingdoc.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.livingdoc.model.BoundedContextDoc;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BoundedContextRenderer.
 *
 * @since 5.0.0
 */
@DisplayName("BoundedContextRenderer")
class BoundedContextRendererTest {

    private final BoundedContextRenderer renderer = new BoundedContextRenderer();

    @Nested
    @DisplayName("Rendering bounded contexts")
    class RenderingBoundedContexts {

        @Test
        @DisplayName("should render table with context data")
        void rendersTableWithData() {
            BoundedContextDoc orderCtx = new BoundedContextDoc(
                    "order", "com.example.order", 1, 2, 3, 1, 7, List.of("Order", "LineItem", "OrderId"));

            BoundedContextDoc inventoryCtx = new BoundedContextDoc(
                    "inventory", "com.example.inventory", 1, 0, 1, 0, 2, List.of("Product", "Sku"));

            String result = renderer.renderBoundedContextsSection(List.of(orderCtx, inventoryCtx));

            assertThat(result).contains("## Bounded Contexts");
            assertThat(result).contains("**Order**");
            assertThat(result).contains("**Inventory**");
            assertThat(result).contains("`com.example.order`");
            assertThat(result).contains("`com.example.inventory`");
        }

        @Test
        @DisplayName("should include header row with expected columns")
        void includesHeaderRow() {
            BoundedContextDoc ctx =
                    new BoundedContextDoc("order", "com.example.order", 1, 0, 0, 0, 1, List.of("Order"));

            String result = renderer.renderBoundedContextsSection(List.of(ctx));

            assertThat(result).contains("Context");
            assertThat(result).contains("Root Package");
            assertThat(result).contains("Aggregates");
            assertThat(result).contains("Entities");
            assertThat(result).contains("VOs");
            assertThat(result).contains("Ports");
            assertThat(result).contains("Total");
        }

        @Test
        @DisplayName("should capitalize context name")
        void capitalizesContextName() {
            BoundedContextDoc ctx =
                    new BoundedContextDoc("order", "com.example.order", 1, 0, 0, 0, 1, List.of("Order"));

            String result = renderer.renderBoundedContextsSection(List.of(ctx));

            assertThat(result).contains("**Order**");
        }
    }

    @Nested
    @DisplayName("Empty input handling")
    class EmptyInputHandling {

        @Test
        @DisplayName("should return empty string for null list")
        void returnsEmptyForNull() {
            String result = renderer.renderBoundedContextsSection(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for empty list")
        void returnsEmptyForEmptyList() {
            String result = renderer.renderBoundedContextsSection(List.of());

            assertThat(result).isEmpty();
        }
    }
}
