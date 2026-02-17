# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [5.1.0] - 2026-02-17

### Breaking Changes

- **Manifest, stale file detection, and overwrite policy removed** - These features relied on a `target/hexaglue/manifest.txt` file that was destroyed by `mvn clean`, making them unusable in CI and standard workflows
  - `GenerationManifest`, `StaleFileCleaner`, `StaleFilePolicy`, `OverwritePolicy` classes removed from `hexaglue-core`
  - `ManifestSupport` removed from `hexaglue-maven-plugin`
  - `staleFilePolicy` Maven parameter removed from all Mojos
  - Generated files are now always overwritten (with a WARN log when the target file already exists)

### Added

- **Comprehensive parameter regression tests** - 30+ integration test projects (`test-param-*`) validating every plugin configuration parameter
  - JPA plugin: `entitySuffix`, `repositorySuffix`, `adapterSuffix`, `mapperSuffix`, `embeddableSuffix`, `tablePrefix`, `infraPackage`, `outputDirectory`, `generateAdapters`, `generateRepositories`, `generateMappers`, `generateEmbeddables`, `enableAuditing`, `enableOptimisticLocking`
  - Living-doc plugin: `outputDir`, `generateDiagrams`, `includeDebugInfo`, `maxProperties`
  - Audit plugin: `failOnError`, `errorOnCritical`, `errorOnBlocker`, `generateDocs`, `reportDirectory`
  - Core parameters: `skip`, `skipValidation`, `failOnUnclassified`, `outputDirectory`, `validationReportPath`
  - Classification parameters: `classificationMode` (explicit, no-inferred, exclude patterns, fail-unclassified via YAML)

- **Overwrite warning in `FileSystemCodeWriter`** - Logs `WARN Overwriting existing file: ...` when writing to an already existing file

- **`AuditFailureResolver`** - New dedicated class for audit failure resolution logic, replacing inline code in Mojos

### Changed

- **Audit plugin configuration simplified** - `AuditConfiguration` refactored with cleaner parameter handling
- **Audit XSD simplified** - Removed non-functional configuration options from `hexaglue-audit.xsd`
- **Living-doc renderers** - `DomainRenderer` and `PortRenderer` improved with unit tests
- **`FileSystemCodeWriter` simplified** - No longer tracks checksums or applies overwrite policies; always writes
- **`MultiModuleCodeWriter` simplified** - Removed overwrite policy and checksums delegation
- **`PluginExecutor` simplified** - Removed checksums and overwrite policy resolution logic
- **`PluginResult` simplified** - Removed `checksums` field from record

### Removed

- **Manifest infrastructure** (~2,000 lines removed)
  - `GenerationManifest` - SHA-256 checksums tracking
  - `StaleFileCleaner` - Stale file detection and cleanup
  - `StaleFilePolicy` (enum) - WARN/DELETE/FAIL policies
  - `OverwritePolicy` (enum) - ALWAYS/IF_UNCHANGED/NEVER policies
  - `ManifestSupport` - Maven plugin manifest integration
  - All associated tests (`GenerationManifestTest`, `StaleFileCleanerTest`, `OverwritePolicyTest`, `ManifestSupportTest`)

- **Stale/overwrite example projects removed**
  - `sample-jpa-src-always`, `sample-jpa-src-if-unchanged`, `sample-jpa-src-never`
  - `sample-jpa-src-stale-delete`

### Fixed

- **Audit plugin documentation** - Corrected distinction between Maven Mojo parameters and YAML plugin configuration keys
- **Plugin documentation** - Fixed report paths, removed references to non-functional options

## [5.0.0] - 2026-02-13

### Breaking Changes

- **Sealed `ArchType` hierarchy** - Complete rewrite of the architectural model
  - `DomainType` sealed interface with permits: `AggregateRoot`, `Entity`, `ValueObject`, `Identifier`, `DomainEvent`, `DomainService`
  - `PortType` sealed interface with permits: `DrivingPort`, `DrivenPort`
  - `ApplicationType` sealed interface with permits: `ApplicationService`, `CommandHandler`, `QueryHandler`
  - `UnclassifiedType` for types that couldn't be classified

- **Legacy IR package removed** - `io.hexaglue.spi.ir` package completely removed (-13,750 lines)
  - `IrSnapshot` removed - Use `ArchitecturalModel` instead
  - `DomainModel`, `PortModel` removed - Use `model.registry()` API
  - All plugins migrated to new `ArchType` API

- **IR models relocated** - Classification and core models moved from `io.hexaglue.spi` to `io.hexaglue.arch.model`

### Added

- **New architectural model API**
  - `ElementRef` - Type-safe references to architectural elements
  - `ElementRegistry` - Unified registry for type access with `registry.all(Type.class)`
  - `ResolutionResult` - Result type for element resolution
  - Fluent Query API for domain model traversal
  - `RepositoryInterfaceCriterion` for improved repository detection

