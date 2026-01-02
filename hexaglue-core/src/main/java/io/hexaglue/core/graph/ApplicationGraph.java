package io.hexaglue.core.graph;

import io.hexaglue.core.graph.index.GraphIndexes;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.query.DefaultGraphQuery;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.*;

/**
 * The complete application graph containing all types, members, and relationships.
 *
 * <p>The graph maintains several invariants:
 * <ul>
 *   <li><b>G-1</b>: Edge endpoints must exist as nodes before the edge is added</li>
 *   <li><b>G-2</b>: Node ids are unique - adding a duplicate throws an exception</li>
 *   <li><b>G-3</b>: The graph is append-only - nodes and edges cannot be removed</li>
 * </ul>
 *
 * <p>Nodes are stored in insertion order for deterministic iteration.
 */
public final class ApplicationGraph {

    private final Map<NodeId, Node> nodes = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private final GraphIndexes indexes = new GraphIndexes();
    private GraphMetadata metadata;

    /**
     * Creates a new application graph with the given metadata.
     */
    public ApplicationGraph(GraphMetadata metadata) {
        this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
    }

    /**
     * Adds a node to the graph.
     *
     * @param node the node to add
     * @throws IllegalArgumentException if a node with the same id already exists (invariant G-2)
     */
    public void addNode(Node node) {
        Objects.requireNonNull(node, "node cannot be null");

        // Invariant G-2: No duplicate node ids
        if (nodes.containsKey(node.id())) {
            throw new IllegalArgumentException("Duplicate node id: " + node.id());
        }

        nodes.put(node.id(), node);
        indexes.indexNode(node);
    }

    /**
     * Adds an edge to the graph.
     *
     * @param edge the edge to add
     * @throws IllegalArgumentException if either endpoint doesn't exist (invariant G-1)
     */
    public void addEdge(Edge edge) {
        Objects.requireNonNull(edge, "edge cannot be null");

        // Invariant G-1: Both endpoints must exist
        if (!nodes.containsKey(edge.from())) {
            throw new IllegalArgumentException("Edge from unknown node: " + edge.from());
        }
        if (!nodes.containsKey(edge.to())) {
            throw new IllegalArgumentException("Edge to unknown node: " + edge.to());
        }

        edges.add(edge);
        indexes.indexEdge(edge);
    }

    // === Node access ===

    /**
     * Returns the node with the given id, or empty if not found.
     */
    public Optional<Node> node(NodeId id) {
        return Optional.ofNullable(nodes.get(id));
    }

    /**
     * Returns the type node with the given id, or empty if not found or not a type.
     */
    public Optional<TypeNode> typeNode(NodeId id) {
        Node node = nodes.get(id);
        return node instanceof TypeNode t ? Optional.of(t) : Optional.empty();
    }

    /**
     * Returns the type node with the given qualified name, or empty if not found.
     */
    public Optional<TypeNode> typeNode(String qualifiedName) {
        return typeNode(NodeId.type(qualifiedName));
    }

    /**
     * Returns the field node with the given id, or empty if not found or not a field.
     */
    public Optional<FieldNode> fieldNode(NodeId id) {
        Node node = nodes.get(id);
        return node instanceof FieldNode f ? Optional.of(f) : Optional.empty();
    }

    /**
     * Returns the method node with the given id, or empty if not found or not a method.
     */
    public Optional<MethodNode> methodNode(NodeId id) {
        Node node = nodes.get(id);
        return node instanceof MethodNode m ? Optional.of(m) : Optional.empty();
    }

    /**
     * Returns true if a node with the given id exists.
     */
    public boolean containsNode(NodeId id) {
        return nodes.containsKey(id);
    }

    /**
     * Returns all nodes in insertion order.
     */
    public Collection<Node> nodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Returns all type nodes in insertion order.
     */
    public List<TypeNode> typeNodes() {
        return nodes.values().stream()
                .filter(n -> n instanceof TypeNode)
                .map(n -> (TypeNode) n)
                .toList();
    }

    /**
     * Returns all member nodes (fields, methods, constructors).
     */
    public List<MemberNode> memberNodes() {
        return nodes.values().stream()
                .filter(n -> n instanceof MemberNode)
                .map(n -> (MemberNode) n)
                .toList();
    }

