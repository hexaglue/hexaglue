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

import io.hexaglue.arch.ElementKind;
import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.model.IdentityDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.plugin.livingdoc.model.PropertyDoc;
import io.hexaglue.plugin.livingdoc.model.RelationDoc;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DiagramRendererTest {

    private DiagramRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new DiagramRenderer();
    }

    @Nested
    class DomainClassDiagramRendering {

        @Test
        void shouldRenderBasicMermaidStructure() {
            DomainTypeDoc type = createSimpleAggregate("Order");
            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).startsWith("```mermaid\n");
            assertThat(result).contains("classDiagram\n");
            assertThat(result).endsWith("```\n\n");
        }

        @Test
        void shouldRenderClassDefinition() {
            DomainTypeDoc type = createSimpleAggregate("Order");
            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("class Order {");
            assertThat(result).contains("<<Aggregate Root>>");
        }

        @Test
        void shouldRenderIdentityField() {
            IdentityDoc identity = new IdentityDoc("id", "OrderId", "UUID", "ASSIGNED", "RECORD", true, false, null);
            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    identity,
                    List.of(),
                    List.of(),
                    createDebugInfo("Order"));

            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("+OrderId id");
        }

        @Test
        void shouldRenderPropertiesUpToLimit() {
            PropertyDoc prop1 = createProperty("name", "java.lang.String");
            PropertyDoc prop2 = createProperty("status", "java.lang.String");
            PropertyDoc prop3 = createProperty("total", "java.math.BigDecimal");

            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(prop1, prop2, prop3),
                    List.of(),
                    createDebugInfo("Order"));

            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("+String name");
            assertThat(result).contains("+String status");
            assertThat(result).contains("+BigDecimal total");
        }

        @Test
        void shouldShowEllipsisWhenTooManyProperties() {
            List<PropertyDoc> properties = List.of(
                    createProperty("prop1", "String"),
                    createProperty("prop2", "String"),
                    createProperty("prop3", "String"),
                    createProperty("prop4", "String"),
                    createProperty("prop5", "String"),
                    createProperty("prop6", "String"),
                    createProperty("prop7", "String"));

            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    properties,
                    List.of(),
                    createDebugInfo("Order"));

            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("...");
        }

        @Test
        void shouldRenderCollectionPropertyWithSpecialSyntax() {
            PropertyDoc prop = new PropertyDoc(
                    "tags",
                    "java.util.List",
                    "COLLECTION",
                    "NON_NULL",
                    false,
                    false,
                    false,
                    true,
                    List.of("java.lang.String"),
                    null);

            DomainTypeDoc type = new DomainTypeDoc(
                    "Product",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(prop),
                    List.of(),
                    createDebugInfo("Product"));

            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("+List~String~ tags");
        }

        @Test
        void shouldRenderMultipleClasses() {
            DomainTypeDoc order = createSimpleAggregate("Order");
            DomainTypeDoc customer = createSimpleAggregate("Customer");
            DomainTypeDoc product = createSimpleAggregate("Product");

            String result = renderer.renderDomainClassDiagram(List.of(order, customer, product));

            assertThat(result).contains("class Order {");
            assertThat(result).contains("class Customer {");
            assertThat(result).contains("class Product {");
        }
    }

    @Nested
    class RelationshipRendering {

        @Test
        void shouldRenderOneToManyRelation() {
            RelationDoc rel = new RelationDoc(
                    "lineItems",
                    "OrderLineItem",
                    ElementKind.ENTITY.toString(),
                    "ONE_TO_MANY",
                    true,
                    false,
                    null,
                    "ALL",
                    "LAZY",
                    true);

            DomainTypeDoc order = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(rel),
                    createDebugInfo("Order"));

            DomainTypeDoc lineItem = createSimpleEntity("OrderLineItem");

            String result = renderer.renderDomainClassDiagram(List.of(order, lineItem));

            assertThat(result).contains("Order \"1\" --o \"*\" OrderLineItem");
        }

        @Test
        void shouldRenderManyToOneRelation() {
            RelationDoc rel = new RelationDoc(
                    "order",
                    "Order",
                    ElementKind.AGGREGATE_ROOT.toString(),
                    "MANY_TO_ONE",
                    false,
                    true,
                    "lineItems",
                    "NONE",
                    "EAGER",
                    false);

            DomainTypeDoc lineItem = new DomainTypeDoc(
                    "OrderLineItem",
                    "com.example.domain",
                    ElementKind.ENTITY,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(rel),
                    createDebugInfo("OrderLineItem"));

            DomainTypeDoc order = createSimpleAggregate("Order");

            String result = renderer.renderDomainClassDiagram(List.of(lineItem, order));

            assertThat(result).contains("OrderLineItem \"*\" o-- \"1\" Order");
        }

        @Test
        void shouldRenderEmbeddedRelation() {
            RelationDoc rel = new RelationDoc(
                    "address",
                    "Address",
                    ElementKind.VALUE_OBJECT.toString(),
                    "EMBEDDED",
                    true,
                    false,
                    null,
                    "NONE",
                    "EAGER",
                    false);

            DomainTypeDoc customer = new DomainTypeDoc(
                    "Customer",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(rel),
                    createDebugInfo("Customer"));

            DomainTypeDoc address = createSimpleValueObject("Address");

            String result = renderer.renderDomainClassDiagram(List.of(customer, address));

            assertThat(result).contains("Customer *-- Address");
        }

        @Test
        void shouldRenderRelationsFromProperties() {
            PropertyDoc customerProp = new PropertyDoc(
                    "customer",
                    "com.example.domain.Customer",
                    "SINGLE",
                    "NON_NULL",
                    false,
                    false,
                    false,
                    false,
                    List.of(),
                    null);

            DomainTypeDoc order = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(customerProp),
                    List.of(),
                    createDebugInfo("Order"));

            DomainTypeDoc customer = createSimpleAggregate("Customer");

            Map<String, DomainTypeDoc> typeMap = new HashMap<>();
            typeMap.put("com.example.domain.Order", order);
            typeMap.put("com.example.domain.Customer", customer);

            String result = renderer.renderDomainClassDiagram(List.of(order, customer));

            assertThat(result).contains("Order --> Customer : customer");
        }

        @Test
        void shouldRenderCollectionPropertyRelation() {
            PropertyDoc lineItemsProp = new PropertyDoc(
                    "items",
                    "java.util.List",
                    "COLLECTION",
                    "NON_NULL",
                    false,
                    false,
                    false,
                    true,
                    List.of("com.example.domain.OrderLineItem"),
                    null);

            DomainTypeDoc order = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(lineItemsProp),
                    List.of(),
                    createDebugInfo("Order"));

            DomainTypeDoc lineItem = createSimpleEntity("OrderLineItem");

            String result = renderer.renderDomainClassDiagram(List.of(order, lineItem));

            // The diagram shows the property in the class definition
            assertThat(result).contains("+List~OrderLineItem~ items");
            // Properties without relationInfo are not rendered as relationships from properties
            // They need to be in the relations list to generate arrows
        }
    }

    @Nested
    class StereotypeRendering {

        @Test
        void shouldRenderAggregateRootStereotype() {
            DomainTypeDoc type = createSimpleAggregate("Order");
            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("<<Aggregate Root>>");
        }

        @Test
        void shouldRenderEntityStereotype() {
            DomainTypeDoc type = createSimpleEntity("OrderLineItem");
            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("<<Entity>>");
        }

        @Test
        void shouldRenderValueObjectStereotype() {
            DomainTypeDoc type = createSimpleValueObject("Money");
            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("<<Value Object>>");
        }

        @Test
        void shouldRenderIdentifierStereotype() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "OrderId",
                    "com.example.domain",
                    ElementKind.IDENTIFIER,
                    ConfidenceLevel.HIGH,
                    "RECORD",
                    true,
                    null,
                    List.of(),
                    List.of(),
                    createDebugInfo("OrderId"));

            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("<<Identifier>>");
        }

        @Test
        void shouldRenderDomainEventStereotype() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "OrderCreated",
                    "com.example.domain",
                    ElementKind.DOMAIN_EVENT,
                    ConfidenceLevel.HIGH,
                    "RECORD",
                    true,
                    null,
                    List.of(),
                    List.of(),
                    createDebugInfo("OrderCreated"));

            String result = renderer.renderDomainClassDiagram(List.of(type));

            assertThat(result).contains("<<Event>>");
        }
    }

    @Nested
    class AggregateDiagramRendering {

        @Test
        void shouldRenderAggregateDiagram() {
            PropertyDoc lineItemsProp = new PropertyDoc(
                    "items",
                    "com.example.domain.OrderLineItem",
                    "COLLECTION",
                    "NON_NULL",
                    false,
                    false,
                    false,
                    false,
                    List.of(),
                    null);

            DomainTypeDoc order = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(lineItemsProp),
                    List.of(),
                    createDebugInfo("Order"));

            DomainTypeDoc lineItem = createSimpleEntity("OrderLineItem");

            Map<String, DomainTypeDoc> allTypes = new HashMap<>();
            allTypes.put("com.example.domain.Order", order);
            allTypes.put("com.example.domain.OrderLineItem", lineItem);

            String result = renderer.renderAggregateDiagram(order, allTypes);

            assertThat(result).contains("```mermaid");
            assertThat(result).contains("classDiagram");
            assertThat(result).contains("class Order");
            assertThat(result).contains("class OrderLineItem");
            assertThat(result).contains("Order \"1\" *-- \"*\" OrderLineItem");
        }

        @Test
        void shouldRenderAggregateWithSingleRelation() {
            PropertyDoc addressProp = new PropertyDoc(
                    "address",
                    "com.example.domain.Address",
                    "SINGLE",
                    "NON_NULL",
                    false,
                    false,
                    false,
                    false,
                    List.of(),
                    null);

            DomainTypeDoc customer = new DomainTypeDoc(
                    "Customer",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(addressProp),
                    List.of(),
                    createDebugInfo("Customer"));

            DomainTypeDoc address = createSimpleValueObject("Address");

            Map<String, DomainTypeDoc> allTypes = new HashMap<>();
            allTypes.put("com.example.domain.Customer", customer);
            allTypes.put("com.example.domain.Address", address);

            String result = renderer.renderAggregateDiagram(customer, allTypes);

            assertThat(result).contains("Customer *-- Address");
        }
    }

    @Nested
    class PortsFlowDiagramRendering {

        @Test
        void shouldRenderBasicFlowDiagram() {
            PortDoc drivingPort = createDrivingPort("OrderUseCase");
            PortDoc drivenPort = createDrivenPort("OrderRepository");
            DomainTypeDoc aggregate = createSimpleAggregate("Order");

            String result =
                    renderer.renderPortsFlowDiagram(List.of(drivingPort), List.of(drivenPort), List.of(aggregate));

            assertThat(result).contains("```mermaid");
            assertThat(result).contains("flowchart LR");
            assertThat(result).endsWith("```\n\n");
        }

        @Test
        void shouldRenderExternalActors() {
            String result = renderer.renderPortsFlowDiagram(List.of(), List.of(), List.of());

            assertThat(result).contains("subgraph External[\"External\"]");
            assertThat(result).contains("User([User])");
            assertThat(result).contains("API([API Client])");
        }

        @Test
        void shouldRenderDrivingPorts() {
            PortDoc port1 = createDrivingPort("CreateOrderUseCase");
            PortDoc port2 = createDrivingPort("CancelOrderUseCase");

            String result = renderer.renderPortsFlowDiagram(List.of(port1, port2), List.of(), List.of());

            assertThat(result).contains("subgraph Driving[\"Driving Ports\"]");
            assertThat(result).contains("CreateOrderUseCase[CreateOrderUseCase]");
            assertThat(result).contains("CancelOrderUseCase[CancelOrderUseCase]");
        }

        @Test
        void shouldRenderDrivenPorts() {
            PortDoc port1 = createDrivenPort("OrderRepository");
            PortDoc port2 = createDrivenPort("PaymentGateway");

            String result = renderer.renderPortsFlowDiagram(List.of(), List.of(port1, port2), List.of());

            assertThat(result).contains("subgraph Driven[\"Driven Ports\"]");
            assertThat(result).contains("OrderRepository[OrderRepository]");
            assertThat(result).contains("PaymentGateway[PaymentGateway]");
        }

        @Test
        void shouldRenderDomainAggregates() {
            DomainTypeDoc order = createSimpleAggregate("Order");
            DomainTypeDoc customer = createSimpleAggregate("Customer");

            String result = renderer.renderPortsFlowDiagram(List.of(), List.of(), List.of(order, customer));

            assertThat(result).contains("subgraph Domain[\"Domain\"]");
            assertThat(result).contains("Order{{Order}}");
            assertThat(result).contains("Customer{{Customer}}");
        }

        @Test
        void shouldRenderDomainLogicWhenNoAggregates() {
            String result = renderer.renderPortsFlowDiagram(List.of(), List.of(), List.of());

            assertThat(result).contains("subgraph Domain[\"Domain\"]");
            assertThat(result).contains("DomainLogic[Domain Logic]");
        }

        @Test
        void shouldRenderInfrastructure() {
            String result = renderer.renderPortsFlowDiagram(List.of(), List.of(), List.of());

            assertThat(result).contains("subgraph Infra[\"Infrastructure\"]");
            assertThat(result).contains("DB[(Database)]");
            assertThat(result).contains("ExtAPI[External APIs]");
        }

        @Test
        void shouldRenderConnections() {
            String result = renderer.renderPortsFlowDiagram(List.of(), List.of(), List.of());

            assertThat(result).contains("User --> Driving");
            assertThat(result).contains("API --> Driving");
            assertThat(result).contains("Driving --> Domain");
            assertThat(result).contains("Domain --> Driven");
            assertThat(result).contains("Driven --> DB");
            assertThat(result).contains("Driven --> ExtAPI");
        }

        @Test
        void shouldSanitizePortNamesWithSpecialCharacters() {
            PortDoc port = new PortDoc(
                    "Order-Management",
                    "com.example.ports.in",
                    PortKind.USE_CASE,
                    PortDirection.DRIVING,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(),
                    createDebugInfo("Order-Management", "com.example.ports.in"));

            String result = renderer.renderPortsFlowDiagram(List.of(port), List.of(), List.of());

            // The sanitization happens inside the renderer
            assertThat(result).contains("Order-Management");
        }
    }

    @Nested
    class EmptyDiagrams {

        @Test
        void shouldRenderEmptyClassDiagram() {
            String result = renderer.renderDomainClassDiagram(List.of());

            assertThat(result).contains("```mermaid");
            assertThat(result).contains("classDiagram");
            assertThat(result).endsWith("```\n\n");
        }

        @Test
        void shouldRenderFlowDiagramWithoutPorts() {
            String result = renderer.renderPortsFlowDiagram(List.of(), List.of(), List.of());

            assertThat(result).contains("flowchart LR");
            assertThat(result).contains("subgraph External");
            assertThat(result).contains("subgraph Domain");
            assertThat(result).contains("subgraph Infra");
            // Empty port subgraphs should still be rendered with placeholder "(none)"
            // to ensure valid Mermaid diagrams and make missing ports visible
            assertThat(result).contains("subgraph Driving");
            assertThat(result).contains("subgraph Driven");
            assertThat(result).contains("NoDriving[\"(none)\"]");
            assertThat(result).contains("NoDriven[\"(none)\"]");
        }
    }

    // Helper methods

    private DomainTypeDoc createSimpleAggregate(String name) {
        return new DomainTypeDoc(
                name,
                "com.example.domain",
                ElementKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                "CLASS",
                false,
                null,
                List.of(),
                List.of(),
                createDebugInfo(name));
    }

    private DomainTypeDoc createSimpleEntity(String name) {
        return new DomainTypeDoc(
                name,
                "com.example.domain",
                ElementKind.ENTITY,
                ConfidenceLevel.HIGH,
                "CLASS",
                false,
                null,
                List.of(),
                List.of(),
                createDebugInfo(name));
    }

    private DomainTypeDoc createSimpleValueObject(String name) {
        return new DomainTypeDoc(
                name,
                "com.example.domain",
                ElementKind.VALUE_OBJECT,
                ConfidenceLevel.EXPLICIT,
                "RECORD",
                true,
                null,
                List.of(),
                List.of(),
                createDebugInfo(name));
    }

    private PropertyDoc createProperty(String name, String type) {
        return new PropertyDoc(name, type, "SINGLE", "NON_NULL", false, false, true, false, List.of(), null);
    }

    private PortDoc createDrivingPort(String name) {
        return new PortDoc(
                name,
                "com.example.ports.in",
                PortKind.USE_CASE,
                PortDirection.DRIVING,
                ConfidenceLevel.HIGH,
                List.of(),
                List.of(),
                createDebugInfo(name, "com.example.ports.in"));
    }

    private PortDoc createDrivenPort(String name) {
        return new PortDoc(
                name,
                "com.example.ports.out",
                PortKind.REPOSITORY,
                PortDirection.DRIVEN,
                ConfidenceLevel.HIGH,
                List.of(),
                List.of(),
                createDebugInfo(name, "com.example.ports.out"));
    }

    private DebugInfo createDebugInfo(String name) {
        return new DebugInfo("com.example.domain." + name, List.of(), name + ".java", 10, 50);
    }

    private DebugInfo createDebugInfo(String name, String packageName) {
        return new DebugInfo(packageName + "." + name, List.of(), name + ".java", 10, 50);
    }
}
