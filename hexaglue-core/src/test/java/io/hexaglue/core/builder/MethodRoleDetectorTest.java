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

package io.hexaglue.core.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.MethodRole;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.ParameterInfo;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MethodRoleDetector}.
 *
 * @since 5.0.0
 */
@DisplayName("MethodRoleDetector")
class MethodRoleDetectorTest {

    private MethodRoleDetector detector;
    private TypeNode declaringType;

    @BeforeEach
    void setUp() {
        detector = new MethodRoleDetector();
        declaringType = TypeNode.builder()
                .qualifiedName("com.example.Order")
                .simpleName("Order")
                .form(JavaForm.CLASS)
                .build();
    }

    @Nested
    @DisplayName("Getter Detection")
    class GetterDetection {

        @Test
        @DisplayName("should detect getX pattern as getter")
        void shouldDetectGetXPatternAsGetter() {
            MethodNode method = createMethod("getName", "java.lang.String", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.GETTER);
        }

        @Test
        @DisplayName("should detect isX pattern for boolean as getter")
        void shouldDetectIsXPatternForBooleanAsGetter() {
            MethodNode method = createMethod("isActive", "boolean", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.GETTER);
        }

        @Test
        @DisplayName("should detect isX pattern for Boolean as getter")
        void shouldDetectIsXPatternForBooleanWrapperAsGetter() {
            MethodNode method = createMethod("isEnabled", "java.lang.Boolean", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.GETTER);
        }

        @Test
        @DisplayName("should not detect get alone as getter")
        void shouldNotDetectGetAloneAsGetter() {
            MethodNode method = createMethod("get", "java.lang.String", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).doesNotContain(MethodRole.GETTER);
        }

