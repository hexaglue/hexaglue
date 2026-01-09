# HexaGlue v2 Classification Algorithm Baseline

## Document Purpose

This document captures the classification algorithm state in HexaGlue v2 before implementing the v3 deterministic redesign. It serves as a reference for comparison and validation.

**Version**: 2.0.0-SNAPSHOT  
**Date**: 2026-01-08  
**Status**: Baseline for v3 redesign  

---

## Overview

HexaGlue v2 uses a **single-pass, criteria-based classification algorithm** that:

1. Builds semantic indexes (anchors, CoreAppClass, interface facts)
2. Classifies ports (interfaces) first using semantic analysis
3. Classifies domain types with port context available
4. Uses weighted scoring and deterministic tie-breaking

---

## Classification Pipeline

### Architecture

```
Source Code → SpoonFrontend → JavaSemanticModel → GraphBuilder → ApplicationGraph
    → SinglePassClassifier → ClassificationResults → IrExporter → IrSnapshot → Plugins
```

### SinglePassClassifier Phases

#### Phase 1: Semantic Index Building

**AnchorDetector** - Identifies architectural anchor points:
- Use cases (classes with @UseCase or "UseCase" suffix)
- Application services
- Stateless facades with injected dependencies

**CoreAppClassDetector** - Identifies core application classes:
- Classes that orchestrate domain logic
- Determined by relationship to anchors
- Used as pivot for port classification

**InterfaceFactsIndex** - Computes semantic facts for interfaces:
- `implementedByCore`: Interface implemented by CoreAppClass → DRIVING port candidate
- `usedByCore`: Interface used by CoreAppClass → DRIVEN port candidate
- `missingImpl`: No implementation found in domain
- `internalImplOnly`: Only domain-internal implementations exist
- Port annotations checked (jMolecules, custom annotations)

#### Phase 2: Port Classification

**Key Principle**: Ports are classified based on relationship to CoreAppClass, not names.

**Semantic Criteria** (priority 85):

```java
// DRIVING (Primary) Port
if (implementedByCore) {
    classify as DRIVING_PORT (HIGH confidence)
}

// DRIVEN (Secondary) Port
if (usedByCore && (missingImpl || internalImplOnly) && hasPortAnnotation) {
    PortKind kind = classify as REPOSITORY | GATEWAY | NOTIFICATION_PROVIDER
    classify as kind (HIGH confidence, DRIVEN direction)
}

// Relaxed DRIVEN (without annotation)
if (usedByCore && (missingImpl || internalImplOnly) && !hasPortAnnotation) {
    PortKind kind = classify as REPOSITORY | GATEWAY | NOTIFICATION_PROVIDER
    classify as kind (MEDIUM confidence, DRIVEN direction)
}
```

**Fallback**: If semantic criteria don't match, use criteria-based classification:
- Naming conventions (Repository, Gateway, UseCase suffixes)
- Package structure (*.port.in, *.port.out)
- Signature patterns (CRUD methods, Query/Command patterns)
- Explicit annotations

#### Phase 3: Domain Type Classification

**Criteria Engine** evaluates ordered criteria against each type.

**Default Criteria** (priority descending):