- **Multi-module support** - Single reactor execution across Maven multi-module projects
  - `ModuleDescriptor` (record) - Module description: moduleId, role, baseDir, sourceRoots, basePackage
  - `ModuleRole` (enum) - Architectural role: DOMAIN, INFRASTRUCTURE, APPLICATION, API, ASSEMBLY, SHARED
  - `ModuleIndex` - Type-to-module index with `moduleOf(TypeId)`, `typesInModule(moduleId)`, `modulesByRole(role)`
  - `ModuleSourceSet` (record) - Module source layout: sourceRoots, classpathEntries, outputDirectory
  - `MultiModuleCodeWriter` - Routes generated code to the correct module via `Map<String, FileSystemCodeWriter>`
  - `ReactorGenerateMojo` - `reactor-generate` goal with `@Mojo(aggregator = true)`
  - `ReactorAuditMojo` - `reactor-audit` goal with `@Mojo(aggregator = true)`
  - `ReactorEngineConfigBuilder` - Unified `EngineConfig` from `MavenSession` with YAML > convention > SHARED resolution chain
  - `HexaGlueLifecycleParticipant` - Auto-detection of multi-module projects, injects `reactor-*` goals
  - `ModuleRoleDetector` - Convention-based role detection via module name suffixes
  - `TargetModuleValidator` - Fail-fast validation of plugin `targetModule` routing
  - `CodeWriter` default methods: `writeJavaSource(moduleId, ...)`, `getOutputDirectory(moduleId)`, `isMultiModule()`
  - Zero breaking change for mono-module: `ArchitecturalModel.moduleIndex()` returns `Optional.empty()`

- **JPA plugin multi-module routing** - `targetModule` configuration to route generated JPA code to infrastructure modules

