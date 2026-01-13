# HexaGlue Validation Demo

This example demonstrates the **HexaGlue Validation Phase** features introduced in v3.

## Features Demonstrated

| Feature | Description | File |
|---------|-------------|------|
| `hexaglue:validate` goal | Validate classification without generating code | `pom.xml` |
| `classification.exclude` | Exclude types by glob pattern | `hexaglue.yaml` |
| `classification.explicit` | Explicit type classification | `hexaglue.yaml` |
| `failOnUnclassified` | Fail build on UNCLASSIFIED types | `pom.xml` |
| Application Service | CoreAppClass orchestrating domain | `OrderApplicationService.java` |

## Project Structure

```
validation-demo/
├── pom.xml                 # Maven config with validate goal
├── hexaglue.yaml           # Classification configuration
├── README.md               # This file
└── src/main/java/com/example/validation/
    ├── application/
    │   └── OrderApplicationService.java  # APPLICATION_SERVICE (inferred)
    ├── domain/
    │   ├── Order.java          # EXPLICIT via @AggregateRoot
    │   ├── OrderId.java        # EXPLICIT via hexaglue.yaml
    │   ├── OrderLine.java      # EXPLICIT via @Entity
    │   └── AmbiguousClass.java # NOT ANALYZED (no domain signals)
    ├── port/
    │   ├── OrderRepository.java  # DRIVEN port
    │   └── OrderService.java     # DRIVING port
    └── util/
        └── StringUtils.java    # EXCLUDED via pattern
```

## Classification Results

| Type | Classification | Method | Priority |
|------|---------------|--------|----------|
| `Order` | AGGREGATE_ROOT | @AggregateRoot annotation | 100 |
| `OrderLine` | ENTITY | @Entity annotation | 100 |
| `OrderId` | VALUE_OBJECT | hexaglue.yaml explicit | 100 |
| `OrderRepository` | DRIVEN port | @Repository annotation | 100 |
| `OrderService` | DRIVING port | Semantic analysis | 85 |
| `StringUtils` | EXCLUDED | Matches `*.util.*` pattern | - |
| `AmbiguousClass` | NOT ANALYZED | No domain signals | - |

## Understanding Classification Scopes

HexaGlue only classifies types that are part of the **domain model**:

| Type Category | Analyzed? | When |
|---------------|-----------|------|
| jMolecules annotated | Yes | Always (Priority 100) |
| hexaglue.yaml explicit | Yes | Always (Priority 100) |
| Embedded in aggregates | Yes | Fields of @AggregateRoot |
| Repository managed | Yes | Types in repository methods |
| Utility classes | No | Unless explicitly configured |
| Infrastructure | No | Unless explicitly configured |

**UNCLASSIFIED** occurs when a type IS detected as part of the domain model
but cannot be classified deterministically. This is rare when using annotations.

## Running the Demo

### 1. Validation Only

Run the `validate` goal to check classification without generating code:

```bash
mvn hexaglue:validate
```

Expected output:
```
[INFO] HEXAGLUE VALIDATION REPORT
[INFO] ================================================================================
[INFO] CLASSIFICATION SUMMARY
[INFO] --------------------------------------------------------------------------------
[INFO] EXPLICIT:                2 ( 66,7%)
[INFO] INFERRED:                1 ( 33,3%)
[INFO] UNCLASSIFIED:            0 (  0,0%)
[INFO] TOTAL:                   3
[INFO]
[INFO] Status: PASSED
```

### 2. Generate with Validation

Run the standard compile to validate, generate, and audit:

```bash
mvn clean compile
```

This will:
1. Run validation (report classification status)
2. Generate code (living-doc, audit reports)
3. Run architecture audit

### 3. Strict Mode (Fail on UNCLASSIFIED)

Use the `strict` profile to fail the build if UNCLASSIFIED types exist:

```bash
mvn clean compile -Pstrict
```

In this demo, the build passes because all domain types are properly classified.

## hexaglue.yaml Reference

```yaml
classification:
  # Exclude patterns (glob syntax)
  exclude:
    - "*.util.*"           # Exclude utility packages
    - "**.*Exception"      # Exclude exceptions
    - "**.*Config"         # Exclude Spring configs

  # Explicit classifications
  explicit:
    com.example.validation.domain.OrderId: VALUE_OBJECT

  # Validation settings
  validation:
    failOnUnclassified: false  # Set true for CI/CD
    allowInferred: true        # Allow semantic inference

plugins:
  audit:
    enabled: true
    generateDocs: true  # Generate ARCHITECTURE-OVERVIEW.md, etc.
```

## Generated Reports

After running `mvn compile`, check the following outputs:

| Output | Location |
|--------|----------|
| Validation report | `target/hexaglue/validation-report.md` |
| Audit HTML report | `target/hexaglue-reports/audit/audit-report.html` |
| Audit JSON report | `target/hexaglue-reports/audit/audit-report.json` |
| Living documentation | `target/generated-sources/generated-docs/` |

## CI/CD Integration

For CI/CD pipelines, use strict validation:

```xml
<configuration>
    <failOnUnclassified>true</failOnUnclassified>
</configuration>
```

Or via command line:
```bash
mvn compile -Dhexaglue.failOnUnclassified=true
```

This ensures all domain types are explicitly classified before deployment.

## Key Takeaways

1. **Annotations are king**: Use jMolecules annotations for explicit classification
2. **hexaglue.yaml for overrides**: Use explicit section for types you can't annotate
3. **Exclude non-domain types**: Use patterns to exclude utilities, configs, etc.
4. **Validate before generate**: Run `hexaglue:validate` to check classification
5. **Strict mode for CI/CD**: Use `failOnUnclassified=true` in pipelines

---

**HexaGlue - Design, Audit, and Generate Hexagonal Architecture**
