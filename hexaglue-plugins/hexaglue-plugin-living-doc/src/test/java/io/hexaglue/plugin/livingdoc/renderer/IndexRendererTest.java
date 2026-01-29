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

import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.plugin.livingdoc.model.GlossaryEntry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link IndexRenderer}.
 *
 * @since 5.0.0
 */
@DisplayName("IndexRenderer")
class IndexRendererTest {

    private final IndexRenderer renderer = new IndexRenderer();

    @Nested
    @DisplayName("Groups by kind")
    class GroupsByKind {

        @Test
        @DisplayName("should generate separate sections for each ArchKind")
        void generatesSectionsPerKind() {
            List<GlossaryEntry> entries = List.of(
                    new GlossaryEntry(
                            "Order", "A customer order", ArchKind.AGGREGATE_ROOT, "com.example.Order", "com.example"),
                    new GlossaryEntry(
                            "Money", "Monetary value", ArchKind.VALUE_OBJECT, "com.example.Money", "com.example"),
                    new GlossaryEntry(
                            "OrderRepository",
                            "Repository for orders",
                            ArchKind.DRIVEN_PORT,
                            "com.example.port.OrderRepository",
                            "com.example.port"));

            String result = renderer.renderTypeIndex(entries);

            assertThat(result).contains("### Aggregate Roots");
            assertThat(result).contains("### Value Objects");
            assertThat(result).contains("### Driven Ports");
        }
    }

    @Nested
    @DisplayName("Generates links")
    class GeneratesLinks {

        @Test
        @DisplayName("should link domain types to domain.md")
        void linksDomainTypesToDomainMd() {
            List<GlossaryEntry> entries = List.of(new GlossaryEntry(
                    "Order", "A customer order", ArchKind.AGGREGATE_ROOT, "com.example.Order", "com.example"));

            String result = renderer.renderTypeIndex(entries);

            assertThat(result).contains("[Order](./domain.md#order)");
        }

        @Test
        @DisplayName("should link port types to ports.md")
        void linksPortTypesToPortsMd() {
            List<GlossaryEntry> entries = List.of(new GlossaryEntry(
                    "OrderRepository",
                    "Repository for orders",
                    ArchKind.DRIVEN_PORT,
                    "com.example.port.OrderRepository",
                    "com.example.port"));

            String result = renderer.renderTypeIndex(entries);

            assertThat(result).contains("[OrderRepository](./ports.md#orderrepository)");
        }
    }

    @Nested
    @DisplayName("Empty list")
    class EmptyList {

        @Test
        @DisplayName("should return empty string for empty list")
        void returnsEmptyForEmptyList() {
            String result = renderer.renderTypeIndex(List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for null list")
        void returnsEmptyForNullList() {
            String result = renderer.renderTypeIndex(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Sorting within sections")
    class SortedWithinSections {

        @Test
        @DisplayName("should sort types alphabetically within each section")
        void sortsAlphabeticallyWithinSections() {
            List<GlossaryEntry> entries = List.of(
                    new GlossaryEntry(
                            "Product", "A product", ArchKind.AGGREGATE_ROOT, "com.example.Product", "com.example"),
                    new GlossaryEntry("Order", "An order", ArchKind.AGGREGATE_ROOT, "com.example.Order", "com.example"),
                    new GlossaryEntry(
                            "Customer", "A customer", ArchKind.AGGREGATE_ROOT, "com.example.Customer", "com.example"));

            String result = renderer.renderTypeIndex(entries);

            int customerPos = result.indexOf("Customer");
            int orderPos = result.indexOf("Order");
            int productPos = result.indexOf("Product");

            assertThat(customerPos).isLessThan(orderPos);
            assertThat(orderPos).isLessThan(productPos);
        }
    }
}
