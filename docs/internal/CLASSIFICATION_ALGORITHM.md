# Classification Algorithm

***Internal documentation for the single-pass classification algorithm.***

---

## Overview

HexaGlue uses a **single-pass classification algorithm** that classifies domain types and ports in a unified flow. This replaces the earlier multi-pass approach where domain classification happened before port context was available.

**Key principle**: Classification is derived from **relationships in the graph**, not from names alone. The algorithm builds semantic indexes that capture structural relationships, enabling accurate classification even without explicit annotations.

---

## Algorithm Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SINGLE-PASS CLASSIFIER                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              PHASE 1: SEMANTIC INDEX CONSTRUCTION               │   │
│  │                                                                  │   │
│  │   ApplicationGraph                                               │   │
│  │         │                                                        │   │
│  │         ▼                                                        │   │
│  │   ┌─────────────┐    ┌──────────────────┐    ┌───────────────┐  │   │
│  │   │   Anchor    │───▶│   CoreAppClass   │───▶│  Interface    │  │   │
│  │   │  Detector   │    │    Detector      │    │  Facts Index  │  │   │
│  │   └─────────────┘    └──────────────────┘    └───────────────┘  │   │
│  │         │                    │                       │          │   │
│  │         ▼                    ▼                       ▼          │   │
│  │   AnchorContext      CoreAppClassIndex      InterfaceFactsIndex │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                  PHASE 2: PORT CLASSIFICATION                    │   │
│  │                        (PORTS FIRST)                             │   │
│  │                                                                  │   │
│  │   For each interface:                                            │   │
│  │     1. Check InterfaceFacts (semantic classification)            │   │
│  │     2. Fallback to PortClassifier (criteria-based)               │   │
│  │                                                                  │   │
│  │   Output: Map<NodeId, ClassificationResult> (ports)              │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                 PHASE 3: DOMAIN CLASSIFICATION                   │   │
│  │                    (WITH PORT CONTEXT)                           │   │
│  │                                                                  │   │
│  │   For each class/record (non-interface):                         │   │
│  │     - DomainClassifier evaluates criteria                        │   │
│  │     - Port context available via ClassificationContext           │   │
│  │                                                                  │   │
│  │   Output: Map<NodeId, ClassificationResult> (domain)             │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│                         ClassificationResults                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Semantic Index Construction

Before any classification, the algorithm builds three semantic indexes that capture structural relationships in the codebase.

### 1.1 Anchor Detection

**Class**: `AnchorDetector`
**Output**: `AnchorContext`

Anchors classify types based on their infrastructure dependencies. This separates infrastructure code from application core.

| Anchor Kind | Description | Detection |
|-------------|-------------|-----------|
| `DRIVING_ANCHOR` | Framework entry points | `@RestController`, `@KafkaListener`, etc. |
| `INFRA_ANCHOR` | Infrastructure implementations | `@Repository`, `@Entity`, `JdbcTemplate` fields |
| `DOMAIN_ANCHOR` | Pure domain/application code | Default (no infra dependencies) |

**Detection priority**:
1. Driving annotations (RestController, KafkaListener, etc.) → `DRIVING_ANCHOR`
2. Infrastructure annotations (Repository, Entity, etc.) → `INFRA_ANCHOR`
3. Field dependencies on infrastructure types → `INFRA_ANCHOR`
4. Default → `DOMAIN_ANCHOR`

