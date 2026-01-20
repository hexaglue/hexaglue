# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [4.1.0] - 2026-01-20

### Added

- **`ElementRegistry` API** - New unified registry for type access
  - `registry.all(DomainEntity.class)` - Stream all domain entities
  - `registry.all(ValueObject.class)` - Stream all value objects
  - `registry.all(DrivenPort.class)` - Stream all driven ports
  - `registry.all(DrivingPort.class)` - Stream all driving ports
  - Type-safe element querying via `model.registry()`

- **`DomainIndex` and `PortIndex`** - New v4.1.0 enriched type indices
  - `model.domainIndex()` - Optional access to enriched domain types
  - `model.portIndex()` - Optional access to enriched port types
  - Part of the new sealed `ArchType` hierarchy

- **`ClassificationReport`** - New classification report accessible via `model.classificationReport()`
  - Classification statistics with `stats()`
  - Prioritized remediations with `actionRequired()`

- **`GeneratorContext.withPluginContext()`** - New factory for v4.1.0 contexts

### Deprecated

The following methods in `ArchitecturalModel` are deprecated since 4.1.0 and scheduled for removal in v5.0.0:

- `domainEntities()` - Use `registry().all(DomainEntity.class)` instead
- `valueObjects()` - Use `registry().all(ValueObject.class)` instead
- `identifiers()` - Use `registry().all(Identifier.class)` instead
- `domainEvents()` - Use `registry().all(DomainEvent.class)` instead
- `domainServices()` - Use `registry().all(DomainService.class)` instead
- `applicationServices()` - Use `registry().all(ApplicationService.class)` instead
- `drivingPorts()` - Use `registry().all(DrivingPort.class)` instead
- `drivenPorts()` - Use `registry().all(DrivenPort.class)` instead
- `drivingAdapters()` - Use `registry().all(DrivingAdapter.class)` instead
- `drivenAdapters()` - Use `registry().all(DrivenAdapter.class)` instead
- `unclassifiedTypes()` - Use `classificationReport().stats().unclassifiedCount()` instead
- `unclassifiedCount()` - Use `classificationReport().stats().unclassifiedCount()` instead
- `hasUnclassified()` - Use `classificationReport().hasActionRequired()` instead

### Changed

- **Plugin migration** - All plugins now use `model.registry().all(Type.class)` pattern
  - `hexaglue-plugin-jpa` - Migrated to registry pattern
  - `hexaglue-plugin-audit` - Migrated to registry pattern
  - `hexaglue-plugin-living-doc` - Migrated to registry pattern

- **Javadoc annotations** - Added `@since 4.1.0` annotations to new API methods

### Migration Guide

```java
// Before (v4.0.x) - deprecated
model.domainEntities()
        .filter(DomainEntity::isAggregateRoot)
        .forEach(this::processAggregate);

// After (v4.1.0) - preferred
model.registry().all(DomainEntity.class)
        .filter(DomainEntity::isAggregateRoot)
        .forEach(this::processAggregate);

// Or using new indices (for enriched types)
model.domainIndex().ifPresent(domain -> {
    domain.aggregateRoots().forEach(this::processEnrichedAggregate);
});
```

---

## [4.0.0] - 2026-01-16

### Breaking Changes

- **`PluginContext.ir()` removed** - Plugins must now use `context.model()` to access the `ArchitecturalModel`.
  Migration: Replace `context.ir()` with `context.model()` and adapt to the new API.

- **`EngineResult.ir()` removed** - Engine results now provide `model()` instead.
  Migration: Use `result.model()` to access the unified architectural model.

- **`PluginExecutor` no longer accepts `IrSnapshot`** - The executor now works exclusively with `ArchitecturalModel`.

### Deprecated

