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

package io.hexaglue.syntax.spoon;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.syntax.SyntaxProvider;
import io.hexaglue.syntax.TypeForm;
import io.hexaglue.syntax.TypeSyntax;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Spoon syntax implementation.
 *
 * <p>These tests validate the complete flow from source code parsing
 * to syntax abstraction, ensuring all components work together correctly.</p>
 */
@DisplayName("Spoon Integration")
class SpoonIntegrationTest {

    private static SyntaxProvider provider;

    @BeforeAll
    static void setUp() {
        provider = SpoonSyntaxProvider.builder()
                .basePackage("io.hexaglue.syntax.spoon.fixtures")
                .sourceDirectory(Path.of("src/test/java/io/hexaglue/syntax/spoon/fixtures"))
                .build();
    }

    @Nested
    @DisplayName("Type Discovery")
    class TypeDiscoveryTest {

        @Test
        @DisplayName("should discover all type forms")
        void shouldDiscoverAllTypeForms() {
            // when
            List<TypeForm> forms =
                    provider.types().map(TypeSyntax::form).distinct().toList();

            // then
            assertThat(forms).contains(TypeForm.CLASS, TypeForm.INTERFACE, TypeForm.RECORD, TypeForm.ENUM);
        }

        @Test
        @DisplayName("should provide complete type information")
        void shouldProvideCompleteTypeInfo() {
            // given
            TypeSyntax type = provider.type("io.hexaglue.syntax.spoon.fixtures.ClassWithMembers")
                    .orElseThrow();

            // then
            assertThat(type.qualifiedName()).isEqualTo("io.hexaglue.syntax.spoon.fixtures.ClassWithMembers");
            assertThat(type.simpleName()).isEqualTo("ClassWithMembers");
            assertThat(type.packageName()).isEqualTo("io.hexaglue.syntax.spoon.fixtures");
            assertThat(type.form()).isEqualTo(TypeForm.CLASS);
        }
    }

    @Nested
    @DisplayName("Type Hierarchy")
    class TypeHierarchyTest {

        @Test
        @DisplayName("should resolve supertype")
        void shouldResolveSupertype() {
            // given
            TypeSyntax childClass = provider.type("io.hexaglue.syntax.spoon.fixtures.ChildClass")
                    .orElseThrow();

            // then
            assertThat(childClass.superType()).isPresent();
            assertThat(childClass.superType().get().simpleName()).isEqualTo("SimpleClass");
        }

        @Test
        @DisplayName("should resolve implemented interfaces")
        void shouldResolveInterfaces() {
            // given
            TypeSyntax implementing = provider.type("io.hexaglue.syntax.spoon.fixtures.ImplementingClass")
                    .orElseThrow();

            // then
            assertThat(implementing.interfaces()).hasSize(1);
            assertThat(implementing.interfaces().get(0).simpleName()).isEqualTo("SimpleInterface");
        }
    }

    @Nested
    @DisplayName("Member Access")
    class MemberAccessTest {

        @Test
        @DisplayName("should access fields by name")
        void shouldAccessFieldsByName() {
            // given
            TypeSyntax type = provider.type("io.hexaglue.syntax.spoon.fixtures.ClassWithMembers")
                    .orElseThrow();

            // then
            assertThat(type.getField("name")).isPresent();
            assertThat(type.getField("name").get().type().simpleName()).isEqualTo("String");
            assertThat(type.getField("value")).isPresent();
            assertThat(type.getField("value").get().type().simpleName()).isEqualTo("int");
        }

        @Test
        @DisplayName("should access methods by name")
        void shouldAccessMethodsByName() {
            // given
            TypeSyntax type = provider.type("io.hexaglue.syntax.spoon.fixtures.ClassWithMembers")
                    .orElseThrow();

            // then
            assertThat(type.getMethod("getName")).isPresent();
            assertThat(type.getMethod("getName").get().returnType().simpleName())
                    .isEqualTo("String");
            assertThat(type.getMethod("setName")).isPresent();
            assertThat(type.getMethod("setName").get().parameters()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Annotation Access")
    class AnnotationAccessTest {

        @Test
        @DisplayName("should check annotation presence")
        void shouldCheckAnnotationPresence() {
            // given
            TypeSyntax type = provider.type("io.hexaglue.syntax.spoon.fixtures.AnnotatedClass")
                    .orElseThrow();

            // then
            assertThat(type.hasAnnotation("java.lang.Deprecated")).isTrue();
            assertThat(type.hasAnnotation("java.lang.SuppressWarnings")).isTrue();
            assertThat(type.hasAnnotation("java.lang.Override")).isFalse();
        }

        @Test
        @DisplayName("should access annotation values")
        void shouldAccessAnnotationValues() {
            // given
            TypeSyntax type = provider.type("io.hexaglue.syntax.spoon.fixtures.AnnotatedClass")
                    .orElseThrow();

            // then
            assertThat(type.getAnnotation("java.lang.SuppressWarnings")).isPresent();
            var annotation = type.getAnnotation("java.lang.SuppressWarnings").get();
            assertThat(annotation.getString("value")).isPresent();
            assertThat(annotation.getString("value").get()).isEqualTo("all");
        }
    }

    @Nested
    @DisplayName("Record Support")
    class RecordSupportTest {

        @Test
        @DisplayName("should parse record type")
        void shouldParseRecord() {
            // given
            TypeSyntax record = provider.type("io.hexaglue.syntax.spoon.fixtures.SimpleRecord")
                    .orElseThrow();

            // then
            assertThat(record.form()).isEqualTo(TypeForm.RECORD);
            assertThat(record.isRecord()).isTrue();
        }
    }

    @Nested
    @DisplayName("Enum Support")
    class EnumSupportTest {

        @Test
        @DisplayName("should parse enum type")
        void shouldParseEnum() {
            // given
            TypeSyntax enumType = provider.type("io.hexaglue.syntax.spoon.fixtures.SimpleEnum")
                    .orElseThrow();

            // then
            assertThat(enumType.form()).isEqualTo(TypeForm.ENUM);
            assertThat(enumType.isEnum()).isTrue();
        }
    }

    @Nested
    @DisplayName("Source Location")
    class SourceLocationTest {

        @Test
        @DisplayName("should provide source location for all types")
        void shouldProvideSourceLocation() {
            // then
            assertThat(provider.types()).allMatch(t -> t.sourceLocation().isKnown());
        }

        @Test
        @DisplayName("should provide valid line numbers")
        void shouldProvideValidLineNumbers() {
            // then
            assertThat(provider.types()).allMatch(t -> t.sourceLocation().line() > 0);
        }
    }
}
