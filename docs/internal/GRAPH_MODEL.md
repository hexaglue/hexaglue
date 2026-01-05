# Graph Model

***Internal documentation for the ApplicationGraph data structure.***

---

## Overview

The **ApplicationGraph** is the central data structure in HexaGlue's analysis engine. It represents the complete structure of a Java application as a directed graph of nodes (types, fields, methods) and edges (relationships between them).

**Key characteristics**:
- **Immutable after construction** (append-only during build)
- **Indexed** for fast queries
- **Deterministic** (insertion order preserved)

---

## Graph Invariants

The ApplicationGraph maintains three invariants:

| Invariant | Rule | Enforced By |
|-----------|------|-------------|
| **G-1** | Edge endpoints must exist as nodes before the edge is added | `addEdge()` throws if endpoints missing |
| **G-2** | Node IDs are unique - adding a duplicate throws an exception | `addNode()` throws if ID exists |
| **G-3** | The graph is append-only - nodes and edges cannot be removed | No `remove` methods exist |

These invariants ensure:
- Graph consistency (no dangling edges)
- Deterministic behavior (same input → same graph)
- Safe concurrent reads after construction

---

## Node Types

### Node Hierarchy

```
                    Node (abstract)
                      │
         ┌────────────┴────────────┐
         │                         │
     TypeNode                 MemberNode (abstract)
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
                FieldNode    MethodNode    ConstructorNode
```

### NodeId Format

Every node has a unique `NodeId` with format `kind:qualified-path`:

| Kind | Format | Example |
|------|--------|---------|
| `type:` | `type:fqn` | `type:com.example.Order` |
| `field:` | `field:fqn#name` | `field:com.example.Order#id` |
| `method:` | `method:fqn#name(params)` | `method:com.example.Order#getId()` |
| `ctor:` | `ctor:fqn#<init>(params)` | `ctor:com.example.Order#<init>(OrderId)` |

### TypeNode

Represents a Java type (class, interface, record, enum, annotation).

```java
TypeNode {
    NodeId id
    String simpleName          // "Order"
    String qualifiedName       // "com.example.Order"
    String packageName         // "com.example"
    JavaForm form              // CLASS, INTERFACE, RECORD, ENUM, ANNOTATION
    Set<JavaModifier> modifiers // PUBLIC, ABSTRACT, FINAL, etc.
    TypeRef superType          // nullable
    List<TypeRef> interfaces
    List<AnnotationRef> annotations
    SourceRef sourceRef        // file:line:column
}
```

**JavaForm values**:
- `CLASS` - Regular class
- `INTERFACE` - Interface
- `RECORD` - Java record
- `ENUM` - Enumeration
- `ANNOTATION` - Annotation type

### FieldNode

Represents a field or record component.

```java
FieldNode {
    NodeId id
    String simpleName          // "id"
    TypeRef type               // "OrderId"
    Set<JavaModifier> modifiers
    List<AnnotationRef> annotations
}
```

### MethodNode

Represents a method.

```java
MethodNode {
    NodeId id
    String simpleName          // "findById"
    TypeRef returnType
    List<ParameterInfo> parameters
    Set<JavaModifier> modifiers
    List<AnnotationRef> annotations
    List<TypeRef> thrownTypes
}
```

---

## Edge Types

### Edge Structure

```java
Edge {
    NodeId from         // source node
    NodeId to           // target node
    EdgeKind kind       // relationship type
    EdgeOrigin origin   // RAW or DERIVED
    EdgeProof proof     // explanation (required for DERIVED)
}
```

### EdgeOrigin

| Origin | Description | Proof Required |
|--------|-------------|----------------|
| `RAW` | Extracted directly from AST | No |
| `DERIVED` | Computed from RAW edges | Yes |

### EdgeKind Reference

#### RAW Edges (from AST)

| Kind | From → To | Description |
|------|-----------|-------------|
| `EXTENDS` | Type → Supertype | Class extends superclass |
| `IMPLEMENTS` | Type → Interface | Class implements interface |
| `DECLARES` | Type → Member | Type declares field/method/constructor |
| `FIELD_TYPE` | Field → Type | Field has this type |
| `RETURN_TYPE` | Method → Type | Method returns this type |
| `PARAMETER_TYPE` | Method → Type | Method has parameter of this type |
| `THROWS` | Method → Type | Method throws this exception |
| `TYPE_ARGUMENT` | Node → Type | Generic type argument (e.g., `List<Order>` → `Order`) |
| `ANNOTATED_BY` | Node → Type | Element annotated with this annotation |
| `HAS_PARAMETER` | Method → Type | Method has parameter (for indexing) |

