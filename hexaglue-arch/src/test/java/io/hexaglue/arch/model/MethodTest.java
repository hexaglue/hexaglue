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

import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Method}.
 *
 * @since 4.1.0
 */
@DisplayName("Method")
class MethodTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create method with name and return type")
        void shouldCreateWithNameAndReturnType() {
            // given
            String name = "findById";
            TypeRef returnType = TypeRef.of("com.example.Order");

            // when
            Method method = Method.of(name, returnType);

            // then
            assertThat(method.name()).isEqualTo(name);
            assertThat(method.returnType()).isEqualTo(returnType);
            assertThat(method.parameters()).isEmpty();
            assertThat(method.modifiers()).isEmpty();
            assertThat(method.annotations()).isEmpty();
            assertThat(method.documentation()).isEmpty();
            assertThat(method.thrownExceptions()).isEmpty();
        }

        @Test
        @DisplayName("should create method with all attributes")
        void shouldCreateWithAllAttributes() {
            // given
            String name = "findById";
            TypeRef returnType = TypeRef.of("java.util.Optional");
            List<Parameter> params = List.of(Parameter.of("id", TypeRef.of("java.lang.Long")));
            Set<Modifier> modifiers = Set.of(Modifier.PUBLIC);
            List<Annotation> annotations = List.of(Annotation.of("Override"));
            Optional<String> doc = Optional.of("Finds an order by ID");
            List<TypeRef> exceptions = List.of(TypeRef.of("com.example.OrderNotFoundException"));

            // when
            Method method = new Method(name, returnType, params, modifiers, annotations, doc, exceptions);

            // then
            assertThat(method.name()).isEqualTo(name);
            assertThat(method.returnType()).isEqualTo(returnType);
            assertThat(method.parameters()).hasSize(1);
            assertThat(method.modifiers()).containsExactly(Modifier.PUBLIC);
            assertThat(method.annotations()).hasSize(1);
            assertThat(method.documentation()).contains("Finds an order by ID");
            assertThat(method.thrownExceptions()).hasSize(1);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            TypeRef returnType = TypeRef.of("void");
            assertThatThrownBy(() -> Method.of(null, returnType))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            TypeRef returnType = TypeRef.of("void");
            assertThatThrownBy(() -> Method.of("  ", returnType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should reject null return type")
        void shouldRejectNullReturnType() {
            assertThatThrownBy(() -> Method.of("method", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("returnType");
        }
    }

    @Nested
    @DisplayName("Signature")
    class SignatureTests {

        @Test
        @DisplayName("should generate signature for no-arg method")
        void shouldGenerateSignatureForNoArgMethod() {
            // given
            Method method = Method.of("getName", TypeRef.of("java.lang.String"));

            // then
            assertThat(method.signature()).isEqualTo("getName()");
        }

        @Test
        @DisplayName("should generate signature with parameters")
        void shouldGenerateSignatureWithParameters() {
            // given
            Method method = new Method(
                    "setName",
                    TypeRef.primitive("void"),
                    List.of(Parameter.of("name", TypeRef.of("java.lang.String"))),
                    Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of());

            // then
            assertThat(method.signature()).isEqualTo("setName(String)");
        }

        @Test
        @DisplayName("should generate signature with multiple parameters")
        void shouldGenerateSignatureWithMultipleParameters() {
            // given
            Method method = new Method(
                    "transfer",
                    TypeRef.primitive("void"),
                    List.of(
                            Parameter.of("from", TypeRef.of("com.example.Account")),
                            Parameter.of("to", TypeRef.of("com.example.Account")),
                            Parameter.of("amount", TypeRef.of("java.math.BigDecimal"))),
                    Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of());

            // then
            assertThat(method.signature()).isEqualTo("transfer(Account, Account, BigDecimal)");
        }
    }

    @Nested
    @DisplayName("Modifier Checks")
    class ModifierChecks {

        @Test
        @DisplayName("should identify public method")
        void shouldIdentifyPublicMethod() {
            // given
            Method method = new Method(
                    "findById",
                    TypeRef.of("com.example.Order"),
                    List.of(),
                    Set.of(Modifier.PUBLIC),
                    List.of(),
                    Optional.empty(),
                    List.of());

            // then
            assertThat(method.isPublic()).isTrue();
            assertThat(method.isAbstract()).isFalse();
        }

        @Test
        @DisplayName("should identify abstract method")
        void shouldIdentifyAbstractMethod() {
            // given
            Method method = new Method(
                    "process",
                    TypeRef.primitive("void"),
                    List.of(),
                    Set.of(Modifier.PUBLIC, Modifier.ABSTRACT),
                    List.of(),
                    Optional.empty(),
                    List.of());

            // then
            assertThat(method.isPublic()).isTrue();
            assertThat(method.isAbstract()).isTrue();
        }

        @Test
        @DisplayName("should return false for non-public method")
        void shouldReturnFalseForNonPublicMethod() {
            // given
            Method method = new Method(
                    "helper",
                    TypeRef.primitive("void"),
                    List.of(),
                    Set.of(Modifier.PRIVATE),
                    List.of(),
                    Optional.empty(),
                    List.of());

            // then
            assertThat(method.isPublic()).isFalse();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return immutable parameters list")
        void shouldReturnImmutableParametersList() {
            // given
            List<Parameter> params =
                    new java.util.ArrayList<>(List.of(Parameter.of("id", TypeRef.of("java.lang.Long"))));
            Method method = new Method(
                    "findById",
                    TypeRef.of("com.example.Order"),
                    params,
                    Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of());

            // when/then
            assertThatThrownBy(() -> method.parameters().add(Parameter.of("extra", TypeRef.of("String"))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return immutable modifiers set")
        void shouldReturnImmutableModifiersSet() {
            // given
            Method method = new Method(
                    "findById",
                    TypeRef.of("com.example.Order"),
                    List.of(),
                    Set.of(Modifier.PUBLIC),
                    List.of(),
                    Optional.empty(),
                    List.of());

            // when/then
            assertThatThrownBy(() -> method.modifiers().add(Modifier.STATIC))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            Method m1 = Method.of("getName", TypeRef.of("java.lang.String"));
            Method m2 = Method.of("getName", TypeRef.of("java.lang.String"));

            // then
            assertThat(m1).isEqualTo(m2);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when names differ")
        void shouldNotBeEqualWhenNamesDiffer() {
            // given
            TypeRef returnType = TypeRef.of("java.lang.String");
            Method m1 = Method.of("getName", returnType);
            Method m2 = Method.of("getTitle", returnType);

            // then
            assertThat(m1).isNotEqualTo(m2);
        }
    }
}
