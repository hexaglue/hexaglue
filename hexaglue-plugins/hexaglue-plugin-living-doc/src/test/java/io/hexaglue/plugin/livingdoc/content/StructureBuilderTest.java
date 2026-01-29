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

package io.hexaglue.plugin.livingdoc.content;

import static io.hexaglue.plugin.livingdoc.V5TestModelBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.ValueObject;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StructureBuilder}.
 *
 * @since 5.0.0
 */
@DisplayName("StructureBuilder")
class StructureBuilderTest {

    private static final ProjectContext PROJECT = ProjectContext.forTesting("app", "com.example");

    @Nested
    @DisplayName("Single package")
    class SinglePackage {

        @Test
        @DisplayName("should render tree for types in a single package")
        void rendersSimpleTree() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            Entity lineItem = entity("com.example.order.domain.LineItem");

            ArchitecturalModel model = createModel(PROJECT, order, lineItem);

            StructureBuilder builder = new StructureBuilder(model);
            String tree = builder.renderPackageTree();

            assertThat(tree).contains("Order.java");
            assertThat(tree).contains("LineItem.java");
            assertThat(tree).contains("# Aggregate Root");
            assertThat(tree).contains("# Entity");
        }
    }

    @Nested
    @DisplayName("Multiple packages")
    class MultiplePackages {

        @Test
        @DisplayName("should render multi-level tree with different packages")
        void rendersMultiLevelTree() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            var repo = drivenPort("com.example.order.port.OrderRepository", DrivenPortType.REPOSITORY);
            AggregateRoot product = aggregateRoot("com.example.inventory.domain.Product");

            ArchitecturalModel model = createModel(PROJECT, order, repo, product);

            StructureBuilder builder = new StructureBuilder(model);
            String tree = builder.renderPackageTree();

            assertThat(tree).contains("order");
            assertThat(tree).contains("inventory");
            assertThat(tree).contains("domain");
            assertThat(tree).contains("port");
        }
    }

    @Nested
    @DisplayName("Kind annotations")
    class KindAnnotations {

        @Test
        @DisplayName("should annotate each type with its ArchKind")
        void annotatesWithArchKind() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            ValueObject money = valueObject("com.example.order.domain.Money", List.of("amount", "currency"));
            var port = drivingPort("com.example.order.port.OrderUseCase");

            ArchitecturalModel model = createModel(PROJECT, order, money, port);

            StructureBuilder builder = new StructureBuilder(model);
            String tree = builder.renderPackageTree();

            assertThat(tree).contains("# Aggregate Root");
            assertThat(tree).contains("# Value Object");
            assertThat(tree).contains("# Driving Port");
        }
    }

    @Nested
    @DisplayName("Empty model")
    class EmptyModel {

        @Test
        @DisplayName("should return empty string for model with no types")
        void returnsEmptyForNoTypes() {
            ArchitecturalModel model = createModel(PROJECT);

            StructureBuilder builder = new StructureBuilder(model);
            String tree = builder.renderPackageTree();

            assertThat(tree).isEmpty();
        }
    }
}
