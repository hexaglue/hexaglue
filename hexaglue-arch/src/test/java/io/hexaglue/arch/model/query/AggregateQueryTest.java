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

package io.hexaglue.arch.model.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateQuery}.
 *
 * @since 5.0.0
 */
@DisplayName("AggregateQuery")
class AggregateQueryTest {

    private static final TypeId ORDER_ID = TypeId.of("com.example.order.Order");
    private static final TypeId CUSTOMER_ID = TypeId.of("com.example.customer.Customer");
    private static final TypeId PRODUCT_ID = TypeId.of("com.other.product.Product");

    private DomainIndex domainIndex;
    private PortIndex portIndex;
    private AggregateQuery query;

    @BeforeEach
    void setUp() {
        TypeStructure classStructure = TypeStructure.builder(TypeNature.CLASS).build();

        ClassificationTrace aggTrace = ClassificationTrace.highConfidence(ElementKind.AGGREGATE_ROOT, "test", "Test");

        Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

        // Order with entities and events
        AggregateRoot order = AggregateRoot.builder(ORDER_ID, classStructure, aggTrace, idField)
                .entities(List.of(TypeRef.of("com.example.order.OrderLine")))
                .domainEvents(List.of(TypeRef.of("com.example.order.OrderCreated")))
                .build();

        // Customer without entities or events
        AggregateRoot customer = AggregateRoot.builder(CUSTOMER_ID, classStructure, aggTrace, idField)
                .build();

        // Product in different package
        AggregateRoot product = AggregateRoot.builder(PRODUCT_ID, classStructure, aggTrace, idField)
                .build();

        TypeRegistry registry =
                TypeRegistry.builder().add(order).add(customer).add(product).build();

        domainIndex = DomainIndex.from(registry);
        portIndex = PortIndex.from(registry);
        query = AggregateQuery.of(domainIndex, portIndex);
    }

    @Nested
    @DisplayName("Basic Query Operations")
    class BasicQueryOperations {

        @Test
        @DisplayName("toList() should return all aggregates")
        void toListShouldReturnAllAggregates() {
            // when
            List<AggregateRoot> result = query.toList();

            // then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("count() should return aggregate count")
        void countShouldReturnAggregateCount() {
            // when
            long count = query.count();

            // then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("exists() should return true when aggregates exist")
        void existsShouldReturnTrueWhenAggregatesExist() {
            // then
            assertThat(query.exists()).isTrue();
        }

        @Test
        @DisplayName("isEmpty() should return false when aggregates exist")
        void isEmptyShouldReturnFalseWhenAggregatesExist() {
            // then
            assertThat(query.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Filters")
    class Filters {

        @Test
        @DisplayName("withEvents() should filter aggregates with domain events")
        void withEventsShouldFilterAggregatesWithEvents() {
            // when
            List<AggregateRoot> result = query.withEvents().toList();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("withEntities() should filter aggregates with entities")
        void withEntitiesShouldFilterAggregatesWithEntities() {
            // when
            List<AggregateRoot> result = query.withEntities().toList();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("inPackage() should filter by exact package match")
        void inPackageShouldFilterByExactPackageMatch() {
            // when
            List<AggregateRoot> result = query.inPackage("com.example.order").toList();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("inPackageTree() should filter by package prefix")
        void inPackageTreeShouldFilterByPackagePrefix() {
            // when
            List<AggregateRoot> result = query.inPackageTree("com.example").toList();

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("nameMatches() should filter by regex pattern")
        void nameMatchesShouldFilterByRegexPattern() {
            // when
            List<AggregateRoot> result = query.nameMatches(".*er").toList();

            // then
            assertThat(result).hasSize(2); // Order, Customer
        }

        @Test
        @DisplayName("where() should apply custom predicate")
        void whereShouldApplyCustomPredicate() {
            // when
            List<AggregateRoot> result =
                    query.where(agg -> agg.simpleName().startsWith("O")).toList();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("Chained Filters")
    class ChainedFilters {

        @Test
        @DisplayName("multiple filters should be combined with AND")
        void multipleFiltersShouldBeCombinedWithAnd() {
            // when
            List<AggregateRoot> result =
                    query.withEvents().inPackageTree("com.example").toList();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("filters should be immutable")
        void filtersShouldBeImmutable() {
            // given
            AggregateQuery original = query;
            AggregateQuery filtered = query.withEvents();

            // then - original should be unchanged
            assertThat(original.toList()).hasSize(3);
            assertThat(filtered.toList()).hasSize(1);
        }
    }
}
