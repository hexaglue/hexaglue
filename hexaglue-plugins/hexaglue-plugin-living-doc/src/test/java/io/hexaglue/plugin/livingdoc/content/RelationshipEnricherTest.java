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
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.graph.RelationType;
import io.hexaglue.arch.model.graph.RelationshipGraph;
import io.hexaglue.plugin.livingdoc.model.ArchitecturalDependency;
import io.hexaglue.plugin.livingdoc.model.ArchitecturalDependency.Direction;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RelationshipEnricher.
 *
 * @since 5.0.0
 */
@DisplayName("RelationshipEnricher")
class RelationshipEnricherTest {

    private static final String PKG = "com.example.domain";

    @Nested
    @DisplayName("With graph available")
    class WithGraphAvailable {

        @Test
        @DisplayName("should return outgoing dependencies")
        void returnsOutgoingDependencies() {
            TypeId orderId = TypeId.of(PKG + ".Order");
            TypeId lineItemId = TypeId.of(PKG + ".LineItem");

            RelationshipGraph graph = RelationshipGraph.builder()
                    .add(orderId, lineItemId, RelationType.CONTAINS)
                    .build();

            AggregateRoot order = aggregateRoot(PKG + ".Order");
            Entity lineItem = entity(PKG + ".LineItem");

            ArchitecturalModel model =
                    createModelWithGraph(ProjectContext.forTesting("app", PKG), graph, order, lineItem);

            RelationshipEnricher enricher = new RelationshipEnricher(model.compositionIndex());

            List<ArchitecturalDependency> outgoing = enricher.outgoingFrom(orderId);

            assertThat(outgoing).hasSize(1);
            assertThat(outgoing.get(0).targetSimpleName()).isEqualTo("LineItem");
            assertThat(outgoing.get(0).relationType()).isEqualTo(RelationType.CONTAINS);
            assertThat(outgoing.get(0).direction()).isEqualTo(Direction.OUTGOING);
        }

        @Test
        @DisplayName("should return incoming dependencies")
        void returnsIncomingDependencies() {
            TypeId orderId = TypeId.of(PKG + ".Order");
            TypeId lineItemId = TypeId.of(PKG + ".LineItem");

            RelationshipGraph graph = RelationshipGraph.builder()
                    .add(orderId, lineItemId, RelationType.CONTAINS)
                    .build();

            AggregateRoot order = aggregateRoot(PKG + ".Order");
            Entity lineItem = entity(PKG + ".LineItem");

            ArchitecturalModel model =
                    createModelWithGraph(ProjectContext.forTesting("app", PKG), graph, order, lineItem);

            RelationshipEnricher enricher = new RelationshipEnricher(model.compositionIndex());

            List<ArchitecturalDependency> incoming = enricher.incomingTo(lineItemId);

            assertThat(incoming).hasSize(1);
            assertThat(incoming.get(0).targetSimpleName()).isEqualTo("Order");
            assertThat(incoming.get(0).relationType()).isEqualTo(RelationType.CONTAINS);
            assertThat(incoming.get(0).direction()).isEqualTo(Direction.INCOMING);
        }

        @Test
        @DisplayName("should handle multiple relationship types")
        void handlesMultipleRelationTypes() {
            TypeId orderId = TypeId.of(PKG + ".Order");
            TypeId lineItemId = TypeId.of(PKG + ".LineItem");
            TypeId eventId = TypeId.of(PKG + ".OrderPlaced");

            RelationshipGraph graph = RelationshipGraph.builder()
                    .add(orderId, lineItemId, RelationType.CONTAINS)
                    .add(orderId, eventId, RelationType.EMITS)
                    .build();

            AggregateRoot order = aggregateRoot(PKG + ".Order");
            Entity lineItem = entity(PKG + ".LineItem");
            var event = domainEvent(PKG + ".OrderPlaced");

            ArchitecturalModel model =
                    createModelWithGraph(ProjectContext.forTesting("app", PKG), graph, order, lineItem, event);

            RelationshipEnricher enricher = new RelationshipEnricher(model.compositionIndex());

            List<ArchitecturalDependency> outgoing = enricher.outgoingFrom(orderId);

            assertThat(outgoing).hasSize(2);
            assertThat(outgoing)
                    .extracting(ArchitecturalDependency::relationType)
                    .containsExactlyInAnyOrder(RelationType.CONTAINS, RelationType.EMITS);
        }

        @Test
        @DisplayName("should return empty for type with no relationships")
        void returnsEmptyForNoRelationships() {
            TypeId orderId = TypeId.of(PKG + ".Order");
            TypeId lineItemId = TypeId.of(PKG + ".LineItem");

            RelationshipGraph graph = RelationshipGraph.builder()
                    .add(orderId, lineItemId, RelationType.CONTAINS)
                    .build();

            AggregateRoot order = aggregateRoot(PKG + ".Order");
            Entity lineItem = entity(PKG + ".LineItem");

            ArchitecturalModel model =
                    createModelWithGraph(ProjectContext.forTesting("app", PKG), graph, order, lineItem);

            RelationshipEnricher enricher = new RelationshipEnricher(model.compositionIndex());

            // LineItem has no outgoing relationships
            TypeId unknownId = TypeId.of(PKG + ".Unknown");
            assertThat(enricher.outgoingFrom(unknownId)).isEmpty();
            assertThat(enricher.incomingTo(unknownId)).isEmpty();
        }

        @Test
        @DisplayName("should report availability as true")
        void reportsAvailable() {
            RelationshipGraph graph = RelationshipGraph.empty();

            AggregateRoot order = aggregateRoot(PKG + ".Order");

            ArchitecturalModel model = createModelWithGraph(ProjectContext.forTesting("app", PKG), graph, order);

            RelationshipEnricher enricher = new RelationshipEnricher(model.compositionIndex());

            assertThat(enricher.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Without graph (graceful degradation)")
    class WithoutGraph {

        @Test
        @DisplayName("should return empty lists when composition index is absent")
        void returnsEmptyWhenNoGraph() {
            RelationshipEnricher enricher = new RelationshipEnricher(Optional.empty());

            TypeId orderId = TypeId.of(PKG + ".Order");

            assertThat(enricher.outgoingFrom(orderId)).isEmpty();
            assertThat(enricher.incomingTo(orderId)).isEmpty();
        }

        @Test
        @DisplayName("should report availability as false")
        void reportsUnavailable() {
            RelationshipEnricher enricher = new RelationshipEnricher(Optional.empty());

            assertThat(enricher.isAvailable()).isFalse();
        }
    }
}