- **`io.hexaglue.spi.ir` package** - The entire IR package is deprecated and scheduled for removal in v5.0.0.
  - `IrSnapshot` - Use `ArchitecturalModel` instead
  - `DomainModel` - Use `model.query().aggregates()`, `model.query().valueObjects()`, etc.
  - `DomainType` - Use `ArchElement` subtypes (`Aggregate`, `DomainEntity`, `ValueObject`, etc.)
  - `PortModel` - Use `model.query().ports()`, `model.query().drivenPorts()`, etc.
  - `Port` - Use `DrivingPort` or `DrivenPort` from `ArchitecturalModel`
  - `IrMetadata` - Use `ProjectContext` via `model.project()`

- **`hexaglue-spi-arch` module** - Will be merged into `hexaglue-spi` in v5.0.0.
  - `ArchModelPluginContext` - No longer needed, use `PluginContext.model()` directly
  - `PluginContexts` - No longer needed, use `context.model()` directly

### Added

- **`PluginContext.model()`** - Direct access to `ArchitecturalModel` from plugin context
- **`ElementKind` enhancements** - New classification kinds:
  - `INBOUND_ONLY` - ApplicationService that receives commands
  - `OUTBOUND_ONLY` - ApplicationService that emits events
  - `SAGA` - ApplicationService that orchestrates
  - `isApplicationService()` helper method

### Changed

- **Unified architectural model** - All plugins now use the unified `ArchitecturalModel` instead of the legacy IR.
- **`DefaultPluginContext`** - Rewritten to work directly with `ArchitecturalModel`, no longer maintains IR reference.
- **`DefaultHexaGlueEngine`** - No longer uses `IrExporter` in the main pipeline.

### Migration Guide

```java
// Before (v3.x)
IrSnapshot ir = context.ir();
String basePackage = ir.metadata().basePackage();
ir.domain().aggregateRoots().forEach(type -> {
    String name = type.simpleName();
    // ...
});

// After (v4.x)
ArchitecturalModel model = context.model();
String basePackage = model.project().basePackage();
model.query().aggregates().forEach(agg -> {
    String name = agg.simpleName();
    // ...
});
```

---

## [3.x] - Previous Releases

### Added

- **Audit rules implementation** - Graph-aware code quality rules
  - `DependencyNoCyclesRule` - Detects cyclic dependencies using DFS algorithm
  - `DependencyStableRule` - Checks Stable Dependencies Principle (I = Ce/(Ca+Ce))
  - Both rules use `ThreadLocal<Codebase>` pattern for graph-level analysis

- **CachedSpoonAnalyzer enhancements** - Real Spoon AST analysis
  - Method body analysis: invocations, field accesses, cyclomatic complexity
  - Field analysis: type detection, collection handling, annotations
  - LRU caching with statistics for performance optimization

- **Bounded context detection** - Architecture-level cycle detection
  - `findBoundedContextCycles()` in `DefaultArchitectureQuery`
  - Extracts bounded contexts from package structure (3rd segment)

- **Plugin configuration via hexaglue.yaml** - File-based plugin configuration
  - Loads from project root (`hexaglue.yaml` or `hexaglue.yml`)
  - Supports per-plugin configuration sections
  - Graceful fallback when file not present

- **Composite identity support** - Multi-field ID handling
  - `COMPOSITE` strategy in `IdentityStrategy` enum
  - Automatic detection of records with 2+ fields as composite IDs
  - Maps to JPA `@EmbeddedId` strategy

- **Classification inheritance** - Subclass detection improvements
  - `InheritedClassificationCriteria` enhanced for repository-based parent detection
  - Subclasses inherit AGGREGATE_ROOT when parent is repository-managed

- **ProgressiveClassifier integration** - Deep classification with method analysis
  - Integrates `CachedSpoonAnalyzer` for behavioral classification
  - Detects Repository, UseCase, Entity, ValueObject based on method patterns

