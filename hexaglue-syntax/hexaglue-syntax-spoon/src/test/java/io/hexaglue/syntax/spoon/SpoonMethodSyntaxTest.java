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

import io.hexaglue.syntax.MethodSyntax;
import io.hexaglue.syntax.Modifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;

@DisplayName("SpoonMethodSyntax")
class SpoonMethodSyntaxTest {

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
        @DisplayName("should return method name")
        void shouldReturnMethodName() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtMethod<?> method = ctClass.getMethodsByName("getName").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.name()).isEqualTo("getName");
        }

        @Test
        @DisplayName("should return return type")
        void shouldReturnReturnType() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtMethod<?> method = ctClass.getMethodsByName("getName").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.returnType().simpleName()).isEqualTo("String");
        }

        @Test
        @DisplayName("should return void type")
        void shouldReturnVoidType() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtMethod<?> method = ctClass.getMethodsByName("setName").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.returnType().simpleName()).isEqualTo("void");
        }
    }

    @Nested
    @DisplayName("Parameters")
    class ParametersTest {

        @Test
        @DisplayName("should return no parameters for getter")
        void shouldReturnNoParameters() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtMethod<?> method = ctClass.getMethodsByName("getName").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.parameters()).isEmpty();
        }

        @Test
        @DisplayName("should return parameter for setter")
        void shouldReturnParameter() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtMethod<?> method = ctClass.getMethodsByName("setName").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.parameters()).hasSize(1);
            assertThat(syntax.parameters().get(0).name()).isEqualTo("name");
            assertThat(syntax.parameters().get(0).type().simpleName()).isEqualTo("String");
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
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtMethod<?> method = ctClass.getMethodsByName("getName").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.modifiers()).contains(Modifier.PUBLIC);
        }

        @Test
        @DisplayName("should detect abstract method")
        void shouldDetectAbstract() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("AbstractClass"))
                    .get(0);
            CtMethod<?> method = ctClass.getMethodsByName("abstractMethod").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.isAbstract()).isTrue();
            assertThat(syntax.modifiers()).contains(Modifier.ABSTRACT);
        }
    }

    @Nested
    @DisplayName("Signature")
    class SignatureTest {

        @Test
        @DisplayName("should return empty signature for no parameters")
        void shouldReturnEmptySignature() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtMethod<?> method = ctClass.getMethodsByName("getName").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.signature()).isEqualTo("()");
        }

        @Test
        @DisplayName("should return signature with parameter type")
        void shouldReturnSignatureWithParameter() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("ClassWithMembers"))
                    .get(0);
            CtMethod<?> method = ctClass.getMethodsByName("setName").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.signature()).isEqualTo("(String)");
        }
    }

    @Nested
    @DisplayName("InterfaceMethods")
    class InterfaceMethodsTest {

        @Test
        @DisplayName("should detect interface method")
        void shouldDetectInterfaceMethod() {
            // given
            CtInterface<?> ctInterface = model.getElements(
                            (CtInterface<?> i) -> i.getSimpleName().equals("SimpleInterface"))
                    .get(0);
            CtMethod<?> method = ctInterface.getMethodsByName("doSomething").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.name()).isEqualTo("doSomething");
            assertThat(syntax.isAbstract()).isTrue();
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
            CtMethod<?> method = ctClass.getMethodsByName("getName").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

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
            CtMethod<?> method = ctClass.getMethodsByName("getDocumentedField").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.documentation()).isPresent();
            assertThat(syntax.documentation().get()).contains("documented field");
            assertThat(syntax.documentation().get()).doesNotContain("@param");
            assertThat(syntax.documentation().get()).doesNotContain("@return");
        }

        @Test
        @DisplayName("should return empty when no documentation")
        void shouldReturnEmptyWhenNoDocumentation() {
            // given
            CtClass<?> ctClass = model.getElements(
                            (CtClass<?> c) -> c.getSimpleName().equals("DocumentedClass"))
                    .get(0);
            CtMethod<?> method =
                    ctClass.getMethodsByName("getUndocumentedField").get(0);
            MethodSyntax syntax = new SpoonMethodSyntax(method);

            // then
            assertThat(syntax.documentation()).isEmpty();
        }
    }
}
