# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

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
