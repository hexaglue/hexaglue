# Architecture Audit

Validate your Domain-Driven Design and Hexagonal Architecture implementation. This tutorial covers the audit plugin, its constraints, and CI/CD integration.

## What the Audit Validates

The audit plugin checks 14 constraints in two categories:

### DDD Constraints

| Constraint | Severity | Description |
|------------|----------|-------------|
| `ddd:entity-identity` | CRITICAL | Entities must have identity fields |
| `ddd:aggregate-repository` | MAJOR | Aggregate roots must have repositories |
| `ddd:value-object-immutable` | CRITICAL | Value objects must be immutable |
| `ddd:aggregate-cycle` | BLOCKER | No circular dependencies between aggregates |
| `ddd:aggregate-boundary` | MAJOR | Entities accessible only through aggregate root |
| `ddd:aggregate-consistency` | MAJOR | Aggregates maintain proper boundaries |
| `ddd:domain-purity` | MAJOR | Domain layer has no infrastructure dependencies |
| `ddd:event-naming` | MINOR | Domain events named in past tense |

### Hexagonal Constraints

| Constraint | Severity | Description |
|------------|----------|-------------|
| `hexagonal:port-interface` | CRITICAL | Ports must be interfaces |
| `hexagonal:dependency-direction` | BLOCKER | Domain must not depend on infrastructure |
| `hexagonal:layer-isolation` | MAJOR | Layers respect dependency rules |
| `hexagonal:port-direction` | MAJOR | Port direction matches usage |
| `hexagonal:dependency-inversion` | CRITICAL | Dependencies on abstractions only |
| `hexagonal:port-coverage` | MAJOR | Ports have adapter implementations |

### Severity Levels

| Level | Build Impact | Usage |
|-------|--------------|-------|
| **BLOCKER** | Fails immediately | Critical architectural violations |
| **CRITICAL** | Fails if `errorOnCritical: true` | Serious DDD violations |
| **MAJOR** | Warning | Important issues to fix |
| **MINOR** | Info | Best practice violations |

## Setup

### Add the Audit Plugin

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
            <artifactId>hexaglue-plugin-audit</artifactId>
            <version>2.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

### Run the Audit

```bash
mvn compile
```

The audit runs automatically after classification.

## Understanding the Output

### Console Output

```
========================================
  DDD AUDIT REPORT
========================================

PROJECT: my-project
ANALYZED: 12 types, 4 ports
RESULT: PASSED

SUMMARY
-------
Total Constraints: 14
Passed: 14
Failed: 0
Skipped: 0

DDD CONSTRAINTS
---------------
[OK] ddd:entity-identity
[OK] ddd:aggregate-repository
[OK] ddd:value-object-immutable
[OK] ddd:aggregate-cycle

HEXAGONAL CONSTRAINTS
---------------------
[OK] hexagonal:port-interface
[OK] hexagonal:dependency-direction
[OK] hexagonal:layer-isolation
```

### Generated Reports

Reports are generated in `target/hexaglue/audit/`:

| File | Format | Description |
|------|--------|-------------|
| `audit-report.json` | JSON | Machine-readable for CI/CD |
| `audit-report.html` | HTML | Visual report for browsers |
| `audit-report.md` | Markdown | Readable documentation |

## Handling Violations

### Example Violation

```
========================================
  DDD AUDIT REPORT
========================================

RESULT: FAILED

VIOLATIONS
----------
[CRITICAL] ddd:value-object-immutable
  - com.example.domain.Money has setter method: setAmount
    Recommendation: Make Money immutable by removing setters

[MAJOR] ddd:aggregate-repository
  - com.example.domain.Customer has no repository
    Recommendation: Create CustomerRepository interface
```

### Fixing Common Violations

**ddd:value-object-immutable**
```java
// Before (violation)
public class Money {
    private BigDecimal amount;
    public void setAmount(BigDecimal amount) { // Setter!
        this.amount = amount;
    }
}

// After (fixed) - Use record
public record Money(BigDecimal amount, String currency) {}
```

