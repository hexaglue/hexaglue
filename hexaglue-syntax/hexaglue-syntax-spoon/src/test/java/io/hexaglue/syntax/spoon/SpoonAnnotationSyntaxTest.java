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

import io.hexaglue.syntax.AnnotationSyntax;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;

@DisplayName("SpoonAnnotationSyntax")
class SpoonAnnotationSyntaxTest {

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
        @DisplayName("should return annotation qualified name")
        void shouldReturnQualifiedName() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("AnnotatedClass"))
                    .get(0);
            CtAnnotation<?> annotation = ctClass.getAnnotations().stream()
                    .filter(a -> a.getAnnotationType().getSimpleName().equals("Deprecated"))
                    .findFirst()
                    .orElseThrow();
            AnnotationSyntax syntax = new SpoonAnnotationSyntax(annotation);

            // then
            assertThat(syntax.qualifiedName()).isEqualTo("java.lang.Deprecated");
        }

        @Test
        @DisplayName("should return annotation simple name")
        void shouldReturnSimpleName() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("AnnotatedClass"))
                    .get(0);
            CtAnnotation<?> annotation = ctClass.getAnnotations().stream()
                    .filter(a -> a.getAnnotationType().getSimpleName().equals("Deprecated"))
                    .findFirst()
                    .orElseThrow();
            AnnotationSyntax syntax = new SpoonAnnotationSyntax(annotation);

            // then
            assertThat(syntax.simpleName()).isEqualTo("Deprecated");
        }
    }

    @Nested
    @DisplayName("Values")
    class ValuesTest {

        @Test
        @DisplayName("should return annotation values for SuppressWarnings")
        void shouldReturnValues() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("AnnotatedClass"))
                    .get(0);
            CtAnnotation<?> annotation = ctClass.getAnnotations().stream()
                    .filter(a -> a.getAnnotationType().getSimpleName().equals("SuppressWarnings"))
                    .findFirst()
                    .orElseThrow();
            AnnotationSyntax syntax = new SpoonAnnotationSyntax(annotation);

            // then
            assertThat(syntax.values()).isNotEmpty();
            assertThat(syntax.hasValue("value")).isTrue();
        }

        @Test
        @DisplayName("should get String value from annotation")
        void shouldGetStringValue() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("AnnotatedClass"))
                    .get(0);
            CtAnnotation<?> annotation = ctClass.getAnnotations().stream()
                    .filter(a -> a.getAnnotationType().getSimpleName().equals("SuppressWarnings"))
                    .findFirst()
                    .orElseThrow();
            AnnotationSyntax syntax = new SpoonAnnotationSyntax(annotation);

            // then
            assertThat(syntax.getString("value")).isPresent();
            assertThat(syntax.getString("value").get()).isEqualTo("all");
        }
    }
}
