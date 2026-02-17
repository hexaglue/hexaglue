# HexaGlue DDD Audit Plugin

Validates Domain-Driven Design (DDD) and Hexagonal Architecture constraints in your Java codebase.

## Overview

The DDD Audit Plugin analyzes your domain model to ensure adherence to architectural principles and best practices. It identifies violations of DDD patterns, hexagonal architecture rules, and provides quality metrics to guide refactoring efforts.

## Features

- **8 Built-in Validators**: Covers DDD and Hexagonal Architecture constraints
- **4 Quality Metrics**: Provides quantitative measurements for code quality
- **Configurable Severity Levels**: BLOCKER, CRITICAL, MAJOR, MINOR, INFO
- **Build Integration**: Fail builds on critical violations
- **Comprehensive Evidence**: Each violation includes detailed evidence for debugging
- **Multi-Format Reports**: HTML, Markdown, JSON, and Console output
- **Mermaid Diagrams**: 9 diagram types including C4, class diagrams, radar charts
- **Visual Violation Highlighting**: 11 violation types with distinct color styles

## Installation

Add the plugin dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-plugin-audit</artifactId>
    <version>2.0.0</version>
    <scope>provided</scope>
</dependency>
```

The plugin is automatically discovered by HexaGlue via ServiceLoader.

## Usage

### Basic Configuration

Add to your HexaGlue Maven plugin configuration:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>5.0.0</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-audit</artifactId>
            <version>2.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

### Configuration

The audit behavior is controlled via the `<configuration>` parameters of the HexaGlue Maven plugin:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>5.0.0</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
        <!-- Fail build when audit errors are found (default: true) -->
        <failOnError>true</failOnError>
        <!-- Treat BLOCKER violations as errors (default: true) -->
        <errorOnBlocker>true</errorOnBlocker>
        <!-- Treat CRITICAL violations as errors (default: false) -->
        <errorOnCritical>false</errorOnCritical>
        <!-- Custom report directory (default: ${project.build.directory}/hexaglue/reports) -->
        <reportDirectory>${project.build.directory}/hexaglue/reports</reportDirectory>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-audit</artifactId>
            <version>2.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

### Audit Failure Properties

These 3 properties control when the Maven build fails due to audit violations.
They can be set in `pom.xml`, via `-D` on the command line, or in `hexaglue.yaml`.
Precedence: **Maven POM / -D > YAML > defaults**.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `failOnError` | boolean | `true` | Fail the Maven build when audit errors are found |
| `errorOnBlocker` | boolean | `true` | Treat BLOCKER violations as errors |
| `errorOnCritical` | boolean | `false` | Treat CRITICAL violations as errors |

**Decision logic:**
```
errors = (errorOnBlocker ? count(BLOCKER) : 0) + (errorOnCritical ? count(CRITICAL) : 0)
if (failOnError && errors > 0) → build fails
```

### Maven Parameters

These parameters are set in the `<configuration>` block of the Maven plugin:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `basePackage` | string | (required) | Base package to analyze for DDD and Hexagonal violations |
| `reportDirectory` | string | `${project.build.directory}/hexaglue/reports` | Output directory for audit reports (HTML, Markdown, JSON) |
| `skip` | boolean | `false` | Skip audit execution entirely (also via `-Dhexaglue.skip=true`) |
| `failOnUnclassified` | boolean | `false` | Fail the build if unclassified types remain after analysis |
| `validationReportPath` | string | `${project.build.directory}/hexaglue/reports/validation/validation-report.md` | Output path for the Markdown validation report (`validate` goal only) |

### YAML Configuration

Plugin-specific options are configured via `hexaglue.yaml`, placed at the project root alongside `pom.xml`. Options are set under `plugins.io.hexaglue.plugin.audit:`.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `failOnError` | boolean | `true` | Fail build when audit errors are found |
| `errorOnBlocker` | boolean | `true` | Treat BLOCKER violations as errors |
| `errorOnCritical` | boolean | `false` | Treat CRITICAL violations as errors |
| `generateDocs` | boolean | `false` | Generate additional documentation with reports |
| `severityOverrides` | map | `{}` | Custom severity levels for specific constraints (e.g., `"ddd:aggregate-repository": "MINOR"`). Allows downgrading or upgrading the severity of individual constraints. *(Planned — API ready, YAML parsing not yet wired)* |

```yaml
plugins:
  io.hexaglue.plugin.audit:
    errorOnBlocker: true
    errorOnCritical: false
    generateDocs: false
