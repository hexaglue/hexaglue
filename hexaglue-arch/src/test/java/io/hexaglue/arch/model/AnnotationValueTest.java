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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AnnotationValue}.
 *
 * @since 5.0.0
 */
@DisplayName("AnnotationValue")
class AnnotationValueTest {

    @Nested
    @DisplayName("StringVal")
    class StringValTests {

        @Test
        @DisplayName("should create from string value")
        void shouldCreateFromStringValue() {
            var val = new AnnotationValue.StringVal("hello");
            assertThat(val.value()).isEqualTo("hello");
            assertThat(val.rawValue()).isEqualTo("hello");
        }

        @Test
        @DisplayName("should reject null value")
        void shouldRejectNullValue() {
            assertThatThrownBy(() -> new AnnotationValue.StringVal(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("value must not be null");
        }
    }

    @Nested
    @DisplayName("IntVal")
    class IntValTests {

        @Test
        @DisplayName("should create from int value")
        void shouldCreateFromIntValue() {
            var val = new AnnotationValue.IntVal(42);
            assertThat(val.value()).isEqualTo(42);
            assertThat(val.rawValue()).isEqualTo(42);
        }

        @Test
        @DisplayName("should handle negative values")
        void shouldHandleNegativeValues() {
            var val = new AnnotationValue.IntVal(-100);
            assertThat(val.value()).isEqualTo(-100);
        }
    }

    @Nested
    @DisplayName("LongVal")
    class LongValTests {

        @Test
        @DisplayName("should create from long value")
        void shouldCreateFromLongValue() {
            var val = new AnnotationValue.LongVal(123456789012345L);
            assertThat(val.value()).isEqualTo(123456789012345L);
            assertThat(val.rawValue()).isEqualTo(123456789012345L);
        }
    }

    @Nested
    @DisplayName("BoolVal")
    class BoolValTests {

        @Test
        @DisplayName("should create from boolean value")
        void shouldCreateFromBooleanValue() {
            var trueVal = new AnnotationValue.BoolVal(true);
            var falseVal = new AnnotationValue.BoolVal(false);

            assertThat(trueVal.value()).isTrue();
            assertThat(falseVal.value()).isFalse();
            assertThat(trueVal.rawValue()).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("DoubleVal")
    class DoubleValTests {

        @Test
        @DisplayName("should create from double value")
        void shouldCreateFromDoubleValue() {
            var val = new AnnotationValue.DoubleVal(3.14159);
            assertThat(val.value()).isEqualTo(3.14159);
            assertThat(val.rawValue()).isEqualTo(3.14159);
        }
    }

    @Nested
    @DisplayName("CharVal")
    class CharValTests {

        @Test
        @DisplayName("should create from char value")
        void shouldCreateFromCharValue() {
            var val = new AnnotationValue.CharVal('A');
            assertThat(val.value()).isEqualTo('A');
            assertThat(val.rawValue()).isEqualTo('A');
        }
    }

    @Nested
    @DisplayName("ClassVal")
    class ClassValTests {

        @Test
        @DisplayName("should create from qualified name")
        void shouldCreateFromQualifiedName() {
            var val = new AnnotationValue.ClassVal("java.lang.String");
            assertThat(val.qualifiedName()).isEqualTo("java.lang.String");
            assertThat(val.simpleName()).isEqualTo("String");
            assertThat(val.rawValue()).isEqualTo("java.lang.String");
        }

        @Test
        @DisplayName("should handle simple name without package")
        void shouldHandleSimpleNameWithoutPackage() {
            var val = new AnnotationValue.ClassVal("String");
            assertThat(val.simpleName()).isEqualTo("String");
        }

        @Test
        @DisplayName("should reject null qualified name")
        void shouldRejectNullQualifiedName() {
            assertThatThrownBy(() -> new AnnotationValue.ClassVal(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject blank qualified name")
        void shouldRejectBlankQualifiedName() {
            assertThatThrownBy(() -> new AnnotationValue.ClassVal("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("qualifiedName must not be blank");
        }
    }

    @Nested
    @DisplayName("EnumVal")
    class EnumValTests {

        @Test
        @DisplayName("should create from enum type and constant")
        void shouldCreateFromEnumTypeAndConstant() {
            var val = new AnnotationValue.EnumVal("java.time.DayOfWeek", "MONDAY");
            assertThat(val.enumType()).isEqualTo("java.time.DayOfWeek");
            assertThat(val.enumConstant()).isEqualTo("MONDAY");
            assertThat(val.simpleTypeName()).isEqualTo("DayOfWeek");
            assertThat(val.rawValue()).isEqualTo("java.time.DayOfWeek.MONDAY");
        }

        @Test
        @DisplayName("should reject null enum type")
        void shouldRejectNullEnumType() {
            assertThatThrownBy(() -> new AnnotationValue.EnumVal(null, "MONDAY"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject blank enum constant")
        void shouldRejectBlankEnumConstant() {
            assertThatThrownBy(() -> new AnnotationValue.EnumVal("DayOfWeek", "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("enumConstant must not be blank");
        }
    }

    @Nested
    @DisplayName("AnnotationVal")
    class AnnotationValTests {

        @Test
        @DisplayName("should create from nested annotation")
        void shouldCreateFromNestedAnnotation() {
            var nested = Annotation.of("javax.persistence.Column", Map.of("name", "id"));
            var val = new AnnotationValue.AnnotationVal(nested);

            assertThat(val.annotation()).isEqualTo(nested);
            assertThat(val.rawValue()).isEqualTo(nested);
        }

        @Test
        @DisplayName("should reject null annotation")
        void shouldRejectNullAnnotation() {
            assertThatThrownBy(() -> new AnnotationValue.AnnotationVal(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("ArrayVal")
    class ArrayValTests {

        @Test
        @DisplayName("should create from list of values")
        void shouldCreateFromListOfValues() {
            List<AnnotationValue> values =
                    List.of(new AnnotationValue.StringVal("a"), new AnnotationValue.StringVal("b"));
            var val = new AnnotationValue.ArrayVal(values);

            assertThat(val.values()).hasSize(2);
            assertThat(val.size()).isEqualTo(2);
            assertThat(val.isEmpty()).isFalse();
            assertThat(val.rawValue()).isEqualTo(List.of("a", "b"));
        }

        @Test
        @DisplayName("should create empty array")
        void shouldCreateEmptyArray() {
            var val = new AnnotationValue.ArrayVal(List.of());
            assertThat(val.isEmpty()).isTrue();
            assertThat(val.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("should be immutable")
        void shouldBeImmutable() {
            var mutable = new java.util.ArrayList<AnnotationValue>();
            mutable.add(new AnnotationValue.IntVal(1));
            var val = new AnnotationValue.ArrayVal(mutable);

            mutable.add(new AnnotationValue.IntVal(2));

            assertThat(val.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should reject null values")
        void shouldRejectNullValues() {
            assertThatThrownBy(() -> new AnnotationValue.ArrayVal(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Factory Method from()")
    class FactoryMethodTests {

        @Test
        @DisplayName("should convert String")
        void shouldConvertString() {
            var result = AnnotationValue.from("hello");
            assertThat(result).isInstanceOf(AnnotationValue.StringVal.class);
            assertThat(((AnnotationValue.StringVal) result).value()).isEqualTo("hello");
        }

        @Test
        @DisplayName("should convert Integer")
        void shouldConvertInteger() {
            var result = AnnotationValue.from(42);
            assertThat(result).isInstanceOf(AnnotationValue.IntVal.class);
            assertThat(((AnnotationValue.IntVal) result).value()).isEqualTo(42);
        }

        @Test
        @DisplayName("should convert Long")
        void shouldConvertLong() {
            var result = AnnotationValue.from(123456789012345L);
            assertThat(result).isInstanceOf(AnnotationValue.LongVal.class);
        }

        @Test
        @DisplayName("should convert Boolean")
        void shouldConvertBoolean() {
            var result = AnnotationValue.from(true);
            assertThat(result).isInstanceOf(AnnotationValue.BoolVal.class);
        }

        @Test
        @DisplayName("should convert Double")
        void shouldConvertDouble() {
            var result = AnnotationValue.from(3.14);
            assertThat(result).isInstanceOf(AnnotationValue.DoubleVal.class);
        }

        @Test
        @DisplayName("should convert Float to DoubleVal")
        void shouldConvertFloatToDoubleVal() {
            var result = AnnotationValue.from(3.14f);
            assertThat(result).isInstanceOf(AnnotationValue.DoubleVal.class);
        }

        @Test
        @DisplayName("should convert Character")
        void shouldConvertCharacter() {
            var result = AnnotationValue.from('X');
            assertThat(result).isInstanceOf(AnnotationValue.CharVal.class);
        }

        @Test
        @DisplayName("should convert Short to IntVal")
        void shouldConvertShortToIntVal() {
            var result = AnnotationValue.from((short) 123);
            assertThat(result).isInstanceOf(AnnotationValue.IntVal.class);
        }

        @Test
        @DisplayName("should convert Byte to IntVal")
        void shouldConvertByteToIntVal() {
            var result = AnnotationValue.from((byte) 42);
            assertThat(result).isInstanceOf(AnnotationValue.IntVal.class);
        }

        @Test
        @DisplayName("should convert Class")
        void shouldConvertClass() {
            var result = AnnotationValue.from(String.class);
            assertThat(result).isInstanceOf(AnnotationValue.ClassVal.class);
            assertThat(((AnnotationValue.ClassVal) result).qualifiedName()).isEqualTo("java.lang.String");
        }

        @Test
        @DisplayName("should convert Enum")
        void shouldConvertEnum() {
            var result = AnnotationValue.from(java.time.DayOfWeek.MONDAY);
            assertThat(result).isInstanceOf(AnnotationValue.EnumVal.class);
            var enumVal = (AnnotationValue.EnumVal) result;
            assertThat(enumVal.enumConstant()).isEqualTo("MONDAY");
        }

        @Test
        @DisplayName("should convert Annotation")
        void shouldConvertAnnotation() {
            var annotation = Annotation.of("Test");
            var result = AnnotationValue.from(annotation);
            assertThat(result).isInstanceOf(AnnotationValue.AnnotationVal.class);
        }

        @Test
        @DisplayName("should convert List to ArrayVal")
        void shouldConvertListToArrayVal() {
            var result = AnnotationValue.from(List.of("a", "b", "c"));
            assertThat(result).isInstanceOf(AnnotationValue.ArrayVal.class);
            assertThat(((AnnotationValue.ArrayVal) result).size()).isEqualTo(3);
        }

        @Test
        @DisplayName("should convert Object array")
        void shouldConvertObjectArray() {
            var result = AnnotationValue.from(new String[] {"a", "b"});
            assertThat(result).isInstanceOf(AnnotationValue.ArrayVal.class);
        }

        @Test
        @DisplayName("should convert int array")
        void shouldConvertIntArray() {
            var result = AnnotationValue.from(new int[] {1, 2, 3});
            assertThat(result).isInstanceOf(AnnotationValue.ArrayVal.class);
            assertThat(((AnnotationValue.ArrayVal) result).size()).isEqualTo(3);
        }

        @Test
        @DisplayName("should convert boolean array")
        void shouldConvertBooleanArray() {
            var result = AnnotationValue.from(new boolean[] {true, false});
            assertThat(result).isInstanceOf(AnnotationValue.ArrayVal.class);
            assertThat(((AnnotationValue.ArrayVal) result).size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should convert double array")
        void shouldConvertDoubleArray() {
            var result = AnnotationValue.from(new double[] {1.0, 2.0});
            assertThat(result).isInstanceOf(AnnotationValue.ArrayVal.class);
        }

        @Test
        @DisplayName("should convert long array")
        void shouldConvertLongArray() {
            var result = AnnotationValue.from(new long[] {1L, 2L});
            assertThat(result).isInstanceOf(AnnotationValue.ArrayVal.class);
        }

        @Test
        @DisplayName("should convert char array")
        void shouldConvertCharArray() {
            var result = AnnotationValue.from(new char[] {'a', 'b'});
            assertThat(result).isInstanceOf(AnnotationValue.ArrayVal.class);
        }

        @Test
        @DisplayName("should reject null value")
        void shouldRejectNullValue() {
            assertThatThrownBy(() -> AnnotationValue.from(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject unsupported type")
        void shouldRejectUnsupportedType() {
            assertThatThrownBy(() -> AnnotationValue.from(new Object()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported annotation value type");
        }
    }

    @Nested
    @DisplayName("Sealed Interface Permits")
    class SealedInterfaceTests {

        @Test
        @DisplayName("should support instanceof checks with sealed interface")
        void shouldSupportInstanceOfChecks() {
            AnnotationValue stringVal = new AnnotationValue.StringVal("test");
            AnnotationValue intVal = new AnnotationValue.IntVal(42);
            AnnotationValue enumVal = new AnnotationValue.EnumVal("Type", "VALUE");

            assertThat(stringVal).isInstanceOf(AnnotationValue.StringVal.class);
            assertThat(intVal).isInstanceOf(AnnotationValue.IntVal.class);
            assertThat(enumVal).isInstanceOf(AnnotationValue.EnumVal.class);

            // Verify pattern matching with instanceof (Java 17)
            if (stringVal instanceof AnnotationValue.StringVal s) {
                assertThat(s.value()).isEqualTo("test");
            }
        }

        @Test
        @DisplayName("should have exactly 10 permitted subtypes")
        void shouldHaveExactlyTenPermittedSubtypes() {
            // StringVal, IntVal, LongVal, BoolVal, DoubleVal, CharVal, ClassVal, EnumVal, AnnotationVal, ArrayVal
            assertThat(AnnotationValue.class.getPermittedSubclasses()).hasSize(10);
        }
    }
}
