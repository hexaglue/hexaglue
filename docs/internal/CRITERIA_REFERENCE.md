# Criteria Reference

***Complete reference for all classification criteria.***

---

## Overview

HexaGlue uses **criteria-based classification** to determine domain types and ports. Each criteria evaluates a type against specific conditions and returns a match result with confidence level.

This document provides a complete reference of all criteria, organized by:
- Classification target (Domain vs Port)
- Priority level (100 = highest, 50 = lowest)
- Detection method (annotation, semantic, heuristic, naming)

---

## Priority Guidelines

| Priority | Category | Description |
|----------|----------|-------------|
| **100** | Explicit | User-declared via jMolecules annotations |
| **85** | Semantic | Based on structural relationships (CoreAppClass, InterfaceFacts) |
| **75-80** | Strong Heuristic | Multiple converging signals |
| **60-70** | Medium Heuristic | Package patterns, single strong signal |
| **50** | Weak Heuristic | Naming conventions only (fallback) |

---

## Domain Criteria

### Explicit Annotations (Priority 100)

These criteria detect jMolecules annotations and provide `EXPLICIT` confidence.

| Criteria | name() | Target Kind | Condition |
|----------|--------|-------------|-----------|
| `ExplicitAggregateRootCriteria` | `explicit-aggregate-root` | AGGREGATE_ROOT | `@AggregateRoot` annotation present |
| `ExplicitEntityCriteria` | `explicit-entity` | ENTITY | `@Entity` annotation present |
| `ExplicitValueObjectCriteria` | `explicit-value-object` | VALUE_OBJECT | `@ValueObject` annotation present |
| `ExplicitIdentifierCriteria` | `explicit-identifier` | IDENTIFIER | `@Identifier` annotation OR implements `Identifier` interface |
| `ExplicitDomainEventCriteria` | `explicit-domain-event` | DOMAIN_EVENT | `@DomainEvent` annotation present |
| `ImplementsJMoleculesInterfaceCriteria` | `implements-jmolecules-interface` | (varies) | Implements `AggregateRoot`, `Entity`, `ValueObject`, or `Identifier` interface |

**jMolecules annotations** (`org.jmolecules.ddd.annotation`):
- `@AggregateRoot`
- `@Entity`
- `@ValueObject`
- `@Identifier`
- `@DomainEvent`

**jMolecules interfaces** (`org.jmolecules.ddd.types`):
- `AggregateRoot<T, ID>`
- `Entity<T, ID>`
- `ValueObject`
- `Identifier`

---

### Strong Heuristics (Priority 80)

| Criteria | name() | Target Kind | Condition | Confidence |
|----------|--------|-------------|-----------|------------|
| `RepositoryDominantCriteria` | `repository-dominant` | AGGREGATE_ROOT | Type has identity field AND is dominant type in Repository signatures | HIGH |
| `RecordSingleIdCriteria` | `record-single-id` | IDENTIFIER | Record with name ending in `Id` AND exactly one component | HIGH |

**RepositoryDominantCriteria details**:
```
Matches when:
  1. Type has an identity field (named 'id' or type ending with 'Id')
  2. Type appears in Repository port method signatures
  3. Type is the "dominant" type (most frequently referenced)

Example:
  interface OrderRepository {
      Order save(Order order);       // Order is dominant
      Order findById(OrderId id);
  }
  → Order classified as AGGREGATE_ROOT
```

**RecordSingleIdCriteria details**:
```
Matches when:
  1. Type is a Java record
  2. Name ends with "Id" (e.g., OrderId, CustomerId)
  3. Has exactly one record component

Example:
  record OrderId(UUID value) {}  → IDENTIFIER
  record CustomerId(Long id) {}  → IDENTIFIER
```

---

### Inherited Classification (Priority 75)

| Criteria | name() | Target Kind | Condition | Confidence |
|----------|--------|-------------|-----------|------------|
| `InheritedClassificationCriteria` | `inherited-classification` | (inherited) | Extends a type with jMolecules annotation or interface | HIGH |

