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

package io.hexaglue.arch.model.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.graph.RelationshipGraph.Relationship;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RelationshipGraph}.
 *
 * @since 5.0.0
 */
@DisplayName("RelationshipGraph")
class RelationshipGraphTest {

    private static final TypeId ORDER = TypeId.of("com.example.Order");
    private static final TypeId ORDER_LINE = TypeId.of("com.example.OrderLine");
    private static final TypeId ORDER_ID = TypeId.of("com.example.OrderId");
    private static final TypeId ORDER_CREATED = TypeId.of("com.example.OrderCreatedEvent");
    private static final TypeId ORDER_REPOSITORY = TypeId.of("com.example.OrderRepository");
    private static final TypeId CUSTOMER = TypeId.of("com.example.Customer");

    private RelationshipGraph graph;

    @BeforeEach
    void setUp() {
        graph = RelationshipGraph.builder()
                .add(ORDER, ORDER_LINE, RelationType.CONTAINS)
                .add(ORDER, ORDER_ID, RelationType.OWNS)
                .add(ORDER, ORDER_CREATED, RelationType.EMITS)
                .add(ORDER, CUSTOMER, RelationType.REFERENCES)
                .add(ORDER_REPOSITORY, ORDER, RelationType.PERSISTS)
                .build();
    }

    @Nested
    @DisplayName("Relationship Record")
    class RelationshipRecordTests {

        @Test
        @DisplayName("should create relationship with all fields")
        void shouldCreateRelationshipWithAllFields() {
            var rel = new Relationship(ORDER, ORDER_LINE, RelationType.CONTAINS);

            assertThat(rel.source()).isEqualTo(ORDER);
            assertThat(rel.target()).isEqualTo(ORDER_LINE);
            assertThat(rel.type()).isEqualTo(RelationType.CONTAINS);
        }

        @Test
        @DisplayName("should create relationship via factory method")
        void shouldCreateViaFactoryMethod() {
            var rel = Relationship.of(ORDER, ORDER_LINE, RelationType.CONTAINS);

            assertThat(rel.source()).isEqualTo(ORDER);
            assertThat(rel.target()).isEqualTo(ORDER_LINE);
        }

        @Test
        @DisplayName("should reject null source")
        void shouldRejectNullSource() {
            assertThatThrownBy(() -> new Relationship(null, ORDER_LINE, RelationType.CONTAINS))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("source must not be null");
        }

        @Test
        @DisplayName("should reject null target")
        void shouldRejectNullTarget() {
            assertThatThrownBy(() -> new Relationship(ORDER, null, RelationType.CONTAINS))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("target must not be null");
        }

