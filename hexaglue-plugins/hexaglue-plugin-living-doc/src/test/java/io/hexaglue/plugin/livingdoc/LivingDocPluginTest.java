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

import static io.hexaglue.plugin.livingdoc.V5TestModelBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.plugin.livingdoc.generator.DiagramGenerator;
import io.hexaglue.plugin.livingdoc.generator.DomainDocGenerator;
import io.hexaglue.plugin.livingdoc.generator.OverviewGenerator;
import io.hexaglue.plugin.livingdoc.generator.PortDocGenerator;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel;
import io.hexaglue.plugin.livingdoc.model.DocumentationModelFactory;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for LivingDocPlugin using v5 ArchitecturalModel API.
 *
 * @since 4.0.0
 * @since 5.0.0 - Migrated to v5 ArchType API
 */
class LivingDocPluginTest {

    private static final String PKG = "com.example.domain";

    private ArchitecturalModel testModel;

    @BeforeEach
    void setUp() {
        // Create test domain types using V5TestModelBuilder
        AggregateRoot orderAggregate = aggregateRoot(PKG + ".Order", "id", TypeRef.of(PKG + ".OrderId"));
        ValueObject moneyVo = valueObject(PKG + ".Money", field("amount", "java.math.BigDecimal"), field("currency", "java.lang.String"));

        // Create test ports
        Method findById = method("findById", TypeRef.of("java.util.Optional"), List.of(TypeRef.of(PKG + ".OrderId")));
        Method save = method("save", TypeRef.of(PKG + ".Order"), List.of(TypeRef.of(PKG + ".Order")));

        DrivenPort orderRepository =
                drivenPort("com.example.ports.out.OrderRepository", DrivenPortType.REPOSITORY, List.of(findById, save));

        Method placeOrder = method("placeOrder", TypeRef.of(PKG + ".OrderId"), List.of(TypeRef.of("com.example.OrderRequest")));

        DrivingPort orderUseCase = drivingPort("com.example.ports.in.OrderingProducts", List.of(placeOrder));

        // Build the model using V5TestModelBuilder
        testModel =
                createModel(ProjectContext.forTesting("app", PKG), orderAggregate, moneyVo, orderRepository, orderUseCase);
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