**Details**:
```
Propagates parent classification to child classes.

Example:
  @AggregateRoot
  class BaseOrder { }

  class SpecialOrder extends BaseOrder { }
  → SpecialOrder inherits AGGREGATE_ROOT classification
```

---

### Flexible Actor Criteria (Priority 68-72)

These criteria classify types based on their CoreAppClass role.

| Criteria | name() | Priority | Target Kind | Condition | Confidence |
|----------|--------|----------|-------------|-----------|------------|
| `FlexibleSagaCriteria` | `flexible-saga` | 72 | SAGA | Outbound-only CoreAppClass with 2+ driven dependencies AND has state fields | HIGH |
| `FlexibleInboundOnlyCriteria` | `flexible-inbound-only` | 70 | INBOUND_ONLY | CoreAppClass implementing driving ports with no driven dependencies | HIGH |
| `FlexibleOutboundOnlyCriteria` | `flexible-outbound-only` | 68 | OUTBOUND_ONLY | CoreAppClass depending on driven ports without implementing driving ports | HIGH |

**CoreAppClass roles**:
```
┌─────────────────────────────────────────────────────────────┐
│                     CoreAppClass Types                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  PIVOT (APPLICATION_SERVICE)                                 │
│  ├── Implements: DRIVING ports (UseCase, Command, Query)     │
│  └── Depends on: DRIVEN ports (Repository, Gateway)          │
│                                                              │
│  INBOUND_ONLY                                                │
│  ├── Implements: DRIVING ports                               │
│  └── Depends on: (none)                                      │
│                                                              │
│  OUTBOUND_ONLY                                               │
│  ├── Implements: (none)                                      │
│  └── Depends on: DRIVEN ports                                │
│                                                              │
│  SAGA (special OUTBOUND_ONLY)                                │
│  ├── Implements: (none)                                      │
│  ├── Depends on: 2+ DRIVEN ports                             │
│  └── Has state fields (orchestration state)                  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

### Medium Heuristics (Priority 60-70)

| Criteria | name() | Priority | Target Kind | Condition | Confidence |
|----------|--------|----------|-------------|-----------|------------|
| `EmbeddedValueObjectCriteria` | `embedded-value-object` | 70 | VALUE_OBJECT | Immutable, no identity, used as field in aggregate-like container | MEDIUM |
| `HasPortDependenciesCriteria` | `has-port-dependencies` | 65 | APPLICATION_SERVICE | Concrete class with interface-typed fields (port dependencies) | MEDIUM |
| `HasIdentityCriteria` | `has-identity` | 60 | ENTITY | Has field named `id` or field type ending with `Id` | MEDIUM |
| `CollectionElementEntityCriteria` | `collection-element-entity` | 60 | ENTITY | Used as collection element in aggregate AND has identity field | MEDIUM |
| `ImmutableNoIdCriteria` | `immutable-no-id` | 60 | VALUE_OBJECT | Immutable (record or all `private final` fields) with no identity | MEDIUM |

**ImmutableNoIdCriteria details**:
```
Immutability detection:
  1. Java record → always immutable
  2. Class with:
     - All fields are private final
     - No setter methods
     - No mutable collection fields (or defensive copies)

Example:
  record Money(BigDecimal amount, Currency currency) {}  → VALUE_OBJECT

  class Address {
      private final String street;
      private final String city;
      // No setters
  }  → VALUE_OBJECT
```

---

### Naming & Service Heuristics (Priority 55)

| Criteria | name() | Priority | Target Kind | Condition | Confidence |
|----------|--------|----------|-------------|-----------|------------|
| `NamingDomainEventCriteria` | `naming-domain-event` | 55 | DOMAIN_EVENT | Name ends with `Event` | MEDIUM |
| `StatelessNoDependenciesCriteria` | `stateless-no-dependencies` | 55 | DOMAIN_SERVICE | Stateless class with service-like name, domain logic, no port dependencies | MEDIUM |

**StatelessNoDependenciesCriteria details**:
```
Service-like name patterns:
  - *Service
  - *Calculator
  - *Validator
  - *Factory
  - *Builder
  - *Resolver

