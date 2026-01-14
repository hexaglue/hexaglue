# Validation

Validate your domain classification before generating code. This tutorial explains how to use the validation goal and interpret results.

## Why Validate?

Validation ensures all domain types are properly classified before code generation. This catches:

- Ambiguous types that need explicit classification
- Missing annotations or configuration
- Types that shouldn't be in the domain package

## Running Validation

Validation runs automatically with `mvn compile`. To run validation only:

```bash
mvn hexaglue:validate
```

## Understanding the Output

```
[INFO] --- hexaglue:3.0.0-SNAPSHOT:validate ---
[INFO]
[INFO] CLASSIFICATION SUMMARY
[INFO] --------------------------------------------------------------
[INFO] EXPLICIT:                8 ( 66,7%)
[INFO] INFERRED:                4 ( 33,3%)
[INFO] UNCLASSIFIED:            0 (  0,0%)
[INFO] TOTAL:                  12
[INFO]
[INFO] Status: PASSED
```

### Classification Categories

| Category | Meaning |
|----------|---------|
| **EXPLICIT** | Type has jMolecules annotation or hexaglue.yaml explicit entry |
| **INFERRED** | HexaGlue automatically detected the type (semantic analysis) |
| **UNCLASSIFIED** | Type could not be classified with sufficient confidence |

### Status

- **PASSED** - No unclassified types (or `failOnUnclassified: false`)
- **FAILED** - Unclassified types exist and `failOnUnclassified: true`

## Resolving UNCLASSIFIED Types

When validation reports unclassified types:

```
[WARN] UNCLASSIFIED TYPES:
[WARN]   - com.example.domain.AmbiguousClass
[WARN]     Candidates: ENTITY, VALUE_OBJECT
[WARN]     Recommendation: Add @Entity or @ValueObject annotation
```

### Option 1: Add jMolecules Annotation

```java
import org.jmolecules.ddd.annotation.Entity;

@Entity
public class AmbiguousClass {
    // ...
}
```

### Option 2: Configure in hexaglue.yaml

```yaml
classification:
  explicit:
    com.example.domain.AmbiguousClass: ENTITY
```

### Option 3: Exclude from Analysis

If the class isn't part of your domain:

```yaml
classification:
  exclude:
    - "com.example.domain.AmbiguousClass"
    - "*.util.*"
```

## Strict Mode for CI/CD

Fail the build when unclassified types exist:

### Via Maven Configuration

```xml
<configuration>
    <basePackage>com.example</basePackage>
    <failOnUnclassified>true</failOnUnclassified>
</configuration>
```

### Via Command Line

```bash
mvn compile -Dhexaglue.failOnUnclassified=true
```

### Via hexaglue.yaml

```yaml
classification:
  validation:
    failOnUnclassified: true
```

## Validation Report

HexaGlue generates a detailed Markdown report:

```
target/hexaglue/reports/validation/validation-report.md
```

The report contains:
- Classification summary
- List of all classified types with their DomainKind
- List of unclassified types with recommendations
- Port direction analysis

## Example hexaglue.yaml

```yaml
classification:
  # Exclude non-domain types
  exclude:
    - "*.util.*"
    - "**.*Exception"
    - "**.*Config"

  # Explicitly classify ambiguous types
  explicit:
    com.example.domain.OrderId: VALUE_OBJECT
    com.example.domain.Status: VALUE_OBJECT

  # Validation settings
  validation:
    failOnUnclassified: true  # Strict mode for CI/CD
    allowInferred: true       # Accept inferred classifications
```

## Workflow Recommendation

### Development

```xml
<failOnUnclassified>false</failOnUnclassified>
```

Allows exploration and iterative development.

### CI/CD Pipeline

```xml
<failOnUnclassified>true</failOnUnclassified>
```

Ensures all types are explicitly or confidently classified.

## What's Next?

- [Classification](CLASSIFICATION.md) - Deep dive into how classification works
- [Configuration](CONFIGURATION.md) - All configuration options
- [Architecture Audit](ARCHITECTURE_AUDIT.md) - Validate DDD/Hexagonal rules

**Example:** See [tutorial-validation](../examples/tutorial-validation/) for a complete validation example.
