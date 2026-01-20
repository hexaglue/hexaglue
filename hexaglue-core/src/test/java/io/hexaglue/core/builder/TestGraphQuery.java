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

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.model.edges.FieldAccessEdge;
import io.hexaglue.core.graph.model.edges.MethodCallEdge;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.core.graph.style.PackageOrganizationStyle;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Test stub for {@link GraphQuery}.
 *
 * <p>Provides minimal implementation for testing purposes. Override methods as needed.
 *
 * @since 4.1.0
 */
class TestGraphQuery implements GraphQuery {

    @Override
    public Stream<TypeNode> types() {
        return Stream.empty();
    }

    @Override
    public Stream<TypeNode> types(Predicate<TypeNode> predicate) {
        return Stream.empty();
    }

    @Override
    public Stream<TypeNode> typesInPackage(String packageName) {
        return Stream.empty();
    }

    @Override
    public Stream<TypeNode> typesWithForm(JavaForm form) {
        return Stream.empty();
    }

    @Override
    public Stream<TypeNode> typesAnnotatedWith(String annotationQualifiedName) {
        return Stream.empty();
    }

    @Override
    public Stream<FieldNode> fields() {
        return Stream.empty();
    }

    @Override
    public Stream<FieldNode> fields(Predicate<FieldNode> predicate) {
        return Stream.empty();
    }

    @Override
    public Stream<MethodNode> methods() {
        return Stream.empty();
    }

    @Override
    public Stream<MethodNode> methods(Predicate<MethodNode> predicate) {
        return Stream.empty();
    }

    @Override
    public Stream<ConstructorNode> constructors() {
        return Stream.empty();
    }

    @Override
    public List<FieldNode> fieldsOf(TypeNode type) {
        return List.of();
    }

    @Override
    public List<MethodNode> methodsOf(TypeNode type) {
        return List.of();
    }

    @Override
    public List<ConstructorNode> constructorsOf(TypeNode type) {
        return List.of();
    }

    @Override
    public Optional<TypeNode> supertypeOf(TypeNode type) {
        return Optional.empty();
    }

    @Override
    public List<TypeNode> interfacesOf(TypeNode type) {
        return List.of();
    }

    @Override
    public List<TypeNode> subtypesOf(TypeNode type) {
        return List.of();
    }

    @Override
    public List<TypeNode> implementorsOf(TypeNode interfaceType) {
        return List.of();
    }

    @Override
    public List<TypeNode> usersInSignatureOf(TypeNode type) {
        return List.of();
    }

    @Override
    public Optional<TypeNode> type(String qualifiedName) {
        return Optional.empty();
    }

    @Override
    public Optional<TypeNode> type(NodeId id) {
        return Optional.empty();
    }

    @Override
    public Optional<FieldNode> field(NodeId id) {
        return Optional.empty();
    }

    @Override
    public Optional<MethodNode> method(NodeId id) {
        return Optional.empty();
    }

    @Override
    public PackageOrganizationStyle packageOrganizationStyle() {
        return PackageOrganizationStyle.UNKNOWN;
    }

    @Override
    public ConfidenceLevel styleConfidence() {
        return ConfidenceLevel.LOW;
    }

    @Override
    public List<MethodCallEdge> methodCallsFrom(NodeId nodeId) {
        return List.of();
    }

    @Override
    public List<MethodCallEdge> methodCallsTo(NodeId nodeId) {
        return List.of();
    }

    @Override
    public List<FieldAccessEdge> fieldAccessesFrom(NodeId nodeId) {
        return List.of();
    }

    @Override
    public List<FieldAccessEdge> fieldAccessesTo(NodeId nodeId) {
        return List.of();
    }

    @Override
    public List<TypeNode> transitiveDependencies(TypeNode type, int maxDepth) {
        return List.of();
    }

    @Override
    public List<TypeNode> transitiveDependents(TypeNode type, int maxDepth) {
        return List.of();
    }

    @Override
    public boolean hasCyclicDependency(TypeNode type) {
        return false;
    }

    @Override
    public Optional<List<TypeNode>> shortestPath(TypeNode from, TypeNode to) {
        return Optional.empty();
    }

    @Override
    public ApplicationGraph graph() {
        return null;
    }
}
