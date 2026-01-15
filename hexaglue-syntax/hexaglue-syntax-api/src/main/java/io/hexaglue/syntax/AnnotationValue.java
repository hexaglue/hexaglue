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

package io.hexaglue.syntax;

import java.util.List;
import java.util.Objects;

/**
 * Value of an annotation parameter.
 *
 * <p>Annotation values can be:</p>
 * <ul>
 *   <li>Primitives (int, boolean, etc.)</li>
 *   <li>Strings</li>
 *   <li>Enums</li>
 *   <li>Class references</li>
 *   <li>Nested annotations</li>
 *   <li>Arrays of the above</li>
 * </ul>
 *
 * @since 4.0.0
 */
public sealed interface AnnotationValue {

    /**
     * Returns the value kind.
     *
     * @return the kind of this value
     */
    Kind kind();

    /**
     * Kinds of annotation values.
     */
    enum Kind {
        PRIMITIVE,
        STRING,
        ENUM,
        CLASS,
        ANNOTATION,
        ARRAY
    }

    /**
     * A primitive value (int, boolean, char, etc.).
     *
     * @param value the primitive value boxed
     */
    record PrimitiveValue(Object value) implements AnnotationValue {
        @Override
        public Kind kind() {
            return Kind.PRIMITIVE;
        }

        /**
         * Returns the value as an int.
         *
         * @return the int value
         */
        public int asInt() {
            return ((Number) value).intValue();
        }

        /**
         * Returns the value as a boolean.
         *
         * @return the boolean value
         */
        public boolean asBoolean() {
            return (Boolean) value;
        }

        /**
         * Returns the value as a long.
         *
         * @return the long value
         */
        public long asLong() {
            return ((Number) value).longValue();
        }

        /**
         * Returns the value as a double.
         *
         * @return the double value
         */
        public double asDouble() {
            return ((Number) value).doubleValue();
        }

        /**
         * Returns the value as a char.
         *
         * @return the char value
         */
        public char asChar() {
            return (Character) value;
        }
    }

    /**
     * A String value.
     *
     * @param value the string value
     */
    record StringValue(String value) implements AnnotationValue {
        public StringValue {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public Kind kind() {
            return Kind.STRING;
        }
    }

    /**
     * An enum constant value.
     *
     * @param enumType the fully qualified enum type name
     * @param constantName the enum constant name
     */
    record EnumValue(String enumType, String constantName) implements AnnotationValue {
        public EnumValue {
            Objects.requireNonNull(enumType, "enumType");
            Objects.requireNonNull(constantName, "constantName");
        }

        @Override
        public Kind kind() {
            return Kind.ENUM;
        }

        /**
         * Returns the enum constant as an enum of the specified type.
         *
         * @param enumClass the enum class
         * @param <E> the enum type
         * @return the enum constant
         */
        @SuppressWarnings("unchecked")
        public <E extends Enum<E>> E asEnum(Class<E> enumClass) {
            return Enum.valueOf(enumClass, constantName);
        }
    }

    /**
     * A class reference value.
     *
     * @param typeRef the referenced type
     */
    record ClassValue(TypeRef typeRef) implements AnnotationValue {
        public ClassValue {
            Objects.requireNonNull(typeRef, "typeRef");
        }

        @Override
        public Kind kind() {
            return Kind.CLASS;
        }

        /**
         * Returns the qualified name of the class.
         *
         * @return the qualified class name
         */
        public String qualifiedName() {
            return typeRef.qualifiedName();
        }
    }

    /**
     * A nested annotation value.
     *
     * @param annotation the nested annotation
     */
    record AnnotationRefValue(AnnotationSyntax annotation) implements AnnotationValue {
        public AnnotationRefValue {
            Objects.requireNonNull(annotation, "annotation");
        }

        @Override
        public Kind kind() {
            return Kind.ANNOTATION;
        }
    }

    /**
     * An array of annotation values.
     *
     * @param values the array elements
     */
    record ArrayValue(List<AnnotationValue> values) implements AnnotationValue {
        public ArrayValue {
            values = values != null ? List.copyOf(values) : List.of();
        }

        @Override
        public Kind kind() {
            return Kind.ARRAY;
        }

        /**
         * Returns the values as strings (for String arrays).
         *
         * @return list of string values
         */
        public List<String> asStrings() {
            return values.stream()
                    .filter(v -> v instanceof StringValue)
                    .map(v -> ((StringValue) v).value())
                    .toList();
        }
    }

    // Factory methods

    /**
     * Creates a primitive value.
     *
     * @param value the primitive value
     * @return an AnnotationValue
     */
    static AnnotationValue ofPrimitive(Object value) {
        return new PrimitiveValue(value);
    }

    /**
     * Creates a string value.
     *
     * @param value the string
     * @return an AnnotationValue
     */
    static AnnotationValue ofString(String value) {
        return new StringValue(value);
    }

    /**
     * Creates an enum value.
     *
     * @param enumType the enum type qualified name
     * @param constantName the constant name
     * @return an AnnotationValue
     */
    static AnnotationValue ofEnum(String enumType, String constantName) {
        return new EnumValue(enumType, constantName);
    }

    /**
     * Creates a class value.
     *
     * @param typeRef the type reference
     * @return an AnnotationValue
     */
    static AnnotationValue ofClass(TypeRef typeRef) {
        return new ClassValue(typeRef);
    }

    /**
     * Creates an annotation value.
     *
     * @param annotation the nested annotation
     * @return an AnnotationValue
     */
    static AnnotationValue ofAnnotation(AnnotationSyntax annotation) {
        return new AnnotationRefValue(annotation);
    }

    /**
     * Creates an array value.
     *
     * @param values the array elements
     * @return an AnnotationValue
     */
    static AnnotationValue ofArray(List<AnnotationValue> values) {
        return new ArrayValue(values);
    }
}