**ddd:entity-identity**
```java
// Before (violation)
public class Order {
    private String customerName;
    // No identity field!
}

// After (fixed)
public class Order {
    private final OrderId id;  // Identity
    private String customerName;
}
```

**ddd:aggregate-repository**
```java
// Create repository interface for aggregate root
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}
```

**hexagonal:dependency-direction**
```java
// Before (violation) - Domain depends on Spring
@Entity  // JPA annotation in domain!
public class Order { }

// After (fixed) - Pure domain
public class Order {
    // No framework dependencies
}
```

## Configuration Options

Audit failure behavior is controlled by 3 properties, configurable via Maven POM, `-D`, or `hexaglue.yaml`.
Precedence: **Maven POM / -D > YAML > defaults**.

| Property | Default | Description |
|----------|---------|-------------|
| `failOnError` | `true` | Fail the Maven build when audit errors are found |
| `errorOnBlocker` | `true` | Treat BLOCKER violations as errors |
| `errorOnCritical` | `false` | Treat CRITICAL violations as errors |

### YAML Configuration

```yaml
plugins:
  io.hexaglue.plugin.audit:
    errorOnBlocker: true       # Default
    errorOnCritical: true      # Default: false
    generateDocs: false        # Default
```

### Strict Mode (CI/CD)

Enable CRITICAL violations as errors in CI:

```yaml
plugins:
  io.hexaglue.plugin.audit:
    errorOnCritical: true
```

Or via command line:
```bash
mvn compile -Dhexaglue.errorOnCritical=true
```

### Lenient Mode (Development)

Disable build failure during development:

```bash
mvn compile -Dhexaglue.failOnError=false
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Architecture Audit

on: [push, pull_request]

jobs:
  audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run Audit
        run: mvn compile

      - name: Upload Audit Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: audit-report
          path: target/hexaglue/audit/
```

### GitLab CI

```yaml
audit:
  stage: test
  script:
    - mvn compile
  artifacts:
    when: always
    paths:
      - target/hexaglue/audit/
    reports:
      junit: target/hexaglue/audit/audit-report.json
```

### Quality Gate Example

```yaml
- name: Check Audit Results
  run: |
    VIOLATIONS=$(jq '.violations | length' target/hexaglue/audit/audit-report.json)
    if [ "$VIOLATIONS" -gt 0 ]; then
      echo "Architecture violations detected: $VIOLATIONS"
      exit 1
    fi
```

## Metrics and Quality Scores

The audit also calculates architecture metrics:

| Metric | Description | Threshold |
|--------|-------------|-----------|
| Domain Coverage | % of types classified as domain | > 60% |
| Domain Purity | % of domain types without infrastructure deps | 100% |
| Port Coverage | % of ports with implementations | 100% |
| Coupling | Average instability of packages | < 0.7 |
| Aggregate Size | Average entities per aggregate | < 7 |

### Metrics in Report

```json
{
  "metrics": {
    "domainCoverage": 0.75,
    "domainPurity": 1.0,
    "portCoverage": 1.0,
    "averageCoupling": 0.45,
    "aggregateCount": 3
  }
}
```

## Best Practices

1. **Run early, run often** - Integrate audit in CI from day one
2. **Fix blockers immediately** - BLOCKER violations indicate serious issues
3. **Track metrics over time** - Use JSON reports for trend analysis
4. **Review in PRs** - Include audit results in code review

## Combining with Other Plugins

Full production configuration:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
        <failOnUnclassified>true</failOnUnclassified>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-jpa</artifactId>
            <version>2.0.0</version>
        </dependency>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-living-doc</artifactId>
            <version>2.0.0</version>
        </dependency>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-audit</artifactId>
            <version>2.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

## What's Next?

- [Living Documentation](LIVING_DOCUMENTATION.md) - Generate architecture docs
- [Configuration](CONFIGURATION.md) - All configuration options
- [Validation](VALIDATION.md) - Validate classification before audit

**Examples:**
- [tutorial-audit](../examples/tutorial-audit/) - Well-structured project passing all constraints
- [sample-audit-violations](../examples/sample-audit-violations/) - Intentional violations for testing
