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

import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeForm;
import io.hexaglue.syntax.TypeSyntax;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtRecord;

@DisplayName("SpoonTypeSyntax")
class SpoonTypeSyntaxTest {

    private static CtModel model;

    @BeforeAll
    static void setUp() {
        Launcher launcher = new Launcher();
        launcher.addInputResource("src/test/java/io/hexaglue/syntax/spoon/fixtures");
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setComplianceLevel(17);
        launcher.buildModel();
        model = launcher.getModel();
    }

    @Nested
    @DisplayName("Identification")
    class IdentificationTest {

        @Test
        @DisplayName("should return qualified name")
        void shouldReturnQualifiedName() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("SimpleClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.qualifiedName()).isEqualTo("io.hexaglue.syntax.spoon.fixtures.SimpleClass");
        }

        @Test
        @DisplayName("should return simple name")
        void shouldReturnSimpleName() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("SimpleClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.simpleName()).isEqualTo("SimpleClass");
        }

        @Test
        @DisplayName("should return package name")
        void shouldReturnPackageName() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("SimpleClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.packageName()).isEqualTo("io.hexaglue.syntax.spoon.fixtures");
        }
    }

    @Nested
    @DisplayName("TypeForm")
    class TypeFormTest {

        @Test
        @DisplayName("should identify class")
        void shouldIdentifyClass() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("SimpleClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.form()).isEqualTo(TypeForm.CLASS);
            assertThat(syntax.isClass()).isTrue();
            assertThat(syntax.isInterface()).isFalse();
        }

        @Test
        @DisplayName("should identify interface")
        void shouldIdentifyInterface() {
            // given
            CtInterface<?> ctInterface = model.getElements(
                            (CtInterface<?> i) -> i.getSimpleName().equals("SimpleInterface"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctInterface);

            // then
            assertThat(syntax.form()).isEqualTo(TypeForm.INTERFACE);
            assertThat(syntax.isInterface()).isTrue();
        }

        @Test
        @DisplayName("should identify record")
        void shouldIdentifyRecord() {
            // given
            CtRecord ctRecord = model.getElements(
                            (CtRecord r) -> r.getSimpleName().equals("SimpleRecord"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctRecord);

            // then
            assertThat(syntax.form()).isEqualTo(TypeForm.RECORD);
            assertThat(syntax.isRecord()).isTrue();
        }

        @Test
        @DisplayName("should identify enum")
        void shouldIdentifyEnum() {
            // given
            CtEnum<?> ctEnum = model.getElements(
                            (CtEnum<?> e) -> e.getSimpleName().equals("SimpleEnum"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctEnum);

            // then
            assertThat(syntax.form()).isEqualTo(TypeForm.ENUM);
            assertThat(syntax.isEnum()).isTrue();
        }
    }

    @Nested
    @DisplayName("Modifiers")
    class ModifiersTest {

        @Test
        @DisplayName("should include public modifier")
        void shouldIncludePublic() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("SimpleClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.modifiers()).contains(Modifier.PUBLIC);
        }

        @Test
        @DisplayName("should include abstract modifier")
        void shouldIncludeAbstract() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("AbstractClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.modifiers()).contains(Modifier.ABSTRACT);
            assertThat(syntax.isAbstract()).isTrue();
        }

        @Test
        @DisplayName("should include final modifier")
        void shouldIncludeFinal() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("FinalClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.modifiers()).contains(Modifier.FINAL);
            assertThat(syntax.isFinal()).isTrue();
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class InheritanceTest {

        @Test
        @DisplayName("should return supertype")
        void shouldReturnSupertype() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ChildClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.superType()).isPresent();
            assertThat(syntax.superType().get().simpleName()).isEqualTo("SimpleClass");
        }

        @Test
        @DisplayName("should return implemented interfaces")
        void shouldReturnInterfaces() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ImplementingClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.interfaces()).hasSize(1);
            assertThat(syntax.interfaces().get(0).simpleName()).isEqualTo("SimpleInterface");
        }
    }

    @Nested
    @DisplayName("Members")
    class MembersTest {

        @Test
        @DisplayName("should return fields")
        void shouldReturnFields() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.fields()).hasSize(2);
        }

        @Test
        @DisplayName("should return methods")
        void shouldReturnMethods() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.methods()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should return constructors")
        void shouldReturnConstructors() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.constructors()).hasSizeGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Annotations")
    class AnnotationsTest {

        @Test
        @DisplayName("should return annotations")
        void shouldReturnAnnotations() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("AnnotatedClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.annotations()).isNotEmpty();
        }

        @Test
        @DisplayName("should check annotation presence")
        void shouldCheckAnnotationPresence() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("AnnotatedClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.hasAnnotation("java.lang.Deprecated")).isTrue();
        }

        @Test
        @DisplayName("should get specific annotation")
        void shouldGetAnnotation() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("AnnotatedClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.getAnnotation("java.lang.Deprecated")).isPresent();
        }
    }

    @Nested
    @DisplayName("SourceLocation")
    class SourceLocationTest {

        @Test
        @DisplayName("should return source location")
        void shouldReturnSourceLocation() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("SimpleClass"))
                    .get(0);
            TypeSyntax syntax = new SpoonTypeSyntax(ctClass);

            // then
            assertThat(syntax.sourceLocation()).isNotNull();
            assertThat(syntax.sourceLocation().line()).isGreaterThan(0);
        }
    }
}
