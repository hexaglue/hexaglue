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
import java.util.OptionalInt;
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
            Set<MethodRole> roles = Set.of(MethodRole.QUERY);

            // when
            Method method = new Method(
                    name, returnType, params, modifiers, annotations, doc, exceptions, roles, OptionalInt.empty());

            // then
            assertThat(method.name()).isEqualTo(name);
            assertThat(method.returnType()).isEqualTo(returnType);
            assertThat(method.parameters()).hasSize(1);
            assertThat(method.modifiers()).containsExactly(Modifier.PUBLIC);
            assertThat(method.annotations()).hasSize(1);
            assertThat(method.documentation()).contains("Finds an order by ID");
            assertThat(method.thrownExceptions()).hasSize(1);
            assertThat(method.roles()).containsExactly(MethodRole.QUERY);
            assertThat(method.cyclomaticComplexity()).isEmpty();
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
                    List.of(),
                    Set.of(),
                    OptionalInt.empty());

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
                    List.of(),
                    Set.of(),
                    OptionalInt.empty());

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
                    List.of(),
                    Set.of(),
                    OptionalInt.empty());

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
                    List.of(),
                    Set.of(),
                    OptionalInt.empty());

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
                    List.of(),
                    Set.of(),
                    OptionalInt.empty());

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
                    List.of(),
                    Set.of(),
                    OptionalInt.empty());

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
                    List.of(),
                    Set.of(),
                    OptionalInt.empty());

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

    @Nested
    @DisplayName("Method Roles")
    class MethodRoles {

        @Test
        @DisplayName("should create method with roles")
        void shouldCreateMethodWithRoles() {
            // given
            Method method = Method.of("getName", TypeRef.of("String"), Set.of(MethodRole.GETTER));

            // then
            assertThat(method.roles()).containsExactly(MethodRole.GETTER);
        }

        @Test
        @DisplayName("should check hasRole")
        void shouldCheckHasRole() {
            // given
            Method getter = Method.of("getName", TypeRef.of("String"), Set.of(MethodRole.GETTER));
            Method business = Method.of("process", TypeRef.of("void"), Set.of(MethodRole.BUSINESS));

            // then
            assertThat(getter.hasRole(MethodRole.GETTER)).isTrue();
            assertThat(getter.hasRole(MethodRole.SETTER)).isFalse();
            assertThat(business.hasRole(MethodRole.BUSINESS)).isTrue();
        }

        @Test
        @DisplayName("should identify getter method")
        void shouldIdentifyGetterMethod() {
            // given
            Method getter = Method.of("getName", TypeRef.of("String"), Set.of(MethodRole.GETTER));
            Method notGetter = Method.of("process", TypeRef.of("void"), Set.of(MethodRole.BUSINESS));

            // then
            assertThat(getter.isGetter()).isTrue();
            assertThat(notGetter.isGetter()).isFalse();
        }

        @Test
        @DisplayName("should identify setter method")
        void shouldIdentifySetterMethod() {
            // given
            Method setter = Method.of("setName", TypeRef.of("void"), Set.of(MethodRole.SETTER));
            Method notSetter = Method.of("getName", TypeRef.of("String"), Set.of(MethodRole.GETTER));

            // then
            assertThat(setter.isSetter()).isTrue();
            assertThat(notSetter.isSetter()).isFalse();
        }

        @Test
        @DisplayName("should identify factory method")
        void shouldIdentifyFactoryMethod() {
            // given
            Method factory = Method.of("of", TypeRef.of("Order"), Set.of(MethodRole.FACTORY));

            // then
            assertThat(factory.isFactory()).isTrue();
        }

        @Test
        @DisplayName("should identify business method")
        void shouldIdentifyBusinessMethod() {
            // given
            Method business = Method.of("placeOrder", TypeRef.of("Order"), Set.of(MethodRole.BUSINESS));

            // then
            assertThat(business.isBusiness()).isTrue();
        }

        @Test
        @DisplayName("should identify accessor method")
        void shouldIdentifyAccessorMethod() {
            // given
            Method getter = Method.of("getName", TypeRef.of("String"), Set.of(MethodRole.GETTER));
            Method query = Method.of("find", TypeRef.of("Order"), Set.of(MethodRole.QUERY));
            Method business = Method.of("process", TypeRef.of("void"), Set.of(MethodRole.BUSINESS));

            // then
            assertThat(getter.isAccessor()).isTrue();
            assertThat(query.isAccessor()).isTrue();
            assertThat(business.isAccessor()).isFalse();
        }

        @Test
        @DisplayName("should identify mutation method")
        void shouldIdentifyMutationMethod() {
            // given
            Method setter = Method.of("setName", TypeRef.of("void"), Set.of(MethodRole.SETTER));
            Method command = Method.of("create", TypeRef.of("void"), Set.of(MethodRole.COMMAND));
            Method business = Method.of("process", TypeRef.of("void"), Set.of(MethodRole.BUSINESS));
            Method query = Method.of("find", TypeRef.of("Order"), Set.of(MethodRole.QUERY));

            // then
            assertThat(setter.isMutation()).isTrue();
            assertThat(command.isMutation()).isTrue();
            assertThat(business.isMutation()).isTrue();
            assertThat(query.isMutation()).isFalse();
        }

        @Test
        @DisplayName("should identify object method")
        void shouldIdentifyObjectMethod() {
            // given
            Method equals = Method.of("equals", TypeRef.of("boolean"), Set.of(MethodRole.OBJECT_METHOD));
            Method business = Method.of("process", TypeRef.of("void"), Set.of(MethodRole.BUSINESS));

            // then
            assertThat(equals.isObjectMethod()).isTrue();
            assertThat(business.isObjectMethod()).isFalse();
        }

        @Test
        @DisplayName("should support multiple roles")
        void shouldSupportMultipleRoles() {
            // given
            Method method = new Method(
                    "findAndUpdate",
                    TypeRef.of("Order"),
                    List.of(),
                    Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    Set.of(MethodRole.QUERY, MethodRole.COMMAND),
                    OptionalInt.empty());

            // then
            assertThat(method.hasRole(MethodRole.QUERY)).isTrue();
            assertThat(method.hasRole(MethodRole.COMMAND)).isTrue();
            assertThat(method.isAccessor()).isTrue();
            assertThat(method.isMutation()).isTrue();
        }

        @Test
        @DisplayName("should return immutable roles set")
        void shouldReturnImmutableRolesSet() {
            // given
            Method method = Method.of("getName", TypeRef.of("String"), Set.of(MethodRole.GETTER));

            // when/then
            assertThatThrownBy(() -> method.roles().add(MethodRole.SETTER))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
