# HexaGlue

***Design, Audit, and Generate Hexagonal Architecture***

<div align="center">

  <img src="docs/assets/logo-hexaglue.png" alt="HexaGlue" width="400">

[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![Maven 3.8+](https://img.shields.io/badge/Maven-3.8%2B-blue.svg)](https://maven.apache.org/)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

</div>

**HexaGlue is an architectural design tool for Java applications. It audits your architecture for clean architecture compliance, validates quality metrics, and generates infrastructure code automatically.**

---

## What is HexaGlue?

HexaGlue is a compile-time tool that analyzes and validates your application architecture. It provides two distinct capabilities through dedicated Maven goals:

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                              HexaGlue                                        │
│                    Architectural Design Tool                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   hexaglue:audit                         hexaglue:generate                  │
│   ─────────────────                      ──────────────────                 │
│                                                                              │
│   • Validate clean architecture          • Generate JPA entities & repos    │
│   • Detect layering violations           • Generate REST controllers        │
│   • Check dependency cycles              • Generate message handlers        │
│   • Enforce naming conventions           • Generate living documentation    │
│   • Measure quality metrics              • Swap technologies via plugins    │
│   • Fail build on violations             • Extensible plugin system         │
│                                                                              │
│   Reports: HTML, JSON, Markdown          Output: Generated source code      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Architecture Audit

HexaGlue provides a dedicated `audit` goal that validates your architecture against clean architecture principles. It supports multiple architectural styles: **Hexagonal**, **Clean**, **Onion**, and **Layered** architectures.

**Layering Rules** (Dependency Inversion Principle):
- Domain purity - Domain must not depend on infrastructure
- Presentation isolation - Presentation must not bypass application services
- Application boundaries - Application must not depend on presentation

**Dependency Rules**:
- Stable dependencies - Dependencies should flow toward stable components
- No cycles - Detect and report dependency cycles

**Quality Rules**:
- Naming conventions - Validate Repository, DTO, Controller suffixes
- Documentation coverage - Public API and complex methods
- Complexity limits - Cyclomatic complexity, method length

**Output**: Reports in HTML, JSON, Markdown, and Console formats with pass/fail status.

### Code Generation

Based on the analysis, plugins generate production-ready infrastructure code:

- **JPA Plugin**: Entities, Spring Data repositories, MapStruct mappers, port adapters
- **Living Documentation Plugin**: Markdown architecture documentation with Mermaid diagrams
- **More plugins planned**: REST controllers, GraphQL, Kafka, OpenAPI...

---

## How It Works

```text
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Analysis     │────▶│   Classification │────▶│   Audit or      │
│                 │     │   & Validation   │     │   Generation    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
   Parse source            Classify types          Run audit rules
   Build graph             Detect patterns         OR generate code
   Index relationships     Compute confidence      via plugins
```

Three Maven goals are available:

| Goal | Phase | Purpose |
|------|-------|---------|
| `hexaglue:audit` | `verify` | Architecture audit only |
| `hexaglue:generate` | `generate-sources` | Code generation only |
| `hexaglue:generate-and-audit` | `generate-sources` | Both combined |

---

## Quick Start

### Option A: Architecture Audit Only

Validate your architecture without generating code:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>audit</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <basePackage>com.example</basePackage>
        <failOnError>true</failOnError>
        <htmlReport>true</htmlReport>
    </configuration>
</plugin>
```

Run `mvn verify` and check the generated report:

```
target/hexaglue-reports/
├── hexaglue-audit.html    # Rich HTML report
├── hexaglue-audit.json    # Machine-readable (optional)
└── hexaglue-audit.md      # Markdown (optional)
```

**Sample Console Output:**
```
================================================================================
HEXAGLUE AUDIT REPORT
================================================================================
Project: my-application
HexaGlue Version: 3.0.0

SUMMARY
--------------------------------------------------------------------------------
Total Violations: 3
  Errors:   1
  Warnings: 2
  Info:     0
Status: FAILED

QUALITY METRICS
--------------------------------------------------------------------------------
Test Coverage:          85.2%
Documentation Coverage: 72.1%
Technical Debt:         120 minutes
Maintainability Rating: 4.2/5.0

VIOLATIONS
--------------------------------------------------------------------------------
ERROR (1)
  [layering.domain-purity]
  Location: com/example/domain/Order.java:45
  Domain class 'Order' depends on infrastructure class 'JpaRepository'
================================================================================
```

### Option B: Code Generation Only

Generate infrastructure code from your domain:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <basePackage>com.example</basePackage>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-jpa</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-living-doc</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

Run `mvn compile`:

```
target/generated-sources/
├── hexaglue/                              # Generated infrastructure
│   ├── OrderEntity.java
│   ├── OrderJpaRepository.java
│   ├── OrderMapper.java
│   └── OrderAdapter.java
└── generated-docs/docs/architecture/      # Living documentation
    ├── README.md
    ├── domain.md
    ├── ports.md
    └── diagrams.md
```

### Option C: Audit + Generation Combined

For CI/CD pipelines, combine both in a single execution:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate-and-audit</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <basePackage>com.example</basePackage>
        <failOnError>true</failOnError>
        <htmlReport>true</htmlReport>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-jpa</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

---

## Why Use HexaGlue?

### For Architecture Audit

- **Validate conformance** - Ensure your code follows clean architecture principles (DIP, layer separation)
- **Detect violations early** - Layering violations, dependency cycles, naming issues
- **Enforce quality gates** - Fail builds when thresholds are exceeded
- **Track metrics** - Technical debt, documentation coverage, maintainability
- **CI/CD integration** - JSON output for tooling, pass/fail status for pipelines

### For Code Generation

- **Pure domain code** - Your business logic stays clean with zero infrastructure dependencies
- **No boilerplate** - Write a port interface once, get production-ready adapters in seconds
- **Type-safe** - Generation happens at compile time with full type checking
- **Flexible infrastructure** - Swap technologies (REST to GraphQL, MySQL to MongoDB) by changing plugins, not code
- **Extensible** - Add support for any technology through the plugin system

---

## Audit Configuration

### Quality Thresholds

Configure thresholds for quality metrics:

```xml
<configuration>
    <basePackage>com.example</basePackage>
    <failOnError>true</failOnError>
    <failOnWarning>false</failOnWarning>
    <auditConfig>
        <thresholds>
            <maxCyclomaticComplexity>10</maxCyclomaticComplexity>
            <maxMethodLength>50</maxMethodLength>
            <maxClassLength>500</maxClassLength>
            <maxMethodParameters>7</maxMethodParameters>
            <minTestCoverage>80.0</minTestCoverage>
            <minDocumentationCoverage>70.0</minDocumentationCoverage>
            <maxTechnicalDebtMinutes>480</maxTechnicalDebtMinutes>
        </thresholds>
    </auditConfig>
</configuration>
```

### Enable/Disable Rules

```xml
<auditConfig>
    <enabledRules>
        <rule>layering.domain-purity</rule>
        <rule>dependency.no-cycles</rule>
    </enabledRules>
    <disabledRules>
        <rule>complexity.cyclomatic</rule>
    </disabledRules>
</auditConfig>
```

### Report Formats

```xml
<configuration>
    <consoleReport>true</consoleReport>   <!-- Output to logs -->
    <htmlReport>true</htmlReport>          <!-- Rich HTML report -->
    <jsonReport>true</jsonReport>          <!-- Machine-readable -->
    <markdownReport>true</markdownReport>  <!-- Documentation -->
    <reportDirectory>${project.build.directory}/hexaglue-reports</reportDirectory>
</configuration>
```

---

## Plugins

### Official Plugins

| Plugin | Description | Status |
|--------|-------------|--------|
| **JPA Repository** | Spring Data JPA entities, repositories, mappers, adapters | Available |
| **Living Documentation** | Markdown architecture documentation with Mermaid diagrams | Available |
| **REST API** | Spring MVC controllers from driving ports | Planned |
| **OpenAPI** | OpenAPI specification from ports | Planned |
| **Kafka** | Kafka producers and consumers | Planned |
| **GraphQL** | GraphQL schema and resolvers | Planned |

---

## Optional jMolecules Integration

HexaGlue can leverage **jMolecules annotations** to make your domain intent explicit:

```java
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

@AggregateRoot
public class Order {
    @Identity
    private OrderId id;
    // ...
}
```

**jMolecules is not required**: HexaGlue discovers domain concepts automatically through smart heuristics. When present, annotations provide EXPLICIT confidence and remove classification ambiguity.

### DDD Tactical Patterns

| Annotation | Purpose |
|------------|---------|
| `@AggregateRoot` | Marks an aggregate root |
| `@Entity` | Marks a domain entity |
| `@ValueObject` | Marks a value object |
| `@Identity` | Marks an identity field |
| `@Repository` | Marks a repository port |

### Hexagonal Architecture

| Annotation | Purpose |
|------------|---------|
| `@PrimaryPort` | Marks a driving (inbound) port |
| `@SecondaryPort` | Marks a driven (outbound) port |

### Domain Events

| Annotation | Purpose |
|------------|---------|
| `@DomainEvent` | Marks a domain event |
| `@Externalized` | Marks an event for external publication |

---

## Documentation

| Document | Description |
|----------|-------------|
| [Getting Started](docs/GETTING_STARTED.md) | Progressive tutorial from audit to generation |
| [Architecture Audit Guide](docs/ARCHITECTURE_AUDIT.md) | Complete audit configuration and rules reference |
| [User Guide](docs/USER_GUIDE.md) | Complete concepts and features reference |
| [Configuration](docs/CONFIGURATION.md) | Maven plugin and `hexaglue.yaml` reference |
| [SPI Reference](docs/SPI_REFERENCE.md) | Plugin development API |
| [Plugin Development](docs/PLUGIN_DEVELOPMENT.md) | Create your own plugins |

---

## Project Structure

```
hexaglue/
├── hexaglue-maven-plugin/ # Maven plugin (generate, audit, generate-and-audit)
├── hexaglue-core/         # Analysis, classification, and audit engine
├── hexaglue-spi/          # Stable API for plugins (JDK-only)
├── hexaglue-testing/      # Test utilities
├── hexaglue-plugins/      # Official plugins
│   ├── hexaglue-plugin-jpa/        # JPA generation
│   └── hexaglue-plugin-living-doc/ # Architecture documentation
├── examples/              # Working examples
└── docs/                  # Documentation
```

---

## Example Applications

| Example | Description |
|---------|-------------|
| [minimal](examples/minimal/) | Simple example with basic domain |
| [coffeeshop](examples/coffeeshop/) | Coffee ordering application |
| [ecommerce](examples/ecommerce/) | Rich domain with relationships |

---

## Prerequisites

- **Java**: 17 or later
- **Maven**: 3.8 or later

---

## License

HexaGlue is distributed under the **Mozilla Public License 2.0 (MPL-2.0)**.

- May be used in commercial and proprietary products
- Your application code remains your own
- Generated code belongs to you without restriction
- Modifications to HexaGlue source files must be shared under MPL-2.0

[Learn more about MPL-2.0](https://www.mozilla.org/MPL/2.0/)

---

## Support

- [GitHub Issues](https://github.com/hexaglue/hexaglue/issues): Report bugs or request features
- [GitHub Discussions](https://github.com/hexaglue/hexaglue/discussions): Ask questions and share ideas

---

<div align="center">

**HexaGlue - Design, Audit, and Generate Hexagonal Architecture**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
