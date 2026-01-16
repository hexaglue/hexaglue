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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.arch.ports.PortClassification;
import io.hexaglue.arch.ports.PortOperation;
import io.hexaglue.plugin.livingdoc.generator.DiagramGenerator;
import io.hexaglue.plugin.livingdoc.generator.DomainDocGenerator;
import io.hexaglue.plugin.livingdoc.generator.OverviewGenerator;
import io.hexaglue.plugin.livingdoc.generator.PortDocGenerator;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel;
import io.hexaglue.plugin.livingdoc.model.DocumentationModelFactory;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for LivingDocPlugin using v4 ArchitecturalModel API.
 *
 * @since 4.0.0
 */
class LivingDocPluginTest {

    private static final String PKG = "com.example.domain";

    private ArchitecturalModel testModel;

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @BeforeEach
    void setUp() {
        // Create test domain types
        DomainEntity orderAggregate = new DomainEntity(
                ElementId.of(PKG + ".Order"),
                ElementKind.AGGREGATE_ROOT,
                "id",
                TypeRef.of(PKG + ".OrderId"),
                Optional.empty(),
                List.of(),
                null,
                highConfidence(ElementKind.AGGREGATE_ROOT));

        ValueObject moneyVo = ValueObject.of(PKG + ".Money", List.of("amount", "currency"), highConfidence(ElementKind.VALUE_OBJECT));

        // Create test ports
        PortOperation findById = new PortOperation(
                "findById",
                TypeRef.of("java.util.Optional"),
                List.of(TypeRef.of(PKG + ".OrderId")),
                null);
        PortOperation save = new PortOperation("save", TypeRef.of(PKG + ".Order"), List.of(TypeRef.of(PKG + ".Order")), null);

        DrivenPort orderRepository = new DrivenPort(
                ElementId.of("com.example.ports.out.OrderRepository"),
                PortClassification.REPOSITORY,
                List.of(findById, save),
                Optional.empty(),
                List.of(),
                null,
                highConfidence(ElementKind.DRIVEN_PORT));

        PortOperation placeOrder = new PortOperation(
                "placeOrder", TypeRef.of(PKG + ".OrderId"), List.of(TypeRef.of("com.example.OrderRequest")), null);

        DrivingPort orderUseCase = new DrivingPort(
                ElementId.of("com.example.ports.in.OrderingProducts"),
                PortClassification.USE_CASE,
                List.of(placeOrder),
                List.of(),
                null,
                highConfidence(ElementKind.DRIVING_PORT));

        // Build the model
        testModel = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                .add(orderAggregate)
                .add(moneyVo)
                .add(orderRepository)
                .add(orderUseCase)
                .build();
    }

    @Test
    void shouldHaveCorrectPluginId() {
        LivingDocPlugin plugin = new LivingDocPlugin();
        assertThat(plugin.id()).isEqualTo("io.hexaglue.plugin.livingdoc");
    }

    @Test
    void overviewGeneratorShouldGenerateValidMarkdown() {
        DocumentationModel docModel = DocumentationModelFactory.fromArchModel(testModel);
        OverviewGenerator generator = new OverviewGenerator(docModel);
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
        DomainDocGenerator generator = new DomainDocGenerator(testModel);
        String result = generator.generate();

        assertThat(result).contains("# Domain Model");
        assertThat(result).contains("## Aggregate Roots");
        assertThat(result).contains("### Order");
        assertThat(result).contains("## Value Objects");
        assertThat(result).contains("### Money");
    }

    @Test
    void portDocGeneratorShouldGenerateValidMarkdown() {
        PortDocGenerator generator = new PortDocGenerator(testModel);
        String result = generator.generate();

        assertThat(result).contains("# Ports");
        assertThat(result).contains("## Driving Ports (Primary)");
        assertThat(result).contains("### OrderingProducts");
        assertThat(result).contains("## Driven Ports (Secondary)");
        assertThat(result).contains("### OrderRepository");
    }

    @Test
    void diagramGeneratorShouldGenerateMermaidDiagrams() {
        DiagramGenerator generator = new DiagramGenerator(testModel);
        String result = generator.generate();

        assertThat(result).contains("# Architecture Diagrams");
        assertThat(result).contains("## Domain Model");
        assertThat(result).contains("```mermaid");
        assertThat(result).contains("classDiagram");
        assertThat(result).contains("class Order");
        assertThat(result).contains("<<Aggregate Root>>");
    }
}
