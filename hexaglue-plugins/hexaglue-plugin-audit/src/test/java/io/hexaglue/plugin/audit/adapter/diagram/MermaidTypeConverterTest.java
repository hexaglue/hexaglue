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

package io.hexaglue.plugin.audit.adapter.diagram;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MermaidTypeConverter}.
 *
 * @since 5.0.0
 */
@DisplayName("MermaidTypeConverter")
class MermaidTypeConverterTest {

    @Nested
    @DisplayName("convert(TypeRef)")
    class ConvertTypeRef {

        @Test
        @DisplayName("should return 'void' for null TypeRef")
        void shouldReturnVoidForNull() {
            assertThat(MermaidTypeConverter.convert(null)).isEqualTo("void");
        }

        @Test
        @DisplayName("should convert simple type")
        void shouldConvertSimpleType() {
            TypeRef type = TypeRef.of("java.lang.String");
            assertThat(MermaidTypeConverter.convert(type)).isEqualTo("String");
        }

        @Test
        @DisplayName("should convert primitive type")
        void shouldConvertPrimitiveType() {
            TypeRef type = TypeRef.primitive("int");
            assertThat(MermaidTypeConverter.convert(type)).isEqualTo("int");
        }

        @Test
        @DisplayName("should convert List with single type argument")
        void shouldConvertListWithTypeArgument() {
            TypeRef inner = TypeRef.of("com.example.Order");
            TypeRef type = TypeRef.parameterized("java.util.List", List.of(inner));
            assertThat(MermaidTypeConverter.convert(type)).isEqualTo("List~Order~");
        }

        @Test
        @DisplayName("should convert Map with two type arguments")
        void shouldConvertMapWithTypeArguments() {
            TypeRef keyType = TypeRef.of("java.lang.String");
            TypeRef valueType = TypeRef.of("com.example.Order");
            TypeRef type = TypeRef.parameterized("java.util.Map", List.of(keyType, valueType));
            assertThat(MermaidTypeConverter.convert(type)).isEqualTo("Map~String, Order~");
        }

        @Test
        @DisplayName("should convert Optional with type argument")
        void shouldConvertOptionalWithTypeArgument() {
            TypeRef inner = TypeRef.of("com.example.Money");
            TypeRef type = TypeRef.parameterized("java.util.Optional", List.of(inner));
            assertThat(MermaidTypeConverter.convert(type)).isEqualTo("Optional~Money~");
        }

        @Test
        @DisplayName("should convert nested generic types")
        void shouldConvertNestedGenericTypes() {
            TypeRef innerMost = TypeRef.of("com.example.Item");
            TypeRef inner = TypeRef.parameterized("java.util.List", List.of(innerMost));
            TypeRef type = TypeRef.parameterized("java.util.Set", List.of(inner));
            assertThat(MermaidTypeConverter.convert(type)).isEqualTo("Set~List~Item~~");
        }
    }

    @Nested
    @DisplayName("convertString()")
    class ConvertString {

        @Test
        @DisplayName("should return 'void' for null")
        void shouldReturnVoidForNull() {
            assertThat(MermaidTypeConverter.convertString(null)).isEqualTo("void");
        }

        @Test
        @DisplayName("should return 'void' for empty string")
        void shouldReturnVoidForEmpty() {
            assertThat(MermaidTypeConverter.convertString("")).isEqualTo("void");
        }

        @Test
        @DisplayName("should return simple type as-is")
        void shouldReturnSimpleTypeAsIs() {
            assertThat(MermaidTypeConverter.convertString("String")).isEqualTo("String");
        }

        @Test
        @DisplayName("should convert generic type syntax")
        void shouldConvertGenericTypeSyntax() {
            assertThat(MermaidTypeConverter.convertString("List<Order>")).isEqualTo("List~Order~");
        }

        @Test
        @DisplayName("should convert Map type syntax")
        void shouldConvertMapTypeSyntax() {
            assertThat(MermaidTypeConverter.convertString("Map<String, Order>")).isEqualTo("Map~String, Order~");
        }
    }

    @Nested
    @DisplayName("visibilitySymbol()")
    class VisibilitySymbol {

        @Test
        @DisplayName("should return '+' for public")
        void shouldReturnPlusForPublic() {
            assertThat(MermaidTypeConverter.visibilitySymbol(Set.of(Modifier.PUBLIC)))
                    .isEqualTo("+");
        }

        @Test
        @DisplayName("should return '-' for private")
        void shouldReturnMinusForPrivate() {
            assertThat(MermaidTypeConverter.visibilitySymbol(Set.of(Modifier.PRIVATE)))
                    .isEqualTo("-");
        }

        @Test
        @DisplayName("should return '#' for protected")
        void shouldReturnHashForProtected() {
            assertThat(MermaidTypeConverter.visibilitySymbol(Set.of(Modifier.PROTECTED)))
                    .isEqualTo("#");
        }

        @Test
        @DisplayName("should return '~' for package-private (empty modifiers)")
        void shouldReturnTildeForPackagePrivate() {
            assertThat(MermaidTypeConverter.visibilitySymbol(Set.of())).isEqualTo("~");
        }

        @Test
        @DisplayName("should return '~' for null modifiers")
        void shouldReturnTildeForNull() {
            assertThat(MermaidTypeConverter.visibilitySymbol(null)).isEqualTo("~");
        }

        @Test
        @DisplayName("should prioritize visibility over other modifiers")
        void shouldPrioritizeVisibility() {
            assertThat(MermaidTypeConverter.visibilitySymbol(Set.of(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)))
                    .isEqualTo("+");
        }
    }

    @Nested
    @DisplayName("isStatic()")
    class IsStatic {

        @Test
        @DisplayName("should return true when STATIC modifier present")
        void shouldReturnTrueForStatic() {
            assertThat(MermaidTypeConverter.isStatic(Set.of(Modifier.STATIC))).isTrue();
        }

        @Test
        @DisplayName("should return false when STATIC modifier absent")
        void shouldReturnFalseWithoutStatic() {
            assertThat(MermaidTypeConverter.isStatic(Set.of(Modifier.PUBLIC, Modifier.FINAL)))
                    .isFalse();
        }

        @Test
        @DisplayName("should return false for empty modifiers")
        void shouldReturnFalseForEmpty() {
            assertThat(MermaidTypeConverter.isStatic(Set.of())).isFalse();
        }

        @Test
        @DisplayName("should return false for null modifiers")
        void shouldReturnFalseForNull() {
            assertThat(MermaidTypeConverter.isStatic(null)).isFalse();
        }
    }
}