| Priority | Criteria | Target | Confidence | Description |
|----------|----------|--------|------------|-------------|
| 95 | ExplicitAggregateRootCriteria | AGGREGATE_ROOT | HIGH | @AggregateRoot annotation |
| 95 | ExplicitEntityCriteria | ENTITY | HIGH | @Entity annotation (jMolecules) |
| 95 | ExplicitValueObjectCriteria | VALUE_OBJECT | HIGH | @ValueObject annotation |
| 95 | ExplicitDomainEventCriteria | DOMAIN_EVENT | HIGH | @DomainEvent annotation |
| 95 | ExplicitIdentifierCriteria | IDENTIFIER | HIGH | @Identity annotation |
| 90 | ImplementsJMoleculesInterfaceCriteria | ENTITY/VALUE_OBJECT | HIGH | Implements jMolecules.ddd.types interfaces |
| 85 | RepositoryDominantCriteria | AGGREGATE_ROOT | HIGH | Has repository interface |
| 80 | RecordSingleIdCriteria | IDENTIFIER | HIGH | Record with single ID field |
| 75 | HasIdentityCriteria | ENTITY | HIGH | Has identity field |
| 72 | FlexibleSagaCriteria | SAGA | MEDIUM | OUTBOUND_ONLY + 2+ deps + stateful |
| 70 | FlexibleInboundOnlyCriteria | INBOUND_ONLY | MEDIUM | CoreAppClass implements DRIVING only |
| 68 | FlexibleOutboundOnlyCriteria | OUTBOUND_ONLY | MEDIUM | CoreAppClass uses DRIVEN only |
| 65 | HasPortDependenciesCriteria | APPLICATION_SERVICE | MEDIUM | CoreAppClass with both directions |
| 60 | EmbeddedValueObjectCriteria | VALUE_OBJECT | MEDIUM | Embedded in entity |
| 60 | CollectionElementEntityCriteria | ENTITY | MEDIUM | Element of entity collection |
| 55 | ImmutableNoIdCriteria | VALUE_OBJECT | LOW | Immutable with no identity |
| 50 | StatelessNoDependenciesCriteria | VALUE_OBJECT | LOW | Stateless, no dependencies |
| 50 | NamingDomainEventCriteria | DOMAIN_EVENT | LOW | Naming convention (*Event) |
| 45 | UnreferencedInPortsCriteria | VALUE_OBJECT | VERY_LOW | Not referenced by ports |
| 40 | InheritedClassificationCriteria | (from parent) | VERY_LOW | Inherits classification |

**Actor Classification** (CoreAppClass subtypes):

```java
// APPLICATION_SERVICE: Implements DRIVING + uses DRIVEN
if (implementsDriving && usesDriven) {
    classify as APPLICATION_SERVICE
}

// INBOUND_ONLY: Implements DRIVING but no DRIVEN dependencies
if (implementsDriving && !usesDriven) {
    classify as INBOUND_ONLY
}

// OUTBOUND_ONLY: Uses DRIVEN but doesn't implement DRIVING
if (!implementsDriving && usesDriven) {
    classify as OUTBOUND_ONLY
}

// SAGA: OUTBOUND_ONLY + 2+ DRIVEN dependencies + stateful fields
if (OUTBOUND_ONLY && drivenCount >= 2 && hasStatefulFields) {
    classify as SAGA
}
```

---

## Decision Algorithm

### CriteriaEngine

The engine evaluates all criteria and collects **Contributions**:

```java
record Contribution<K>(
    K kind,                   // DomainKind or PortKind
    String criteriaName,
    int priority,             // From criteria
    ConfidenceLevel confidence, // HIGH/MEDIUM/LOW/VERY_LOW
    String justification,
    List<Evidence> evidence,
    Map<String, Object> metadata
)
```

### DefaultDecisionPolicy

**Tie-breaking algorithm** (deterministic):

```
1. Priority (descending) - higher priority wins
2. Confidence weight (descending) - HIGH=100, MEDIUM=75, LOW=50, VERY_LOW=25
3. Criteria name (ascending) - alphabetical order for determinism
```

**Conflict detection**:
- Contributions with different kinds are conflicts
- Severity determined by CompatibilityPolicy:
  - **ERROR**: Incompatible kinds (ENTITY vs VALUE_OBJECT)
  - **WARNING**: Compatible kinds (ENTITY vs AGGREGATE_ROOT)

**Outcomes**:
- **SUCCESS**: Single kind, no conflicts
- **SUCCESS_WITH_CONFLICTS**: Winner selected, but conflicts exist (warnings)
- **CONFLICT**: No winner due to incompatible ties at same priority

---

## CompatibilityPolicy

### Domain Compatibility

