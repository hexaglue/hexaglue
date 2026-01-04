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

package io.hexaglue.spi.ir;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DomainRelation}.
 */
class DomainRelationTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodsTest {

        @Test
        @DisplayName("embedded should create EMBEDDED relation for value object")
        void embeddedCreatesEmbeddedRelation() {
            DomainRelation relation = DomainRelation.embedded("address", "com.example.Address");

            assertThat(relation.propertyName()).isEqualTo("address");
            assertThat(relation.kind()).isEqualTo(RelationKind.EMBEDDED);
            assertThat(relation.targetTypeFqn()).isEqualTo("com.example.Address");
            assertThat(relation.targetKind()).isEqualTo(DomainKind.VALUE_OBJECT);
            assertThat(relation.mappedBy()).isNull();
            assertThat(relation.cascade()).isEqualTo(CascadeType.NONE);
            assertThat(relation.fetch()).isEqualTo(FetchType.EAGER);
            assertThat(relation.orphanRemoval()).isFalse();
        }

        @Test
        @DisplayName("oneToMany should create ONE_TO_MANY relation with cascade ALL")
        void oneToManyCreatesOneToManyRelation() {
            DomainRelation relation = DomainRelation.oneToMany("items", "com.example.LineItem", DomainKind.ENTITY);

            assertThat(relation.propertyName()).isEqualTo("items");
            assertThat(relation.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
            assertThat(relation.targetTypeFqn()).isEqualTo("com.example.LineItem");
            assertThat(relation.targetKind()).isEqualTo(DomainKind.ENTITY);
            assertThat(relation.cascade()).isEqualTo(CascadeType.ALL);
            assertThat(relation.fetch()).isEqualTo(FetchType.LAZY);
            assertThat(relation.orphanRemoval()).isTrue();
        }

        @Test
        @DisplayName("manyToOne should create MANY_TO_ONE relation with no cascade")
        void manyToOneCreatesManyToOneRelation() {
            DomainRelation relation = DomainRelation.manyToOne("order", "com.example.Order");

            assertThat(relation.propertyName()).isEqualTo("order");
            assertThat(relation.kind()).isEqualTo(RelationKind.MANY_TO_ONE);
            assertThat(relation.targetTypeFqn()).isEqualTo("com.example.Order");
            assertThat(relation.targetKind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
            assertThat(relation.cascade()).isEqualTo(CascadeType.NONE);
            assertThat(relation.fetch()).isEqualTo(FetchType.LAZY);
            assertThat(relation.orphanRemoval()).isFalse();
        }
    }

    @Nested
    @DisplayName("Helper methods")
    class HelperMethodsTest {

        @Test
        @DisplayName("mappedByOpt should return empty for owning side")
        void mappedByOptEmptyForOwningSide() {
            DomainRelation relation = DomainRelation.oneToMany("items", "com.example.Item", DomainKind.ENTITY);

            assertThat(relation.mappedByOpt()).isEmpty();
            assertThat(relation.isOwning()).isTrue();
        }

        @Test
        @DisplayName("mappedByOpt should return value for inverse side")
        void mappedByOptForInverseSide() {
            DomainRelation relation = new DomainRelation(
                    "items",
                    RelationKind.ONE_TO_MANY,
                    "com.example.Item",
                    DomainKind.ENTITY,
                    "order", // mappedBy
                    CascadeType.ALL,
                    FetchType.LAZY,
                    true);

            assertThat(relation.mappedByOpt()).isPresent().contains("order");
            assertThat(relation.isOwning()).isFalse();
            assertThat(relation.isBidirectional()).isTrue();
        }

        @Test
        @DisplayName("targetsEntity should return true for ENTITY and AGGREGATE_ROOT")
        void targetsEntityForEntityKinds() {
            DomainRelation entityRelation = DomainRelation.oneToMany("items", "com.example.Item", DomainKind.ENTITY);
            DomainRelation aggregateRelation = DomainRelation.manyToOne("order", "com.example.Order");

            assertThat(entityRelation.targetsEntity()).isTrue();
            assertThat(aggregateRelation.targetsEntity()).isTrue();
        }

        @Test
        @DisplayName("targetsValueObject should return true for VALUE_OBJECT")
        void targetsValueObjectForValueObjectKind() {
            DomainRelation relation = DomainRelation.embedded("address", "com.example.Address");

            assertThat(relation.targetsValueObject()).isTrue();
            assertThat(relation.targetsEntity()).isFalse();
        }

        @Test
        @DisplayName("targetSimpleName should extract simple name from FQN")
        void targetSimpleNameExtractsSimpleName() {
            DomainRelation relation =
                    DomainRelation.oneToMany("items", "com.example.domain.order.LineItem", DomainKind.ENTITY);

            assertThat(relation.targetSimpleName()).isEqualTo("LineItem");
        }

        @Test
        @DisplayName("targetSimpleName should handle simple names without package")
        void targetSimpleNameHandlesNoPackage() {
            DomainRelation relation = DomainRelation.embedded("value", "Money");

            assertThat(relation.targetSimpleName()).isEqualTo("Money");
        }
    }
}
