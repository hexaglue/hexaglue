# HexaGlue Living Documentation Plugin

Generates living documentation in Markdown format from your analyzed domain model and ports.

## Features

- **Architecture Overview** - Summary with metrics and links to detailed documentation
- **Domain Model Documentation** - Comprehensive documentation of aggregates, entities, value objects
- **Ports Documentation** - Documentation of driving and driven ports with method signatures
- **Mermaid Diagrams** - Class diagrams and flow diagrams for visual architecture understanding

## Installation

Add the plugin as a dependency to the HexaGlue Maven plugin:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-living-doc</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

## Generated Files

The plugin generates the following documentation files:

```
target/hexaglue/reports/{outputDir}/
├── README.md      # Architecture overview with summary and diagrams
├── domain.md      # Detailed domain model documentation
├── ports.md       # Ports documentation (driving and driven)
└── diagrams.md    # Mermaid diagrams (if enabled)
```

## Configuration

Configuration is done via `hexaglue.yaml`, placed at the project root alongside `pom.xml`. Options are set under `plugins.io.hexaglue.plugin.livingdoc:`.

| Option | Default | Description |
|--------|---------|-------------|
| `outputDir` | `living-doc` | Output directory for generated documentation (relative to `target/hexaglue/reports/`) |
| `generateDiagrams` | `true` | Whether to generate Mermaid diagrams |
| `maxPropertiesInDiagram` | `5` | Maximum properties shown per class in diagrams |

### YAML Configuration

```yaml
plugins:
  io.hexaglue.plugin.livingdoc:
    outputDir: "living-doc"
    generateDiagrams: true
    maxPropertiesInDiagram: 5
```

## Sample Output

### Architecture Overview (README.md)

```markdown
# Architecture Overview

## Summary

| Metric | Count |
|--------|-------|
| Aggregate Roots | 3 |
| Entities | 2 |
| Value Objects | 5 |
| Driving Ports | 2 |
| Driven Ports | 4 |
```

### Domain Documentation (domain.md)

For each type:
- Kind (Aggregate Root, Entity, Value Object, etc.)
- Package location
- Java construct (CLASS, RECORD, ENUM)
- Classification confidence
- Identity information (for entities)
- Properties with types and cardinality
- Relationships

### Ports Documentation (ports.md)

For each port:
- Kind (Repository, Gateway, Use Case, etc.)
- Direction (Driving/Driven)
- Package location
- Managed domain types
- Method signatures

### Diagrams (diagrams.md)

- **Domain Model Class Diagram** - Shows all domain types with relationships
- **Aggregate Diagrams** - Individual diagrams for each aggregate with its entities
- **Port Interactions Flow** - Hexagonal architecture flow diagram

## Mermaid Support

The generated diagrams use [Mermaid](https://mermaid.js.org/) syntax, which is supported by:
- GitHub
- GitLab
- Most modern Markdown editors
- Documentation platforms (Docusaurus, MkDocs, etc.)

## Plugin ID

`io.hexaglue.plugin.livingdoc`

## Version History

### v5.0.0 (2026-01-28)

- Multi-module support with module topology section in generated docs
- `targetModule` routing for documentation output
- Enhanced architecture overview with module relationships

### v4.1.0 (2026-01-20)

- Migrated to use `model.registry().all(Type.class)` pattern
- Improved documentation generation with enriched type metadata
- Enhanced Mermaid diagram generation for aggregate relationships

### v4.0.0 (2026-01-16)

- Migrated from `IrSnapshot` to `ArchitecturalModel`
- Updated documentation generators to use new model

---

**HexaGlue - Focus on business code, not infrastructure glue.**
