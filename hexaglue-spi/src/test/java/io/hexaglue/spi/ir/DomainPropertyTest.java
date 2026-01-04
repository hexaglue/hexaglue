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
 * Unit tests for {@link DomainProperty}.
 */
class DomainPropertyTest {

    @Nested
    @DisplayName("Backward compatible constructor")
    class BackwardCompatibleConstructorTest {

        @Test
        @DisplayName("should default isEmbedded to false")
        void defaultsIsEmbeddedToFalse() {
            DomainProperty property = new DomainProperty(
                    "name", TypeRef.of("java.lang.String"), Cardinality.SINGLE, Nullability.UNKNOWN, false);

            assertThat(property.isEmbedded()).isFalse();
        }

        @Test
        @DisplayName("should default relationInfo to null")
        void defaultsRelationInfoToNull() {
            DomainProperty property = new DomainProperty(
                    "name", TypeRef.of("java.lang.String"), Cardinality.SINGLE, Nullability.UNKNOWN, false);

            assertThat(property.relationInfo()).isNull();
            assertThat(property.relationInfoOpt()).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasRelation()")
    class HasRelationTest {

        @Test
        @DisplayName("should return true when relationInfo is set")
        void returnsTrueWhenRelationInfoSet() {
            RelationInfo relationInfo = RelationInfo.unidirectional(RelationKind.ONE_TO_MANY, "com.example.Item");
            DomainProperty property = new DomainProperty(
                    "items",
                    TypeRef.parameterized("java.util.List", TypeRef.of("com.example.Item")),
                    Cardinality.COLLECTION,
                    Nullability.NON_NULL,
                    false,
                    false,
                    relationInfo);

            assertThat(property.hasRelation()).isTrue();
        }

        @Test
        @DisplayName("should return false when relationInfo is null")
        void returnsFalseWhenRelationInfoNull() {
            DomainProperty property = new DomainProperty(
                    "name", TypeRef.of("java.lang.String"), Cardinality.SINGLE, Nullability.UNKNOWN, false);

            assertThat(property.hasRelation()).isFalse();
        }
    }

    @Nested
    @DisplayName("isSimple()")
    class IsSimpleTest {

        @Test
        @DisplayName("should return true for simple properties")
        void returnsTrueForSimpleProperties() {
            DomainProperty property = new DomainProperty(
                    "name", TypeRef.of("java.lang.String"), Cardinality.SINGLE, Nullability.UNKNOWN, false);

            assertThat(property.isSimple()).isTrue();
        }

        @Test
        @DisplayName("should return false for identity properties")
        void returnsFalseForIdentityProperties() {
            DomainProperty property = new DomainProperty(
                    "id", TypeRef.of("java.util.UUID"), Cardinality.SINGLE, Nullability.NON_NULL, true);

            assertThat(property.isSimple()).isFalse();
        }

        @Test
        @DisplayName("should return false for embedded properties")
        void returnsFalseForEmbeddedProperties() {
            DomainProperty property = new DomainProperty(
                    "address",
                    TypeRef.of("com.example.Address"),
                    Cardinality.SINGLE,
                    Nullability.UNKNOWN,
                    false,
                    true, // isEmbedded
                    null);

            assertThat(property.isSimple()).isFalse();
        }

        @Test
        @DisplayName("should return false for properties with relations")
        void returnsFalseForPropertiesWithRelations() {
            RelationInfo relationInfo = RelationInfo.unidirectional(RelationKind.MANY_TO_ONE, "com.example.Order");
            DomainProperty property = new DomainProperty(
                    "order",
                    TypeRef.of("com.example.Order"),
                    Cardinality.SINGLE,
                    Nullability.UNKNOWN,
                    false,
                    false,
                    relationInfo);

            assertThat(property.isSimple()).isFalse();
        }
    }

    @Nested
    @DisplayName("isEntityCollection()")
    class IsEntityCollectionTest {

        @Test
        @DisplayName("should return true for ONE_TO_MANY non-embedded collection")
        void returnsTrueForOneToManyNonEmbedded() {
            RelationInfo relationInfo = RelationInfo.unidirectional(RelationKind.ONE_TO_MANY, "com.example.LineItem");
            DomainProperty property = new DomainProperty(
                    "items",
                    TypeRef.parameterized("java.util.List", TypeRef.of("com.example.LineItem")),
                    Cardinality.COLLECTION,
                    Nullability.NON_NULL,
                    false,
                    false,
                    relationInfo);

            assertThat(property.isEntityCollection()).isTrue();
        }

        @Test
        @DisplayName("should return false for ELEMENT_COLLECTION")
        void returnsFalseForElementCollection() {
            RelationInfo relationInfo = RelationInfo.unidirectional(RelationKind.ELEMENT_COLLECTION, "com.example.Tag");
            DomainProperty property = new DomainProperty(
                    "tags",
                    TypeRef.parameterized("java.util.Set", TypeRef.of("com.example.Tag")),
                    Cardinality.COLLECTION,
                    Nullability.NON_NULL,
                    false,
                    false,
                    relationInfo);

            assertThat(property.isEntityCollection()).isFalse();
        }

        @Test
        @DisplayName("should return false when no relation")
        void returnsFalseWhenNoRelation() {
            DomainProperty property = new DomainProperty(
                    "names",
                    TypeRef.parameterized("java.util.List", TypeRef.of("java.lang.String")),
                    Cardinality.COLLECTION,
                    Nullability.NON_NULL,
                    false);

            assertThat(property.isEntityCollection()).isFalse();
        }
    }

    @Nested
    @DisplayName("isEmbeddedCollection()")
    class IsEmbeddedCollectionTest {

        @Test
        @DisplayName("should return true for ELEMENT_COLLECTION")
        void returnsTrueForElementCollection() {
            RelationInfo relationInfo =
                    RelationInfo.unidirectional(RelationKind.ELEMENT_COLLECTION, "com.example.Address");
            DomainProperty property = new DomainProperty(
                    "addresses",
                    TypeRef.parameterized("java.util.List", TypeRef.of("com.example.Address")),
                    Cardinality.COLLECTION,
                    Nullability.NON_NULL,
                    false,
                    false,
                    relationInfo);

            assertThat(property.isEmbeddedCollection()).isTrue();
        }

        @Test
        @DisplayName("should return false for ONE_TO_MANY")
        void returnsFalseForOneToMany() {
            RelationInfo relationInfo = RelationInfo.unidirectional(RelationKind.ONE_TO_MANY, "com.example.LineItem");
            DomainProperty property = new DomainProperty(
                    "items",
                    TypeRef.parameterized("java.util.List", TypeRef.of("com.example.LineItem")),
                    Cardinality.COLLECTION,
                    Nullability.NON_NULL,
                    false,
                    false,
                    relationInfo);

            assertThat(property.isEmbeddedCollection()).isFalse();
        }
    }

    @Nested
    @DisplayName("relationInfoOpt()")
    class RelationInfoOptTest {

        @Test
        @DisplayName("should return empty when null")
        void returnsEmptyWhenNull() {
            DomainProperty property = new DomainProperty(
                    "name", TypeRef.of("java.lang.String"), Cardinality.SINGLE, Nullability.UNKNOWN, false);

            assertThat(property.relationInfoOpt()).isEmpty();
        }

        @Test
        @DisplayName("should return present when set")
        void returnsPresentWhenSet() {
            RelationInfo relationInfo = RelationInfo.unidirectional(RelationKind.EMBEDDED, "com.example.Address");
            DomainProperty property = new DomainProperty(
                    "address",
                    TypeRef.of("com.example.Address"),
                    Cardinality.SINGLE,
                    Nullability.UNKNOWN,
                    false,
                    true,
                    relationInfo);

            assertThat(property.relationInfoOpt()).isPresent().contains(relationInfo);
        }
    }
}