    // === Edge access ===

    /**
     * Returns all edges in insertion order.
     */
    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    /**
     * Returns all edges of the given kind.
     */
    public List<Edge> edges(EdgeKind kind) {
        return edges.stream().filter(e -> e.kind() == kind).toList();
    }

    /**
     * Returns all edges from the given node.
     */
    public List<Edge> edgesFrom(NodeId nodeId) {
        return edges.stream().filter(e -> e.from().equals(nodeId)).toList();
    }

    /**
     * Returns all edges to the given node.
     */
    public List<Edge> edgesTo(NodeId nodeId) {
        return edges.stream().filter(e -> e.to().equals(nodeId)).toList();
    }

    /**
     * Returns all RAW edges.
     */
    public List<Edge> rawEdges() {
        return edges.stream().filter(Edge::isRaw).toList();
    }

    /**
     * Returns all DERIVED edges.
     */
    public List<Edge> derivedEdges() {
        return edges.stream().filter(Edge::isDerived).toList();
    }

    /**
     * Checks if an edge with the given from, to, and kind already exists.
     */
    public boolean containsEdge(NodeId from, NodeId to, EdgeKind kind) {
        return edges.stream().anyMatch(e -> e.from().equals(from) && e.to().equals(to) && e.kind() == kind);
    }

    // === Indexes access ===

    /**
     * Returns the graph indexes for fast queries.
     */
    public GraphIndexes indexes() {
        return indexes;
    }

    /**
     * Returns a query interface for this graph.
     */
    public GraphQuery query() {
        return new DefaultGraphQuery(this);
    }

    // === Metadata and statistics ===

    /**
     * Returns the graph metadata.
     */
    public GraphMetadata metadata() {
        return metadata;
    }

    /**
     * Updates the graph metadata.
     *
     * <p><b>Internal use only.</b> This method is called by GraphBuilder
     * after style detection to enrich the metadata with detected style information.
     * Do not call this method from external code.
     *
     * @param metadata the new metadata
     */
    public void setMetadata(GraphMetadata metadata) {
        this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
    }

    /**
     * Returns the number of nodes.
     */
    public int nodeCount() {
        return nodes.size();
    }

    /**
     * Returns the number of edges.
     */
    public int edgeCount() {
        return edges.size();
    }

    /**
     * Returns the number of type nodes.
     */
    public int typeCount() {
        return indexes.typeCount();
    }

    /**
     * Returns the number of member nodes.
     */
    public int memberCount() {
        return indexes.memberCount();
    }

    // === Convenience query methods ===

    /**
     * Returns the members of the given type.
     */
    public List<MemberNode> membersOf(TypeNode type) {
        return indexes.membersOf(type.id()).stream()
                .map(nodes::get)
                .filter(n -> n instanceof MemberNode)
                .map(n -> (MemberNode) n)
                .toList();
    }

    /**
     * Returns the fields of the given type.
     */
    public List<FieldNode> fieldsOf(TypeNode type) {
        return indexes.membersOf(type.id()).stream()
                .map(nodes::get)
                .filter(n -> n instanceof FieldNode)
                .map(n -> (FieldNode) n)
                .toList();
    }

    /**
     * Returns the methods of the given type.
     */
    public List<MethodNode> methodsOf(TypeNode type) {
        return indexes.membersOf(type.id()).stream()
                .map(nodes::get)
                .filter(n -> n instanceof MethodNode)
                .map(n -> (MethodNode) n)
                .toList();
    }

    /**
     * Returns the supertype of the given type, if any.
     */
    public Optional<TypeNode> supertypeOf(TypeNode type) {
        return indexes.supertypesOf(type.id()).stream().findFirst().flatMap(this::typeNode);
    }

    /**
     * Returns the interfaces implemented by the given type.
     */
    public List<TypeNode> interfacesOf(TypeNode type) {
        return indexes.interfacesOf(type.id()).stream()
                .map(this::typeNode)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public String toString() {
        return "ApplicationGraph[nodes=" + nodeCount() + ", edges=" + edgeCount() + ", types=" + typeCount()
                + ", members=" + memberCount() + "]";
    }
}
