# Architecture Audit

Validate your Domain-Driven Design and Hexagonal Architecture implementation. This tutorial covers the audit plugin, its constraints, and CI/CD integration.

## What the Audit Validates

The audit plugin checks 15 constraints in two categories:

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
| `hexagonal:application-purity` | MAJOR | Application layer has no infrastructure framework dependencies |

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
            <version>3.0.0</version>
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
Total Constraints: 15
Passed: 15
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

The audit calculates 21 architecture metrics across 5 categories:

### Domain & DDD

| Metric | Description | Warning Threshold |
|--------|-------------|-------------------|
| `domain.coverage` | % of types classified as domain | < 30% |
| `domain.purity` | % of domain types without infrastructure deps | < 100% |
| `ddd.value.object.ratio` | % of domain types that are value objects | < 40% |
| `ddd.event.coverage` | % of aggregates emitting domain events | < 50% |
| `ddd.aggregate.coupling` | Average dependencies per aggregate | > 3.0 |

### Aggregate

| Metric | Description | Warning Threshold |
|--------|-------------|-------------------|
| `aggregate.avgSize` | Average methods per aggregate | > 20 |
| `aggregate.repository.coverage` | % of aggregates with a repository | < 100% |
| `aggregate.cohesion.lcom4` | LCOM4 cohesion (connected components) | > 2 |
| `aggregate.boundary` | % of entities accessible only through their aggregate root | < 80% |
| `aggregate.coupling.efferent` | Efferent coupling ratio | > 0.7 |

### Architecture

| Metric | Description | Warning Threshold |
|--------|-------------|-------------------|
| `architecture.propagation.cost` | Impact of a change across the codebase | > 35% |
| `architecture.mmi` | Maintainability & Modularity Index | < 50 |
| `architecture.modularity.q` | Newman modularity (community structure) | < 0.3 |
| `architecture.cohesion.relational` | Relational cohesion ratio | outside [1.5, 4.0] |
| `architecture.visibility.average` | Average type visibility | > 70% |
| `architecture.fan.out.max` | Maximum outgoing dependencies | > 20 |
| `architecture.dependency.depth` | Longest dependency chain | > 7 |
| `architecture.cyclicity.relative` | % of types involved in cycles | > 5% |

### Hexagonal & Code Quality

| Metric | Description | Warning Threshold |
|--------|-------------|-------------------|
| `hexagonal.adapter.independence` | % of adapters with no cross-adapter dependency | < 80% |
| `code.complexity.average` | Average cyclomatic complexity | > 10 |
| `code.boilerplate.ratio` | % of boilerplate code (getters/setters, constructors) | > 50% |

### Metrics in Report

```json
{
  "metrics": {
    "domain.coverage": 0.75,
    "domain.purity": 1.0,
    "aggregate.repository.coverage": 1.0,
    "architecture.propagation.cost": 0.22,
    "architecture.cyclicity.relative": 0.0,
    "hexagonal.adapter.independence": 1.0
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
            <version>3.0.0</version>
        </dependency>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-living-doc</artifactId>
            <version>3.0.0</version>
        </dependency>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-audit</artifactId>
            <version>3.0.0</version>
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