**Why anchors matter**: They allow the algorithm to distinguish between:
- Classes that ARE infrastructure (don't classify as domain)
- Classes that USE infrastructure via ports (classify as domain)

### 1.2 CoreAppClass Detection

**Class**: `CoreAppClassDetector`
**Output**: `CoreAppClassIndex`

A **CoreAppClass** is a `DOMAIN_ANCHOR` class that implements or depends on at least one user-code interface. These are the "pivots" that link ports together.

```
                    CoreAppClass (pivot)
                          │
          ┌───────────────┴───────────────┐
          │                               │
    implementedInterfaces           dependedInterfaces
          │                               │
          ▼                               ▼
    DRIVING ports                   DRIVEN ports
  (provided by app)             (consumed by app)
```

**Detection logic**:
1. Only considers `DOMAIN_ANCHOR` classes
2. Finds implemented user-code interfaces (not JDK/framework)
3. Finds depended user-code interfaces (via constructor/field injection)
4. Creates `CoreAppClass` if at least one interface relationship exists

**CoreAppClass types**:

| Type | Implements | Depends On | Typical Role |
|------|------------|------------|--------------|
| **Pivot** | Yes | Yes | Application Service |
| **Inbound-only** | Yes | No | Simple handler, no external deps |
| **Outbound-only** | No | Yes | Orchestrator, saga |

### 1.3 InterfaceFacts Computation

**Class**: `InterfaceFactsIndex`
**Output**: `InterfaceFactsIndex`

Computes facts about each user-code interface for semantic port classification.

**Computed facts**:

| Fact | Description |
|------|-------------|
| `implsProdCount` | Number of production implementations |
| `missingImpl` | True if no production implementations exist |
| `internalImplOnly` | True if all implementations are domain anchors |
| `usedByCore` | True if depended on by at least one CoreAppClass |
| `implementedByCore` | True if implemented by at least one CoreAppClass |
| `hasPortAnnotation` | True if has jMolecules or naming indicators |

**Port classification rules from facts**:

| Classification | Condition |
|----------------|-----------|
| **DRIVING port** | `implementedByCore = true` |
| **DRIVEN port** | `usedByCore AND (missingImpl OR internalImplOnly) AND hasPortAnnotation` |
| **DRIVEN (relaxed)** | `usedByCore AND (missingImpl OR internalImplOnly)` (without annotation check) |

---

## Phase 2: Port Classification (Ports First)

**Class**: `SinglePassClassifier.classifyPorts()`

Ports are classified **before** domain types. This is the key insight of the single-pass approach: domain classification often needs port context (e.g., "is this type managed by a Repository?").

### Semantic Classification (Priority)

For each interface, check `InterfaceFacts` first:

```java
if (facts.isDrivingPortCandidate()) {
    // Interface is implemented by a CoreAppClass
    return DRIVING port with HIGH confidence
}

if (facts.isDrivenPortCandidate()) {
    // Interface is used by CoreAppClass + (missingImpl OR internalImplOnly) + hasPortAnnotation
    return DRIVEN port with HIGH confidence
}

if (facts.isDrivenPortCandidateWithoutAnnotationCheck()) {
    // Relaxed check without annotation requirement
    return DRIVEN port with MEDIUM confidence
}
```

### Criteria-Based Fallback

If semantic classification doesn't apply, use `PortClassifier` with criteria:

**Port criteria by priority**:

| Priority | Criteria | Target | Direction |
|----------|----------|--------|-----------|
| 100 | `ExplicitRepositoryCriteria` | REPOSITORY | DRIVEN |
| 100 | `ExplicitPrimaryPortCriteria` | USE_CASE | DRIVING |
| 100 | `ExplicitSecondaryPortCriteria` | GENERIC | DRIVEN |
| 85 | `SemanticDrivingPortCriteria` | USE_CASE | DRIVING |
| 85 | `SemanticDrivenPortCriteria` | (varies) | DRIVEN |
| 75 | `CommandPatternCriteria` | COMMAND | DRIVING |
| 75 | `QueryPatternCriteria` | QUERY | DRIVING |
| 75 | `InjectedAsDependencyCriteria` | GENERIC | DRIVEN |
| 70 | `SignatureBasedDrivenPortCriteria` | REPOSITORY | DRIVEN |
| 60 | `PackageInCriteria` | GENERIC | DRIVING |
| 60 | `PackageOutCriteria` | GENERIC | DRIVEN |
| 50 | `NamingRepositoryCriteria` | REPOSITORY | DRIVEN |
| 50 | `NamingUseCaseCriteria` | USE_CASE | DRIVING |
| 50 | `NamingGatewayCriteria` | GATEWAY | DRIVEN |

---

## Phase 3: Domain Classification (With Port Context)

**Class**: `SinglePassClassifier.classifyDomainTypes()` → `DomainClassifier`

Domain classification runs after ports are classified, with full port context available.

### Domain Criteria by Priority

| Priority | Criteria | Target Kind | Description |
|----------|----------|-------------|-------------|
| **100** | `ExplicitAggregateRootCriteria` | AGGREGATE_ROOT | `@AggregateRoot` annotation |
| **100** | `ExplicitEntityCriteria` | ENTITY | `@Entity` annotation |
| **100** | `ExplicitValueObjectCriteria` | VALUE_OBJECT | `@ValueObject` annotation |
| **100** | `ExplicitIdentifierCriteria` | IDENTIFIER | `@Identity` on field |
| **100** | `ExplicitDomainEventCriteria` | DOMAIN_EVENT | `@DomainEvent` annotation |
| **100** | `ImplementsJMoleculesInterfaceCriteria` | (varies) | Implements jMolecules interfaces |
| **80** | `RepositoryDominantCriteria` | AGGREGATE_ROOT | Used in Repository signature |
| **80** | `RecordSingleIdCriteria` | IDENTIFIER | Record with single ID-type field |
| **75** | `InheritedClassificationCriteria` | (inherited) | Inherits from classified type |
| **72** | `FlexibleSagaCriteria` | SAGA | Outbound-only + 2+ deps + stateful |
| **70** | `EmbeddedValueObjectCriteria` | VALUE_OBJECT | Embedded in another type |
| **70** | `FlexibleInboundOnlyCriteria` | APPLICATION_SERVICE | Inbound-only CoreAppClass |
| **68** | `FlexibleOutboundOnlyCriteria` | APPLICATION_SERVICE | Outbound-only CoreAppClass |
| **65** | `HasPortDependenciesCriteria` | APPLICATION_SERVICE | Has port dependencies |
| **60** | `HasIdentityCriteria` | ENTITY | Has `id` field or `*Id` type |
| **60** | `CollectionElementEntityCriteria` | ENTITY | Element of entity collection |
| **60** | `ImmutableNoIdCriteria` | VALUE_OBJECT | Immutable, no identity |
| **55** | `NamingDomainEventCriteria` | DOMAIN_EVENT | Name ends with `Event` |
| **55** | `StatelessNoDependenciesCriteria` | DOMAIN_SERVICE | Stateless, no dependencies |
| **50** | `UnreferencedInPortsCriteria` | VALUE_OBJECT | Not referenced in ports |

---

## Tie-Breaking Algorithm

When multiple criteria match, the algorithm uses deterministic tie-breaking:

```
1. Priority (descending)      - Higher priority wins
2. Confidence (descending)    - Higher confidence wins
3. Criteria name (ascending)  - Alphabetical for determinism
```

**Example**:

```
Type: Order (has @AggregateRoot, also matched by RepositoryDominantCriteria)

Matches:
  - ExplicitAggregateRootCriteria: priority=100, confidence=EXPLICIT
  - RepositoryDominantCriteria: priority=80, confidence=HIGH

Winner: ExplicitAggregateRootCriteria (priority 100 > 80)
```

---

## Confidence Levels

| Level | Description | Source |
|-------|-------------|--------|
| `EXPLICIT` | Developer explicitly declared | jMolecules annotations |
| `HIGH` | Strong signals converge | Semantic facts, naming + structure |
| `MEDIUM` | Moderate signals | Package patterns, single heuristic |
| `LOW` | Weak signals | Default inference, needs verification |

**Reliability check**:
```java
confidence.isReliable()  // true for EXPLICIT and HIGH
```

---

## Conflict Detection

Conflicts occur when multiple criteria match with different target kinds.

### Compatible Kinds

Some kinds are considered compatible and don't trigger conflicts:

| Pair | Reason |
|------|--------|
| `AGGREGATE_ROOT` + `ENTITY` | Aggregate root is a special case of entity |

### Conflict Resolution

```
1. If winner has HIGHER priority than conflicts → No conflict (winner takes all)
2. If winner has SAME priority as incompatible conflicts → Status = CONFLICT
3. If conflicts are compatible → No conflict (winner takes all)
```

**Example conflict**:
```
Type: SomeClass

Matches at priority 60:
  - HasIdentityCriteria → ENTITY
  - ImmutableNoIdCriteria → VALUE_OBJECT

Result: CONFLICT (same priority, incompatible kinds)
```

---

## Key Design Decisions

### Why "Ports First"?

In the old multi-pass approach, domain classification happened before port context was available. This caused issues like:

```
// Old approach problem:
Order has UUID id field
  Pass 1: Classified as ENTITY (has identity, but Repository not yet known)
  Pass 2: Repository<Order> found, but too late to change classification
```

With "ports first":
```
// New approach:
1. OrderRepository classified as DRIVEN/REPOSITORY
2. Order classified with port context → AGGREGATE_ROOT (Repository-dominant)
```

### Why Semantic Indexes?

Criteria-based classification relies on local signals (annotations, names). Semantic indexes provide **global context**:

- **Anchor**: "Is this class infrastructure or domain?"
- **CoreAppClass**: "Which classes are the pivots connecting ports?"
- **InterfaceFacts**: "What's the implementation status of this interface?"

This enables classification based on **relationships**, not just **names**.

### Why Deterministic Tie-Breaking?

Without deterministic tie-breaking, classification could vary between builds if criteria evaluation order changed. The three-level comparator ensures:

1. Higher priority always wins
2. Higher confidence breaks ties
3. Alphabetical order ensures consistency

---

## Files Reference

| Component | Location |
|-----------|----------|
| SinglePassClassifier | `classification/SinglePassClassifier.java` |
| AnchorDetector | `classification/anchor/AnchorDetector.java` |
| AnchorContext | `classification/anchor/AnchorContext.java` |
| CoreAppClassDetector | `classification/semantic/CoreAppClassDetector.java` |
| CoreAppClass | `classification/semantic/CoreAppClass.java` |
| InterfaceFactsIndex | `classification/semantic/InterfaceFactsIndex.java` |
| InterfaceFacts | `classification/semantic/InterfaceFacts.java` |
| DomainClassifier | `classification/domain/DomainClassifier.java` |
| PortClassifier | `classification/port/PortClassifier.java` |
| Domain criteria | `classification/domain/criteria/*.java` |
| Port criteria | `classification/port/criteria/*.java` |

---

## See Also

- [User Guide](../USER_GUIDE.md) - Classification from user perspective
- [SPI Reference](../SPI_REFERENCE.md) - IR model consumed by plugins
- [GRAPH_MODEL.md](GRAPH_MODEL.md) - ApplicationGraph structure (TODO)
- [CRITERIA_REFERENCE.md](CRITERIA_REFERENCE.md) - Complete criteria matrix (TODO)

---

<div align="center">

**HexaGlue Internal Documentation**

</div>
