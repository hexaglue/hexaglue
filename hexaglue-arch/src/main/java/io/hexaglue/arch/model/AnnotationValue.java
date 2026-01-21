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

import java.util.List;
import java.util.Objects;

/**
 * Represents a typed annotation attribute value.
 *
 * <p>Unlike the raw {@link Object} values in {@link Annotation#values()}, this sealed
 * interface provides type-safe access to annotation attribute values, preserving
 * their exact type information from the source code.</p>
 *
 * <h2>Supported Value Types</h2>
 * <ul>
 *   <li>{@link StringVal} - String literals</li>
 *   <li>{@link IntVal} - Integer literals (int)</li>
 *   <li>{@link LongVal} - Long literals (long)</li>
 *   <li>{@link BoolVal} - Boolean literals</li>
 *   <li>{@link DoubleVal} - Double literals (includes float)</li>
 *   <li>{@link CharVal} - Character literals</li>
 *   <li>{@link ClassVal} - Class references (e.g., {@code String.class})</li>
 *   <li>{@link EnumVal} - Enum constant references</li>
 *   <li>{@link AnnotationVal} - Nested annotations</li>
 *   <li>{@link ArrayVal} - Arrays of any of the above</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AnnotationValue value = annotation.getTypedValue("name").orElseThrow();
 *
 * // Pattern matching (Java 17+)
 * switch (value) {
 *     case StringVal(var s) -> System.out.println("String: " + s);
 *     case IntVal(var i) -> System.out.println("Int: " + i);
 *     case EnumVal(var type, var constant) -> System.out.println("Enum: " + type + "." + constant);
 *     case ArrayVal(var elements) -> elements.forEach(System.out::println);
 *     default -> System.out.println("Other: " + value.rawValue());
 * }
 *
 * // Factory method
 * AnnotationValue fromString = AnnotationValue.from("hello");
 * AnnotationValue fromInt = AnnotationValue.from(42);
 * }</pre>
 *
 * @since 5.0.0
 */
