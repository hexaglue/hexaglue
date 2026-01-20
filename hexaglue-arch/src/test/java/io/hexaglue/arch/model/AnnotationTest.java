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

package io.hexaglue.arch.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Annotation}.
 *
 * @since 4.1.0
 */
@DisplayName("Annotation")
class AnnotationTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create annotation with qualified name only")
        void shouldCreateWithQualifiedNameOnly() {
            // given
            String qualifiedName = "javax.persistence.Entity";

            // when
            Annotation annotation = Annotation.of(qualifiedName);

            // then
            assertThat(annotation.qualifiedName()).isEqualTo(qualifiedName);
            assertThat(annotation.simpleName()).isEqualTo("Entity");
            assertThat(annotation.values()).isEmpty();
        }

        @Test
        @DisplayName("should create annotation with values")
        void shouldCreateWithValues() {
            // given
            String qualifiedName = "javax.persistence.Table";
            Map<String, Object> values = Map.of("name", "orders", "schema", "public");

            // when
            Annotation annotation = Annotation.of(qualifiedName, values);

            // then
            assertThat(annotation.qualifiedName()).isEqualTo(qualifiedName);
            assertThat(annotation.values()).containsEntry("name", "orders");
            assertThat(annotation.values()).containsEntry("schema", "public");
        }

        @Test
        @DisplayName("should extract simple name from qualified name")
        void shouldExtractSimpleName() {
            // given
            String qualifiedName = "org.jmolecules.ddd.annotation.AggregateRoot";

            // when
            Annotation annotation = Annotation.of(qualifiedName);

            // then
            assertThat(annotation.simpleName()).isEqualTo("AggregateRoot");
        }

        @Test
        @DisplayName("should handle simple name without package")
        void shouldHandleSimpleNameWithoutPackage() {
            // given
            String simpleName = "Deprecated";

            // when
            Annotation annotation = Annotation.of(simpleName);

            // then
            assertThat(annotation.simpleName()).isEqualTo("Deprecated");
            assertThat(annotation.qualifiedName()).isEqualTo("Deprecated");
        }

        @Test
        @DisplayName("should reject null qualified name")
        void shouldRejectNullQualifiedName() {
            assertThatThrownBy(() -> Annotation.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("qualifiedName");
        }

        @Test
        @DisplayName("should reject blank qualified name")
        void shouldRejectBlankQualifiedName() {
            assertThatThrownBy(() -> Annotation.of("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return immutable values map")
        void shouldReturnImmutableValuesMap() {
            // given
            Map<String, Object> values = new java.util.HashMap<>();
            values.put("name", "orders");
            Annotation annotation = Annotation.of("javax.persistence.Table", values);

            // when/then
            assertThatThrownBy(() -> annotation.values().put("new", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should not be affected by original map modifications")
        void shouldNotBeAffectedByOriginalMapModifications() {
            // given
            Map<String, Object> values = new java.util.HashMap<>();
            values.put("name", "orders");
            Annotation annotation = Annotation.of("javax.persistence.Table", values);

            // when
            values.put("extra", "value");

            // then
            assertThat(annotation.values()).hasSize(1);
            assertThat(annotation.values()).doesNotContainKey("extra");
        }
    }

    @Nested
    @DisplayName("Value Access")
    class ValueAccess {

        @Test
        @DisplayName("should check if value exists")
        void shouldCheckIfValueExists() {
            // given
            Annotation annotation = Annotation.of("javax.persistence.Table", Map.of("name", "orders"));

            // then
            assertThat(annotation.hasValue("name")).isTrue();
            assertThat(annotation.hasValue("schema")).isFalse();
        }

        @Test
        @DisplayName("should get value as Optional")
        void shouldGetValueAsOptional() {
            // given
            Annotation annotation = Annotation.of("javax.persistence.Table", Map.of("name", "orders"));

            // then
            assertThat(annotation.getValue("name")).contains("orders");
            assertThat(annotation.getValue("schema")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when qualified names and values match")
        void shouldBeEqualWhenNamesAndValuesMatch() {
            // given
            Annotation a1 = Annotation.of("javax.persistence.Table", Map.of("name", "orders"));
            Annotation a2 = Annotation.of("javax.persistence.Table", Map.of("name", "orders"));

            // then
            assertThat(a1).isEqualTo(a2);
            assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when qualified names differ")
        void shouldNotBeEqualWhenNamesDiffer() {
            // given
            Annotation a1 = Annotation.of("javax.persistence.Table");
            Annotation a2 = Annotation.of("javax.persistence.Entity");

            // then
            assertThat(a1).isNotEqualTo(a2);
        }
    }
}
