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

package io.hexaglue.plugin.livingdoc;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.livingdoc.generator.DiagramGenerator;
import io.hexaglue.plugin.livingdoc.generator.DomainDocGenerator;
import io.hexaglue.plugin.livingdoc.generator.OverviewGenerator;
import io.hexaglue.plugin.livingdoc.generator.PortDocGenerator;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainModel;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IdentityStrategy;
import io.hexaglue.spi.ir.IdentityWrapperKind;
import io.hexaglue.spi.ir.IrMetadata;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.JavaConstruct;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.spi.ir.PortMethod;
import io.hexaglue.spi.ir.PortModel;
import io.hexaglue.spi.ir.SourceRef;
import io.hexaglue.spi.ir.TypeRef;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LivingDocPluginTest {

    private IrSnapshot testIr;

    @BeforeEach
    void setUp() {
        // Create test domain types
        TypeRef uuidType = TypeRef.of("java.util.UUID");
        TypeRef orderIdType = TypeRef.of("com.example.domain.OrderId");

        Identity orderId =
                new Identity("id", orderIdType, uuidType, IdentityStrategy.ASSIGNED, IdentityWrapperKind.RECORD);

        DomainType orderAggregate = new DomainType(
                "com.example.domain.Order",
                "Order",
                "com.example.domain",
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(orderId),
                List.of(),
                List.of(),
                List.of(),
                SourceRef.unknown());

        DomainType moneyVo = new DomainType(
                "com.example.domain.Money",
                "Money",
                "com.example.domain",
                DomainKind.VALUE_OBJECT,
                ConfidenceLevel.EXPLICIT,
                JavaConstruct.RECORD,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                SourceRef.unknown());

        DomainModel domain = new DomainModel(List.of(orderAggregate, moneyVo));

        // Create test ports
        Port orderRepository = new Port(
                "com.example.ports.out.OrderRepository",
                "OrderRepository",
                "com.example.ports.out",
                PortKind.REPOSITORY,
                PortDirection.DRIVEN,
                ConfidenceLevel.HIGH,
                List.of("com.example.domain.Order"),
                "com.example.domain.Order",
                List.of(
                        new PortMethod("findById", "Optional<Order>", List.of("OrderId")),
                        new PortMethod("save", "Order", List.of("Order"))),
                List.of(),
                SourceRef.unknown());

        Port orderUseCase = new Port(
                "com.example.ports.in.OrderingProducts",
                "OrderingProducts",
                "com.example.ports.in",
                PortKind.USE_CASE,
                PortDirection.DRIVING,
                ConfidenceLevel.MEDIUM,
                List.of(),
                null,
                List.of(new PortMethod("placeOrder", "OrderId", List.of("OrderRequest"))),
                List.of(),
                SourceRef.unknown());

        PortModel ports = new PortModel(List.of(orderRepository, orderUseCase));

        // Create IR metadata
        IrMetadata metadata = IrMetadata.withDefaults("com.example", Instant.now(), "2.0.0-SNAPSHOT", 2, 2);

        testIr = new IrSnapshot(domain, ports, metadata);
    }

    @Test
    void shouldHaveCorrectPluginId() {
        LivingDocPlugin plugin = new LivingDocPlugin();
        assertThat(plugin.id()).isEqualTo("io.hexaglue.plugin.livingdoc");
    }

    @Test
    void overviewGeneratorShouldGenerateValidMarkdown() {
        OverviewGenerator generator = new OverviewGenerator(testIr);
        String result = generator.generate(true);

        assertThat(result).contains("# Architecture Overview");
        assertThat(result).contains("## Summary");
        assertThat(result).contains("| Aggregate Roots | 1 |");
        assertThat(result).contains("| Value Objects | 1 |");
        assertThat(result).contains("| Driving Ports | 1 |");
        assertThat(result).contains("| Driven Ports | 1 |");
        assertThat(result).contains("```mermaid");
    }

    @Test
    void domainDocGeneratorShouldGenerateValidMarkdown() {
        DomainDocGenerator generator = new DomainDocGenerator(testIr);
        String result = generator.generate();

        assertThat(result).contains("# Domain Model");
        assertThat(result).contains("## Aggregate Roots");
        assertThat(result).contains("### Order");
        assertThat(result).contains("## Value Objects");
        assertThat(result).contains("### Money");
        assertThat(result).contains("| **Kind** | Aggregate Root |");
    }

    @Test
    void portDocGeneratorShouldGenerateValidMarkdown() {
        PortDocGenerator generator = new PortDocGenerator(testIr);
        String result = generator.generate();

        assertThat(result).contains("# Ports");
        assertThat(result).contains("## Driving Ports (Primary)");
        assertThat(result).contains("### OrderingProducts");
        assertThat(result).contains("## Driven Ports (Secondary)");
        assertThat(result).contains("### OrderRepository");
        assertThat(result).contains("| **Kind** | Repository |");
    }

    @Test
    void diagramGeneratorShouldGenerateMermaidDiagrams() {
        DiagramGenerator generator = new DiagramGenerator(testIr);
        String result = generator.generate();

        assertThat(result).contains("# Architecture Diagrams");
        assertThat(result).contains("## Domain Model");
        assertThat(result).contains("```mermaid");
        assertThat(result).contains("classDiagram");
        assertThat(result).contains("class Order");
        assertThat(result).contains("<<Aggregate Root>>");
    }
}
