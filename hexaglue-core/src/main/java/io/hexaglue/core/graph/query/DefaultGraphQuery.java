package io.hexaglue.core.graph.query;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.index.GraphIndexes;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.style.PackageOrganizationStyle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Default implementation of {@link GraphQuery}.
 */
public final class DefaultGraphQuery implements GraphQuery {

    private final ApplicationGraph graph;
    private final GraphIndexes indexes;

    public DefaultGraphQuery(ApplicationGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
        this.indexes = graph.indexes();
    }

    // === Type queries ===

    @Override
    public Stream<TypeNode> types() {
        return graph.typeNodes().stream();
    }

    @Override
    public Stream<TypeNode> types(Predicate<TypeNode> predicate) {
        return types().filter(predicate);
    }

    @Override
    public Stream<TypeNode> typesInPackage(String packageName) {
        return indexes.typesByPackage(packageName).stream().map(graph::typeNode).flatMap(Optional::stream);
    }

    @Override
    public Stream<TypeNode> typesWithForm(JavaForm form) {
        return indexes.typesByForm(form).stream().map(graph::typeNode).flatMap(Optional::stream);
    }

    @Override
    public Stream<TypeNode> typesAnnotatedWith(String annotationQualifiedName) {
        return indexes.byAnnotation(annotationQualifiedName).stream()
                .filter(NodeId::isType)
                .map(graph::typeNode)
                .flatMap(Optional::stream);
    }

    // === Member queries ===

    @Override
    public Stream<FieldNode> fields() {
        return graph.memberNodes().stream().filter(m -> m instanceof FieldNode).map(m -> (FieldNode) m);
    }

    @Override
    public Stream<FieldNode> fields(Predicate<FieldNode> predicate) {
        return fields().filter(predicate);
    }

    @Override
    public Stream<MethodNode> methods() {
        return graph.memberNodes().stream().filter(m -> m instanceof MethodNode).map(m -> (MethodNode) m);
    }

    @Override
    public Stream<MethodNode> methods(Predicate<MethodNode> predicate) {
        return methods().filter(predicate);
    }

    @Override
    public Stream<ConstructorNode> constructors() {
        return graph.memberNodes().stream()
                .filter(m -> m instanceof ConstructorNode)
                .map(m -> (ConstructorNode) m);
    }

    // === Relationship queries ===

    @Override
    public List<FieldNode> fieldsOf(TypeNode type) {
        return graph.fieldsOf(type);
    }

    @Override
    public List<MethodNode> methodsOf(TypeNode type) {
        return graph.methodsOf(type);
    }

    @Override
    public List<ConstructorNode> constructorsOf(TypeNode type) {
        return indexes.membersOf(type.id()).stream()
                .map(id -> graph.node(id).orElse(null))
                .filter(n -> n instanceof ConstructorNode)
                .map(n -> (ConstructorNode) n)
                .toList();
    }

    @Override
    public Optional<TypeNode> supertypeOf(TypeNode type) {
        return graph.supertypeOf(type);
    }

    @Override
    public List<TypeNode> interfacesOf(TypeNode type) {
        return graph.interfacesOf(type);
    }

    @Override
    public List<TypeNode> subtypesOf(TypeNode type) {
        return indexes.subtypesOf(type.id()).stream()
                .map(graph::typeNode)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<TypeNode> implementorsOf(TypeNode interfaceType) {
        return indexes.implementorsOf(interfaceType.id()).stream()
                .map(graph::typeNode)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<TypeNode> usersInSignatureOf(TypeNode type) {
        return indexes.interfacesUsingInSignature(type.id()).stream()
                .map(graph::typeNode)
                .flatMap(Optional::stream)
                .toList();
    }

    // === Lookup queries ===

    @Override
    public Optional<TypeNode> type(String qualifiedName) {
        return graph.typeNode(qualifiedName);
    }

    @Override
    public Optional<TypeNode> type(NodeId id) {
        return graph.typeNode(id);
    }

    @Override
    public Optional<FieldNode> field(NodeId id) {
        return graph.fieldNode(id);
    }

    @Override
    public Optional<MethodNode> method(NodeId id) {
        return graph.methodNode(id);
    }

    // === Style queries ===

    @Override
    public PackageOrganizationStyle packageOrganizationStyle() {
        return graph.metadata().style();
    }

    @Override
    public ConfidenceLevel styleConfidence() {
        return graph.metadata().styleConfidence();
    }

    // === Graph access ===

    @Override
    public ApplicationGraph graph() {
        return graph;
    }
}