```

### Skip Audit

```bash
mvn clean verify -Dhexaglue.skip=true
```

## Constraints

### DDD Constraints

| ID | Severity | Description |
|----|----------|-------------|
| `ddd:entity-identity` | CRITICAL | Entities and aggregate roots must have identity fields (annotated with `@Id` or similar) |
| `ddd:aggregate-repository` | MAJOR | Each aggregate root should have a corresponding repository interface |
| `ddd:value-object-immutable` | CRITICAL | Value objects must be immutable (no setter methods) |
| `ddd:aggregate-cycle` | BLOCKER | Aggregate roots must not have circular dependencies |
| `ddd:aggregate-consistency` | MAJOR | Aggregates must maintain proper boundaries (single ownership, size limits, boundary respect) |

### Hexagonal Architecture Constraints

| ID | Severity | Description |
|----|----------|-------------|
| `hexagonal:port-interface` | CRITICAL | Ports must be defined as interfaces, not concrete classes |
| `hexagonal:dependency-direction` | BLOCKER | Domain layer must not depend on Infrastructure layer |
| `hexagonal:layer-isolation` | MAJOR | Layers must respect dependency rules (Domain → Application → Infrastructure) |

See [CONSTRAINTS.md](CONSTRAINTS.md) for detailed descriptions and examples.

## Metrics

| Metric Name | Description | Warning Threshold |
|-------------|-------------|-------------------|
| `aggregate.avgSize` | Average number of methods per aggregate root | > 20 methods |
| `aggregate.coupling.efferent` | Average outgoing dependencies between aggregates | > 3 dependencies |
| `domain.coverage` | Percentage of types classified in domain layer | < 30% |
| `port.coverage` | Percentage of aggregates with repository ports | < 100% |

### Interpreting Metrics

**Aggregate Size**: Large aggregates (>20 methods) are harder to understand and maintain. Consider splitting into multiple aggregates or refactoring to domain services.

**Coupling**: High coupling between aggregates (>3 dependencies) indicates tight dependencies. Review aggregate boundaries and consider using domain events for loose coupling.

**Domain Coverage**: Low domain coverage (<30%) may indicate anemic domain model or business logic leaking into infrastructure. Aim for rich domain models.

**Port Coverage**: All aggregates should have repository ports (100%). Missing repositories indicate incomplete persistence abstraction.

## Output

The plugin generates an `AuditSnapshot` with:

- **Violations**: List of constraint violations with severity, location, and evidence
- **Metrics**: Calculated quality metrics with threshold warnings
- **Architecture Style**: Detected architecture (Hexagonal, Layered, etc.)
- **Build Outcome**: PASS or FAIL based on violations

### Example Output

```
[INFO] Executing DDD audit with 8 constraints
[INFO] Audit complete: 3 violations, 4 metrics
[WARNING] Violation: Entity 'OrderLine' missing identity field (CRITICAL)
  Location: com.example.domain.OrderLine:1:1
  Evidence: No field annotated with @Id, @EmbeddedId, or identity marker
[ERROR] Violation: Circular dependency between aggregates: Order -> Customer -> Order (BLOCKER)
  Location: com.example.domain.Order:1:1
[INFO] Metric: aggregate.avgSize = 15.0 methods (threshold: 20.0)
[INFO] Metric: domain.coverage = 45.0% (threshold: 30.0%)
[ERROR] Audit FAILED: 1 blocker, 1 critical violations
```

## Report Formats

The plugin generates reports in 4 formats:

| Format | File | Description |
|--------|------|-------------|
| **HTML** | `target/hexaglue/reports/audit/audit-report.html` | Interactive report with styled diagrams |
| **Markdown** | `target/hexaglue/reports/audit/AUDIT-REPORT.md` | Documentation-friendly format |
| **JSON** | `target/hexaglue/reports/audit/audit-report.json` | Machine-readable for CI/CD integration |
| **Console** | stdout | Build output summary |

### Report Sections

Each report contains 5 sections:

1. **Verdict**: Score, grade (A-F), status, KPIs, immediate action required
2. **Architecture**: Inventory, C4 diagrams, domain model, relationships
3. **Issues**: Violations grouped by theme with impact and fix suggestions
4. **Remediation**: Prioritized actions with effort estimates
5. **Appendix**: Score breakdown, metrics, constraints, package zones

### JSON for CI/CD Integration

The JSON report includes `typeViolations` for automated analysis:

```bash
# Extract cycle violations
jq '.architecture.typeViolations[] | select(.violationType == "CYCLE")' audit-report.json

