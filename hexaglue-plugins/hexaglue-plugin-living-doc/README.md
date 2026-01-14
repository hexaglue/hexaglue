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
docs/architecture/
├── README.md      # Architecture overview with summary and diagrams
├── domain.md      # Detailed domain model documentation
├── ports.md       # Ports documentation (driving and driven)
└── diagrams.md    # Mermaid diagrams (if enabled)
```

## Configuration

Configuration is planned for future versions. Currently the plugin uses these defaults:

| Option | Default | Description |
|--------|---------|-------------|
| `outputDir` | `docs/architecture` | Output directory for generated documentation |
| `generateDiagrams` | `true` | Whether to generate Mermaid diagrams |

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

---

**HexaGlue - Focus on business code, not infrastructure glue.**
