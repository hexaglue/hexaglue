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
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.plugin.livingdoc.model.GlossaryEntry;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GlossaryBuilder}.
 *
 * @since 5.0.0
 */
@DisplayName("GlossaryBuilder")
class GlossaryBuilderTest {

    private static final ProjectContext PROJECT = ProjectContext.forTesting("app", "com.example");

    @Nested
    @DisplayName("buildAll")
    class BuildAll {

        @Test
        @DisplayName("should return entries sorted alphabetically by term")
        void returnsSortedEntries() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            Entity lineItem = entity("com.example.order.domain.LineItem");
            ValueObject address = valueObject("com.example.order.domain.Address", List.of("street", "city"));

            ArchitecturalModel model = createModel(PROJECT, order, lineItem, address);

            GlossaryBuilder builder = new GlossaryBuilder(model);
            List<GlossaryEntry> entries = builder.buildAll();

            assertThat(entries).hasSize(3);
            assertThat(entries).extracting(GlossaryEntry::term).containsExactly("Address", "LineItem", "Order");
        }

        @Test
        @DisplayName("should contain all types from the model")
        void containsAllTypes() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            var drivingPort = drivingPort("com.example.order.port.OrderUseCase");
            var drivenPort = drivenPort("com.example.order.port.OrderRepository", DrivenPortType.REPOSITORY);

            ArchitecturalModel model = createModel(PROJECT, order, drivingPort, drivenPort);

            GlossaryBuilder builder = new GlossaryBuilder(model);
            List<GlossaryEntry> entries = builder.buildAll();

            assertThat(entries).hasSize(3);
            assertThat(entries)
                    .extracting(GlossaryEntry::archKind)
                    .containsExactlyInAnyOrder(ArchKind.AGGREGATE_ROOT, ArchKind.DRIVING_PORT, ArchKind.DRIVEN_PORT);
        }

        @Test
        @DisplayName("should populate qualifiedName and packageName")
        void populatesQualifiedNameAndPackage() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");

            ArchitecturalModel model = createModel(PROJECT, order);

            GlossaryBuilder builder = new GlossaryBuilder(model);
            List<GlossaryEntry> entries = builder.buildAll();

            assertThat(entries).hasSize(1);
            GlossaryEntry entry = entries.get(0);
            assertThat(entry.qualifiedName()).isEqualTo("com.example.order.domain.Order");
            assertThat(entry.packageName()).isEqualTo("com.example.order.domain");
        }
    }

    @Nested
    @DisplayName("With documentation")
    class WithDocumentation {

        @Test
        @DisplayName("should use Javadoc as definition when available")
        void usesJavadocWhenAvailable() {
            Field idField = Field.builder("id", TypeRef.of("java.util.UUID"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();

            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .documentation("A customer order with line items")
                    .fields(List.of(idField))
                    .build();

            AggregateRoot order = AggregateRoot.builder(
                            TypeId.of("com.example.order.domain.Order"),
                            structure,
                            highConfidence(ElementKind.AGGREGATE_ROOT),
                            idField)
                    .build();

            ArchitecturalModel model = createModel(PROJECT, order);

            GlossaryBuilder builder = new GlossaryBuilder(model);
            List<GlossaryEntry> entries = builder.buildAll();

            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).definition()).isEqualTo("A customer order with line items");
        }
    }

    @Nested
    @DisplayName("Fallback definition")
    class FallbackDefinition {

        @Test
        @DisplayName("should generate fallback when no Javadoc is present")
        void generatesFallbackWithoutJavadoc() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");

            ArchitecturalModel model = createModel(PROJECT, order);

            GlossaryBuilder builder = new GlossaryBuilder(model);
            List<GlossaryEntry> entries = builder.buildAll();

            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).definition()).isEqualTo("Order (Aggregate Root)");
        }

        @Test
        @DisplayName("should format all ArchKind values as readable labels")
        void formatsAllKindLabels() {
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.AGGREGATE_ROOT)).isEqualTo("Aggregate Root");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.ENTITY)).isEqualTo("Entity");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.VALUE_OBJECT)).isEqualTo("Value Object");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.IDENTIFIER)).isEqualTo("Identifier");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.DOMAIN_EVENT)).isEqualTo("Domain Event");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.DOMAIN_SERVICE)).isEqualTo("Domain Service");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.DRIVING_PORT)).isEqualTo("Driving Port");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.DRIVEN_PORT)).isEqualTo("Driven Port");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.APPLICATION_SERVICE))
                    .isEqualTo("Application Service");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.COMMAND_HANDLER))
                    .isEqualTo("Command Handler");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.QUERY_HANDLER)).isEqualTo("Query Handler");
            assertThat(GlossaryBuilder.formatKindLabel(ArchKind.UNCLASSIFIED)).isEqualTo("Unclassified");
        }
    }

    @Nested
    @DisplayName("byContext")
    class ByContext {

        @Test
        @DisplayName("should group entries by bounded context")
        void groupsByContext() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            AggregateRoot product = aggregateRoot("com.example.inventory.domain.Product");
            ValueObject sku = valueObject("com.example.inventory.domain.Sku", List.of("code"));

            ArchitecturalModel model = createModel(PROJECT, order, product, sku);

            GlossaryBuilder glossaryBuilder = new GlossaryBuilder(model);
            BoundedContextDetector detector = new BoundedContextDetector(model);

            Map<String, List<GlossaryEntry>> byContext = glossaryBuilder.byContext(detector);

            assertThat(byContext).containsKeys("order", "inventory");
            assertThat(byContext.get("order")).hasSize(1);
            assertThat(byContext.get("inventory")).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Empty model")
    class EmptyModel {

        @Test
        @DisplayName("should return empty list for model with no types")
        void returnsEmptyForNoTypes() {
            ArchitecturalModel model = createModel(PROJECT);

            GlossaryBuilder builder = new GlossaryBuilder(model);
            List<GlossaryEntry> entries = builder.buildAll();

            assertThat(entries).isEmpty();
        }
    }
}