# Count violations by type
jq '.architecture.typeViolations | group_by(.violationType) | map({type: .[0].violationType, count: length})' audit-report.json
```

## Mermaid Diagrams

The plugin generates 9 diagram types:

| Diagram | Type | Description |
|---------|------|-------------|
| `scoreRadar` | radar-beta | Quality score visualization |
| `c4Context` | C4Context | System context diagram |
| `c4Component` | C4Component | Component architecture |
| `domainModel` | classDiagram | Domain model with stereotypes |
| `aggregateDiagrams` | classDiagram | Per-aggregate detail views |
| `violationsPie` | pie | Violation distribution |
| `packageZones` | quadrantChart | Package classification |
| `applicationLayer` | classDiagram | Application services and handlers |
| `portsLayer` | classDiagram | Driving and driven ports |
| `fullArchitecture` | C4Component | Complete hexagonal architecture view |

### Visual Violation Highlighting

Types with violations are highlighted with distinct styles in diagrams:

| Violation Type | Color | Constraint |
|----------------|-------|------------|
| CYCLE | Red `#FF5978` | ddd:aggregate-cycle |
| MUTABLE_VALUE_OBJECT | Orange `#FF9800` | ddd:value-object-immutable |
| IMPURE_DOMAIN | Purple `#9C27B0` | ddd:domain-purity |
| BOUNDARY_VIOLATION | Red `#E53935` | ddd:aggregate-boundary |
| MISSING_IDENTITY | Yellow `#FBC02D` | ddd:entity-identity |
| MISSING_REPOSITORY | Blue `#1976D2` | ddd:aggregate-repository |
| EVENT_NAMING | Cyan `#00ACC1` | ddd:event-naming |
| PORT_UNCOVERED | Teal `#00897B` | hexagonal:port-coverage |
| DEPENDENCY_INVERSION | Amber `#FFB300` | hexagonal:dependency-inversion |
| LAYER_VIOLATION | Grey `#616161` | hexagonal:layer-isolation |
| PORT_NOT_INTERFACE | Brown `#8D6E63` | hexagonal:port-interface |

## Extending the Plugin

### Custom Validators

Implement the `ConstraintValidator` interface:

```java
public class CustomValidator implements ConstraintValidator {

    @Override
    public ConstraintId constraintId() {
        return ConstraintId.of("custom:my-rule");
    }

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        // Your validation logic
        return violations;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
```

Register in `DefaultConstraints` or create a custom `ConstraintRegistry`.

### Custom Metrics

Implement the `MetricCalculator` interface:

```java
public class CustomMetricCalculator implements MetricCalculator {

    @Override
    public String metricName() {
        return "custom.metric";
    }

    @Override
    public Metric calculate(Codebase codebase) {
        // Calculate metric
        return Metric.of("custom.metric", value, "unit", "description");
    }
}
```

## Architecture

The plugin follows Hexagonal Architecture:

- **Domain**: Core audit logic (`Violation`, `Constraint`, `Metric`, `Evidence`)
- **Adapters**: Validators, metric calculators, architecture query
- **Ports**: `ConstraintValidator`, `MetricCalculator` interfaces
- **Infrastructure**: Configuration, orchestration, SPI integration

## Testing

Run the test suite:

```bash
cd hexaglue-plugins/hexaglue-plugin-audit
mvn test
```

Current test coverage: **176 tests** covering:
- 8 validator implementations
- 4 metric calculators
- Domain model (violations, evidence, constraints)
- Cycle detection and dependency analysis

## Requirements

- Java 17 or higher
- HexaGlue 5.0.0 or higher
- Maven 3.8+ or Gradle 7.0+

## License

This plugin is part of the HexaGlue project.

Copyright (c) 2026 Scalastic

Licensed under the Mozilla Public License 2.0 (MPL 2.0).
See LICENSE file for details.

## Support

- Documentation: https://hexaglue.io/docs/plugins/audit
- Issues: https://github.com/scalastic/hexaglue/issues
- Discussion: https://github.com/scalastic/hexaglue/discussions

## Version History

### v5.0.0 (2026-01-28)

Major enhancements to reports and diagrams:

- **Multi-format reports**: HTML, Markdown, JSON, Console with 5 structured sections
- **JSON `typeViolations`**: Serialized for CI/CD automation
- **HTML/Markdown parity**: Identical data and diagrams across formats
- **11 violation types**: Extended from 4 to cover all DDD and Hexagonal constraints
- **Visual violation styles**: Color-coded highlighting in all diagrams
- **3 new diagrams**: `applicationLayer`, `portsLayer`, `fullArchitecture`
- **Attributes and methods**: Class diagrams show typed fields and public methods
- **Proper HTML encoding**: `<<Stereotype>>` encoded correctly per format

### v4.1.0 (2026-01-20)

- Migrated to use `model.registry().all(Type.class)` pattern
- Added `ClassificationReport` support via `model.classificationReport()`
- C4 diagram builders now use `ElementRegistry` for type access
- Improved inventory building with enriched type metadata

### v4.0.0 (2026-01-16)

- Migrated from `IrSnapshot` to `ArchitecturalModel`
- Updated validators to use new architectural model
- Enhanced metric calculators with graph-based analysis

### v3.0.0 (2026-01-09)

Initial release with:
- 8 DDD and Hexagonal Architecture validators
- 4 quality metrics
- Comprehensive test suite (176 tests)
- Full SPI integration with ArchitectureQuery
- Configurable severity levels and constraint selection