Conditions:
  1. Has service-like suffix
  2. No state fields (or only immutable config)
  3. No port dependencies (interface-typed fields)
  4. Has domain logic methods
```

---

### Low Priority Heuristics (Priority 50)

| Criteria | name() | Priority | Target Kind | Condition | Confidence |
|----------|--------|----------|-------------|-----------|------------|
| `UnreferencedInPortsCriteria` | `unreferenced-in-ports` | 50 | ENTITY | Has identity field but NOT referenced in any port signature | LOW |

**Details**:
```
Identifies "internal entities" - entities that are part of an aggregate
but not directly accessed via ports.

Example:
  class Order {
      private OrderId id;
      private List<LineItem> items;  // LineItem is internal
  }

  interface OrderRepository {
      Order findById(OrderId id);    // Order referenced
                                     // LineItem NOT referenced
  }

  → LineItem classified as ENTITY (internal)
```

---

## Port Criteria

### Explicit Annotations (Priority 100)

| Criteria | name() | Target Kind | Direction | Condition |
|----------|--------|-------------|-----------|-----------|
| `ExplicitRepositoryCriteria` | `explicit-repository` | REPOSITORY | DRIVEN | `@Repository` annotation (jMolecules) |
| `ExplicitPrimaryPortCriteria` | `explicit-primary-port` | USE_CASE | DRIVING | `@PrimaryPort` annotation OR implements `PrimaryPort` |
| `ExplicitSecondaryPortCriteria` | `explicit-secondary-port` | GATEWAY | DRIVEN | `@SecondaryPort` annotation OR implements `SecondaryPort` |

**jMolecules port annotations**:
- `@org.jmolecules.ddd.annotation.Repository`
- `@org.jmolecules.architecture.hexagonal.PrimaryPort`
- `@org.jmolecules.architecture.hexagonal.SecondaryPort`

---

### Semantic Criteria (Priority 85)

| Criteria | name() | Target Kind | Direction | Condition | Confidence |
|----------|--------|-------------|-----------|-----------|------------|
| `SemanticDrivingPortCriteria` | `semantic-driving` | USE_CASE | DRIVING | Interface implemented by at least one CoreAppClass | HIGH |
| `SemanticDrivenPortCriteria` | `semantic-driven` | (varies) | DRIVEN | Interface used by CoreAppClass AND (no prod impl OR internal-only impl) AND has port annotation | HIGH |

**Semantic classification logic**:
```
DRIVING port detection:
  implementedByCore = true
  → Application PROVIDES this interface implementation
  → External actors CALL this interface

DRIVEN port detection:
  usedByCore = true
  AND (missingImpl OR internalImplOnly)
  AND hasPortAnnotation
  → Application CONSUMES this interface
  → Infrastructure IMPLEMENTS this interface
```

---

### CQRS Pattern Criteria (Priority 75)

| Criteria | name() | Target Kind | Direction | Condition | Confidence |
|----------|--------|-------------|-----------|-----------|------------|
| `CommandPatternCriteria` | `command-pattern` | COMMAND | DRIVING | Has command-style methods (create*, update*, delete*) | HIGH |
| `QueryPatternCriteria` | `query-pattern` | QUERY | DRIVING | Has query-style methods (get*, find*, list*, search*) | HIGH |

**Command patterns**:
```
Method name prefixes: create, add, update, modify, delete, remove, cancel, process, execute, handle
Parameter types: *Command, *Request
Return types: void, CompletableFuture<Void>
```

**Query patterns**:
```
Method name prefixes: get, find, list, search, query, fetch, load, read, count, exists
Parameter types: *Query, *Criteria, *Filter
Return types: Optional<T>, List<T>, Page<T>, Stream<T>
```

---

### Dependency Injection Criteria (Priority 75)

| Criteria | name() | Target Kind | Direction | Condition | Confidence |
|----------|--------|-------------|-----------|-----------|------------|
| `InjectedAsDependencyCriteria` | `injected-as-dependency` | REPOSITORY | DRIVEN | Interface used as field type in application classes | HIGH |

**Details**:
```
Detects interfaces injected as dependencies:
  1. Interface appears as field type in multiple classes
  2. Excludes interfaces with driving port naming (UseCase, Handler, etc.)
  3. Implies the interface is a DRIVEN port (dependency to be provided)

