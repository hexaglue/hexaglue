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

package io.hexaglue.plugin.livingdoc.model;

import static io.hexaglue.plugin.livingdoc.V5TestModelBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.plugin.livingdoc.content.DomainContentSelector;
import io.hexaglue.plugin.livingdoc.content.PortContentSelector;
import io.hexaglue.plugin.livingdoc.renderer.DiagramRenderer;
import io.hexaglue.syntax.TypeRef;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LivingDocDiagramSet}.
 *
 * @since 5.0.0
 */
class LivingDocDiagramSetTest {

    private static final String PKG = "com.example.domain";

    @Nested
    class RecordValidation {

        @Test
        void shouldRejectNullArchitectureOverview() {
            assertThatThrownBy(() -> new LivingDocDiagramSet(null, "domain", Map.of(), "ports", "deps"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("architectureOverview");
        }

        @Test
        void shouldRejectNullDomainModel() {
            assertThatThrownBy(() -> new LivingDocDiagramSet("overview", null, Map.of(), "ports", "deps"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("domainModel");
        }

        @Test
        void shouldRejectNullPortsFlow() {
            assertThatThrownBy(() -> new LivingDocDiagramSet("overview", "domain", Map.of(), null, "deps"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("portsFlow");
        }

        @Test
        void shouldRejectNullDependencyGraph() {
            assertThatThrownBy(() -> new LivingDocDiagramSet("overview", "domain", Map.of(), "ports", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("dependencyGraph");
        }

        @Test
        void shouldMakeAggregateDiagramsUnmodifiable() {
            Map<String, String> mutable = new LinkedHashMap<>();
            mutable.put("Order", "diagram1");

            LivingDocDiagramSet set = new LivingDocDiagramSet("overview", "domain", mutable, "ports", "deps");

            assertThatThrownBy(() -> set.aggregateDiagrams().put("New", "diagram2"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldPreserveAggregateInsertionOrder() {
            Map<String, String> ordered = new LinkedHashMap<>();
            ordered.put("Order", "diagram1");
            ordered.put("Customer", "diagram2");
            ordered.put("Product", "diagram3");

            LivingDocDiagramSet set = new LivingDocDiagramSet("overview", "domain", ordered, "ports", "deps");

            assertThat(set.aggregateDiagrams().keySet()).containsExactly("Order", "Customer", "Product");
        }

        @Test
        void shouldAcceptEmptyAggregateDiagrams() {
            LivingDocDiagramSet set = new LivingDocDiagramSet("overview", "domain", Map.of(), "ports", "deps");

            assertThat(set.aggregateDiagrams()).isEmpty();
        }
    }

    @Nested
    class DiagramGeneration {

        private ArchitecturalModel buildTestModel() {
            AggregateRoot orderAggregate = aggregateRoot(PKG + ".Order", "id", TypeRef.of(PKG + ".OrderId"));
            ValueObject moneyVo = valueObject(
                    PKG + ".Money", field("amount", "java.math.BigDecimal"), field("currency", "java.lang.String"));

            Method findById =
                    method("findById", TypeRef.of("java.util.Optional"), List.of(TypeRef.of(PKG + ".OrderId")));
            Method save = method("save", TypeRef.of(PKG + ".Order"), List.of(TypeRef.of(PKG + ".Order")));
            DrivenPort orderRepository = drivenPort(
                    "com.example.ports.out.OrderRepository", DrivenPortType.REPOSITORY, List.of(findById, save));

            Method placeOrder =
                    method("placeOrder", TypeRef.of(PKG + ".OrderId"), List.of(TypeRef.of("com.example.OrderRequest")));
            DrivingPort orderUseCase = drivingPort("com.example.ports.in.OrderingProducts", List.of(placeOrder));

            return createModel(
                    ProjectContext.forTesting("app", PKG), orderAggregate, moneyVo, orderRepository, orderUseCase);
        }

        @Test
        void shouldGenerateAllFiveDiagrams() {
            ArchitecturalModel model = buildTestModel();
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);
            DomainContentSelector domainSelector = new DomainContentSelector(model);
            PortContentSelector portSelector = new PortContentSelector(model);
            DiagramRenderer renderer = new DiagramRenderer();

            LivingDocDiagramSet set = LivingDocDiagramSet.generate(docModel, domainSelector, portSelector, renderer);

            assertThat(set.architectureOverview()).isNotBlank();
            assertThat(set.domainModel()).isNotBlank();
            assertThat(set.aggregateDiagrams()).isNotEmpty();
            assertThat(set.portsFlow()).isNotBlank();
            assertThat(set.dependencyGraph()).isNotBlank();
        }

        @Test
        void shouldGenerateOverviewWithMermaidCodeBlock() {
            ArchitecturalModel model = buildTestModel();
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);
            DomainContentSelector domainSelector = new DomainContentSelector(model);
            PortContentSelector portSelector = new PortContentSelector(model);
            DiagramRenderer renderer = new DiagramRenderer();

            LivingDocDiagramSet set = LivingDocDiagramSet.generate(docModel, domainSelector, portSelector, renderer);

            assertThat(set.architectureOverview()).contains("```mermaid");
            assertThat(set.architectureOverview()).contains("graph LR");
        }

        @Test
        void shouldGenerateDomainModelWithClassDiagram() {
            ArchitecturalModel model = buildTestModel();
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);
            DomainContentSelector domainSelector = new DomainContentSelector(model);
            PortContentSelector portSelector = new PortContentSelector(model);
            DiagramRenderer renderer = new DiagramRenderer();

            LivingDocDiagramSet set = LivingDocDiagramSet.generate(docModel, domainSelector, portSelector, renderer);

            assertThat(set.domainModel()).contains("classDiagram");
        }

        @Test
        void shouldGenerateAggregateDiagramsPerRoot() {
            ArchitecturalModel model = buildTestModel();
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);
            DomainContentSelector domainSelector = new DomainContentSelector(model);
            PortContentSelector portSelector = new PortContentSelector(model);
            DiagramRenderer renderer = new DiagramRenderer();

            LivingDocDiagramSet set = LivingDocDiagramSet.generate(docModel, domainSelector, portSelector, renderer);

            assertThat(set.aggregateDiagrams()).containsKey("Order");
            assertThat(set.aggregateDiagrams().get("Order")).contains("classDiagram");
        }

        @Test
        void shouldGenerateEmptyAggregateDiagramsWhenNoAggregates() {
            ValueObject moneyVo = valueObject(
                    PKG + ".Money", field("amount", "java.math.BigDecimal"), field("currency", "java.lang.String"));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), moneyVo);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);
            DomainContentSelector domainSelector = new DomainContentSelector(model);
            PortContentSelector portSelector = new PortContentSelector(model);
            DiagramRenderer renderer = new DiagramRenderer();

            LivingDocDiagramSet set = LivingDocDiagramSet.generate(docModel, domainSelector, portSelector, renderer);

            assertThat(set.aggregateDiagrams()).isEmpty();
        }

        @Test
        void shouldGeneratePortsFlowWithFlowchartLR() {
            ArchitecturalModel model = buildTestModel();
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);
            DomainContentSelector domainSelector = new DomainContentSelector(model);
            PortContentSelector portSelector = new PortContentSelector(model);
            DiagramRenderer renderer = new DiagramRenderer();

            LivingDocDiagramSet set = LivingDocDiagramSet.generate(docModel, domainSelector, portSelector, renderer);

            assertThat(set.portsFlow()).contains("flowchart LR");
        }

        @Test
        void shouldGenerateDependencyGraphWithFlowchartTB() {
            ArchitecturalModel model = buildTestModel();
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);
            DomainContentSelector domainSelector = new DomainContentSelector(model);
            PortContentSelector portSelector = new PortContentSelector(model);
            DiagramRenderer renderer = new DiagramRenderer();

            LivingDocDiagramSet set = LivingDocDiagramSet.generate(docModel, domainSelector, portSelector, renderer);

            assertThat(set.dependencyGraph()).contains("flowchart TB");
        }
    }
}
