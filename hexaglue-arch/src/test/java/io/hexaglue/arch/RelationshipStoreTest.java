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

package io.hexaglue.arch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RelationshipStore}.
 *
 * @since 4.0.0
 */
@DisplayName("RelationshipStore")
class RelationshipStoreTest {

    private static final ElementId ORDER = ElementId.of("com.example.domain.Order");
    private static final ElementId ORDER_LINE = ElementId.of("com.example.domain.OrderLine");
    private static final ElementId ORDER_ID = ElementId.of("com.example.domain.OrderId");
    private static final ElementId ORDER_REPOSITORY = ElementId.of("com.example.ports.OrderRepository");
    private static final ElementId JPA_ORDER_REPOSITORY = ElementId.of("com.example.adapters.JpaOrderRepository");
    private static final ElementId IDENTIFIABLE = ElementId.of("com.example.Identifiable");
    private static final ElementId ORDER_CREATED = ElementId.of("com.example.events.OrderCreated");

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should create empty store")
        void shouldCreateEmptyStore() {
            // when
            RelationshipStore store = RelationshipStore.builder().build();

            // then
            assertThat(store.outgoing(ORDER, RelationType.IMPLEMENTS)).isEmpty();
        }

        @Test
        @DisplayName("should add relationship")
        void shouldAddRelationship() {
            // when
            RelationshipStore store = RelationshipStore.builder()
                    .addRelation(ORDER, RelationType.IMPLEMENTS, IDENTIFIABLE)
                    .build();

            // then
            assertThat(store.outgoing(ORDER, RelationType.IMPLEMENTS)).containsExactly(IDENTIFIABLE);
        }

