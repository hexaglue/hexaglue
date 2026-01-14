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

## Installation

Add the plugin dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-plugin-audit</artifactId>
    <version>3.0.0</version>
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
    <version>3.0.0</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-audit</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</plugin>
```

### Advanced Configuration

```xml
<plugin>
    <id>io.hexaglue.plugin.audit.ddd</id>
    <enabled>true</enabled>
    <config>
        <!-- Fail build on BLOCKER or CRITICAL violations (default: true) -->
        <allowCriticalViolations>false</allowCriticalViolations>

        <!-- Enable specific constraints (comma-separated) -->
        <enabledConstraints>
            ddd:entity-identity,
            ddd:aggregate-repository,
            ddd:value-object-immutable,
            ddd:aggregate-cycle,
            ddd:aggregate-consistency,
            hexagonal:port-interface,
            hexagonal:dependency-direction,
            hexagonal:layer-isolation
        </enabledConstraints>

        <!-- Enable specific metrics (comma-separated) -->
        <enabledMetrics>
            aggregate.avgSize,
            aggregate.coupling.efferent,
            domain.coverage,
            port.coverage
        </enabledMetrics>

        <!-- Override severity for specific constraints -->
        <severity>
            <ddd:entity-identity>BLOCKER</ddd:entity-identity>
            <ddd:aggregate-repository>MAJOR</ddd:aggregate-repository>
        </severity>
    </config>
</plugin>
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
- HexaGlue 3.0.0 or higher
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

### 3.0.0 (2026-01-09)

Initial release with:
- 8 DDD and Hexagonal Architecture validators
- 4 quality metrics
- Comprehensive test suite (176 tests)
- Full SPI integration with ArchitectureQuery
- Configurable severity levels and constraint selection
