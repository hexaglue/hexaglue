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
            assertThat(annotation.typedValues()).isEmpty();
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
            assertThat(annotation.typedValues()).hasSize(2);
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

        @Test
        @DisplayName("should create annotation with explicit typed values")
        void shouldCreateWithExplicitTypedValues() {
            // given
            String qualifiedName = "javax.persistence.Table";
            Map<String, Object> values = Map.of("name", "orders");
            Map<String, AnnotationValue> typedValues = Map.of("name", new AnnotationValue.StringVal("orders"));

            // when
            Annotation annotation = Annotation.of(qualifiedName, values, typedValues);

            // then
            assertThat(annotation.values()).containsEntry("name", "orders");
            assertThat(annotation.typedValues()).containsKey("name");
            assertThat(annotation.typedValues().get("name")).isInstanceOf(AnnotationValue.StringVal.class);
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
        @DisplayName("should return immutable typed values map")
        void shouldReturnImmutableTypedValuesMap() {
            // given
            Map<String, Object> values = new java.util.HashMap<>();
            values.put("name", "orders");
            Annotation annotation = Annotation.of("javax.persistence.Table", values);

            // when/then
            assertThatThrownBy(() -> annotation.typedValues().put("new", new AnnotationValue.StringVal("value")))
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
            assertThat(annotation.typedValues()).hasSize(1);
            assertThat(annotation.typedValues()).doesNotContainKey("extra");
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
    @DisplayName("Typed Value Access")
    class TypedValueAccess {

        @Test
        @DisplayName("should check if typed value exists")
        void shouldCheckIfTypedValueExists() {
            // given
            Annotation annotation = Annotation.of("javax.persistence.Table", Map.of("name", "orders"));

            // then
            assertThat(annotation.hasTypedValue("name")).isTrue();
            assertThat(annotation.hasTypedValue("schema")).isFalse();
        }

        @Test
        @DisplayName("should get typed value as Optional")
        void shouldGetTypedValueAsOptional() {
            // given
            Annotation annotation = Annotation.of("javax.persistence.Table", Map.of("name", "orders"));

            // then
            assertThat(annotation.getTypedValue("name")).isPresent();
            assertThat(annotation.getTypedValue("name").get()).isInstanceOf(AnnotationValue.StringVal.class);
            assertThat(annotation.getTypedValue("schema")).isEmpty();
        }

        @Test
        @DisplayName("should convert string value to StringVal")
        void shouldConvertStringValueToStringVal() {
            // given
            Annotation annotation = Annotation.of("javax.persistence.Table", Map.of("name", "orders"));

            // when
            AnnotationValue typedValue = annotation.getTypedValue("name").orElseThrow();

            // then
            assertThat(typedValue).isInstanceOf(AnnotationValue.StringVal.class);
            assertThat(((AnnotationValue.StringVal) typedValue).value()).isEqualTo("orders");
        }

        @Test
        @DisplayName("should convert int value to IntVal")
        void shouldConvertIntValueToIntVal() {
            // given
            Annotation annotation = Annotation.of("javax.persistence.Column", Map.of("length", 255));

            // when
            AnnotationValue typedValue = annotation.getTypedValue("length").orElseThrow();

            // then
            assertThat(typedValue).isInstanceOf(AnnotationValue.IntVal.class);
            assertThat(((AnnotationValue.IntVal) typedValue).value()).isEqualTo(255);
        }

        @Test
        @DisplayName("should convert boolean value to BoolVal")
        void shouldConvertBooleanValueToBoolVal() {
            // given
            Annotation annotation = Annotation.of("javax.persistence.Column", Map.of("nullable", false));

            // when
            AnnotationValue typedValue = annotation.getTypedValue("nullable").orElseThrow();

            // then
            assertThat(typedValue).isInstanceOf(AnnotationValue.BoolVal.class);
            assertThat(((AnnotationValue.BoolVal) typedValue).value()).isFalse();
        }

        @Test
        @DisplayName("should convert long value to LongVal")
        void shouldConvertLongValueToLongVal() {
            // given
            Annotation annotation = Annotation.of("custom.Timeout", Map.of("duration", 5000L));

            // when
            AnnotationValue typedValue = annotation.getTypedValue("duration").orElseThrow();

            // then
            assertThat(typedValue).isInstanceOf(AnnotationValue.LongVal.class);
            assertThat(((AnnotationValue.LongVal) typedValue).value()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("should convert double value to DoubleVal")
        void shouldConvertDoubleValueToDoubleVal() {
            // given
            Annotation annotation = Annotation.of("custom.Precision", Map.of("value", 0.001));

            // when
            AnnotationValue typedValue = annotation.getTypedValue("value").orElseThrow();

            // then
            assertThat(typedValue).isInstanceOf(AnnotationValue.DoubleVal.class);
            assertThat(((AnnotationValue.DoubleVal) typedValue).value()).isEqualTo(0.001);
        }
    }

    @Nested
    @DisplayName("Has Values")
    class HasValues {

        @Test
        @DisplayName("should return false when no values")
        void shouldReturnFalseWhenNoValues() {
            // given
            Annotation annotation = Annotation.of("javax.persistence.Entity");

            // then
            assertThat(annotation.hasValues()).isFalse();
        }

        @Test
        @DisplayName("should return true when has values")
        void shouldReturnTrueWhenHasValues() {
            // given
            Annotation annotation = Annotation.of("javax.persistence.Table", Map.of("name", "orders"));

            // then
            assertThat(annotation.hasValues()).isTrue();
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