#### DERIVED Edges (computed)

| Kind | From → To | Description | Computed From |
|------|-----------|-------------|---------------|
| `USES_IN_SIGNATURE` | Interface → Type | Type used in interface method signatures | RETURN_TYPE + PARAMETER_TYPE on interface methods |
| `USES_AS_COLLECTION_ELEMENT` | Node → Type | Type is element of a collection | TYPE_ARGUMENT on collection types |
| `REFERENCES` | Type → Type | General type reference | All type reference edges |

### Edge Proofs

DERIVED edges require an `EdgeProof` explaining their derivation:

```java
EdgeProof {
    String reason       // Human-readable explanation
    List<Edge> sources  // RAW edges this was derived from
}
```

Example:
```java
// DERIVED edge: OrderRepository → Order (USES_IN_SIGNATURE)
EdgeProof {
    reason: "Type 'Order' used in method 'save(Order)' return type"
    sources: [
        Edge(method:OrderRepository#save(Order), type:Order, RETURN_TYPE, RAW)
    ]
}
```

---

## Graph Building

### Build Phases

```
JavaSemanticModel
       │
       ▼
┌──────────────────────────────────────────────────────┐
│                    GraphBuilder                       │
├──────────────────────────────────────────────────────┤
│                                                       │
│  Pass 1: Create TypeNodes                            │
│    └── For each source type → TypeNode               │
│                                                       │
│  Pass 1.5: Detect Style                              │
│    └── StyleDetector → PackageOrganizationStyle      │
│                                                       │
│  Pass 2: Create Members + RAW Edges                  │
│    ├── For each field → FieldNode + FIELD_TYPE edge  │
│    ├── For each method → MethodNode + edges          │
│    └── Hierarchy edges (EXTENDS, IMPLEMENTS)         │
│                                                       │
│  Pass 3: Compute DERIVED Edges                       │
│    └── DerivedEdgeComputer analyzes RAW edges        │
│                                                       │
└──────────────────────────────────────────────────────┘
       │
       ▼
ApplicationGraph
```

### Style Detection

During Pass 1.5, the `StyleDetector` analyzes package patterns to detect the codebase organization style:

| Style | Description | Detected By |
|-------|-------------|-------------|
| `HEXAGONAL` | Ports & Adapters pattern | `ports.in`, `ports.out`, `adapters` packages |
| `BY_LAYER` | Layer-based organization | `controller`, `service`, `repository` packages |
| `BY_FEATURE` | Feature-based modules | Consistent feature subpackages |
| `FLAT` | Single package | All types in one package |
| `UNKNOWN` | No clear pattern | Default |

Style influences classification confidence (e.g., `PackageInCriteria` has HIGH confidence in HEXAGONAL style).

---

## Graph Indexes

The `GraphIndexes` class maintains indexes for fast queries. Indexes are built incrementally as nodes and edges are added.

### Primary Indexes (by node attribute)

| Index | Key → Value | Query Method |
|-------|-------------|--------------|
| `byPackage` | Package name → Type IDs | `typesByPackage(String)` |
| `byJavaForm` | JavaForm → Type IDs | `typesByForm(JavaForm)` |
| `byAnnotation` | Annotation FQN → Node IDs | `byAnnotation(String)` |
| `allTypes` | - → All type IDs | `allTypes()` |
| `allMembers` | - → All member IDs | `allMembers()` |

### Relationship Indexes (from edges)

| Index | Key → Value | Query Method |
|-------|-------------|--------------|
| `declaredMembers` | Type ID → Member IDs | `membersOf(NodeId)` |
| `declaringType` | Member ID → Type ID | `declaringTypeOf(NodeId)` |
| `subtypes` | Type ID → Subtype IDs | `subtypesOf(NodeId)` |
| `supertypes` | Type ID → Supertype IDs | `supertypesOf(NodeId)` |
| `implementors` | Interface ID → Implementor IDs | `implementorsOf(NodeId)` |
| `implementedInterfaces` | Type ID → Interface IDs | `interfacesOf(NodeId)` |
| `usedInSignatureOf` | Type ID → Interface IDs | `interfacesUsingInSignature(NodeId)` |
| `fieldsByType` | Type ID → Field IDs | `fieldsOfType(NodeId)` |
| `methodsByReturnType` | Type ID → Method IDs | `methodsReturning(NodeId)` |
| `methodsByParameterType` | Type ID → Method IDs | `methodsWithParameter(NodeId)` |