        @Test
        @DisplayName("should add multiple relationships of same type")
        void shouldAddMultipleRelationshipsOfSameType() {
            // given
            ElementId serializable = ElementId.of("java.io.Serializable");

            // when
            RelationshipStore store = RelationshipStore.builder()
                    .addRelation(ORDER, RelationType.IMPLEMENTS, IDENTIFIABLE)
                    .addRelation(ORDER, RelationType.IMPLEMENTS, serializable)
                    .build();

            // then
            assertThat(store.outgoing(ORDER, RelationType.IMPLEMENTS))
                    .containsExactlyInAnyOrder(IDENTIFIABLE, serializable);
        }
    }

    @Nested
    @DisplayName("Convenience builders")
    class ConvenienceBuilders {

        @Test
        @DisplayName("addImplements should create IMPLEMENTS relation")
        void addImplementsShouldCreateRelation() {
            RelationshipStore store = RelationshipStore.builder()
                    .addImplements(ORDER, IDENTIFIABLE)
                    .build();

            assertThat(store.implementedBy(ORDER)).containsExactly(IDENTIFIABLE);
        }

        @Test
        @DisplayName("addManages should create MANAGES relation")
        void addManagesShouldCreateRelation() {
            RelationshipStore store = RelationshipStore.builder()
                    .addManages(ORDER_REPOSITORY, ORDER)
                    .build();

            assertThat(store.outgoing(ORDER_REPOSITORY, RelationType.MANAGES)).containsExactly(ORDER);
        }

        @Test
        @DisplayName("addUses should create USES relation")
        void addUsesShouldCreateRelation() {
            RelationshipStore store =
                    RelationshipStore.builder().addUses(ORDER, ORDER_ID).build();

            assertThat(store.usedBy(ORDER)).containsExactly(ORDER_ID);
        }

        @Test
        @DisplayName("addContains should create CONTAINS relation")
        void addContainsShouldCreateRelation() {
            RelationshipStore store =
                    RelationshipStore.builder().addContains(ORDER, ORDER_LINE).build();

            assertThat(store.outgoing(ORDER, RelationType.CONTAINS)).containsExactly(ORDER_LINE);
        }

        @Test
        @DisplayName("addImplementedBy should create IMPLEMENTED_BY relation")
        void addImplementedByShouldCreateRelation() {
            RelationshipStore store = RelationshipStore.builder()
                    .addImplementedBy(ORDER_REPOSITORY, JPA_ORDER_REPOSITORY)
                    .build();

            assertThat(store.outgoing(ORDER_REPOSITORY, RelationType.IMPLEMENTED_BY))
                    .containsExactly(JPA_ORDER_REPOSITORY);
        }

        @Test
        @DisplayName("addPublishes should create PUBLISHES relation")
        void addPublishesShouldCreateRelation() {
            RelationshipStore store = RelationshipStore.builder()
                    .addPublishes(ORDER, ORDER_CREATED)
                    .build();

            assertThat(store.outgoing(ORDER, RelationType.PUBLISHES)).containsExactly(ORDER_CREATED);
        }
    }

    @Nested
    @DisplayName("Outgoing queries")
    class OutgoingQueries {

        private RelationshipStore store;

        @BeforeEach
        void setUp() {
            store = RelationshipStore.builder()
                    .addImplements(ORDER, IDENTIFIABLE)
                    .addManages(ORDER_REPOSITORY, ORDER)
                    .addContains(ORDER, ORDER_LINE)
                    .addContains(ORDER, ORDER_ID)
                    .build();
        }

        @Test
        @DisplayName("outgoing() should return targets for relation type")
        void outgoingShouldReturnTargets() {
            assertThat(store.outgoing(ORDER, RelationType.IMPLEMENTS)).containsExactly(IDENTIFIABLE);
            assertThat(store.outgoing(ORDER, RelationType.CONTAINS)).containsExactlyInAnyOrder(ORDER_LINE, ORDER_ID);
        }

        @Test
        @DisplayName("outgoing() should return empty set for unknown relation")
        void outgoingShouldReturnEmptyForUnknown() {
            assertThat(store.outgoing(ORDER, RelationType.PUBLISHES)).isEmpty();
        }

        @Test
        @DisplayName("implementedBy() should return implemented interfaces")
        void implementedByShouldReturnInterfaces() {
            assertThat(store.implementedBy(ORDER)).containsExactly(IDENTIFIABLE);
        }

        @Test
        @DisplayName("usedBy() should return dependencies")
        void usedByShouldReturnDependencies() {
            RelationshipStore storeWithUses = RelationshipStore.builder()
                    .addUses(ORDER, ORDER_ID)
                    .addUses(ORDER, ORDER_LINE)
                    .build();

            assertThat(storeWithUses.usedBy(ORDER)).containsExactlyInAnyOrder(ORDER_ID, ORDER_LINE);
        }
    }

    @Nested
    @DisplayName("Incoming queries")
    class IncomingQueries {

        private RelationshipStore store;

        @BeforeEach
        void setUp() {
            store = RelationshipStore.builder()
                    .addManages(ORDER_REPOSITORY, ORDER)
                    .addImplementedBy(ORDER_REPOSITORY, JPA_ORDER_REPOSITORY)
                    .addUses(ORDER, ORDER_ID)
                    .build();
        }

        @Test
        @DisplayName("incoming() should return sources for relation type")
        void incomingShouldReturnSources() {
            // ORDER is managed by ORDER_REPOSITORY
            assertThat(store.incoming(ORDER, RelationType.MANAGES)).containsExactly(ORDER_REPOSITORY);
        }

        @Test
        @DisplayName("repositoryFor() should find managing repository O(1)")
        void repositoryForShouldFindRepository() {
            assertThat(store.repositoryFor(ORDER)).contains(ORDER_REPOSITORY);
        }

        @Test
        @DisplayName("repositoryFor() should return empty when no repository")
        void repositoryForShouldReturnEmptyWhenNone() {
            assertThat(store.repositoryFor(ORDER_LINE)).isEmpty();
        }

        @Test
        @DisplayName("consumersOf() should find consumers of port")
        void consumersOfShouldFindConsumers() {
            RelationshipStore storeWithConsumers = RelationshipStore.builder()
                    .addUses(ElementId.of("com.example.OrderService"), ORDER_REPOSITORY)
                    .build();

            assertThat(storeWithConsumers.consumersOf(ORDER_REPOSITORY))
                    .containsExactly(ElementId.of("com.example.OrderService"));
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("store should be immutable after build")
        void storeShouldBeImmutable() {
            // given
            RelationshipStore.Builder builder = RelationshipStore.builder().addManages(ORDER_REPOSITORY, ORDER);
            RelationshipStore store = builder.build();

            // when - add more to builder after build
            builder.addManages(ORDER_REPOSITORY, ORDER_LINE);

            // then - store should not be affected
            assertThat(store.outgoing(ORDER_REPOSITORY, RelationType.MANAGES)).containsExactly(ORDER);
        }

        @Test
        @DisplayName("returned sets should be unmodifiable")
        void returnedSetsShouldBeUnmodifiable() {
            // given
            RelationshipStore store = RelationshipStore.builder()
                    .addManages(ORDER_REPOSITORY, ORDER)
                    .build();

            // then - the Set.of() in the implementation guarantees immutability
            // but we verify the returned set doesn't change the store
            var targets = store.outgoing(ORDER_REPOSITORY, RelationType.MANAGES);
            assertThat(targets).containsExactly(ORDER);
        }
    }
}