Example:
  class OrderService {
      private final OrderRepository orderRepo;  // OrderRepository detected as DRIVEN
      private final PaymentGateway payment;     // PaymentGateway detected as DRIVEN
  }
```

---

### Signature-Based Criteria (Priority 70)

| Criteria | name() | Target Kind | Direction | Condition | Confidence |
|----------|--------|-------------|-----------|-----------|------------|
| `SignatureBasedDrivenPortCriteria` | `signature-based-driven` | REPOSITORY | DRIVEN | Interface manipulates aggregate-like types (types with identity fields) | MEDIUM |

**Details**:
```
Repository detection via method signatures:
  1. Methods accept or return types with identity fields
  2. CRUD-like method patterns (save, find, delete, etc.)
  3. Return types are domain types or Optional/List of domain types

Example:
  interface SomePort {
      Order save(Order order);           // Manipulates Order (has identity)
      Optional<Order> findById(OrderId); // Returns Order
      void delete(OrderId id);           // Accepts identity
  }
  → Classified as REPOSITORY
```

---

### Package-Based Criteria (Priority 60)

| Criteria | name() | Target Kind | Direction | Condition | Confidence |
|----------|--------|-------------|-----------|-----------|------------|
| `PackageInCriteria` | `package-in` | USE_CASE | DRIVING | Package contains `.in`, `.inbound`, or `.primary` | MEDIUM/HIGH* |
| `PackageOutCriteria` | `package-out` | REPOSITORY | DRIVEN | Package contains `.out`, `.outbound`, or `.secondary` | MEDIUM/HIGH* |

*Confidence is HIGH when `PackageOrganizationStyle` is `HEXAGONAL`.

**Package patterns**:
```
DRIVING (inbound):
  - *.ports.in.*
  - *.port.in.*
  - *.inbound.*
  - *.primary.*
  - *.driving.*

DRIVEN (outbound):
  - *.ports.out.*
  - *.port.out.*
  - *.outbound.*
  - *.secondary.*
  - *.driven.*
```

---

### Naming Convention Criteria (Priority 50)

| Criteria | name() | Target Kind | Direction | Condition | Confidence |
|----------|--------|-------------|-----------|-----------|------------|
| `NamingRepositoryCriteria` | `naming-repository` | REPOSITORY | DRIVEN | Name ends with `Repository` or `Repositories` | LOW |
| `NamingUseCaseCriteria` | `naming-use-case` | USE_CASE | DRIVING | Name ends with `UseCase`, `Service`, or `Handler` | LOW |
| `NamingGatewayCriteria` | `naming-gateway` | GATEWAY | DRIVEN | Name ends with `Gateway`, `Client`, `Adapter`, or `Port` | LOW |

**Note**: These are fallback criteria with LOW confidence. They are demoted from priority 80 to 50 to favor semantic and structural criteria.

---

## Criteria Evaluation Order

```
For each type:
  1. Evaluate ALL criteria
  2. Filter to matching criteria
  3. Sort by:
     a. Priority (descending) - higher wins
     b. Confidence (descending) - higher wins
     c. Name (ascending) - alphabetical for determinism
  4. Winner = first criteria after sorting
  5. Detect conflicts (other matches with different target kinds)
  6. Return ClassificationResult