        @Test
        @DisplayName("should not detect getX with parameters as getter")
        void shouldNotDetectGetXWithParametersAsGetter() {
            MethodNode method =
                    createMethod("getName", "java.lang.String", List.of(ParameterInfo.of("index", TypeRef.of("int"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).doesNotContain(MethodRole.GETTER);
        }

        @Test
        @DisplayName("should not detect void getX as getter")
        void shouldNotDetectVoidGetXAsGetter() {
            MethodNode method = createMethod("getName", "void", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).doesNotContain(MethodRole.GETTER);
        }
    }

    @Nested
    @DisplayName("Setter Detection")
    class SetterDetection {

        @Test
        @DisplayName("should detect setX pattern as setter")
        void shouldDetectSetXPatternAsSetter() {
            MethodNode method =
                    createMethod("setName", "void", List.of(ParameterInfo.of("name", TypeRef.of("java.lang.String"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.SETTER);
        }

        @Test
        @DisplayName("should not detect setX with no parameters as setter")
        void shouldNotDetectSetXWithNoParametersAsSetter() {
            MethodNode method = createMethod("setName", "void", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).doesNotContain(MethodRole.SETTER);
        }

        @Test
        @DisplayName("should not detect setX with return value as setter")
        void shouldNotDetectSetXWithReturnValueAsSetter() {
            MethodNode method = createMethod(
                    "setName", "com.example.Order", List.of(ParameterInfo.of("name", TypeRef.of("java.lang.String"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).doesNotContain(MethodRole.SETTER);
        }

        @Test
        @DisplayName("should not detect setX with multiple parameters as setter")
        void shouldNotDetectSetXWithMultipleParametersAsSetter() {
            MethodNode method = createMethod(
                    "setAddress",
                    "void",
                    List.of(
                            ParameterInfo.of("street", TypeRef.of("java.lang.String")),
                            ParameterInfo.of("city", TypeRef.of("java.lang.String"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).doesNotContain(MethodRole.SETTER);
        }
    }

    @Nested
    @DisplayName("Factory Detection")
    class FactoryDetection {

        @Test
        @DisplayName("should detect static of method as factory")
        void shouldDetectStaticOfMethodAsFactory() {
            MethodNode method = createStaticMethod("of", "com.example.Order", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.FACTORY);
        }

        @Test
        @DisplayName("should detect static create method as factory")
        void shouldDetectStaticCreateMethodAsFactory() {
            MethodNode method = createStaticMethod("create", "java.lang.String", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.FACTORY);
        }

        @Test
        @DisplayName("should detect static method returning own type as factory")
        void shouldDetectStaticMethodReturningOwnTypeAsFactory() {
            MethodNode method = createStaticMethod("fromString", "com.example.Order", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.FACTORY);
        }

        @Test
        @DisplayName("should not detect instance method as factory")
        void shouldNotDetectInstanceMethodAsFactory() {
            MethodNode method = createMethod("of", "com.example.Order", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).doesNotContain(MethodRole.FACTORY);
        }
    }

    @Nested
    @DisplayName("Object Method Detection")
    class ObjectMethodDetection {

        @Test
        @DisplayName("should detect equals method")
        void shouldDetectEqualsMethod() {
            MethodNode method =
                    createMethod("equals", "boolean", List.of(ParameterInfo.of("obj", TypeRef.of("java.lang.Object"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.OBJECT_METHOD);
        }

        @Test
        @DisplayName("should detect hashCode method")
        void shouldDetectHashCodeMethod() {
            MethodNode method = createMethod("hashCode", "int", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.OBJECT_METHOD);
        }

        @Test
        @DisplayName("should detect toString method")
        void shouldDetectToStringMethod() {
            MethodNode method = createMethod("toString", "java.lang.String", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.OBJECT_METHOD);
        }

        @Test
        @DisplayName("should not detect equals with wrong signature")
        void shouldNotDetectEqualsWithWrongSignature() {
            MethodNode method = createMethod(
                    "equals", "boolean", List.of(ParameterInfo.of("other", TypeRef.of("com.example.Order"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).doesNotContain(MethodRole.OBJECT_METHOD);
        }
    }

    @Nested
    @DisplayName("Lifecycle Detection")
    class LifecycleDetection {

        @Test
        @DisplayName("should detect @PostConstruct annotation")
        void shouldDetectPostConstructAnnotation() {
            MethodNode method = createMethodWithAnnotations(
                    "initialize", "void", List.of(), List.of(AnnotationRef.of("javax.annotation.PostConstruct")));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.LIFECYCLE);
        }

        @Test
        @DisplayName("should detect @PreDestroy annotation")
        void shouldDetectPreDestroyAnnotation() {
            MethodNode method = createMethodWithAnnotations(
                    "cleanup", "void", List.of(), List.of(AnnotationRef.of("jakarta.annotation.PreDestroy")));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.LIFECYCLE);
        }

        @Test
        @DisplayName("should detect init method by name")
        void shouldDetectInitMethodByName() {
            MethodNode method = createMethod("init", "void", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.LIFECYCLE);
        }

        @Test
        @DisplayName("should detect destroy method by name")
        void shouldDetectDestroyMethodByName() {
            MethodNode method = createMethod("destroy", "void", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.LIFECYCLE);
        }

        @Test
        @DisplayName("should detect close method by name")
        void shouldDetectCloseMethodByName() {
            MethodNode method = createMethod("close", "void", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.LIFECYCLE);
        }
    }

    @Nested
    @DisplayName("Validation Detection")
    class ValidationDetection {

        @Test
        @DisplayName("should detect validate method")
        void shouldDetectValidateMethod() {
            MethodNode method = createMethod("validate", "void", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.VALIDATION);
        }

        @Test
        @DisplayName("should detect validateOrder method")
        void shouldDetectValidateOrderMethod() {
            MethodNode method = createMethod("validateOrder", "void", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.VALIDATION);
        }

        @Test
        @DisplayName("should detect checkConstraints method")
        void shouldDetectCheckConstraintsMethod() {
            MethodNode method = createMethod("checkConstraints", "boolean", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.VALIDATION);
        }

        @Test
        @DisplayName("should detect ensureValid method")
        void shouldDetectEnsureValidMethod() {
            MethodNode method = createMethod("ensureValid", "void", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.VALIDATION);
        }

        @Test
        @DisplayName("should detect verifyState method")
        void shouldDetectVerifyStateMethod() {
            MethodNode method = createMethod("verifyState", "void", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.VALIDATION);
        }

        @Test
        @DisplayName("should detect isValid method as validation")
        void shouldDetectIsValidMethodAsValidation() {
            MethodNode method = createMethod("isValid", "boolean", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.VALIDATION);
        }
    }

    @Nested
    @DisplayName("Command and Query Detection")
    class CommandAndQueryDetection {

        @Test
        @DisplayName("should detect void method with params as command")
        void shouldDetectVoidMethodWithParamsAsCommand() {
            MethodNode method = createMethod(
                    "processPayment", "void", List.of(ParameterInfo.of("amount", TypeRef.of("java.math.BigDecimal"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.COMMAND);
        }

        @Test
        @DisplayName("should detect findById as query")
        void shouldDetectFindByIdAsQuery() {
            MethodNode method = createMethod(
                    "findById", "com.example.Order", List.of(ParameterInfo.of("id", TypeRef.of("java.lang.Long"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.QUERY);
        }

        @Test
        @DisplayName("should detect listAll as query")
        void shouldDetectListAllAsQuery() {
            MethodNode method = createMethod("listAll", "java.util.List", List.of());

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.QUERY);
        }

        @Test
        @DisplayName("should detect search as query")
        void shouldDetectSearchAsQuery() {
            MethodNode method = createMethod(
                    "searchByName",
                    "java.util.List",
                    List.of(ParameterInfo.of("name", TypeRef.of("java.lang.String"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.QUERY);
        }
    }

    @Nested
    @DisplayName("Business Method Detection")
    class BusinessMethodDetection {

        @Test
        @DisplayName("should detect non-accessor method as business")
        void shouldDetectNonAccessorMethodAsBusiness() {
            MethodNode method = createMethod(
                    "placeOrder",
                    "com.example.Order",
                    List.of(ParameterInfo.of("items", TypeRef.of("java.util.List"))));

            Set<MethodRole> roles = detector.detect(method, declaringType);

            assertThat(roles).contains(MethodRole.BUSINESS);
        }
    }

    @Nested
    @DisplayName("No Declaring Type")
    class NoDeclaringType {

        @Test
        @DisplayName("should work without declaring type")
        void shouldWorkWithoutDeclaringType() {
            MethodNode method = createMethod("getName", "java.lang.String", List.of());

            Set<MethodRole> roles = detector.detect(method, null);

            assertThat(roles).contains(MethodRole.GETTER);
        }
    }

    // Helper methods

    private MethodNode createMethod(String name, String returnType, List<ParameterInfo> parameters) {
        return createMethodWithAnnotations(name, returnType, parameters, List.of());
    }

    private MethodNode createMethodWithAnnotations(
            String name, String returnType, List<ParameterInfo> parameters, List<AnnotationRef> annotations) {
        return MethodNode.builder()
                .declaringTypeName("com.example.Order")
                .simpleName(name)
                .returnType(TypeRef.of(returnType))
                .parameters(parameters)
                .annotations(annotations)
                .build();
    }

    private MethodNode createStaticMethod(String name, String returnType, List<ParameterInfo> parameters) {
        return MethodNode.builder()
                .declaringTypeName("com.example.Order")
                .simpleName(name)
                .returnType(TypeRef.of(returnType))
                .parameters(parameters)
                .modifiers(Set.of(JavaModifier.PUBLIC, JavaModifier.STATIC))
                .build();
    }
}
