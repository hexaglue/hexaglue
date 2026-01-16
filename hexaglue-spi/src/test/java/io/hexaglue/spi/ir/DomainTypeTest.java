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

import io.hexaglue.arch.ElementKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DomainType}.
 */
class DomainTypeTest {

    @Nested
    @DisplayName("Backward compatible constructor")
    class BackwardCompatibleConstructorTest {

        @Test
        @DisplayName("should default relations to empty list")
        void defaultsRelationsToEmptyList() {
            DomainType type = new DomainType(
                    "com.example.Order",
                    "Order",
                    "com.example",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.empty(),
                    List.of(),
                    List.of(),
                    SourceRef.unknown());

            assertThat(type.relations()).isEmpty();
            assertThat(type.hasRelations()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasRelations()")
    class HasRelationsTest {

        @Test
        @DisplayName("should return true when relations list is not empty")
        void returnsTrueWhenRelationsPresent() {
            DomainRelation relation = DomainRelation.oneToMany("items", "com.example.LineItem", ElementKind.ENTITY);

            DomainType type = new DomainType(
                    "com.example.Order",
                    "Order",
                    "com.example",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.empty(),
                    List.of(),
                    List.of(relation),
                    List.of(),
                    SourceRef.unknown());

            assertThat(type.hasRelations()).isTrue();
        }

        @Test
        @DisplayName("should return false when relations list is empty")
        void returnsFalseWhenNoRelations() {
            DomainType type = new DomainType(
                    "com.example.Money",
                    "Money",
                    "com.example",
                    ElementKind.VALUE_OBJECT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.RECORD,
                    Optional.empty(),
                    List.of(),
                    List.of(),
                    List.of(),
                    SourceRef.unknown());

            assertThat(type.hasRelations()).isFalse();
        }
    }

    @Nested
    @DisplayName("relationsOfKind()")
    class RelationsOfKindTest {

        @Test
        @DisplayName("should filter relations by kind")
        void filtersRelationsByKind() {
            DomainRelation oneToMany = DomainRelation.oneToMany("items", "com.example.LineItem", ElementKind.ENTITY);
            DomainRelation embedded = DomainRelation.embedded("address", "com.example.Address");
            DomainRelation manyToOne = DomainRelation.manyToOne("customer", "com.example.Customer");

            DomainType type = new DomainType(
                    "com.example.Order",
                    "Order",
                    "com.example",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.empty(),
                    List.of(),
                    List.of(oneToMany, embedded, manyToOne),
                    List.of(),
                    SourceRef.unknown());

            assertThat(type.relationsOfKind(RelationKind.ONE_TO_MANY)).containsExactly(oneToMany);
            assertThat(type.relationsOfKind(RelationKind.EMBEDDED)).containsExactly(embedded);
            assertThat(type.relationsOfKind(RelationKind.MANY_TO_ONE)).containsExactly(manyToOne);
            assertThat(type.relationsOfKind(RelationKind.MANY_TO_MANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("embeddedRelations()")
    class EmbeddedRelationsTest {

        @Test
        @DisplayName("should return only EMBEDDED and ELEMENT_COLLECTION relations")
        void returnsOnlyEmbeddedAndElementCollection() {
            DomainRelation embedded = DomainRelation.embedded("address", "com.example.Address");
            DomainRelation elementCollection = new DomainRelation(
                    "tags",
                    RelationKind.ELEMENT_COLLECTION,
                    "com.example.Tag",
                    ElementKind.VALUE_OBJECT,
                    null,
                    CascadeType.NONE,
                    FetchType.LAZY,
                    false);
            DomainRelation oneToMany = DomainRelation.oneToMany("items", "com.example.LineItem", ElementKind.ENTITY);

            DomainType type = new DomainType(
                    "com.example.Order",
                    "Order",
                    "com.example",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.empty(),
                    List.of(),
                    List.of(embedded, elementCollection, oneToMany),
                    List.of(),
                    SourceRef.unknown());

            assertThat(type.embeddedRelations()).containsExactlyInAnyOrder(embedded, elementCollection);
        }
    }

    @Nested
    @DisplayName("entityRelations()")
    class EntityRelationsTest {

        @Test
        @DisplayName("should return only relations targeting entities")
        void returnsOnlyEntityTargetingRelations() {
            DomainRelation oneToMany = DomainRelation.oneToMany("items", "com.example.LineItem", ElementKind.ENTITY);
            DomainRelation manyToOne = DomainRelation.manyToOne("customer", "com.example.Customer");
            DomainRelation embedded = DomainRelation.embedded("address", "com.example.Address");

            DomainType type = new DomainType(
                    "com.example.Order",
                    "Order",
                    "com.example",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.empty(),
                    List.of(),
                    List.of(oneToMany, manyToOne, embedded),
                    List.of(),
                    SourceRef.unknown());

            assertThat(type.entityRelations()).containsExactlyInAnyOrder(oneToMany, manyToOne);
        }
    }

    @Nested
    @DisplayName("Type classification helpers")
    class TypeClassificationHelpersTest {

        @Test
        @DisplayName("isAggregateRoot should return true for AGGREGATE_ROOT kind")
        void isAggregateRootReturnsTrue() {
            DomainType type = createType(ElementKind.AGGREGATE_ROOT);
            assertThat(type.isAggregateRoot()).isTrue();
            assertThat(type.isEntity()).isTrue(); // Aggregate roots are also entities
        }

        @Test
        @DisplayName("isEntity should return true for ENTITY kind")
        void isEntityReturnsTrue() {
            DomainType type = createType(ElementKind.ENTITY);
            assertThat(type.isEntity()).isTrue();
            assertThat(type.isAggregateRoot()).isFalse();
        }

        @Test
        @DisplayName("isValueObject should return true for VALUE_OBJECT kind")
        void isValueObjectReturnsTrue() {
            DomainType type = createType(ElementKind.VALUE_OBJECT);
            assertThat(type.isValueObject()).isTrue();
            assertThat(type.isEntity()).isFalse();
        }

        @Test
        @DisplayName("isRecord should return true for RECORD construct")
        void isRecordReturnsTrue() {
            DomainType type = new DomainType(
                    "com.example.Money",
                    "Money",
                    "com.example",
                    ElementKind.VALUE_OBJECT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.RECORD,
                    Optional.empty(),
                    List.of(),
                    List.of(),
                    SourceRef.unknown());

            assertThat(type.isRecord()).isTrue();
        }

        private DomainType createType(ElementKind kind) {
            return new DomainType(
                    "com.example.Test",
                    "Test",
                    "com.example",
                    kind,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.empty(),
                    List.of(),
                    List.of(),
                    SourceRef.unknown());
        }
    }
}
