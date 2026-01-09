# Architecture Audit Guide

***Validate your application architecture with automated rules and quality metrics.***

---

## Overview

HexaGlue's architecture audit validates your codebase against clean architecture principles and code quality standards. It detects violations, measures metrics, and can fail your build when thresholds are exceeded.

**What the audit validates:**
- **Clean Architecture principles** - Domain purity, layer separation, dependency direction
- **Dependency health** - Stable dependencies, no cycles
- **Code quality** - Complexity, documentation, naming conventions

**Supported architectural styles:**
HexaGlue detects and validates multiple architecture styles: Hexagonal, Clean, Onion, and Layered architectures. The core rules apply to any architecture that follows the Dependency Inversion Principle.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Maven Goals](#maven-goals)
3. [Architecture Detection](#architecture-detection)
4. [Audit Rules Reference](#audit-rules-reference)
5. [Quality Thresholds](#quality-thresholds)
6. [Report Formats](#report-formats)
7. [Configuration Reference](#configuration-reference)
8. [CI/CD Integration](#cicd-integration)
9. [Understanding Audit Results](#understanding-audit-results)
10. [Troubleshooting](#troubleshooting)

---

## Quick Start

Add the HexaGlue Maven plugin with the `audit` goal:

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

Run the audit:

```bash
mvn verify
```

Check the report at `target/hexaglue-reports/hexaglue-audit.html`.

---

## Maven Goals

HexaGlue provides three Maven goals:

| Goal | Phase | Description |
|------|-------|-------------|
| `hexaglue:audit` | `verify` | Architecture audit only |
| `hexaglue:generate` | `generate-sources` | Code generation only |
| `hexaglue:generate-and-audit` | `generate-sources` | Both combined |

### audit

Runs architecture audit without code generation.

```bash
mvn hexaglue:audit
# or during the verify phase
mvn verify
```

### generate-and-audit

Combines code generation and audit in a single execution. Useful for CI/CD pipelines.

```bash
mvn hexaglue:generate-and-audit
```

---

## Architecture Detection

HexaGlue automatically detects your application's architectural style based on package structure, naming conventions, and dependency patterns.

### Detected Styles

| Style | Detection Signals |
|-------|-------------------|
| **HEXAGONAL** | `ports.in`, `ports.out`, `adapter`, driving/driven patterns |
| **CLEAN** | `usecase`, `entity`, `gateway`, clear boundary layers |
| **ONION** | Concentric layer structure, `core`, `domain`, `infrastructure` |
| **LAYERED** | Traditional `controller`, `service`, `repository` structure |
| **UNKNOWN** | Cannot determine dominant style |

The detected style is reported in the audit output and may influence which rules are applied.

---

## Audit Rules Reference

HexaGlue includes built-in rules organized by category. These rules enforce clean architecture principles that apply across different architectural styles.

### Layering Rules

These rules enforce layer separation and the Dependency Inversion Principle.

#### `hexaglue.layer.domain-purity`

**Severity**: ERROR

The domain layer must not depend on infrastructure or framework classes.

**What it checks:**
- Domain classes should not import Spring, JPA, or other framework classes
- Domain should only depend on other domain classes and JDK types
- No infrastructure annotations on domain classes

**Example violation:**
```java
package com.example.domain;

import jakarta.persistence.Entity;  // VIOLATION: Infrastructure annotation

@Entity  // Domain should not have JPA annotations
public class Order {
    // ...
}
```

**Fix:** Keep domain classes pure. Move persistence concerns to the infrastructure layer.

```java
// Domain layer - pure
package com.example.domain;

public class Order {
    private OrderId id;
    // Pure business logic
}

// Infrastructure layer - separate
package com.example.infrastructure.persistence;

@Entity
@Table(name = "orders")
public class OrderEntity {
    // JPA concerns here
}
```

---

#### `hexaglue.layer.presentation-no-domain`

**Severity**: WARNING

Presentation layer should not bypass application services to access domain directly.

**What it checks:**
- Controllers should not directly instantiate domain objects
- Presentation layer should interact through use cases or application services

---

#### `hexaglue.layer.application-no-presentation`

**Severity**: WARNING

Application layer should not depend on presentation layer.

**What it checks:**
- Application services should not import controller classes
- Application layer should be presentation-agnostic

---

### Dependency Rules

These rules enforce healthy dependency patterns.

#### `hexaglue.dependency.stable`

**Severity**: WARNING

Dependencies should flow toward more stable components.

**What it checks:**
- Instability metric (Ce / (Ca + Ce)) of dependencies
- Violations when a component depends on a less stable component

**Principle:** Depend in the direction of stability. Less stable (more likely to change) components should depend on more stable (abstract, unlikely to change) components.

---

#### `hexaglue.dependency.no-cycles`

**Severity**: ERROR

No circular dependencies between packages or classes.

**What it checks:**
- Package-level cycles
- Class-level cycles within a package

**Example violation:**
```
com.example.order -> com.example.customer -> com.example.order (CYCLE)
```

**Fix options:**
1. Extract common interface to a shared package
2. Use dependency inversion (depend on abstractions)
3. Merge packages if they're conceptually one unit

---

### Naming Rules

These rules enforce naming conventions.

#### `hexaglue.naming.repository-suffix`

**Severity**: INFO

Repository interfaces should end with `Repository` suffix.

---

#### `hexaglue.naming.dto-suffix`

**Severity**: INFO

Data Transfer Objects should end with `Dto` or `DTO` suffix.

---

#### `hexaglue.naming.controller-suffix`

**Severity**: INFO

Controller classes should end with `Controller` suffix.

---

### Documentation Rules

#### `hexaglue.doc.public-api`

**Severity**: WARNING

Public API methods should be documented with Javadoc.

---

#### `hexaglue.doc.complex-methods`

**Severity**: INFO

Complex methods should be documented to explain their logic.

---

### Complexity Rules

#### `hexaglue.complexity.cyclomatic`

**Severity**: WARNING

Methods should not exceed maximum cyclomatic complexity.

**Default threshold**: 10

---

#### `hexaglue.complexity.method-length`

**Severity**: WARNING

Methods should not exceed maximum line count.

**Default threshold**: 50 lines

---

## Quality Thresholds

Configure thresholds to enforce quality standards:

```xml
<configuration>
    <auditConfig>
        <thresholds>
            <!-- Complexity -->
            <maxCyclomaticComplexity>10</maxCyclomaticComplexity>
            <maxMethodLength>50</maxMethodLength>
            <maxClassLength>500</maxClassLength>
            <maxMethodParameters>7</maxMethodParameters>
            <maxNestingDepth>4</maxNestingDepth>

            <!-- Coverage -->
            <minTestCoverage>80.0</minTestCoverage>
            <minDocumentationCoverage>70.0</minDocumentationCoverage>

            <!-- Technical Debt -->
            <maxTechnicalDebtMinutes>480</maxTechnicalDebtMinutes>

            <!-- Maintainability -->
            <minMaintainabilityRating>3.0</minMaintainabilityRating>
        </thresholds>
    </auditConfig>
</configuration>
```

### Threshold Reference

| Threshold | Default | Description |
|-----------|---------|-------------|
| `maxCyclomaticComplexity` | 10 | Max cyclomatic complexity per method |
| `maxMethodLength` | 50 | Max lines per method |
| `maxClassLength` | 500 | Max lines per class |
| `maxMethodParameters` | 7 | Max parameters per method |
| `maxNestingDepth` | 4 | Max nesting depth |
| `minTestCoverage` | 80.0 | Min test coverage (%) |
| `minDocumentationCoverage` | 70.0 | Min documentation coverage (%) |
| `maxTechnicalDebtMinutes` | 480 | Max technical debt (8 hours) |
| `minMaintainabilityRating` | 3.0 | Min maintainability (0-5 scale) |

---

## Report Formats

HexaGlue generates reports in multiple formats.

### Console Report

Output directly to Maven logs. Enabled by default.

```xml
<consoleReport>true</consoleReport>
```

**Sample output:**
```
================================================================================
HEXAGLUE AUDIT REPORT
================================================================================
Project: my-application
Detected Architecture: HEXAGONAL
HexaGlue Version: 3.0.0
Analysis Duration: 2.3s

SUMMARY
--------------------------------------------------------------------------------
Total Violations: 5
  Errors:   2
  Warnings: 2
  Info:     1
Status: FAILED

QUALITY METRICS
--------------------------------------------------------------------------------
Test Coverage:          85.2%
Documentation Coverage: 72.1%
Technical Debt:         120 minutes
Maintainability Rating: 4.2/5.0

VIOLATIONS
--------------------------------------------------------------------------------
ERROR (2)
  [hexaglue.layer.domain-purity]
  Location: com/example/domain/Order.java:45
  Domain type 'Order' has field 'repository' of infrastructure type 'JpaRepository'

  [hexaglue.dependency.no-cycles]
  Location: com/example/order
  Cycle detected: order -> customer -> order
================================================================================
```

### HTML Report

Rich, styled report for viewing in a browser.

```xml
<htmlReport>true</htmlReport>
```

**Output:** `target/hexaglue-reports/hexaglue-audit.html`

### JSON Report

Machine-readable format for tooling integration.

```xml
<jsonReport>true</jsonReport>
```

**Output:** `target/hexaglue-reports/hexaglue-audit.json`

**Sample structure:**
```json
{
  "metadata": {
    "timestamp": "2026-01-09T10:30:00Z",
    "projectName": "my-application",
    "hexaglueVersion": "3.0.0",
    "detectedStyle": "HEXAGONAL"
  },
  "summary": {
    "passed": false,
    "totalViolations": 5,
    "errors": 2,
    "warnings": 2,
    "info": 1
  },
  "qualityMetrics": {
    "testCoverage": 85.2,
    "documentationCoverage": 72.1,
    "technicalDebtMinutes": 120,
    "maintainabilityRating": 4.2
  },
  "violations": [
    {
      "ruleId": "hexaglue.layer.domain-purity",
      "severity": "ERROR",
      "message": "Domain type 'Order' has field of infrastructure type",
      "location": "com/example/domain/Order.java:45"
    }
  ]
}
```

### Markdown Report

```xml
<markdownReport>true</markdownReport>
```

**Output:** `target/hexaglue-reports/hexaglue-audit.md`

---

## Configuration Reference

### Full Configuration Example

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
        <!-- Required: base package to analyze -->
        <basePackage>com.example</basePackage>

        <!-- Build behavior -->
        <skip>false</skip>
        <failOnError>true</failOnError>
        <failOnWarning>false</failOnWarning>

        <!-- Report formats -->
        <consoleReport>true</consoleReport>
        <htmlReport>true</htmlReport>
        <jsonReport>false</jsonReport>
        <markdownReport>false</markdownReport>
        <reportDirectory>${project.build.directory}/hexaglue-reports</reportDirectory>

        <!-- Classification profile -->
        <classificationProfile>default</classificationProfile>

        <!-- Audit configuration -->
        <auditConfig>
            <!-- Enable specific rules -->
            <enabledRules>
                <rule>hexaglue.layer.domain-purity</rule>
                <rule>hexaglue.dependency.no-cycles</rule>
            </enabledRules>

            <!-- Disable specific rules -->
            <disabledRules>
                <rule>hexaglue.complexity.cyclomatic</rule>
            </disabledRules>

            <!-- Quality thresholds -->
            <thresholds>
                <maxCyclomaticComplexity>15</maxCyclomaticComplexity>
                <maxMethodLength>60</maxMethodLength>
                <minTestCoverage>75.0</minTestCoverage>
            </thresholds>
        </auditConfig>
    </configuration>
</plugin>
```

### Command Line Properties

```bash
# Skip audit
mvn verify -Dhexaglue.skip=true

# Set base package
mvn verify -Dhexaglue.basePackage=com.myapp

# Fail on warnings
mvn verify -Dhexaglue.failOnWarning=true
```

---

## CI/CD Integration

### GitHub Actions

```yaml
name: Build and Audit

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build and Audit
        run: mvn verify

      - name: Upload Audit Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: hexaglue-audit-report
          path: target/hexaglue-reports/
```

### GitLab CI

```yaml
audit:
  stage: verify
  script:
    - mvn verify
  artifacts:
    when: always
    paths:
      - target/hexaglue-reports/
```

### Quality Gate Pattern

```xml
<configuration>
    <failOnError>true</failOnError>
    <failOnWarning>false</failOnWarning>
    <auditConfig>
        <thresholds>
            <minTestCoverage>80.0</minTestCoverage>
            <maxTechnicalDebtMinutes>240</maxTechnicalDebtMinutes>
        </thresholds>
    </auditConfig>
</configuration>
```

---

## Understanding Audit Results

### Violation Severities

| Severity | Build Impact | Description |
|----------|--------------|-------------|
| **ERROR** | Fails build (if `failOnError=true`) | Critical architectural violations |
| **WARNING** | Fails build (if `failOnWarning=true`) | Potential issues to address |
| **INFO** | Never fails build | Suggestions for improvement |

### Quality Metrics

| Metric | Range | Good Value | Description |
|--------|-------|------------|-------------|
| Test Coverage | 0-100% | > 80% | Percentage of code covered by tests |
| Documentation Coverage | 0-100% | > 70% | Percentage of public API documented |
| Technical Debt | minutes | < 480 (8h) | Estimated effort to fix all issues |
| Maintainability Rating | 0-5 | > 4.0 | Overall code maintainability score |

---

## Troubleshooting

### Audit Not Running

**Check:**
1. Goal is configured: `<goal>audit</goal>`
2. Not skipped: `<skip>false</skip>`
3. Phase is reached: `mvn verify`

### No Violations Found

**Check:**
1. `basePackage` matches your source packages
2. Rules are not all disabled
3. Source files are in `src/main/java`

### Report Not Generated

**Check:**
1. Report format is enabled: `<htmlReport>true</htmlReport>`
2. `reportDirectory` is writable
3. Audit completed without fatal errors

---

## Summary

| Task | Command |
|------|---------|
| Run audit only | `mvn hexaglue:audit` or `mvn verify` |
| Generate + audit | `mvn hexaglue:generate-and-audit` |
| Skip audit | `mvn verify -Dhexaglue.skip=true` |
| View HTML report | Open `target/hexaglue-reports/hexaglue-audit.html` |

---

<div align="center">

**HexaGlue - Design, Audit, and Generate Hexagonal Architecture**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
