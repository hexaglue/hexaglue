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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TypeRef}.
 *
 * @since 5.0.0
 */
class TypeRefTest {

    @Nested
    @DisplayName("JVM binary name normalization")
    class JvmBinaryNameNormalizationTest {

        @Test
        @DisplayName("of() should normalize single nested type $ to .")
        void ofNormalizesSingleNestedType() {
            TypeRef typeRef = TypeRef.of("com.example.Outer$Inner");

            assertThat(typeRef.qualifiedName()).isEqualTo("com.example.Outer.Inner");
        }

        @Test
        @DisplayName("of() should normalize multiple nested type $ to .")
        void ofNormalizesMultipleNestedTypes() {
            TypeRef typeRef = TypeRef.of("com.example.A$B$C");

            assertThat(typeRef.qualifiedName()).isEqualTo("com.example.A.B.C");
        }

        @Test
        @DisplayName("constructor should normalize $ in qualifiedName")
        void constructorNormalizesDollarSign() {
            TypeRef typeRef = new TypeRef("A$B$C", "C", List.of(), false, false, 0);

            assertThat(typeRef.qualifiedName()).isEqualTo("A.B.C");
        }

        @Test
        @DisplayName("of() should leave regular qualified name unchanged")
        void ofLeavesRegularNameUnchanged() {
            TypeRef typeRef = TypeRef.of("java.lang.String");

            assertThat(typeRef.qualifiedName()).isEqualTo("java.lang.String");
        }

        @Test
        @DisplayName("primitive() should leave primitive name unchanged")
        void primitiveLeavesNameUnchanged() {
            TypeRef typeRef = TypeRef.primitive("int");

            assertThat(typeRef.qualifiedName()).isEqualTo("int");
        }

        @Test
        @DisplayName("parameterized() should normalize $ in qualifiedName")
        void parameterizedNormalizesDollarSign() {
            TypeRef typeRef = TypeRef.parameterized("com.example.Outer$Inner", List.of(TypeRef.of("java.lang.String")));

            assertThat(typeRef.qualifiedName()).isEqualTo("com.example.Outer.Inner");
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodsTest {

        @Test
        @DisplayName("of() should create simple type reference")
        void ofCreatesSimpleTypeRef() {
            TypeRef typeRef = TypeRef.of("com.example.Order");

            assertThat(typeRef.qualifiedName()).isEqualTo("com.example.Order");
            assertThat(typeRef.simpleName()).isEqualTo("Order");
            assertThat(typeRef.typeArguments()).isEmpty();
            assertThat(typeRef.isPrimitive()).isFalse();
            assertThat(typeRef.isArray()).isFalse();
        }

        @Test
        @DisplayName("of() should extract simple name from qualified name")
        void ofExtractsSimpleName() {
            TypeRef typeRef = TypeRef.of("java.util.UUID");

            assertThat(typeRef.simpleName()).isEqualTo("UUID");
        }

        @Test
        @DisplayName("of() should handle class without package")
        void ofHandlesNoPackage() {
            TypeRef typeRef = TypeRef.of("Order");

            assertThat(typeRef.qualifiedName()).isEqualTo("Order");
            assertThat(typeRef.simpleName()).isEqualTo("Order");
        }

        @Test
        @DisplayName("primitive() should create primitive type reference")
        void primitiveCreatesPrimitiveTypeRef() {
            TypeRef typeRef = TypeRef.primitive("int");

            assertThat(typeRef.qualifiedName()).isEqualTo("int");
            assertThat(typeRef.simpleName()).isEqualTo("int");
            assertThat(typeRef.isPrimitive()).isTrue();
        }

        @Test
        @DisplayName("parameterized() should create parameterized type reference")
        void parameterizedCreatesParameterizedTypeRef() {
            TypeRef elementType = TypeRef.of("com.example.Order");
            TypeRef listType = TypeRef.parameterized("java.util.List", List.of(elementType));

            assertThat(listType.qualifiedName()).isEqualTo("java.util.List");
            assertThat(listType.simpleName()).isEqualTo("List");
            assertThat(listType.typeArguments()).hasSize(1);
            assertThat(listType.typeArguments().get(0)).isEqualTo(elementType);
        }
    }

    @Nested
    @DisplayName("isParameterized()")
    class IsParameterizedTest {

        @Test
        @DisplayName("should return false for simple types")
        void returnsFalseForSimpleTypes() {
            TypeRef typeRef = TypeRef.of("com.example.Order");

            assertThat(typeRef.isParameterized()).isFalse();
        }

        @Test
        @DisplayName("should return true for parameterized types")
        void returnsTrueForParameterizedTypes() {
            TypeRef listType = TypeRef.parameterized("java.util.List", List.of(TypeRef.of("com.example.Order")));

            assertThat(listType.isParameterized()).isTrue();
        }
    }

    @Nested
    @DisplayName("toSourceString()")
    class ToSourceStringTest {

        @Test
        @DisplayName("should return simple name for non-generic types")
        void returnsSimpleName() {
            TypeRef typeRef = TypeRef.of("com.example.Order");

            assertThat(typeRef.toSourceString()).isEqualTo("Order");
        }

        @Test
        @DisplayName("should include type arguments for generic types")
        void includesTypeArguments() {
            TypeRef listType = TypeRef.parameterized("java.util.List", List.of(TypeRef.of("java.lang.String")));

            assertThat(listType.toSourceString()).isEqualTo("List<String>");
        }

        @Test
        @DisplayName("should include array dimensions")
        void includesArrayDimensions() {
            TypeRef arrayType = new TypeRef("java.lang.String", "String", List.of(), false, true, 2);

            assertThat(arrayType.toSourceString()).isEqualTo("String[][]");
        }
    }
}
