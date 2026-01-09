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

package io.hexaglue.core.enrichment;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.spi.enrichment.SemanticLabel;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BehavioralPatternEnricherTest {

    private ApplicationGraph graph;
    private BehavioralPatternEnricher enricher;

    @BeforeEach
    void setUp() {
        GraphMetadata metadata = GraphMetadata.of("com.example", 17, 0);
        graph = new ApplicationGraph(metadata);
        enricher = new BehavioralPatternEnricher(graph);
    }

    // === Factory method detection ===

    @Test
    void shouldDetectFactoryMethod() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode factoryMethod = createMethod(
                orderType,
                "createOrder",
                TypeRef.of("com.example.Order"),
                Set.of(JavaModifier.PUBLIC, JavaModifier.STATIC),
                List.of());

        addMethodToType(orderType, factoryMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.get(factoryMethod.qualifiedName())).contains(SemanticLabel.FACTORY_METHOD);
    }

    @Test
    void shouldNotDetectNonStaticMethodAsFactory() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode instanceMethod = createMethod(
                orderType, "getOrder", TypeRef.of("com.example.Order"), Set.of(JavaModifier.PUBLIC), List.of());

        addMethodToType(orderType, instanceMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.getOrDefault(instanceMethod.qualifiedName(), Set.of()))
                .doesNotContain(SemanticLabel.FACTORY_METHOD);
    }

    // === Invariant validator detection ===

    @Test
    void shouldDetectInvariantValidator() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode validateMethod = createMethod(
                orderType, "validateQuantity", TypeRef.of("void"), Set.of(JavaModifier.PRIVATE), List.of());

        addMethodToType(orderType, validateMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.get(validateMethod.qualifiedName())).contains(SemanticLabel.INVARIANT_VALIDATOR);
    }

    @Test
    void shouldDetectCheckPrefixAsValidator() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode checkMethod = createMethod(
                orderType, "checkInventory", TypeRef.of("boolean"), Set.of(JavaModifier.PUBLIC), List.of());

        addMethodToType(orderType, checkMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.get(checkMethod.qualifiedName())).contains(SemanticLabel.INVARIANT_VALIDATOR);
    }

    // === Collection manager detection ===

    @Test
    void shouldDetectCollectionManager() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode addMethod = createMethod(
                orderType,
                "addLineItem",
                TypeRef.of("void"),
                Set.of(JavaModifier.PUBLIC),
                List.of(new ParameterInfo("item", TypeRef.of("com.example.LineItem"), List.of())));

        addMethodToType(orderType, addMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.get(addMethod.qualifiedName())).contains(SemanticLabel.COLLECTION_MANAGER);
    }

    @Test
    void shouldNotDetectCollectionManagerWithoutParameters() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode addMethod =
                createMethod(orderType, "addAll", TypeRef.of("void"), Set.of(JavaModifier.PUBLIC), List.of());

        addMethodToType(orderType, addMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.getOrDefault(addMethod.qualifiedName(), Set.of()))
                .doesNotContain(SemanticLabel.COLLECTION_MANAGER);
    }

    // === Lifecycle method detection ===

    @Test
    void shouldDetectLifecycleMethod() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode activateMethod =
                createMethod(orderType, "activate", TypeRef.of("void"), Set.of(JavaModifier.PUBLIC), List.of());

        addMethodToType(orderType, activateMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.get(activateMethod.qualifiedName())).contains(SemanticLabel.LIFECYCLE_METHOD);
    }

    @Test
    void shouldDetectCancelAsLifecycle() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode cancelMethod =
                createMethod(orderType, "cancel", TypeRef.of("void"), Set.of(JavaModifier.PUBLIC), List.of());

        addMethodToType(orderType, cancelMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.get(cancelMethod.qualifiedName())).contains(SemanticLabel.LIFECYCLE_METHOD);
    }

    // === Getter/Setter detection ===

    @Test
    void shouldDetectGetter() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode getterMethod = createMethod(
                orderType, "getId", TypeRef.of("java.lang.String"), Set.of(JavaModifier.PUBLIC), List.of());

        addMethodToType(orderType, getterMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.get(getterMethod.qualifiedName())).contains(SemanticLabel.GETTER);
    }

    @Test
    void shouldDetectSetter() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        MethodNode setterMethod = createMethod(
                orderType,
                "setStatus",
                TypeRef.of("void"),
                Set.of(JavaModifier.PUBLIC),
                List.of(new ParameterInfo("status", TypeRef.of("java.lang.String"), List.of())));

        addMethodToType(orderType, setterMethod);

        var labels = enricher.enrichMethods();

        assertThat(labels.get(setterMethod.qualifiedName())).contains(SemanticLabel.SETTER);
    }

    // === Type-level detection ===

    @Test
    void shouldDetectImmutableType() {
        TypeNode valueType = createType("com.example.Money");
        graph.addNode(valueType);

        FieldNode amountField = createField(valueType, "amount", TypeRef.of("java.math.BigDecimal"), true);
        FieldNode currencyField = createField(valueType, "currency", TypeRef.of("java.util.Currency"), true);

        addFieldToType(valueType, amountField);
        addFieldToType(valueType, currencyField);

        var labels = enricher.enrichTypes();

        assertThat(labels.get(valueType.qualifiedName())).contains(SemanticLabel.IMMUTABLE_TYPE);
    }

    @Test
    void shouldNotDetectMutableTypeAsImmutable() {
        TypeNode orderType = createType("com.example.Order");
        graph.addNode(orderType);

        FieldNode statusField = createField(orderType, "status", TypeRef.of("java.lang.String"), false);

        addFieldToType(orderType, statusField);

        var labels = enricher.enrichTypes();

        assertThat(labels.getOrDefault(orderType.qualifiedName(), Set.of()))
                .doesNotContain(SemanticLabel.IMMUTABLE_TYPE);
    }

    // === Test utilities ===

    private TypeNode createType(String qualifiedName) {
        return TypeNode.builder()
                .qualifiedName(qualifiedName)
                .simpleName(extractSimpleName(qualifiedName))
                .packageName(extractPackageName(qualifiedName))
                .form(io.hexaglue.core.frontend.JavaForm.CLASS)
                .modifiers(Set.of(JavaModifier.PUBLIC))
                .build();
    }

    private MethodNode createMethod(
            TypeNode declaringType,
            String name,
            TypeRef returnType,
            Set<JavaModifier> modifiers,
            List<ParameterInfo> params) {
        MethodNode method = MethodNode.builder()
                .declaringTypeName(declaringType.qualifiedName())
                .simpleName(name)
                .returnType(returnType)
                .modifiers(modifiers)
                .parameters(params)
                .build();
        return method;
    }

    private void addMethodToType(TypeNode type, MethodNode method) {
        graph.addNode(method);
        graph.addEdge(Edge.raw(type.id(), method.id(), EdgeKind.DECLARES));
    }

    private FieldNode createField(TypeNode declaringType, String name, TypeRef type, boolean isFinal) {
        Set<JavaModifier> modifiers =
                isFinal ? Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL) : Set.of(JavaModifier.PRIVATE);

        return FieldNode.builder()
                .declaringTypeName(declaringType.qualifiedName())
                .simpleName(name)
                .type(type)
                .modifiers(modifiers)
                .build();
    }

    private void addFieldToType(TypeNode type, FieldNode field) {
        graph.addNode(field);
        graph.addEdge(Edge.raw(type.id(), field.id(), EdgeKind.DECLARES));
    }

    private String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