```java
// Compatible pairs (can coexist):
AGGREGATE_ROOT ↔ ENTITY
ENTITY ↔ AGGREGATE_ROOT

// All others are INCOMPATIBLE
```

Rationale: An AGGREGATE_ROOT is a special ENTITY with repository access.

### Port Compatibility

All port kinds within the same direction are compatible:
- REPOSITORY, GATEWAY, NOTIFICATION_PROVIDER (all DRIVEN)
- Different port kinds across directions are incompatible

---

## Key Design Principles

### 1. Relationship-Based Classification

Classification derives from **graph relationships**, not naming:
- CoreAppClass pivot distinguishes DRIVING (implemented) from DRIVEN (used)
- Semantic facts computed once, reused across criteria

### 2. Explicit Over Implicit

Priority order favors explicit signals:
1. Annotations (priority 95)
2. Structural patterns (80-85)
3. Naming conventions (50-55)
4. Fallback heuristics (40-45)

### 3. Deterministic Tie-Breaking

Given identical code, classification always produces the same result:
- Stable priority ordering
- Confidence weights are discrete (not floating point)
- Alphabetical criteria name as final tie-breaker

### 4. Port-Aware Domain Classification

Domain classification has access to port results:
- Enables actor classification (APPLICATION_SERVICE, etc.)
- Criteria can query which interfaces are ports

---

## Known Limitations (v2)

### Non-Determinism Risks

1. **Criteria ordering**: Adding criteria between priorities can change results
2. **Confidence inflation**: Multiple HIGH criteria at same priority cause instability
3. **Floating point risks**: Confidence weights are integers, but downstream could compute ratios

### Complexity

1. **Criteria proliferation**: 20+ criteria with overlapping logic
2. **Priority assignment**: Manual priority assignment is error-prone
3. **Testing burden**: Each criteria combination must be tested

### Extensibility

1. **Profile overrides**: CriteriaProfile can override priorities, but no validation
2. **Custom criteria**: Adding criteria requires understanding priority space
3. **Plugin visibility**: Plugins only see final IR, not classification rationale

---

## Test Coverage

### Classification Tests

- `SinglePassClassifierTest` - End-to-end classification
- `DomainClassifierTest` - Domain criteria evaluation
- `PortClassifierTest` - Port criteria evaluation
- `CriteriaEngineTest` - Engine mechanics
- `DefaultDecisionPolicyTest` - Tie-breaking algorithm
- `ClassificationGoldenFilesTest` - Golden file regression tests
- `ClassificationIntegrationTest` - Full pipeline with examples

### Mutation Coverage

Not yet implemented (Phase 0 task).

---

## Performance Characteristics

### Time Complexity

- **Spoon parsing**: O(n) where n = source code size
- **Graph building**: O(v + e) where v = types, e = relationships
- **Anchor detection**: O(v)
- **CoreAppClass detection**: O(v + e)
- **Interface facts**: O(v_interfaces * e)
- **Port classification**: O(v_interfaces * c_port) where c_port = port criteria count
- **Domain classification**: O(v * c_domain) where c_domain = domain criteria count

**Overall**: O(n + v * e + v * c) where c = max(c_port, c_domain)

For typical projects:
- v (types) ~ 100-1000
- e (edges) ~ 1000-10000
- c (criteria) ~ 20-30

### Space Complexity

- ApplicationGraph: O(v + e)
- SemanticIndexes: O(v)
- ClassificationResults: O(v)

**Overall**: O(v + e)

---

## References

### Source Files

- `SinglePassClassifier.java` - Main classifier
- `CriteriaEngine.java` - Generic evaluation engine
- `DefaultDecisionPolicy.java` - Tie-breaking logic
- `DomainClassifier.java` - Domain-specific classifier
- `PortClassifier.java` - Port-specific classifier
- `CompatibilityPolicy.java` - Conflict detection
- `classification/domain/criteria/*` - Domain criteria implementations
- `classification/port/criteria/*` - Port criteria implementations
