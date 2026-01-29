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

package io.hexaglue.plugin.livingdoc.generator;

import static io.hexaglue.plugin.livingdoc.V5TestModelBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.plugin.livingdoc.content.GlossaryBuilder;
import io.hexaglue.plugin.livingdoc.content.StructureBuilder;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel;
import io.hexaglue.plugin.livingdoc.model.DocumentationModelFactory;
import io.hexaglue.plugin.livingdoc.model.GlossaryEntry;
import io.hexaglue.plugin.livingdoc.renderer.IndexRenderer;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OverviewGenerator} with the new content sections.
 *
 * @since 5.0.0
 */
@DisplayName("OverviewGenerator")
class OverviewGeneratorTest {

    private static final ProjectContext PROJECT = ProjectContext.forTesting("app", "com.example");

    @Nested
    @DisplayName("Glossary section")
    class GlossarySection {

        @Test
        @DisplayName("should include glossary section with entries")
        void includesGlossary() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            ValueObject money = valueObject("com.example.order.domain.Money", List.of("amount", "currency"));

            ArchitecturalModel model = createModel(PROJECT, order, money);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            GlossaryBuilder glossaryBuilder = new GlossaryBuilder(model);
            List<GlossaryEntry> entries = glossaryBuilder.buildAll();

            OverviewGenerator generator = new OverviewGenerator(docModel, null, List.of(), entries, null, null);
            String result = generator.generate();

            assertThat(result).contains("## Glossaire du Domaine");
            assertThat(result).contains("**Money**");
            assertThat(result).contains("**Order**");
            assertThat(result).contains("Aggregate Root");
            assertThat(result).contains("Value Object");
        }

        @Test
        @DisplayName("should skip glossary section when no entries")
        void skipsGlossaryWhenEmpty() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            ArchitecturalModel model = createModel(PROJECT, order);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            OverviewGenerator generator = new OverviewGenerator(docModel, null, List.of(), List.of(), null, null);
            String result = generator.generate();

            assertThat(result).doesNotContain("## Glossaire du Domaine");
        }
    }

    @Nested
    @DisplayName("Structure section")
    class StructureSection {

        @Test
        @DisplayName("should include structure section with package tree")
        void includesStructure() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            Entity lineItem = entity("com.example.order.domain.LineItem");

            ArchitecturalModel model = createModel(PROJECT, order, lineItem);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            StructureBuilder structureBuilder = new StructureBuilder(model);
            String packageTree = structureBuilder.renderPackageTree();

            OverviewGenerator generator =
                    new OverviewGenerator(docModel, null, List.of(), List.of(), packageTree, null);
            String result = generator.generate();

            assertThat(result).contains("## Structure du Projet");
            assertThat(result).contains("Order.java");
            assertThat(result).contains("LineItem.java");
        }

        @Test
        @DisplayName("should skip structure section when no tree")
        void skipsStructureWhenEmpty() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            ArchitecturalModel model = createModel(PROJECT, order);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            OverviewGenerator generator = new OverviewGenerator(docModel, null, List.of(), List.of(), null, null);
            String result = generator.generate();

            assertThat(result).doesNotContain("## Structure du Projet");
        }
    }

    @Nested
    @DisplayName("Index section")
    class IndexSection {

        @Test
        @DisplayName("should include index section with type links")
        void includesIndex() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            var repo = drivenPort("com.example.order.port.OrderRepository", DrivenPortType.REPOSITORY);

            ArchitecturalModel model = createModel(PROJECT, order, repo);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            GlossaryBuilder glossaryBuilder = new GlossaryBuilder(model);
            List<GlossaryEntry> entries = glossaryBuilder.buildAll();
            IndexRenderer indexRenderer = new IndexRenderer();

            OverviewGenerator generator =
                    new OverviewGenerator(docModel, null, List.of(), entries, null, indexRenderer);
            String result = generator.generate();

            assertThat(result).contains("## Index des Types");
            assertThat(result).contains("domain.md");
            assertThat(result).contains("ports.md");
        }

        @Test
        @DisplayName("should skip index section when no renderer")
        void skipsIndexWhenNoRenderer() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            ArchitecturalModel model = createModel(PROJECT, order);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            GlossaryBuilder glossaryBuilder = new GlossaryBuilder(model);

            OverviewGenerator generator =
                    new OverviewGenerator(docModel, null, List.of(), glossaryBuilder.buildAll(), null, null);
            String result = generator.generate();

            assertThat(result).doesNotContain("## Index des Types");
        }
    }

    @Nested
    @DisplayName("Backward compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("should work with the 2-arg constructor")
        void twoArgConstructorWorks() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            ArchitecturalModel model = createModel(PROJECT, order);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            OverviewGenerator generator = new OverviewGenerator(docModel, null);
            String result = generator.generate();

            assertThat(result).contains("# Architecture Overview");
            assertThat(result).contains("## Summary");
            assertThat(result).doesNotContain("## Glossaire du Domaine");
            assertThat(result).doesNotContain("## Structure du Projet");
            assertThat(result).doesNotContain("## Index des Types");
        }

        @Test
        @DisplayName("should work with the 3-arg constructor")
        void threeArgConstructorWorks() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            ArchitecturalModel model = createModel(PROJECT, order);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            OverviewGenerator generator = new OverviewGenerator(docModel, null, List.of());
            String result = generator.generate();

            assertThat(result).contains("# Architecture Overview");
            assertThat(result).doesNotContain("## Glossaire du Domaine");
        }
    }
}
