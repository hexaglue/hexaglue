# HexaGlue Module Architecture Reference

> **DEPRECATED**: This document describes v4.0.0 and is significantly outdated.
> The IR layer (`hexaglue-spi-arch`, `IrExporter`, `IrSnapshot`) has been removed since v5.0.0.
> Plugins now use `PluginContext.model()` to access the `ArchitecturalModel` directly.
> The REST plugin (v3.1.0) is not covered here.
> For current module structure, see the main project CLAUDE.md.

> Version: 4.0.0 (outdated)
> Date: 2026-01-16
> Status: Deprecated

## Table of Contents

1. [Overview](#1-overview)
2. [Module Hierarchy](#2-module-hierarchy)
3. [Layer Architecture](#3-layer-architecture)
4. [Data Flow](#4-data-flow)
5. [Key Interfaces](#5-key-interfaces)
6. [Module Dependencies](#6-module-dependencies)
7. [Design Patterns](#7-design-patterns)
8. [Plugin Architecture](#8-plugin-architecture)

---

## 1. Overview

HexaGlue is a compile-time code generation engine for Java applications following Hexagonal Architecture. It analyzes domain code and generates infrastructure adapters (JPA entities, repositories, REST controllers, etc.) automatically.

### Core Principles

- **Parser Abstraction**: Syntax layer decouples analysis from specific AST implementations
- **Unified Architectural Model**: Single source of truth for all classified elements
- **Zero-Dependency Syntax API**: `hexaglue-syntax-api` has no external dependencies
- **Type-Safe References**: `ElementRef<T>` provides compile-time safety for cross-element navigation
- **Explainability**: `ClassificationTrace` provides human-readable explanations for every classification decision

> **Etat v4.0.0**: Le package `io.hexaglue.spi.ir` est deprecated. Utiliser `ArchitecturalModel` via `context.model()`.

---

## 2. Module Hierarchy

```
hexaglue-parent (v4.0.0)
│
├── build/tools                      # Build utilities
│
├── hexaglue-syntax/                 # Parser Abstraction Layer
│   ├── hexaglue-syntax-api          # Interfaces (ZERO external deps)
│   └── hexaglue-syntax-spoon        # Spoon implementation
│
├── hexaglue-arch/                   # Unified Architectural Model (v4)
│
├── hexaglue-spi/                    # Plugin SPI
│   └── io.hexaglue.spi.ir/          # ⚠️ DEPRECATED (removal v5.0.0)
├── hexaglue-spi-arch/               # ⚠️ DEPRECATED (removal v5.0.0)
│
├── hexaglue-core/                   # Analysis Engine
│
├── hexaglue-testing/                # Test utilities
│
├── hexaglue-plugins/                # Official plugins
│   ├── hexaglue-plugin-jpa          # JPA entity/repository generation
│   ├── hexaglue-plugin-audit        # Architecture audit & C4 diagrams
│   └── hexaglue-plugin-living-doc   # Architecture documentation
│
├── hexaglue-maven-plugin/           # Maven integration
│
└── hexaglue-benchmarks/             # Performance benchmarks
```

### Module Versioning

| Module Group | Version | Notes |
|--------------|---------|-------|
| Core modules | 4.0.0 | Via hexaglue-parent |
| Plugins | 1.0.0 | Independent versioning |

---

## 3. Layer Architecture

HexaGlue is organized into distinct layers with strict dependency rules:

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CONSUMER LAYER                              │
│   ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐    │
│   │ maven-plugin │  │  benchmarks  │  │       testing         │    │
│   └──────────────┘  └──────────────┘  └───────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         PLUGIN LAYER                                │
│   ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐    │
│   │  plugin-jpa  │  │ plugin-audit │  │   plugin-living-doc   │    │
│   └──────────────┘  └──────────────┘  └───────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          CORE LAYER                                 │
│                       ┌──────────────┐                              │
│                       │ hexaglue-core│                              │
│                       └──────────────┘                              │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       CONTRACT LAYER                                │
│         ┌─────────────────────┐  ┌──────────────────────┐          │
│         │    hexaglue-spi     │  │   hexaglue-spi-arch  │          │
│         │  (ZERO ext. deps)   │  │  ⚠️ DEPRECATED       │          │
│         │  *.ir DEPRECATED    │  │  (removal v5.0.0)    │          │
│         └─────────────────────┘  └──────────────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       MODEL LAYER                                   │
│                      ┌──────────────┐                               │
│                      │ hexaglue-arch│                               │
│                      └──────────────┘                               │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SYNTAX LAYER                                   │
│   ┌─────────────────────┐        ┌──────────────────────────┐      │
│   │  hexaglue-syntax-api│        │  hexaglue-syntax-spoon   │      │
│   │  (ZERO ext. deps)   │◄───────│     (Spoon impl.)        │      │
│   └─────────────────────┘        └──────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

| Layer | Responsibility |
|-------|----------------|
| **Syntax** | AST abstraction, source code parsing |
| **Model** | Unified architectural model, element registry |
| **Contract** | Plugin SPI, extension points (IR format deprecated) |
| **Core** | Classification engine, graph analysis |
| **Plugin** | Code generation, auditing, documentation |
| **Consumer** | Maven goals, CLI integration |

---

## 4. Data Flow

The data flows from source code through the analysis pipeline to plugin execution:

```
┌─────────────────────────────────────────────────────────────────────┐
│                     SOURCE CODE FILES                               │
│                    (*.java in base package)                         │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   1. SYNTAX PROVIDER                                │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │              SpoonSyntaxProvider                            │    │
│  │  - Parses Java source files using Spoon AST                 │    │
│  │  - Produces: TypeSyntax, MethodSyntax, FieldSyntax,         │    │
│  │              AnnotationSyntax, ConstructorSyntax            │    │
│  └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Stream<TypeSyntax>
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│               2. ARCHITECTURAL MODEL BUILDER                        │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │            ArchitecturalModelBuilder                        │    │
│  │                                                             │    │
│  │  Step 1: Index types                                        │    │
│  │    - Repository dominant types                              │    │
│  │    - Subtype relationships                                  │    │
│  │                                                             │    │
│  │  Step 2: Build ClassificationContext                        │    │
│  │    - Provides indexed lookups for classifiers               │    │
│  │                                                             │    │
│  │  Step 3: Classify each type                                 │    │
│  │    - Interfaces → PortClassifier                            │    │
│  │    - Classes/Records → DomainClassifier                     │    │
│  │                                                             │    │
│  │  Step 4: Create ArchElement instances                       │    │
│  │    - DomainEntity, ValueObject, Identifier, etc.            │    │
│  │    - DrivingPort, DrivenPort                                │    │
│  │    - Each with ClassificationTrace                          │    │
│  │                                                             │    │
│  │  Step 5: Build ElementRegistry & RelationshipStore          │    │
│  └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ ArchitecturalModel
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  3. ARCHITECTURAL MODEL                             │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │                  ArchitecturalModel                         │    │
│  │                                                             │    │
│  │  ┌─────────────────┐  ┌─────────────────────────────┐      │    │
│  │  │ ElementRegistry │  │    RelationshipStore        │      │    │
│  │  │ - All elements  │  │ - MANAGES (repo→aggregate)  │      │    │
│  │  │ - Index by kind │  │ - IMPLEMENTS (impl→port)    │      │    │
│  │  │ - Index by pkg  │  │ - DEPENDS_ON, REFERENCES    │      │    │
│  │  └─────────────────┘  └─────────────────────────────┘      │    │
│  │                                                             │    │
│  │  Domain: Aggregate, DomainEntity, ValueObject,              │    │
│  │          Identifier, DomainEvent, DomainService             │    │
│  │                                                             │    │
│  │  Ports:  DrivingPort, DrivenPort, ApplicationService        │    │
│  │                                                             │    │
│  │  Adapters: DrivingAdapter, DrivenAdapter                    │    │
│  └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ PluginContext (with model())
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     4. PLUGIN EXECUTION                             │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │                 Plugin Executor                             │    │
│  │                                                             │    │
│  │  1. Load plugins via ServiceLoader                          │    │
│  │  2. Resolve dependencies (topological sort)                 │    │
│  │  3. Execute each plugin in order                            │    │
│  │     - Plugin receives PluginContext                         │    │
│  │     - Plugin accesses model via context.model()             │    │
│  │     - Plugin generates code via context.writer()            │    │
│  │     - Plugin reports issues via context.diagnostics()       │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌───────────────────────┐       │
│  │  JpaPlugin  │  │ AuditPlugin │  │   LivingDocPlugin     │       │
│  │ Generates:  │  │ Generates:  │  │ Generates:            │       │
│  │ - @Entity   │  │ - Reports   │  │ - Documentation       │       │
│  │ - @Repository│ │ - C4 diagrams│ │ - Architecture views  │       │
│  │ - Mappers   │  │ - Metrics   │  │                       │       │
│  └─────────────┘  └─────────────┘  └───────────────────────┘       │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     5. OUTPUT                                       │
│  - Generated Java source files (target/generated-sources)          │
│  - Audit reports (HTML, JSON, Markdown)                            │
│  - C4 architecture diagrams                                         │
│  - Living documentation                                             │
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow Summary

| Stage | Input | Output | Key Classes |
|-------|-------|--------|-------------|
| 1. Parsing | Java sources | `Stream<TypeSyntax>` | `SpoonSyntaxProvider` |
| 2. Building | `TypeSyntax` | `ArchitecturalModel` | `ArchitecturalModelBuilder` |
| 3. Model | - | Registry + Relations | `ElementRegistry`, `RelationshipStore` |
| 4. Plugins | `PluginContext` | Generated code | `HexaGluePlugin` implementations |

---

## 5. Key Interfaces

### 5.1 Syntax Layer

#### SyntaxProvider
**Location**: `hexaglue-syntax-api/src/main/java/io/hexaglue/syntax/SyntaxProvider.java`

```java
public interface SyntaxProvider {
    Stream<TypeSyntax> types();                    // All types in scope
    Optional<TypeSyntax> type(String qualifiedName); // Specific type
    SyntaxMetadata metadata();                     // Analysis metadata
    SyntaxCapabilities capabilities();             // Parser capabilities
}
```

#### TypeSyntax
**Location**: `hexaglue-syntax-api/src/main/java/io/hexaglue/syntax/TypeSyntax.java`

Represents a type (class, interface, record, enum):
- `qualifiedName()`, `simpleName()`, `packageName()`
- `form()` - CLASS, INTERFACE, RECORD, ENUM, ANNOTATION
- `modifiers()` - Access modifiers
- `superType()`, `interfaces()` - Type hierarchy
- `fields()`, `methods()`, `constructors()` - Members
- `annotations()` - Type annotations

### 5.2 Model Layer

#### ArchitecturalModel
**Location**: `hexaglue-arch/src/main/java/io/hexaglue/arch/ArchitecturalModel.java`

```java
public record ArchitecturalModel(
    ProjectContext project,
    ElementRegistry registry,           // All classified elements
    RelationshipStore relationships,    // O(1) relationship lookups
    AnalysisMetadata analysisMetadata
) {
    // Domain access
    Stream<Aggregate> aggregates();
    Stream<DomainEntity> domainEntities();
    Stream<ValueObject> valueObjects();
    Stream<Identifier> identifiers();
    Stream<DomainEvent> domainEvents();

    // Port access
    Stream<DrivingPort> drivingPorts();
    Stream<DrivenPort> drivenPorts();
    Stream<ApplicationService> applicationServices();

    // Reference resolution
    <T extends ArchElement> Optional<T> resolve(ElementRef<T> ref);

    // Relationship queries (O(1))
    Optional<DrivenPort> repositoryFor(ElementId aggregateId);

    // Fluent query API
    ModelQuery query();
}
```

#### ElementRegistry
**Location**: `hexaglue-arch/src/main/java/io/hexaglue/arch/ElementRegistry.java`

Central registry - single source of truth:
- `get(ElementId id)` - Get element by ID
- `get(ElementId id, Class<T> type)` - Type-safe get
- `all(Class<T> type)` - Stream of elements by type
- `ofKind(ElementKind kind)` - Stream by kind
- `inPackage(String packageName)` - Stream by package

#### ElementRef<T>
**Location**: `hexaglue-arch/src/main/java/io/hexaglue/arch/ElementRef.java`

Type-safe reference:
```java
public record ElementRef<T extends ArchElement>(
    ElementId id,
    Class<T> expectedType
) {
    ResolutionResult<T> resolve(ElementRegistry registry);
    Optional<T> resolveOpt(ElementRegistry registry);
    T resolveOrThrow(ElementRegistry registry);
}
```

### 5.3 Contract Layer

#### HexaGluePlugin
**Location**: `hexaglue-spi/src/main/java/io/hexaglue/spi/plugin/HexaGluePlugin.java`

```java
public interface HexaGluePlugin {
    String id();                          // Unique identifier
    List<String> dependsOn();             // Plugin dependencies
    PluginCategory category();            // GENERATOR, AUDIT, etc.
    void execute(PluginContext context);  // Main execution
}
```

#### PluginContext (v4)
**Location**: `hexaglue-spi/src/main/java/io/hexaglue/spi/plugin/PluginContext.java`

```java
public interface PluginContext {
    ArchitecturalModel model();           // v4 unified model
    PluginConfig config();                // Configuration
    CodeWriter writer();                  // Code generation
    DiagnosticReporter diagnostics();     // Issue reporting
    <T> void setOutput(String key, T value);  // Plugin output sharing
}
```

#### ArchModelPluginContext (DEPRECATED)
**Location**: `hexaglue-spi-arch/src/main/java/io/hexaglue/spi/arch/ArchModelPluginContext.java`

> ⚠️ **Deprecated**: Ce module sera supprimé en v5.0.0. Utiliser `PluginContext.model()` directement.

```java
@Deprecated(forRemoval = true, since = "4.0.0")
public interface ArchModelPluginContext extends PluginContext {
    // Plus nécessaire - PluginContext.model() est maintenant disponible
}
```

### 5.4 Builder Layer

#### ArchitecturalModelBuilder
**Location**: `hexaglue-arch/src/main/java/io/hexaglue/arch/builder/ArchitecturalModelBuilder.java`

Orchestrates the classification process:
1. Index types (repository dominant types, subtypes)
2. Build `ClassificationContext`
3. Run `DomainClassifier` on classes/records
4. Run `PortClassifier` on interfaces
5. Create `ArchElement` instances
6. Build and return `ArchitecturalModel`

---

## 6. Module Dependencies

### Dependency Graph

```
                    hexaglue-syntax-api
                    (ZERO ext. deps)
                           ▲
              ┌────────────┼────────────┐
              │            │            │
    hexaglue-syntax-spoon  │    hexaglue-arch
       (spoon-core)        │            │
              │            │            │
              │            │            ▼
              │            │    hexaglue-spi
              │            │    (ZERO ext. deps)
              │            │    *.ir DEPRECATED
              │            │            │
              │            │            ▼
              │            │    hexaglue-spi-arch ⚠️
              │            │    (DEPRECATED v5.0.0)
              │            ├────────────┤
              │            │            │
              └────────────▼────────────┘
                     hexaglue-core
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
       maven-plugin     plugins     testing
```

> **Note v5.0.0**: Le module `hexaglue-spi-arch` sera supprimé. Le package `io.hexaglue.spi.ir`
> sera également supprimé. Les plugins utiliseront directement `PluginContext.model()`.

### External Dependencies by Module

| Module | External Dependencies | Scope | Status |
|--------|----------------------|-------|--------|
| hexaglue-syntax-api | None | - | Active |
| hexaglue-syntax-spoon | spoon-core 11.3.0 | compile | Active |
| hexaglue-arch | None | - | Active |
| hexaglue-spi | None | - | Active (*.ir deprecated) |
| hexaglue-spi-arch | None | - | ⚠️ DEPRECATED |
| hexaglue-core | spoon-core, snakeyaml (optional), slf4j-api | compile | Active |
| hexaglue-maven-plugin | snakeyaml, maven APIs (provided) | compile/provided | Active |
| hexaglue-plugin-jpa | javapoet 0.7.0, jakarta-persistence (provided) | compile/provided | Active |
| hexaglue-plugin-audit | None | - | Active |
| hexaglue-plugin-living-doc | None | - | Active |

### Dependency Rules

1. **hexaglue-syntax-api**: MUST have ZERO external dependencies
2. **hexaglue-spi**: MUST have ZERO external dependencies
3. **hexaglue-arch**: MUST depend on syntax-api, NOT on implementations
4. **Plugins**: Depend only on spi/spi-arch, not on core

---

## 7. Design Patterns

### 7.1 Type-Safe References

`ElementRef<T>` uses `Class<T>` for type-safe resolution:

```java
ElementRef<Aggregate> ref = ElementRef.of(id, Aggregate.class);
Optional<Aggregate> agg = ref.resolveOpt(registry);  // Type-safe
```

### 7.2 Immutability

All model classes are records with defensive copying:
- `ArchitecturalModel`, `Aggregate`, `DomainEntity`, etc.
- Collections are copied via `List.copyOf()`, `Map.copyOf()`
- No setter methods

### 7.3 Sealed Interfaces

`ArchElement` is a sealed interface controlling subtypes:

```java
public sealed interface ArchElement
    permits ArchElement.Marker {

    ElementId id();
    ElementKind kind();
    // ...

    non-sealed interface Marker extends ArchElement {}
}
```

Domain elements implement `ArchElement.Marker`:
- `Aggregate`, `DomainEntity`, `ValueObject`, `Identifier`, etc.

### 7.4 O(1) Lookups

`ElementRegistry` and `RelationshipStore` are indexed:
- `byKind`: Map<ElementKind, List<ElementId>>
- `byPackage`: Map<String, List<ElementId>>
- `repositoryFor`: Map<ElementId, ElementId>

### 7.5 Fluent Query API

```java
model.query()
     .aggregates()
     .withRepository()
     .inPackage("com.example.domain")
     .forEach(agg -> process(agg));
```

### 7.6 Evidence-Based Classification

`ClassificationTrace` explains every decision:

```java
ClassificationTrace trace = entity.classificationTrace();
String explanation = trace.explain();  // Human-readable
List<String> hints = trace.remediationHints();  // Actionable
```

### 7.7 Extensible Classification

`ClassificationCriterion` interface for custom rules:

```java
public interface ClassificationCriterion {
    CriterionMatch matches(TypeSyntax type, ClassificationContext context);
}
```

---

## 8. Plugin Architecture

### 8.1 Plugin Discovery

Plugins are discovered via `ServiceLoader`:

```
META-INF/services/io.hexaglue.spi.plugin.HexaGluePlugin
```

### 8.2 Plugin Execution Order

1. Load all plugins
2. Resolve dependencies (topological sort)
3. Filter by enabled categories
4. Execute in dependency order
5. Handle failures (skip dependents)

### 8.3 Plugin Categories

| Category | Purpose | Examples |
|----------|---------|----------|
| `GENERATOR` | Code generation | JPA, REST controllers |
| `AUDIT` | Architecture validation | Hexagonal compliance |
| `ENRICHMENT` | Semantic labeling | - |
| `ANALYSIS` | Custom analysis | Metrics, reports |

### 8.4 Plugin Context Access

**v4 plugins** (current):
```java
public void execute(PluginContext context) {
    ArchitecturalModel model = context.model();

    model.aggregates().forEach(agg -> {
        System.out.println(agg.id().simpleName());
        System.out.println(agg.classificationTrace().explain());
    });
}
```

**v3 plugins** (deprecated - removal v5.0.0):
```java
// ⚠️ DEPRECATED - Ne pas utiliser dans du nouveau code
public void execute(PluginContext context) {
    IrSnapshot ir = context.ir();  // SUPPRIMÉ en v4.0.0
    // Utiliser context.model() à la place
}
```

### 8.5 Plugin Output Sharing

Plugins can share data:

```java
// In JPA plugin
Map<String, JpaEntity> entities = generateEntities();
context.setOutput("generated-entities", entities);

// In Liquibase plugin (depends on JPA)
Optional<Map<String, JpaEntity>> entities = context.getOutput(
    "io.hexaglue.plugin.jpa", "generated-entities", Map.class);
```

---

## Appendix A: File Paths

### Syntax Layer
- `hexaglue-syntax/hexaglue-syntax-api/src/main/java/io/hexaglue/syntax/`
  - `SyntaxProvider.java`
  - `TypeSyntax.java`
  - `MethodSyntax.java`, `FieldSyntax.java`, `AnnotationSyntax.java`
- `hexaglue-syntax/hexaglue-syntax-spoon/src/main/java/io/hexaglue/syntax/spoon/`
  - `SpoonSyntaxProvider.java`
  - `SpoonTypeSyntax.java`, `SpoonMethodSyntax.java`, etc.

### Model Layer
- `hexaglue-arch/src/main/java/io/hexaglue/arch/`
  - `ArchitecturalModel.java`
  - `ElementRegistry.java`, `ElementRef.java`, `ElementId.java`
  - `RelationshipStore.java`, `RelationType.java`
  - `ClassificationTrace.java`
- `hexaglue-arch/src/main/java/io/hexaglue/arch/domain/`
  - `Aggregate.java`, `DomainEntity.java`, `ValueObject.java`, etc.
- `hexaglue-arch/src/main/java/io/hexaglue/arch/ports/`
  - `DrivingPort.java`, `DrivenPort.java`, `ApplicationService.java`
- `hexaglue-arch/src/main/java/io/hexaglue/arch/builder/`
  - `ArchitecturalModelBuilder.java`
  - `DomainClassifier.java`, `PortClassifier.java`

### Contract Layer
- `hexaglue-spi/src/main/java/io/hexaglue/spi/plugin/`
  - `HexaGluePlugin.java`, `PluginContext.java`
- `hexaglue-spi/src/main/java/io/hexaglue/spi/ir/` ⚠️ **DEPRECATED** (removal v5.0.0)
  - `IrSnapshot.java`, `DomainModel.java`, `DomainType.java`, etc.
- `hexaglue-spi-arch/src/main/java/io/hexaglue/spi/arch/` ⚠️ **DEPRECATED** (removal v5.0.0)
  - `ArchModelPluginContext.java`

### Plugin Layer
- `hexaglue-plugins/hexaglue-plugin-jpa/src/main/java/io/hexaglue/plugin/jpa/`
- `hexaglue-plugins/hexaglue-plugin-audit/src/main/java/io/hexaglue/plugin/audit/`
- `hexaglue-plugins/hexaglue-plugin-living-doc/src/main/java/io/hexaglue/plugin/livingdoc/`

### Consumer Layer
- `hexaglue-maven-plugin/src/main/java/io/hexaglue/maven/`
  - `HexaGlueMojo.java` (generate goal)
  - `AuditMojo.java`, `ValidateMojo.java`

---

## Appendix B: Glossary

| Term | Definition |
|------|------------|
| **Aggregate** | A cluster of domain objects treated as a unit |
| **Aggregate Root** | Entry point to an aggregate, ensures consistency |
| **ClassificationTrace** | Explains why an element was classified |
| **DrivenPort** | Secondary/outbound port (e.g., repository interface) |
| **DrivingPort** | Primary/inbound port (e.g., use case interface) |
| **ElementId** | Unique identifier (qualified name only) |
| **ElementRef** | Type-safe reference to another element |
| **ElementRegistry** | Central registry of all classified elements |
| **IR** | Intermediate Representation (⚠️ DEPRECATED - use `ArchitecturalModel`) |
| **RelationshipStore** | Indexed storage for element relationships |
| **SyntaxProvider** | Abstract parser interface |
| **TypeSyntax** | AST representation of a type |
| **ValueObject** | Immutable domain object without identity |