        @Test
        @DisplayName("should reject null type")
        void shouldRejectNullType() {
            assertThatThrownBy(() -> new Relationship(ORDER, ORDER_LINE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("type must not be null");
        }
    }

    @Nested
    @DisplayName("Navigation from Source")
    class NavigationFromSource {

        @Test
        @DisplayName("should find all relationships from source")
        void shouldFindAllRelationshipsFromSource() {
            var relationships = graph.from(ORDER).toList();

            assertThat(relationships).hasSize(4);
            assertThat(relationships)
                    .extracting(Relationship::target)
                    .containsExactlyInAnyOrder(ORDER_LINE, ORDER_ID, ORDER_CREATED, CUSTOMER);
        }

        @Test
        @DisplayName("should return empty stream for unknown source")
        void shouldReturnEmptyStreamForUnknownSource() {
            var unknown = TypeId.of("com.example.Unknown");
            assertThat(graph.from(unknown).toList()).isEmpty();
        }

        @Test
        @DisplayName("should reject null source")
        void shouldRejectNullSource() {
            assertThatThrownBy(() -> graph.from(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Navigation to Target")
    class NavigationToTarget {

        @Test
        @DisplayName("should find all relationships to target")
        void shouldFindAllRelationshipsToTarget() {
            var relationships = graph.to(ORDER).toList();

            assertThat(relationships).hasSize(1);
            assertThat(relationships.get(0).source()).isEqualTo(ORDER_REPOSITORY);
            assertThat(relationships.get(0).type()).isEqualTo(RelationType.PERSISTS);
        }

        @Test
        @DisplayName("should return empty stream for unknown target")
        void shouldReturnEmptyStreamForUnknownTarget() {
            var unknown = TypeId.of("com.example.Unknown");
            assertThat(graph.to(unknown).toList()).isEmpty();
        }

        @Test
        @DisplayName("should reject null target")
        void shouldRejectNullTarget() {
            assertThatThrownBy(() -> graph.to(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("All Relationships")
    class AllRelationships {

        @Test
        @DisplayName("should return all relationships")
        void shouldReturnAllRelationships() {
            assertThat(graph.all().toList()).hasSize(5);
        }

        @Test
        @DisplayName("should filter by type")
        void shouldFilterByType() {
            var containsRels = graph.ofType(RelationType.CONTAINS).toList();
            assertThat(containsRels).hasSize(1);
            assertThat(containsRels.get(0).target()).isEqualTo(ORDER_LINE);
        }

        @Test
        @DisplayName("should return empty for unused type")
        void shouldReturnEmptyForUnusedType() {
            assertThat(graph.ofType(RelationType.EXTENDS).toList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Relationship Checks")
    class RelationshipChecks {

        @Test
        @DisplayName("hasRelation should return true for existing relationship")
        void hasRelationShouldReturnTrueForExisting() {
            assertThat(graph.hasRelation(ORDER, ORDER_LINE, RelationType.CONTAINS))
                    .isTrue();
        }

        @Test
        @DisplayName("hasRelation should return false for non-existing relationship")
        void hasRelationShouldReturnFalseForNonExisting() {
            assertThat(graph.hasRelation(ORDER, ORDER_LINE, RelationType.EMITS)).isFalse();
            assertThat(graph.hasRelation(ORDER_LINE, ORDER, RelationType.CONTAINS))
                    .isFalse();
        }

        @Test
        @DisplayName("hasAnyRelation should return true when any relationship exists")
        void hasAnyRelationShouldReturnTrueWhenAnyExists() {
            assertThat(graph.hasAnyRelation(ORDER, ORDER_LINE)).isTrue();
            assertThat(graph.hasAnyRelation(ORDER, CUSTOMER)).isTrue();
        }

        @Test
        @DisplayName("hasAnyRelation should return false when no relationship exists")
        void hasAnyRelationShouldReturnFalseWhenNoneExists() {
            assertThat(graph.hasAnyRelation(ORDER_LINE, ORDER)).isFalse();
        }
    }

    @Nested
    @DisplayName("Related Types")
    class RelatedTypes {

        @Test
        @DisplayName("relatedTo should return all targets")
        void relatedToShouldReturnAllTargets() {
            Set<TypeId> related = graph.relatedTo(ORDER);
            assertThat(related).containsExactlyInAnyOrder(ORDER_LINE, ORDER_ID, ORDER_CREATED, CUSTOMER);
        }

        @Test
        @DisplayName("relatedFrom should return all sources")
        void relatedFromShouldReturnAllSources() {
            Set<TypeId> related = graph.relatedFrom(ORDER);
            assertThat(related).containsExactly(ORDER_REPOSITORY);
        }

        @Test
        @DisplayName("relatedTo should return empty for isolated type")
        void relatedToShouldReturnEmptyForIsolatedType() {
            assertThat(graph.relatedTo(ORDER_LINE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Graph Metadata")
    class GraphMetadata {

        @Test
        @DisplayName("should return correct size")
        void shouldReturnCorrectSize() {
            assertThat(graph.size()).isEqualTo(5);
        }

        @Test
        @DisplayName("should report not empty")
        void shouldReportNotEmpty() {
            assertThat(graph.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("should return all types")
        void shouldReturnAllTypes() {
            assertThat(graph.allTypes())
                    .containsExactlyInAnyOrder(ORDER, ORDER_LINE, ORDER_ID, ORDER_CREATED, CUSTOMER, ORDER_REPOSITORY);
        }
    }

    @Nested
    @DisplayName("Empty Graph")
    class EmptyGraph {

        @Test
        @DisplayName("should create empty graph")
        void shouldCreateEmptyGraph() {
            var empty = RelationshipGraph.empty();

            assertThat(empty.isEmpty()).isTrue();
            assertThat(empty.size()).isZero();
            assertThat(empty.allTypes()).isEmpty();
        }

        @Test
        @DisplayName("empty builder should create empty graph")
        void emptyBuilderShouldCreateEmptyGraph() {
            var empty = RelationshipGraph.builder().build();
            assertThat(empty.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should add relationship via add(source, target, type)")
        void shouldAddViaTripleArgs() {
            var g = RelationshipGraph.builder()
                    .add(ORDER, ORDER_LINE, RelationType.CONTAINS)
                    .build();

            assertThat(g.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should add relationship object directly")
        void shouldAddRelationshipObject() {
            var rel = Relationship.of(ORDER, ORDER_LINE, RelationType.CONTAINS);
            var g = RelationshipGraph.builder().add(rel).build();

            assertThat(g.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should add all from another graph")
        void shouldAddAllFromAnotherGraph() {
            var g1 = RelationshipGraph.builder()
                    .add(ORDER, ORDER_LINE, RelationType.CONTAINS)
                    .build();

            var g2 = RelationshipGraph.builder()
                    .add(ORDER, ORDER_ID, RelationType.OWNS)
                    .addAll(g1)
                    .build();

            assertThat(g2.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should reject null relationship")
        void shouldRejectNullRelationship() {
            assertThatThrownBy(() -> RelationshipGraph.builder().add(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject null graph in addAll")
        void shouldRejectNullGraphInAddAll() {
            assertThatThrownBy(() -> RelationshipGraph.builder().addAll(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("graph should be immutable after creation")
        void graphShouldBeImmutableAfterCreation() {
            var builder = RelationshipGraph.builder().add(ORDER, ORDER_LINE, RelationType.CONTAINS);

            var g = builder.build();

            // Modify builder after build
            builder.add(ORDER, ORDER_ID, RelationType.OWNS);

            // Original graph should be unchanged
            assertThat(g.size()).isEqualTo(1);
        }
    }
}
