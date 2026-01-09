# User Guide

***Complete concepts and features reference for HexaGlue.***

---

## Overview

HexaGlue is an architectural design tool that analyzes your Java source code at compile time. It provides two main capabilities:

1. **Architecture Audit** - Validates your code against clean architecture principles and reports violations
2. **Code Generation** - Classifies domain types and ports, then generates infrastructure code through plugins

This guide explains the core concepts, classification system, and how HexaGlue makes its decisions.

---

## Table of Contents

1. [Architecture](#architecture)
2. [Architecture Audit](#architecture-audit)
3. [Intermediate Representation (IR)](#intermediate-representation-ir)
4. [Domain Classification](#domain-classification)
5. [Port Detection](#port-detection)
6. [Package Organization Styles](#package-organization-styles)
7. [Confidence Levels](#confidence-levels)
8. [jMolecules Integration](#jmolecules-integration)
9. [Classification Profiles](#classification-profiles)
10. [Troubleshooting Classification](#troubleshooting-classification)

---

## Architecture

HexaGlue follows a multi-phase architecture with two output paths:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Analysis     │────▶│  Classification │────▶│      Audit      │
│  (hexaglue-core)│     │   & Validation  │     │    (rules)      │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                │                        │
                                │                        ▼
                                │               ┌─────────────────┐
                                │               │  Audit Reports  │
                                │               │ (HTML/JSON/MD)  │
                                │               └─────────────────┘
                                ▼
                        ┌─────────────────┐     ┌─────────────────┐
                        │       IR        │────▶│   Generation    │
                        │  (hexaglue-spi) │     │    (plugins)    │
                        └─────────────────┘     └─────────────────┘
```

1. **Analysis Phase** - Parses Java source files and builds a semantic model
2. **Classification Phase** - Classifies domain types and ports with confidence levels
3. **Audit Phase** (optional) - Validates architecture against rules and generates reports
4. **IR Phase** - Builds an immutable Intermediate Representation
5. **Generation Phase** - Plugins receive the IR and generate code

### Maven Goals

| Goal | Phase | Description |
|------|-------|-------------|
| `hexaglue:audit` | `verify` | Architecture audit only |
| `hexaglue:generate` | `generate-sources` | Code generation only |
| `hexaglue:generate-and-audit` | `generate-sources` | Both combined |

### Key Design Principles

- **Non-invasive**: HexaGlue never modifies your source code
- **Compile-time**: All analysis happens during Maven build
- **Plugin-based**: Generation logic is in plugins, not the core
- **Framework-agnostic**: Core has no framework dependencies

---

## Architecture Audit

HexaGlue's audit capability validates your codebase against clean architecture principles. It supports multiple architectural styles: **Hexagonal**, **Clean**, **Onion**, and **Layered**.

### What the Audit Validates

**Layering Rules** (Dependency Inversion Principle):
- Domain layer must not depend on infrastructure
- Presentation must not bypass application services
- Application must not depend on presentation

**Dependency Rules**:
- Dependencies should flow toward stable components
- No circular dependencies between packages

**Quality Rules**:
- Naming conventions (Repository, DTO, Controller suffixes)
- Documentation coverage for public APIs
- Complexity limits (cyclomatic complexity, method length)

### Audit Output

The audit produces:

| Format | File | Usage |
|--------|------|-------|
| Console | (logs) | Quick viewing in CI/CD |
| HTML | `hexaglue-audit.html` | Rich interactive report |
| JSON | `hexaglue-audit.json` | Machine-readable for tooling |
| Markdown | `hexaglue-audit.md` | Documentation in repositories |

**Output location**: `target/hexaglue-reports/`

### Violation Severities

| Severity | Build Impact | Description |
|----------|--------------|-------------|
| **ERROR** | Fails build (if `failOnError=true`) | Critical architectural violations |
| **WARNING** | Fails build (if `failOnWarning=true`) | Potential issues to address |
| **INFO** | Never fails build | Suggestions for improvement |

### Quality Metrics

The audit computes these quality metrics:

| Metric | Range | Good Value |
|--------|-------|------------|
| Test Coverage | 0-100% | > 80% |
| Documentation Coverage | 0-100% | > 70% |
| Technical Debt | minutes | < 480 (8h) |
| Maintainability Rating | 0-5 | > 4.0 |

### Learn More

For complete audit configuration, rules reference, and CI/CD integration, see the [Architecture Audit Guide](ARCHITECTURE_AUDIT.md).

---

## Intermediate Representation (IR)

The IR is the central data model passed between analysis and generation phases.

### Structure

```
IrSnapshot
├── DomainModel
│   └── List<DomainType>
│       ├── qualifiedName, simpleName, packageName
│       ├── DomainKind (AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, ...)
│       ├── ConfidenceLevel (EXPLICIT, HIGH, MEDIUM, LOW)
│       ├── Identity (fieldName, type, strategy)
│       ├── List<DomainProperty>
│       └── List<DomainRelation>
├── PortModel
│   └── List<Port>
│       ├── qualifiedName, simpleName, packageName
│       ├── PortKind (REPOSITORY, GATEWAY, USE_CASE, ...)
│       ├── PortDirection (DRIVING, DRIVEN)
│       └── List<PortMethod>
└── IrMetadata
    ├── basePackage
    ├── timestamp
    └── engineVersion
```

### DomainKind Reference

| Kind | Description | Detection Signals |
|------|-------------|-------------------|
| `AGGREGATE_ROOT` | Entry point to an aggregate | Has identity, managed by repository, or `@AggregateRoot` |
| `ENTITY` | Entity within an aggregate | Has identity, not an aggregate root |
| `VALUE_OBJECT` | Immutable by attributes | Record, or immutable class, or `@ValueObject` |
| `IDENTIFIER` | Wraps identity value | Single-field record/class used as ID |
| `DOMAIN_EVENT` | Something that happened | Suffix `Event`, or `@DomainEvent` |
| `DOMAIN_SERVICE` | Stateless operation | Suffix `Service` in domain package |

### PortKind Reference

| Kind | Description | Detection Signals |
|------|-------------|-------------------|
| `REPOSITORY` | Persistence abstraction | Suffix `Repository`, CRUD-like methods |
| `GATEWAY` | External system interface | Suffix `Gateway`, `Client`, `Adapter` |
| `USE_CASE` | Business operation | Suffix `UseCase`, in `ports.in` package |
| `COMMAND` | Command handler | Suffix `Command`, single execute method |
| `QUERY` | Query handler | Suffix `Query`, read-only methods |
| `EVENT_PUBLISHER` | Event publishing | Suffix `Publisher`, `EventEmitter` |
| `GENERIC` | Unclassified port | Default when no pattern matches |

### PortDirection Reference

| Direction | Also Known As | Package Patterns |
|-----------|---------------|------------------|
| `DRIVING` | Primary, Inbound | `ports.in`, `port.in`, `driving` |
| `DRIVEN` | Secondary, Outbound | `ports.out`, `port.out`, `driven` |

---

## Domain Classification

HexaGlue uses multiple signals to classify domain types.

### Classification Priority

Classifications are determined in priority order:

1. **Explicit annotations** (jMolecules) - highest priority
2. **Repository relationships** - types managed by repositories
3. **Structural patterns** - records, immutability
4. **Naming conventions** - suffixes, package names
5. **Heuristics** - identity fields, relationships

### Aggregate Root Detection

A type is classified as `AGGREGATE_ROOT` when:

| Signal | Confidence | Example |
|--------|------------|---------|
| `@AggregateRoot` annotation | EXPLICIT | `@AggregateRoot class Order` |
| Used in Repository signature | HIGH | `OrderRepository.save(Order)` |
| Has identity + in aggregate package | MEDIUM | `order/Order.java` with `id` field |
| Has identity + multiple entity relationships | MEDIUM | `Order` with `List<LineItem>` |

### Entity Detection

A type is classified as `ENTITY` when:

| Signal | Confidence | Example |
|--------|------------|---------|
| `@Entity` annotation | EXPLICIT | `@Entity class LineItem` |
| Has identity + owned by aggregate | HIGH | `LineItem` referenced by `Order` |
| Has identity + mutable | MEDIUM | Class with `id` field and setters |

### Value Object Detection

A type is classified as `VALUE_OBJECT` when:

| Signal | Confidence | Example |
|--------|------------|---------|
| `@ValueObject` annotation | EXPLICIT | `@ValueObject class Money` |
| Is a Java record | HIGH | `record Money(...)` |
| Immutable class (final fields, no setters) | HIGH | All `private final` fields |
| Embedded in entity | MEDIUM | Used as `@Embedded` |

### Identifier Detection

A type is classified as `IDENTIFIER` when:

| Signal | Confidence | Example |
|--------|------------|---------|
| `@Identity` on field | EXPLICIT | Field annotated with `@Identity` |
| Single-field record with `Id` suffix | HIGH | `record OrderId(UUID value)` |
| Wraps UUID/Long + used as identity | MEDIUM | `CustomerId` wrapping `UUID` |

### Domain Event Detection

A type is classified as `DOMAIN_EVENT` when:

| Signal | Confidence | Example |
|--------|------------|---------|
| `@DomainEvent` annotation | EXPLICIT | `@DomainEvent class OrderPlaced` |
| Suffix `Event` + record | HIGH | `record OrderPlacedEvent(...)` |
| In `events` package + immutable | MEDIUM | `domain.events.OrderPlaced` |

---

## Port Detection

Ports are interfaces that define boundaries in hexagonal architecture.

### Port Discovery

HexaGlue looks for interfaces in these locations:

1. Packages matching `ports`, `port`, `adapter` patterns
2. Interfaces with specific suffixes (`Repository`, `Gateway`, etc.)
3. Interfaces annotated with jMolecules `@Repository`, `@Service`

### Repository Detection

An interface is classified as `REPOSITORY` when:

| Signal | Confidence | Example |
|--------|------------|---------|
| `@Repository` annotation | EXPLICIT | `@Repository interface OrderRepo` |
| Suffix `Repository` | HIGH | `OrderRepository` |
| CRUD-like methods (save, find, delete) | HIGH | `Order save(Order)` |
| In `ports.out` or similar package | MEDIUM | `ports.out.OrderRepository` |

### Direction Detection

Port direction determines if it's called by external actors (DRIVING) or by the application (DRIVEN).

**DRIVING (Primary/Inbound)**:
- Package: `ports.in`, `port.in`, `driving`, `primary`
- Suffixes: `UseCase`, `Query`, `Command`
- Annotations: `@PrimaryPort`

**DRIVEN (Secondary/Outbound)**:
- Package: `ports.out`, `port.out`, `driven`, `secondary`
- Suffixes: `Repository`, `Gateway`, `Client`
- Annotations: `@SecondaryPort`
- Default when no clear signal

---

## Package Organization Styles

HexaGlue detects your project's package organization to improve classification accuracy.

### Detected Styles

| Style | Pattern | Example |
|-------|---------|---------|
| `HEXAGONAL` | ports.in/out, domain, application | `com.example.ports.in.OrderUseCase` |
| `BY_LAYER` | controller, service, repository | `com.example.service.OrderService` |
| `BY_FEATURE` | feature/domain, feature/port | `com.example.order.domain.Order` |
| `MIXED` | Combination of styles | Various patterns |
| `UNKNOWN` | Cannot determine | Flat structure |

### Style Detection Impact

The detected style influences classification:

- **HEXAGONAL**: Strong trust in port direction from package
- **BY_LAYER**: Maps layers to hexagonal concepts
- **BY_FEATURE**: Looks for domain/port patterns within features

---

## Confidence Levels

Every classification has an associated confidence level.

### Confidence Hierarchy

| Level | Meaning | Source |
|-------|---------|--------|
| `EXPLICIT` | Developer explicitly declared | jMolecules annotations |
| `HIGH` | Strong signals converge | Repository relationship + naming |
| `MEDIUM` | Some signals present | Package pattern or naming alone |
| `LOW` | Weak signals only | Heuristics, needs verification |

### Using Confidence in Plugins

Plugins can filter by confidence:

```java
// Process only reliable classifications
for (DomainType type : domain.aggregateRoots()) {
    if (type.confidence().isReliable()) { // EXPLICIT or HIGH
        generateCode(type);
    } else {
        context.diagnostics().warn(
            "Skipping " + type.simpleName() + " (low confidence)"
        );
    }
}
```

---

## jMolecules Integration

HexaGlue optionally integrates with [jMolecules](https://github.com/xmolecules/jmolecules) annotations.

### Supported Annotations

**Domain Annotations** (`org.jmolecules.ddd.annotation`):

| Annotation | Maps To |
|------------|---------|
| `@AggregateRoot` | `DomainKind.AGGREGATE_ROOT` |
| `@Entity` | `DomainKind.ENTITY` |
| `@ValueObject` | `DomainKind.VALUE_OBJECT` |
| `@Identity` | Identity field marker |
| `@DomainEvent` | `DomainKind.DOMAIN_EVENT` |

**Port Annotations** (`org.jmolecules.architecture.hexagonal`):

| Annotation | Maps To |
|------------|---------|
| `@PrimaryPort` | `PortDirection.DRIVING` |
| `@SecondaryPort` | `PortDirection.DRIVEN` |

**Repository Annotation** (`org.jmolecules.ddd.annotation`):

| Annotation | Maps To |
|------------|---------|
| `@Repository` | `PortKind.REPOSITORY` |

### Using jMolecules

Add jMolecules to your project:

```xml
<dependency>
    <groupId>org.jmolecules</groupId>
    <artifactId>jmolecules-ddd</artifactId>
    <version>1.9.0</version>
</dependency>
```

Annotate your domain:

```java
import org.jmolecules.ddd.annotation.*;

@AggregateRoot
public class Order {

    @Identity
    private OrderId id;

    private List<LineItem> items;
}

@ValueObject
public record Money(BigDecimal amount, Currency currency) {}
```

### Without jMolecules

jMolecules is **optional**. Without annotations, HexaGlue uses heuristics:

```java
// Still detected as AGGREGATE_ROOT through heuristics:
// - Has identity field (id)
// - Managed by OrderRepository
// - Contains entity collection (items)
public class Order {
    private OrderId id;
    private List<LineItem> items;
}
```

---

## Classification Profiles

Classification profiles allow you to adjust how HexaGlue prioritizes different detection criteria. This is useful when your codebase follows conventions that differ from HexaGlue's defaults.

### Available Profiles

| Profile | Description |
|---------|-------------|
| *(default)* | Legacy behavior with standard priorities |
| `default` | Documented reference configuration |
| `strict` | Explicit annotations win over heuristics |
| `annotation-only` | Only explicit annotations are trusted |
| `repository-aware` | Better detection for plural-named repositories |

### When to Use Profiles

**`repository-aware`** - Use when your driven ports use plural names:

```java
// This interface might be misclassified as DRIVING/COMMAND
// because save() and delete() match command patterns
public interface Orders {
    Order save(Order order);      // Looks like command
    Optional<Order> findById(OrderId id);
    void delete(Order order);     // Looks like command
}
```

With `repository-aware`, signature-based detection (types with identity) and package-based detection (`ports.out`) take precedence over command pattern detection.

**`strict`** - Use when you have consistent jMolecules annotations:

```java
@SecondaryPort
@Repository
public interface OrderRepository {
    // Annotations are trusted over naming patterns
}
```

**`annotation-only`** - Use for maximum control:

```java
// Only types with explicit annotations will be classified
@AggregateRoot
public class Order { ... }

// This will NOT be classified (no annotation)
public class Product { ... }
```

### Configuring a Profile

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <configuration>
        <basePackage>com.example</basePackage>
        <classificationProfile>repository-aware</classificationProfile>
    </configuration>
</plugin>
```

Or via command line:

```bash
mvn compile -Dhexaglue.classificationProfile=strict
```

### How Profiles Work

Profiles adjust the **priority** of classification criteria. Higher priority criteria win when multiple criteria match.

**Example: Default vs Repository-Aware**

| Criteria | Default Priority | Repository-Aware |
|----------|-----------------|------------------|
| `port.pattern.command` | 75 | **72** |
| `port.signature.drivenPort` | 70 | **78** |
| `port.package.out` | 60 | **74** |

In `repository-aware`, signature-based detection (78) beats command pattern (72), so `Orders` with CRUD methods in `ports.out` is correctly classified as DRIVEN/REPOSITORY.

---

## Troubleshooting Classification

### Type Not Detected as Expected

**Problem**: `Order` is classified as `ENTITY` instead of `AGGREGATE_ROOT`

**Solutions**:

1. **Add explicit annotation**:
   ```java
   @AggregateRoot
   public class Order { ... }
   ```

2. **Ensure repository relationship**:
   ```java
   public interface OrderRepository {
       Order save(Order order);  // Links Order as aggregate
   }
   ```

3. **Check package structure**:
   - Use `domain.aggregate` or `domain.order` packages
   - Avoid generic `domain.model` packages

### Port Direction Wrong

**Problem**: Repository detected as `DRIVING` instead of `DRIVEN`

**Solutions**:

1. **Use a classification profile** (recommended for plural-named ports):
   ```xml
   <configuration>
       <classificationProfile>repository-aware</classificationProfile>
   </configuration>
   ```

2. **Use standard package**:
   ```
   ports.out.OrderRepository  // Clear DRIVEN signal
   ```

3. **Add annotation**:
   ```java
   @SecondaryPort
   public interface OrderRepository { ... }
   ```

### Missing Types in IR

**Problem**: A type doesn't appear in the generated IR

**Causes**:
- Type is outside the `basePackage`
- Type is not in a recognized domain/port package
- Type has no domain characteristics

**Solutions**:

1. **Verify basePackage** in Maven plugin configuration
2. **Place types** in domain/port packages
3. **Add markers** (identity field, annotations, etc.)

### Low Confidence Classifications

**Problem**: Types have `LOW` confidence

**This means** HexaGlue isn't sure about the classification.

**To improve confidence**:

1. Use jMolecules annotations (→ EXPLICIT)
2. Follow naming conventions (→ HIGH)
3. Use standard package patterns (→ HIGH)
4. Ensure repository relationships (→ HIGH)

### Viewing Classification Results

To see what HexaGlue detected, enable debug output:

```bash
mvn compile -X
```

Or use the Living Documentation plugin to generate a report of all detected types and ports.

---

## Summary

| Concept | What It Does |
|---------|--------------|
| **IR** | Immutable model of analyzed application |
| **DomainModel** | Contains all domain types |
| **DomainType** | Single domain type with classification |
| **DomainKind** | Type classification (AGGREGATE_ROOT, etc.) |
| **PortModel** | Contains all ports |
| **Port** | Single port with direction |
| **PortDirection** | DRIVING (in) or DRIVEN (out) |
| **ConfidenceLevel** | How reliable the classification is |
| **Package Style** | Detected project organization |

---

<div align="center">

**HexaGlue - Design, Audit, and Generate Hexagonal Architecture**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