public sealed interface AnnotationValue
        permits AnnotationValue.StringVal,
                AnnotationValue.IntVal,
                AnnotationValue.LongVal,
                AnnotationValue.BoolVal,
                AnnotationValue.DoubleVal,
                AnnotationValue.CharVal,
                AnnotationValue.ClassVal,
                AnnotationValue.EnumVal,
                AnnotationValue.AnnotationVal,
                AnnotationValue.ArrayVal {

    /**
     * Returns the raw value as an Object.
     *
     * <p>This provides backward compatibility with untyped value access.
     * Prefer using pattern matching on the concrete types when possible.</p>
     *
     * @return the raw value
     */
    Object rawValue();

    /**
     * A String annotation value.
     *
     * @param value the string value
     */
    record StringVal(String value) implements AnnotationValue {
        /**
         * Creates a new StringVal.
         *
         * @param value the string value, must not be null
         * @throws NullPointerException if value is null
         */
        public StringVal {
            Objects.requireNonNull(value, "value must not be null");
        }

        @Override
        public Object rawValue() {
            return value;
        }
    }

    /**
     * An integer annotation value.
     *
     * @param value the integer value
     */
    record IntVal(int value) implements AnnotationValue {
        @Override
        public Object rawValue() {
            return value;
        }
    }

    /**
     * A long annotation value.
     *
     * @param value the long value
     */
    record LongVal(long value) implements AnnotationValue {
        @Override
        public Object rawValue() {
            return value;
        }
    }

    /**
     * A boolean annotation value.
     *
     * @param value the boolean value
     */
    record BoolVal(boolean value) implements AnnotationValue {
        @Override
        public Object rawValue() {
            return value;
        }
    }

    /**
     * A double annotation value (also used for float).
     *
     * @param value the double value
     */
    record DoubleVal(double value) implements AnnotationValue {
        @Override
        public Object rawValue() {
            return value;
        }
    }

    /**
     * A character annotation value.
     *
     * @param value the character value
     */
    record CharVal(char value) implements AnnotationValue {
        @Override
        public Object rawValue() {
            return value;
        }
    }

    /**
     * A class reference annotation value.
     *
     * <p>Stores the fully qualified name of the class referenced in the annotation.</p>
     *
     * @param qualifiedName the fully qualified class name (e.g., "java.lang.String")
     */
    record ClassVal(String qualifiedName) implements AnnotationValue {
        /**
         * Creates a new ClassVal.
         *
         * @param qualifiedName the class name, must not be null or blank
         * @throws NullPointerException if qualifiedName is null
         * @throws IllegalArgumentException if qualifiedName is blank
         */
        public ClassVal {
            Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
            if (qualifiedName.isBlank()) {
                throw new IllegalArgumentException("qualifiedName must not be blank");
            }
        }

        @Override
        public Object rawValue() {
            return qualifiedName;
        }

        /**
         * Returns the simple name of the class.
         *
         * @return the simple class name
         */
        public String simpleName() {
            int lastDot = qualifiedName.lastIndexOf('.');
            return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
        }
    }

    /**
     * An enum constant annotation value.
     *
     * @param enumType the fully qualified name of the enum type
     * @param enumConstant the name of the enum constant
     */
    record EnumVal(String enumType, String enumConstant) implements AnnotationValue {
        /**
         * Creates a new EnumVal.
         *
         * @param enumType the enum type, must not be null or blank
         * @param enumConstant the enum constant, must not be null or blank
         * @throws NullPointerException if any argument is null
         * @throws IllegalArgumentException if any argument is blank
         */
        public EnumVal {
            Objects.requireNonNull(enumType, "enumType must not be null");
            Objects.requireNonNull(enumConstant, "enumConstant must not be null");
            if (enumType.isBlank()) {
                throw new IllegalArgumentException("enumType must not be blank");
            }
            if (enumConstant.isBlank()) {
                throw new IllegalArgumentException("enumConstant must not be blank");
            }
        }

        @Override
        public Object rawValue() {
            return enumType + "." + enumConstant;
        }

        /**
         * Returns the simple name of the enum type.
         *
         * @return the simple enum type name
         */
        public String simpleTypeName() {
            int lastDot = enumType.lastIndexOf('.');
            return lastDot >= 0 ? enumType.substring(lastDot + 1) : enumType;
        }
    }

    /**
     * A nested annotation value.
     *
     * @param annotation the nested annotation
     */
    record AnnotationVal(Annotation annotation) implements AnnotationValue {
        /**
         * Creates a new AnnotationVal.
         *
         * @param annotation the annotation, must not be null
         * @throws NullPointerException if annotation is null
         */
        public AnnotationVal {
            Objects.requireNonNull(annotation, "annotation must not be null");
        }

        @Override
        public Object rawValue() {
            return annotation;
        }
    }

    /**
     * An array annotation value.
     *
     * @param values the array elements (immutable)
     */
    record ArrayVal(List<AnnotationValue> values) implements AnnotationValue {
        /**
         * Creates a new ArrayVal.
         *
         * @param values the array values, must not be null
         * @throws NullPointerException if values is null
         */
        public ArrayVal {
            Objects.requireNonNull(values, "values must not be null");
            values = List.copyOf(values);
        }

        @Override
        public Object rawValue() {
            return values.stream().map(AnnotationValue::rawValue).toList();
        }

        /**
         * Returns whether this array is empty.
         *
         * @return true if the array has no elements
         */
        public boolean isEmpty() {
            return values.isEmpty();
        }

        /**
         * Returns the number of elements in this array.
         *
         * @return the array size
         */
        public int size() {
            return values.size();
        }
    }

    /**
     * Creates an AnnotationValue from a raw Object value.
     *
     * <p>This factory method handles the conversion from untyped annotation values
     * (as typically returned by annotation processing) to typed AnnotationValue instances.</p>
     *
     * <h2>Conversion Rules</h2>
     * <ul>
     *   <li>{@code String} → {@link StringVal}</li>
     *   <li>{@code Integer} → {@link IntVal}</li>
     *   <li>{@code Long} → {@link LongVal}</li>
     *   <li>{@code Boolean} → {@link BoolVal}</li>
     *   <li>{@code Double} or {@code Float} → {@link DoubleVal}</li>
     *   <li>{@code Character} → {@link CharVal}</li>
     *   <li>{@code Class} → {@link ClassVal}</li>
     *   <li>{@code Enum} → {@link EnumVal}</li>
     *   <li>{@code Annotation} → {@link AnnotationVal}</li>
     *   <li>{@code List} or Array → {@link ArrayVal}</li>
     * </ul>
     *
     * @param value the raw value to convert, must not be null
     * @return the typed AnnotationValue
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if the value type is not supported
     */
    static AnnotationValue from(Object value) {
        Objects.requireNonNull(value, "value must not be null");

        if (value instanceof String s) {
            return new StringVal(s);
        }
        if (value instanceof Integer i) {
            return new IntVal(i);
        }
        if (value instanceof Long l) {
            return new LongVal(l);
        }
        if (value instanceof Boolean b) {
            return new BoolVal(b);
        }
        if (value instanceof Double d) {
            return new DoubleVal(d);
        }
        if (value instanceof Float f) {
            return new DoubleVal(f.doubleValue());
        }
        if (value instanceof Character c) {
            return new CharVal(c);
        }
        if (value instanceof Short s) {
            return new IntVal(s.intValue());
        }
        if (value instanceof Byte b) {
            return new IntVal(b.intValue());
        }
        if (value instanceof Class<?> c) {
            return new ClassVal(c.getName());
        }
        if (value instanceof Enum<?> e) {
            return new EnumVal(e.getClass().getName(), e.name());
        }
        if (value instanceof Annotation a) {
            return new AnnotationVal(a);
        }
        if (value instanceof List<?> list) {
            List<AnnotationValue> converted =
                    list.stream().map(AnnotationValue::from).toList();
            return new ArrayVal(converted);
        }
        if (value.getClass().isArray()) {
            return fromArray(value);
        }

        throw new IllegalArgumentException(
                "Unsupported annotation value type: " + value.getClass().getName());
    }

    /**
     * Converts an array value to ArrayVal.
     */
    private static ArrayVal fromArray(Object array) {
        if (array instanceof Object[] objArray) {
            List<AnnotationValue> values =
                    java.util.Arrays.stream(objArray).map(AnnotationValue::from).toList();
            return new ArrayVal(values);
        }
        if (array instanceof int[] intArray) {
            List<AnnotationValue> values = java.util.Arrays.stream(intArray)
                    .mapToObj(IntVal::new)
                    .map(v -> (AnnotationValue) v)
                    .toList();
            return new ArrayVal(values);
        }
        if (array instanceof long[] longArray) {
            List<AnnotationValue> values = java.util.Arrays.stream(longArray)
                    .mapToObj(LongVal::new)
                    .map(v -> (AnnotationValue) v)
                    .toList();
            return new ArrayVal(values);
        }
        if (array instanceof double[] doubleArray) {
            List<AnnotationValue> values = java.util.Arrays.stream(doubleArray)
                    .mapToObj(DoubleVal::new)
                    .map(v -> (AnnotationValue) v)
                    .toList();
            return new ArrayVal(values);
        }
        if (array instanceof boolean[] boolArray) {
            List<AnnotationValue> values = new java.util.ArrayList<>();
            for (boolean b : boolArray) {
                values.add(new BoolVal(b));
            }
            return new ArrayVal(values);
        }
        if (array instanceof char[] charArray) {
            List<AnnotationValue> values = new java.util.ArrayList<>();
            for (char c : charArray) {
                values.add(new CharVal(c));
            }
            return new ArrayVal(values);
        }
        if (array instanceof float[] floatArray) {
            List<AnnotationValue> values = new java.util.ArrayList<>();
            for (float f : floatArray) {
                values.add(new DoubleVal(f));
            }
            return new ArrayVal(values);
        }
        if (array instanceof short[] shortArray) {
            List<AnnotationValue> values = new java.util.ArrayList<>();
            for (short s : shortArray) {
                values.add(new IntVal(s));
            }
            return new ArrayVal(values);
        }
        if (array instanceof byte[] byteArray) {
            List<AnnotationValue> values = new java.util.ArrayList<>();
            for (byte b : byteArray) {
                values.add(new IntVal(b));
            }
            return new ArrayVal(values);
        }
        throw new IllegalArgumentException(
                "Unsupported array type: " + array.getClass().getName());
    }
}
