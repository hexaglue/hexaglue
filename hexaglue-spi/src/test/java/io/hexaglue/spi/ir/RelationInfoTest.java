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
 * Unit tests for {@link RelationInfo}.
 */
class RelationInfoTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodsTest {

        @Test
        @DisplayName("unidirectional should create owning relation")
        void unidirectionalCreatesOwningRelation() {
            RelationInfo info = RelationInfo.unidirectional(
                    RelationKind.ONE_TO_MANY, "com.example.LineItem", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
            assertThat(info.targetType()).isEqualTo("com.example.LineItem");
            assertThat(info.mappedBy()).isNull();
            assertThat(info.owning()).isTrue();
        }

        @Test
        @DisplayName("owning should create owning side relation")
        void owningCreatesOwningSide() {
            RelationInfo info = RelationInfo.owning(
                    RelationKind.MANY_TO_ONE, "com.example.Order", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.owning()).isTrue();
            assertThat(info.isBidirectional()).isFalse();
        }

        @Test
        @DisplayName("inverse should create non-owning side with mappedBy")
        void inverseCreatesNonOwningSide() {
            RelationInfo info = RelationInfo.inverse(
                    RelationKind.ONE_TO_MANY, "com.example.LineItem", "order", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.owning()).isFalse();
            assertThat(info.mappedBy()).isEqualTo("order");
            assertThat(info.isBidirectional()).isTrue();
        }
    }

    @Nested
    @DisplayName("Helper methods")
    class HelperMethodsTest {

        @Test
        @DisplayName("mappedByOpt should return Optional with value when set")
        void mappedByOptWithValue() {
            RelationInfo info = RelationInfo.inverse(
                    RelationKind.ONE_TO_MANY, "com.example.Item", "parent", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.mappedByOpt()).isPresent().contains("parent");
        }

        @Test
        @DisplayName("mappedByOpt should return empty when null")
        void mappedByOptEmpty() {
            RelationInfo info = RelationInfo.unidirectional(
                    RelationKind.MANY_TO_ONE, "com.example.Order", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.mappedByOpt()).isEmpty();
        }

        @Test
        @DisplayName("isCollection should return true for ONE_TO_MANY")
        void isCollectionForOneToMany() {
            RelationInfo info = RelationInfo.unidirectional(
                    RelationKind.ONE_TO_MANY, "com.example.Item", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.isCollection()).isTrue();
        }

        @Test
        @DisplayName("isCollection should return true for MANY_TO_MANY")
        void isCollectionForManyToMany() {
            RelationInfo info = RelationInfo.unidirectional(
                    RelationKind.MANY_TO_MANY, "com.example.Tag", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.isCollection()).isTrue();
        }

        @Test
        @DisplayName("isCollection should return true for ELEMENT_COLLECTION")
        void isCollectionForElementCollection() {
            RelationInfo info = RelationInfo.unidirectional(
                    RelationKind.ELEMENT_COLLECTION, "com.example.Address", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.isCollection()).isTrue();
        }

        @Test
        @DisplayName("isCollection should return false for ONE_TO_ONE")
        void isCollectionFalseForOneToOne() {
            RelationInfo info = RelationInfo.unidirectional(
                    RelationKind.ONE_TO_ONE, "com.example.Profile", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.isCollection()).isFalse();
        }

        @Test
        @DisplayName("isCollection should return false for MANY_TO_ONE")
        void isCollectionFalseForManyToOne() {
            RelationInfo info = RelationInfo.unidirectional(
                    RelationKind.MANY_TO_ONE, "com.example.Order", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.isCollection()).isFalse();
        }

        @Test
        @DisplayName("isEmbedded should return true for EMBEDDED")
        void isEmbeddedForEmbedded() {
            RelationInfo info = RelationInfo.unidirectional(
                    RelationKind.EMBEDDED, "com.example.Address", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.isEmbedded()).isTrue();
        }

        @Test
        @DisplayName("isEmbedded should return true for ELEMENT_COLLECTION")
        void isEmbeddedForElementCollection() {
            RelationInfo info = RelationInfo.unidirectional(
                    RelationKind.ELEMENT_COLLECTION, "com.example.Tag", CascadeType.NONE, FetchType.LAZY, null);

            assertThat(info.isEmbedded()).isTrue();
        }

        @Test
        @DisplayName("isEmbedded should return false for entity relations")
        void isEmbeddedFalseForEntityRelations() {
            assertThat(RelationInfo.unidirectional(
                                    RelationKind.ONE_TO_MANY, "X", CascadeType.NONE, FetchType.LAZY, null)
                            .isEmbedded())
                    .isFalse();
            assertThat(RelationInfo.unidirectional(
                                    RelationKind.MANY_TO_ONE, "X", CascadeType.NONE, FetchType.LAZY, null)
                            .isEmbedded())
                    .isFalse();
            assertThat(RelationInfo.unidirectional(
                                    RelationKind.ONE_TO_ONE, "X", CascadeType.NONE, FetchType.LAZY, null)
                            .isEmbedded())
                    .isFalse();
            assertThat(RelationInfo.unidirectional(
                                    RelationKind.MANY_TO_MANY, "X", CascadeType.NONE, FetchType.LAZY, null)
                            .isEmbedded())
                    .isFalse();
        }
    }
}