- **Classification engine refactoring** - Unified and extensible classification system
  - `CriteriaEngine<K>` - Generic evaluation engine shared by domain and port classifiers
  - `CriteriaProfile` - Configurable criteria priorities via YAML profiles (`default.yaml`, `strict.yaml`, `annotation-only.yaml`)
  - `IdentifiedCriteria` - Stable criteria identifiers for configuration (e.g., `domain.explicit.aggregateRoot`)
  - `ConflictSeverity` - Distinguish ERROR (incompatible) from WARNING (compatible) conflicts
  - `CompatibilityPolicy` - Define which classification kinds can coexist
  - `Contribution<K>` - Unified contribution model with metadata support

- **Graph-based analysis engine** - Complete rewrite of the classification system
  - Single-pass graph construction for better context awareness
  - Declarative classification criteria with priority and confidence levels
  - Package organization style detection (HEXAGONAL, BY_LAYER, FLAT)

- **hexaglue-spi** - Stable Service Provider Interface for plugins
  - `DomainType`, `Port`, `DomainRelation` records for IR representation
  - `TypeRef` with full generic type support
  - `HexaGluePlugin` interface for plugin development
  - `CodeWriter` for safe code generation with merge support

- **hexaglue-core** - Core analysis engine
  - `ApplicationGraph` model with nodes and edges
  - `DomainClassifier` and `PortClassifier` with composable criteria
  - `StyleDetector` for package organization detection
  - Spoon-based source code analysis

- **hexaglue-maven-plugin** - Maven build integration
  - `hexaglue:generate` goal for compile-time code generation
  - Configuration via `hexaglue.yaml`
  - Integration tests for validation

- **hexaglue-plugin-jpa** - JPA infrastructure generator
  - Entity generation with `@Entity`, `@Table`, `@Id` annotations
  - Embeddable generation for value objects
  - Spring Data JPA repository interfaces
  - MapStruct mapper generation for domain/entity conversion
  - Port adapter generation implementing repository ports
  - Support for relations: `@OneToMany`, `@ManyToOne`, `@Embedded`, `@ElementCollection`
  - Configurable auditing (`@CreatedDate`, `@LastModifiedDate`)
  - Configurable optimistic locking (`@Version`)

- **hexaglue-plugin-living-doc** - Living documentation generator
  - Architecture overview in Markdown
  - Domain model documentation
  - Ports documentation with direction indicators
  - Mermaid diagrams for visual representation

- **hexaglue-testing** - Testing harness for plugin development
  - In-memory compilation support
  - Assertion helpers for generated code

- **Examples**
  - `coffeeshop` - Coffee shop domain with orders and beverages
  - `minimal` - Minimal quick-start example
  - `ecommerce` - Rich e-commerce domain with multiple aggregates

### Changed

- Unified monorepo structure (previously split into engine/plugins/examples)
- jMolecules annotations are now the primary classification signal
- Port direction detection improved with package-based signals
- `DomainClassifier` and `PortClassifier` now use shared `CriteriaEngine` for consistent evaluation
- `Conflict` record now includes `severity` field (ERROR/WARNING) for better diagnostics
- Classification criteria priorities can be overridden via YAML profiles without code changes

### Fixed

- Identity wrapper types (e.g., `OrderId`) correctly unwrapped in adapters
- Parameter types properly imported in generated adapters
- Generic return types (`Optional<T>`, `List<T>`) correctly inferred

## [1.0.0] - TBD

Initial stable release.

---

## Version History

| Version | Date | Description |
|---------|------|-------------|
| 2.0.0-SNAPSHOT | In Development | Graph-based analysis rewrite |

---

## Migration Guide

### From v1.x to v2.x

The v2 release includes a complete rewrite of the analysis engine. While the SPI remains stable, some behavioral changes may affect your projects:

1. **Classification accuracy** - The graph-based approach may classify types differently. Review generated code after upgrade.

2. **Plugin API** - Plugins using only the SPI should work without changes. Internal APIs have changed significantly.

---

## Links

- [Documentation](docs/)
- [Contributing](CONTRIBUTING.md)
- [License](LICENSE)

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