- **Audit plugin enhancements**
  - `hexagonal:application-purity` constraint (#23)
  - Module Topology section in audit reports
  - Remediation templates for all 15 audit constraints
  - Report restructured with JSON pivot, Mermaid diagrams, and enriched violations
  - Violations displayed on C4 diagrams with ELK layout, attributes and methods

- **Living-doc plugin enhancements**
  - Module topology in generated documentation
  - Glossary, structure, bounded contexts, and index sections
  - Javadoc and `sourceLocation` extraction pipeline
  - Type-level, field-level, and method-level Javadoc propagation

- **Golden file tests for regression detection**
  - `GoldenFileTest` in `hexaglue-core` with 3 domain examples (Coffeeshop, Banking, E-commerce)
  - `JpaCodegenGoldenFileTest` in `hexaglue-plugin-jpa` with 10 golden files
  - Determinism tests ensuring stable output across multiple runs

- **PITest mutation testing integration**
  - `make mutation` target for running mutation tests on `hexaglue-core`
  - PITest configuration with JUnit 5 support
  - Baseline metrics: 39% mutation score, 69% line coverage, 62% test strength

- **Auto-inject `jakarta.persistence-api` and MapStruct** dependencies for JPA plugin users

### Fixed

- **Classification improvements**
  - Driving ports now detected via `UseCases` suffix and `ports.in` packages (H1)
  - Domain enums correctly classified as `VALUE_OBJECT` (H2)
  - Coupling metric threshold inversion fixed - efferent/afferent correctly calculated (H3)
  - Classification rate now displays as percentage (H4)
  - Pivot `CoreAppClasses` correctly classified as `APPLICATION_SERVICE` (#24)

- **JPA plugin fixes**
  - Generate `toDomain()` via `reconstitute()` for rich domain objects (#8)
  - Generate child `@Entity` with `@OneToMany` for aggregate sub-entities (#9, #18)
  - Generate child entity conversion methods in MapStruct mappers (#19)
  - Fix reconstitution for records (#10)
  - Normalize nested type names from JVM binary format (#11)
  - Fix `mapToXxx()` for foreign key identity reconstitution (#12)
  - Unwrap single-value record VOs to primitives instead of `@Embedded` (#13)
  - Fix `isX()` getter for primitive boolean in reconstitution (#14)
  - Convert embedded VOs via `toDomain()` in reconstitution method (#15)
  - Convert `Instant` audit fields to `LocalDateTime` in reconstitution (#16)
  - Support parameterless Spring Data query methods with embedded conditions (#17)
  - Prevent double suffix in adapter class naming (#7)
  - Filter duplicate audit fields when `enableAuditing=true` (#6)
  - SQL reserved words (`order`, `user`, etc.) escaped in column names (C3)
  - `@AttributeOverrides` generated for duplicate embedded types (M11)
  - Simple wrapper embeddables skipped to avoid redundant code (M13)
  - Record ID accessors use `id()` vs class accessors use `getId()` (M15)
  - Boolean getters use `isActive()` convention instead of `getActive()` (B9)
  - Repository and identifier types correctly resolved (C4)
  - Method inference for adapter implementations fixed (C1)

- **Audit plugin fixes**
  - Port-coverage adapter detection logic corrected
  - Application-purity dependencies populated for excluded types (#25)
  - 6 visualization and UX bugs corrected in audit reports
  - 3-strategy lookup in `PortDirectionValidator` to eliminate false positives
  - Application services support and classification guard added
  - `DomainPurityValidator` severity changed to `CRITICAL` to fail build on violations
  - Invalid JSON caused by decimal commas fixed (C5)
  - Version display corrected to show actual HexaGlue version (M1)
  - `affectedType` extraction from violations fixed (M2)
  - `hasAdapter` flag now correctly populated (M8)
  - `DomainEvent` no longer misclassified as DRIVING port (M9)
  - Severity levels aligned between rules and blockers (M10)
  - Evidence field populated in violations (B1)
  - `totalConstraints` metric calculated (B2)
  - Zone of Pain excludes domain packages (B3)

- **Living-doc plugin fixes**
  - Nullability correctly shows `NON_NULL` for primitives (M3)
  - Cardinality correctly shows `COLLECTION` for List/Set fields (M4)
  - Duplicate `id` field removed from diagrams (M5)
  - Mermaid diagrams use valid node references (M6)
  - Generic types fully displayed (e.g., `List<OrderItem>` not `List`) (M7)
  - Collection relations displayed in diagrams (M14)
  - Source location shows actual file path instead of "synthetic" (B4)
  - Identity field correctly marked with `isIdentity: true` (B5)
  - Annotations included in documentation (B6)
  - Identifiers listed in README overview (B8)

- **Documentation improvements**
  - Javadoc references domain types correctly (B10)
  - Report accuracy improved with proper metrics

### Changed

- **All plugins migrated to v5 architecture**
  - `hexaglue-plugin-jpa` - Uses new `ArchType` API
  - `hexaglue-plugin-audit` - Uses new `ArchType` API
  - `hexaglue-plugin-living-doc` - Uses new `ArchType` API

- **Classification and core models relocated** from `io.hexaglue.spi` to `io.hexaglue.arch.model`

- **Null safety enforced** - `null` returns replaced with `Optional` and logging added to silent catch blocks across all modules

- **Test infrastructure modernized**
  - `TestModelBuilder` for creating test fixtures with v5 API
  - `V5TestModelBuilder` for living-doc tests
  - `sample-jpa-bugfixes` integration tests for all JPA bug fixes
  - Comprehensive test coverage for all plugin components

### Removed

- **Dead code cleanup (-3,900 lines)**
  - `classification.deterministic` - `DeterministicClassifier` superseded by `SinglePassClassifier`
  - `classification.anomaly` - `AnomalyDetector`, `Anomaly`, `AnomalyType`
  - `classification.discriminator` - All discriminator classes
  - `classification.model` - `InboundModelCriteria`, `OutboundModelCriteria`, `ModelKind`
  - `graph.algorithm` - `TarjanCycleDetector`, `Cycle`, `CycleDetectionConfig`
  - `graph.composition` - `CompositionGraph` and related classes

- **Legacy IR completely removed (-13,750 lines)**
  - `io.hexaglue.spi.ir` package
  - `IrSnapshot`, `DomainModel`, `PortModel`, `DomainType`, `Port`
  - All IR-related tests and builders

- **Deprecated code removed** - `PropertyFieldSpec` constructor, orphan IT parameters, deprecated SPI methods with no usages

### Migration Guide

```java
// Before (v4.x)
model.domainEntities()
    .filter(DomainEntity::isAggregateRoot)
    .forEach(this::process);

// After (v5.0)
model.registry().all(AggregateRoot.class)
    .forEach(this::process);

// Or using pattern matching
model.registry().all(DomainType.class).forEach(type -> {
    switch (type) {
        case AggregateRoot ar -> processAggregate(ar);
        case Entity e -> processEntity(e);
        case ValueObject vo -> processValueObject(vo);
        // ...
    }
});

// Multi-module: access module index (Optional, empty in mono-module)
model.moduleIndex().ifPresent(index -> {
    ModuleDescriptor module = index.moduleOf(type.id());
    index.modulesByRole(ModuleRole.INFRASTRUCTURE).forEach(m -> { ... });
});
```

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
| 5.1.0 | 2026-02-17 | Remove manifest/stale/overwrite, add parameter regression tests |
| 5.0.0 | 2026-02-13 | Sealed ArchType hierarchy, multi-module support, legacy IR removed |
| 4.1.0 | 2026-01-20 | ElementRegistry API, DomainIndex, PortIndex |
| 4.0.0 | 2026-01-16 | Unified ArchitecturalModel, IR deprecated |

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
