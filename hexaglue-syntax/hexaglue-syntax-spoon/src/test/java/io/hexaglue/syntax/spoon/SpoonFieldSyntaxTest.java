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

import io.hexaglue.syntax.FieldSyntax;
import io.hexaglue.syntax.Modifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;

@DisplayName("SpoonFieldSyntax")
class SpoonFieldSyntaxTest {

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
        @DisplayName("should return field name")
        void shouldReturnFieldName() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtField<?> field = ctClass.getField("name");
            FieldSyntax syntax = new SpoonFieldSyntax(field);

            // then
            assertThat(syntax.name()).isEqualTo("name");
        }

        @Test
        @DisplayName("should return field type")
        void shouldReturnFieldType() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtField<?> field = ctClass.getField("name");
            FieldSyntax syntax = new SpoonFieldSyntax(field);

            // then
            assertThat(syntax.type().simpleName()).isEqualTo("String");
            assertThat(syntax.type().qualifiedName()).isEqualTo("java.lang.String");
        }

        @Test
        @DisplayName("should return primitive field type")
        void shouldReturnPrimitiveType() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtField<?> field = ctClass.getField("value");
            FieldSyntax syntax = new SpoonFieldSyntax(field);

            // then
            assertThat(syntax.type().simpleName()).isEqualTo("int");
            assertThat(syntax.type().isPrimitive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Modifiers")
    class ModifiersTest {

        @Test
        @DisplayName("should include private modifier")
        void shouldIncludePrivate() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtField<?> field = ctClass.getField("name");
            FieldSyntax syntax = new SpoonFieldSyntax(field);

            // then
            assertThat(syntax.modifiers()).contains(Modifier.PRIVATE);
            assertThat(syntax.isPrivate()).isTrue();
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
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtField<?> field = ctClass.getField("name");
            FieldSyntax syntax = new SpoonFieldSyntax(field);

            // then
            assertThat(syntax.sourceLocation()).isNotNull();
            assertThat(syntax.sourceLocation().line()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Documentation")
    class DocumentationTest {

        @Test
        @DisplayName("should return documentation when present")
        void shouldReturnDocumentationWhenPresent() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("DocumentedClass"))
                    .get(0);
            CtField<?> field = ctClass.getField("documentedField");
            FieldSyntax syntax = new SpoonFieldSyntax(field);

            // then
            assertThat(syntax.documentation()).isPresent();
            assertThat(syntax.documentation().get()).contains("documented field");
        }

        @Test
        @DisplayName("should return empty when no documentation")
        void shouldReturnEmptyWhenNoDocumentation() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("DocumentedClass"))
                    .get(0);
            CtField<?> field = ctClass.getField("undocumentedField");
            FieldSyntax syntax = new SpoonFieldSyntax(field);

            // then
            assertThat(syntax.documentation()).isEmpty();
        }
    }
}
