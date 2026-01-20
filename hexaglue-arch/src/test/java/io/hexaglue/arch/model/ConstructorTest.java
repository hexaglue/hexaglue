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
 * Tests for {@link Constructor}.
 *
 * @since 4.1.0
 */
@DisplayName("Constructor")
class ConstructorTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create constructor with parameters")
        void shouldCreateWithParameters() {
            // given
            List<Parameter> params = List.of(Parameter.of("name", TypeRef.of("java.lang.String")));

            // when
            Constructor ctor = Constructor.of(params);

            // then
            assertThat(ctor.parameters()).hasSize(1);
            assertThat(ctor.modifiers()).isEmpty();
            assertThat(ctor.annotations()).isEmpty();
            assertThat(ctor.documentation()).isEmpty();
            assertThat(ctor.thrownExceptions()).isEmpty();
        }

        @Test
        @DisplayName("should create no-arg constructor")
        void shouldCreateNoArgConstructor() {
            // when
            Constructor ctor = Constructor.noArg();

            // then
            assertThat(ctor.parameters()).isEmpty();
        }

        @Test
        @DisplayName("should create constructor with all attributes")
        void shouldCreateWithAllAttributes() {
            // given
            List<Parameter> params = List.of(Parameter.of("value", TypeRef.of("java.lang.String")));
            Set<Modifier> modifiers = Set.of(Modifier.PUBLIC);
            List<Annotation> annotations = List.of(Annotation.of("javax.inject.Inject"));
            Optional<String> doc = Optional.of("Creates a new instance");
            List<TypeRef> exceptions = List.of(TypeRef.of("java.lang.IllegalArgumentException"));

            // when
            Constructor ctor = new Constructor(params, modifiers, annotations, doc, exceptions);

            // then
            assertThat(ctor.parameters()).hasSize(1);
            assertThat(ctor.modifiers()).containsExactly(Modifier.PUBLIC);
            assertThat(ctor.annotations()).hasSize(1);
            assertThat(ctor.documentation()).contains("Creates a new instance");
            assertThat(ctor.thrownExceptions()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Signature")
    class SignatureTests {

        @Test
        @DisplayName("should generate signature for no-arg constructor")
        void shouldGenerateSignatureForNoArgConstructor() {
            // given
            Constructor ctor = Constructor.noArg();

            // then
            assertThat(ctor.signature()).isEqualTo("()");
        }

        @Test
        @DisplayName("should generate signature with parameters")
        void shouldGenerateSignatureWithParameters() {
            // given
            Constructor ctor = Constructor.of(List.of(Parameter.of("name", TypeRef.of("java.lang.String"))));

            // then
            assertThat(ctor.signature()).isEqualTo("(String)");
        }

        @Test
        @DisplayName("should generate signature with multiple parameters")
        void shouldGenerateSignatureWithMultipleParameters() {
            // given
            Constructor ctor = Constructor.of(List.of(
                    Parameter.of("id", TypeRef.of("java.lang.Long")),
                    Parameter.of("name", TypeRef.of("java.lang.String")),
                    Parameter.of("price", TypeRef.of("java.math.BigDecimal"))));

            // then
            assertThat(ctor.signature()).isEqualTo("(Long, String, BigDecimal)");
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
            Constructor ctor = new Constructor(params, Set.of(), List.of(), Optional.empty(), List.of());

            // when/then
            assertThatThrownBy(() -> ctor.parameters().add(Parameter.of("extra", TypeRef.of("String"))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should not be affected by original list modifications")
        void shouldNotBeAffectedByOriginalListModifications() {
            // given
            List<Parameter> params =
                    new java.util.ArrayList<>(List.of(Parameter.of("id", TypeRef.of("java.lang.Long"))));
            Constructor ctor = new Constructor(params, Set.of(), List.of(), Optional.empty(), List.of());

            // when
            params.add(Parameter.of("extra", TypeRef.of("String")));

            // then
            assertThat(ctor.parameters()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            Constructor c1 = Constructor.noArg();
            Constructor c2 = Constructor.noArg();

            // then
            assertThat(c1).isEqualTo(c2);
            assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when parameters differ")
        void shouldNotBeEqualWhenParametersDiffer() {
            // given
            Constructor c1 = Constructor.noArg();
            Constructor c2 = Constructor.of(List.of(Parameter.of("id", TypeRef.of("java.lang.Long"))));

            // then
            assertThat(c1).isNotEqualTo(c2);
        }
    }
}