```

---

## Files Reference

### Domain Criteria

| File | Location |
|------|----------|
| AbstractExplicitAnnotationCriteria | `domain/criteria/AbstractExplicitAnnotationCriteria.java` |
| ExplicitAggregateRootCriteria | `domain/criteria/ExplicitAggregateRootCriteria.java` |
| ExplicitEntityCriteria | `domain/criteria/ExplicitEntityCriteria.java` |
| ExplicitValueObjectCriteria | `domain/criteria/ExplicitValueObjectCriteria.java` |
| ExplicitIdentifierCriteria | `domain/criteria/ExplicitIdentifierCriteria.java` |
| ExplicitDomainEventCriteria | `domain/criteria/ExplicitDomainEventCriteria.java` |
| ImplementsJMoleculesInterfaceCriteria | `domain/criteria/ImplementsJMoleculesInterfaceCriteria.java` |
| RepositoryDominantCriteria | `domain/criteria/RepositoryDominantCriteria.java` |
| RecordSingleIdCriteria | `domain/criteria/RecordSingleIdCriteria.java` |
| InheritedClassificationCriteria | `domain/criteria/InheritedClassificationCriteria.java` |
| FlexibleSagaCriteria | `domain/criteria/FlexibleSagaCriteria.java` |
| FlexibleInboundOnlyCriteria | `domain/criteria/FlexibleInboundOnlyCriteria.java` |
| FlexibleOutboundOnlyCriteria | `domain/criteria/FlexibleOutboundOnlyCriteria.java` |
| EmbeddedValueObjectCriteria | `domain/criteria/EmbeddedValueObjectCriteria.java` |
| HasPortDependenciesCriteria | `domain/criteria/HasPortDependenciesCriteria.java` |
| HasIdentityCriteria | `domain/criteria/HasIdentityCriteria.java` |
| CollectionElementEntityCriteria | `domain/criteria/CollectionElementEntityCriteria.java` |
| ImmutableNoIdCriteria | `domain/criteria/ImmutableNoIdCriteria.java` |
| NamingDomainEventCriteria | `domain/criteria/NamingDomainEventCriteria.java` |
| StatelessNoDependenciesCriteria | `domain/criteria/StatelessNoDependenciesCriteria.java` |
| UnreferencedInPortsCriteria | `domain/criteria/UnreferencedInPortsCriteria.java` |

### Port Criteria

| File | Location |
|------|----------|
| AbstractExplicitPortAnnotationCriteria | `port/criteria/AbstractExplicitPortAnnotationCriteria.java` |
| ExplicitRepositoryCriteria | `port/criteria/ExplicitRepositoryCriteria.java` |
| ExplicitPrimaryPortCriteria | `port/criteria/ExplicitPrimaryPortCriteria.java` |
| ExplicitSecondaryPortCriteria | `port/criteria/ExplicitSecondaryPortCriteria.java` |
| SemanticDrivingPortCriteria | `port/criteria/SemanticDrivingPortCriteria.java` |
| SemanticDrivenPortCriteria | `port/criteria/SemanticDrivenPortCriteria.java` |
| CommandPatternCriteria | `port/criteria/CommandPatternCriteria.java` |
| QueryPatternCriteria | `port/criteria/QueryPatternCriteria.java` |
| InjectedAsDependencyCriteria | `port/criteria/InjectedAsDependencyCriteria.java` |
| SignatureBasedDrivenPortCriteria | `port/criteria/SignatureBasedDrivenPortCriteria.java` |
| PackageInCriteria | `port/criteria/PackageInCriteria.java` |
| PackageOutCriteria | `port/criteria/PackageOutCriteria.java` |
| NamingRepositoryCriteria | `port/criteria/NamingRepositoryCriteria.java` |
| NamingUseCaseCriteria | `port/criteria/NamingUseCaseCriteria.java` |
| NamingGatewayCriteria | `port/criteria/NamingGatewayCriteria.java` |

---

## See Also

- [CLASSIFICATION_ALGORITHM.md](CLASSIFICATION_ALGORITHM.md) - How criteria are evaluated
- [User Guide](../USER_GUIDE.md) - Classification from user perspective
- [SPI Reference](../SPI_REFERENCE.md) - IR model with DomainKind and PortKind

---

<div align="center">

**HexaGlue Internal Documentation**

</div>
