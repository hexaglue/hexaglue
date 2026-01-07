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

class ClassDiagramBuilderTest {

    @Test
    void shouldGenerateEmptyClassDiagram() {
        String result = new ClassDiagramBuilder().build();

        assertThat(result).isEqualTo("```mermaid\nclassDiagram\n```\n\n");
    }

    @Test
    void shouldGenerateClassWithStereotype() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .stereotype("Aggregate Root")
                .end()
                .build();

        assertThat(result)
                .contains("class Order {")
                .contains("<<Aggregate Root>>")
                .contains("}");
    }

    @Test
    void shouldGenerateClassWithFields() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .field("UUID", "id")
                .field("String", "name")
                .end()
                .build();

        assertThat(result)
                .contains("class Order {")
                .contains("+UUID id")
                .contains("+String name")
                .contains("}");
    }

    @Test
    void shouldGenerateClassWithMethods() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .method("addItem(LineItem)")
                .method("getTotal() Money")
                .end()
                .build();

        assertThat(result)
                .contains("class Order {")
                .contains("+addItem(LineItem)")
                .contains("+getTotal() Money")
                .contains("}");
    }

    @Test
    void shouldGenerateClassWithGenericTypes() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .field("List~LineItem~", "items")
                .field("Optional~Address~", "address")
                .end()
                .build();

        assertThat(result).contains("+List~LineItem~ items").contains("+Optional~Address~ address");
    }

    @Test
    void shouldGenerateCompleteClass() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .stereotype("Aggregate Root")
                .field("UUID", "id")
                .field("List~LineItem~", "items")
                .method("addItem(LineItem)")
                .method("removeItem(UUID)")
                .end()
                .build();

        assertThat(result)
                .contains("class Order {")
                .contains("<<Aggregate Root>>")
                .contains("+UUID id")
                .contains("+List~LineItem~ items")
                .contains("+addItem(LineItem)")
                .contains("+removeItem(UUID)")
                .contains("}");
    }

    @Test
    void shouldGenerateCompositionRelationship() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .end()
                .addClass("LineItem")
                .end()
                .composition("Order", "LineItem")
                .build();

        assertThat(result).contains("Order *-- LineItem");
    }

    @Test
    void shouldGenerateCompositionWithCardinality() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .end()
                .addClass("LineItem")
                .end()
                .composition("Order", "1", "*", "LineItem")
                .build();

        assertThat(result).contains("Order \"1\" *-- \"*\" LineItem");
    }

    @Test
    void shouldGenerateAggregationRelationship() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .end()
                .addClass("Customer")
                .end()
                .aggregation("Order", "Customer")
                .build();

        assertThat(result).contains("Order o-- Customer");
    }

    @Test
    void shouldGenerateAssociationRelationship() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .end()
                .addClass("Product")
                .end()
                .association("Order", "Product")
                .build();

        assertThat(result).contains("Order --> Product");
    }

    @Test
    void shouldGenerateAssociationWithLabel() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .end()
                .addClass("Product")
                .end()
                .association("Order", "Product", "contains")
                .build();

        assertThat(result).contains("Order --> Product : contains");
    }

    @Test
    void shouldGenerateDependencyRelationship() {
        String result = new ClassDiagramBuilder()
                .addClass("OrderService")
                .end()
                .addClass("EmailService")
                .end()
                .dependency("OrderService", "EmailService")
                .build();

        assertThat(result).contains("OrderService ..> EmailService");
    }

    @Test
    void shouldGenerateInheritanceRelationship() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .end()
                .addClass("Entity")
                .end()
                .inheritance("Order", "Entity")
                .build();

        assertThat(result).contains("Order <|-- Entity");
    }

    @Test
    void shouldGenerateNote() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .end()
                .note("Order", "This is the main aggregate root")
                .build();

        assertThat(result).contains("note for Order \"This is the main aggregate root\"");
    }

    @Test
    void shouldSanitizeClassNames() {
        String result = new ClassDiagramBuilder()
                .addClass("Order-Service")
                .end()
                .addClass("Line.Item")
                .end()
                .composition("Order-Service", "Line.Item")
                .build();

        assertThat(result)
                .contains("class Order_Service {")
                .contains("class Line_Item {")
                .contains("Order_Service *-- Line_Item");
    }

    @Test
    void shouldAllowRawMermaidSyntax() {
        String result = new ClassDiagramBuilder()
                .raw("class CustomClass")
                .raw("CustomClass : +customMethod()")
                .build();

        assertThat(result).contains("CustomClass").contains("CustomClass : +customMethod()");
    }

    @Test
    void shouldGenerateMultipleClasses() {
        String result = new ClassDiagramBuilder()
                .addClass("Order")
                .stereotype("Aggregate Root")
                .field("UUID", "id")
                .end()
                .addClass("LineItem")
                .stereotype("Entity")
                .field("String", "product")
                .end()
                .addClass("Address")
                .stereotype("Value Object")
                .end()
                .composition("Order", "LineItem")
                .association("Order", "Address")
                .build();

        assertThat(result)
                .contains("class Order {")
                .contains("<<Aggregate Root>>")
                .contains("class LineItem {")
                .contains("<<Entity>>")
                .contains("class Address {")
                .contains("<<Value Object>>")
                .contains("Order *-- LineItem")
                .contains("Order --> Address");
    }

    @Test
    void shouldWrapDiagramInCodeFence() {
        String result = new ClassDiagramBuilder().addClass("Test").end().build();

        assertThat(result).startsWith("```mermaid\n").endsWith("```\n\n");
    }
}