---

## GraphQuery API

The `GraphQuery` interface provides a high-level API for querying the graph. It combines indexes with node lookups for convenient access.

### Type Queries

```java
// Find type by name
Optional<TypeNode> type = query.type("com.example.Order");

// Filter types
Stream<TypeNode> interfaces = query.types(t -> t.isInterface());
Stream<TypeNode> records = query.types(t -> t.isRecord());

// By package
Stream<TypeNode> domainTypes = query.typesInPackage("com.example.domain");
```

### Relationship Queries

```java
// Hierarchy
List<TypeNode> interfaces = query.interfacesOf(type);
List<TypeNode> implementors = query.implementorsOf(interface);
List<TypeNode> subtypes = query.subtypesOf(type);

// Members
List<FieldNode> fields = query.fieldsOf(type);
List<MethodNode> methods = query.methodsOf(type);

// Type usage
Set<TypeNode> usedIn = query.typesUsingInSignature(type);
```

### Edge Queries

```java
// Outgoing edges
Stream<Edge> outgoing = query.edgesFrom(nodeId);
Stream<Edge> outgoingOfKind = query.edgesFrom(nodeId, EdgeKind.IMPLEMENTS);

// Incoming edges
Stream<Edge> incoming = query.edgesTo(nodeId);

// Derived edges
Stream<Edge> derived = query.derivedEdges();
```

---

## Files Reference

| Component | Location |
|-----------|----------|
| ApplicationGraph | `graph/ApplicationGraph.java` |
| GraphBuilder | `graph/builder/GraphBuilder.java` |
| DerivedEdgeComputer | `graph/builder/DerivedEdgeComputer.java` |
| StyleDetector | `graph/style/StyleDetector.java` |
| GraphIndexes | `graph/index/GraphIndexes.java` |
| GraphQuery | `graph/query/GraphQuery.java` |
| DefaultGraphQuery | `graph/query/DefaultGraphQuery.java` |

### Model Classes

| Component | Location |
|-----------|----------|
| Node (abstract) | `graph/model/Node.java` |
| TypeNode | `graph/model/TypeNode.java` |
| FieldNode | `graph/model/FieldNode.java` |
| MethodNode | `graph/model/MethodNode.java` |
| ConstructorNode | `graph/model/ConstructorNode.java` |
| NodeId | `graph/model/NodeId.java` |
| Edge | `graph/model/Edge.java` |
| EdgeKind | `graph/model/EdgeKind.java` |
| EdgeOrigin | `graph/model/EdgeOrigin.java` |
| EdgeProof | `graph/model/EdgeProof.java` |
| AnnotationRef | `graph/model/AnnotationRef.java` |
| GraphMetadata | `graph/model/GraphMetadata.java` |

---

## Usage Example

```java
// Build graph from source
JavaSemanticModel model = SpoonFrontend.analyze(sourceRoot);
GraphMetadata metadata = GraphMetadata.builder()
    .basePackage("com.example")
    .build();

ApplicationGraph graph = new GraphBuilder().build(model, metadata);

// Query the graph
GraphQuery query = graph.query();

// Find all repositories
query.types(t -> t.isInterface() && t.hasRepositorySuffix())
    .forEach(repo -> {
        System.out.println("Repository: " + repo.simpleName());

        // Find types used in signature
        query.edgesFrom(repo.id(), EdgeKind.USES_IN_SIGNATURE)
            .map(e -> query.type(e.to()))
            .filter(Optional::isPresent)
            .forEach(type -> System.out.println("  Manages: " + type.get().simpleName()));
    });
```

---

## See Also

- [CLASSIFICATION_ALGORITHM.md](CLASSIFICATION_ALGORITHM.md) - How the graph is used for classification
- [CRITERIA_REFERENCE.md](CRITERIA_REFERENCE.md) - Criteria that query the graph
- [SPI Reference](../SPI_REFERENCE.md) - IR model exported from classification

---

<div align="center">

**HexaGlue Internal Documentation**

</div>
