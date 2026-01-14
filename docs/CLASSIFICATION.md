# Classification System

HexaGlue analyzes your codebase and classifies domain types automatically. This tutorial explains how classification works and how to resolve ambiguous types.

## How Classification Works

HexaGlue uses a **priority-based system** where higher priority criteria win:

| Priority | Source | Examples |
|----------|--------|----------|
| **100** | Explicit | jMolecules annotations, hexaglue.yaml explicit |
| **85** | Semantic | Relationship-based analysis (CoreAppClass patterns) |
| **80** | Heuristics | Strong structural patterns |
| **< 70** | Rejected | Type becomes UNCLASSIFIED |

Types that cannot be classified with Priority >= 70 become `UNCLASSIFIED` and require explicit clarification.

## Domain Kinds

HexaGlue classifies types into 12 categories:

### Domain Types

| Kind | Description | Typical Pattern |
|------|-------------|-----------------|
| **AGGREGATE_ROOT** | Entry point with identity and invariants | `Order`, `Customer`, `Product` |
| **ENTITY** | Identity within an aggregate (not root) | `OrderLine` within `Order` |
| **VALUE_OBJECT** | Immutable, defined by attributes | `Money`, `Address` |
| **IDENTIFIER** | Value object wrapping an identity | `OrderId`, `CustomerId` |
| **DOMAIN_EVENT** | Immutable record of domain occurrence | `OrderPlaced` |
| **EXTERNALIZED_EVENT** | Event for external publication | Integration events |
| **DOMAIN_SERVICE** | Stateless domain operation (no port deps) | `PricingCalculator` |

### Actor Types (CoreAppClass)

| Kind | Description | Pattern |
|------|-------------|---------|
| **APPLICATION_SERVICE** | Implements DRIVING + depends on DRIVEN | Main orchestrators |
| **INBOUND_ONLY** | Implements DRIVING, no DRIVEN deps | Query handlers |
| **OUTBOUND_ONLY** | Depends on DRIVEN, no DRIVING impl | Background processors |
| **SAGA** | OUTBOUND_ONLY + 2+ DRIVEN deps + stateful | Long-running processes |

### Error State

| Kind | Description |
|------|-------------|
| **UNCLASSIFIED** | Type could not be classified (Priority < 70) |

## Port Direction Detection

HexaGlue detects port direction based on relationships with CoreAppClass (your application services):

### DRIVING Ports (Inbound)

```
External Actor (REST, CLI, etc.)
        ↓
  [DRIVING PORT] ← Interface IMPLEMENTED by CoreAppClass
        ↓
    CoreAppClass
```

Detection: Interface is implemented by at least one CoreAppClass.

### DRIVEN Ports (Outbound)

```
    CoreAppClass
        ↓
  [DRIVEN PORT] ← Interface USED by CoreAppClass
        ↓
  Infrastructure (DB, API, etc.)
```

Detection: Interface is used by CoreAppClass + implementation is missing or internal.

## Making Classification Explicit

### Method 1: jMolecules Annotations (Recommended)

Add annotations to your domain classes:

```java
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.annotation.Repository;

@AggregateRoot
public class Order {
    private OrderId id;
    private List<OrderLine> lines;
}

@Entity
public class OrderLine {
    private String productId;
    private int quantity;
}

@ValueObject
public record Money(BigDecimal amount, String currency) {}

@Repository
public interface OrderRepository {
    Order save(Order order);
}
```

Add jMolecules as a provided dependency (no runtime impact):

```xml
<dependency>
    <groupId>org.jmolecules</groupId>
    <artifactId>jmolecules-ddd</artifactId>
    <version>1.9.0</version>
    <scope>provided</scope>
</dependency>
```

### Method 2: hexaglue.yaml Configuration

For classes you cannot annotate (third-party, legacy code):

```yaml
classification:
  explicit:
    com.example.OrderId: VALUE_OBJECT
    com.thirdparty.StatusCode: VALUE_OBJECT
    com.legacy.Helper: DOMAIN_SERVICE
```

## Excluding Types from Analysis

Exclude utility classes, exceptions, and non-domain types:

```yaml
classification:
  exclude:
    - "*.util.*"           # com.example.util.* packages
    - "**.*Exception"      # All exception classes
    - "**.*Config"         # All configuration classes
    - "**.*Test"           # All test classes
```

**Glob patterns:**
- `*` matches within a package segment
- `**` matches across package segments

## Resolving UNCLASSIFIED Types

When HexaGlue cannot classify a type, it becomes `UNCLASSIFIED`. Resolution options:

1. **Add jMolecules annotation** (recommended)
   ```java
   @AggregateRoot
   public class Order { }
   ```

2. **Configure in hexaglue.yaml**
   ```yaml
   classification:
     explicit:
       com.example.AmbiguousClass: ENTITY
   ```

3. **Exclude from analysis** (if not domain)
   ```yaml
   classification:
     exclude:
       - "com.example.AmbiguousClass"
   ```

## Classification Summary

After running `mvn compile`, HexaGlue outputs a summary:

```
[INFO] CLASSIFICATION SUMMARY
[INFO] --------------------------------------------------------------
[INFO] EXPLICIT:                8 ( 66,7%)  ← Annotated or configured
[INFO] INFERRED:                4 ( 33,3%)  ← Automatically detected
[INFO] UNCLASSIFIED:            0 (  0,0%)  ← Require attention
[INFO] TOTAL:                  12
[INFO]
[INFO] Status: PASSED
```

## Strict Mode for CI/CD

Fail the build if unclassified types remain:

```xml
<configuration>
    <basePackage>com.example</basePackage>
    <failOnUnclassified>true</failOnUnclassified>
</configuration>
```

Or via command line:
```bash
mvn compile -Dhexaglue.failOnUnclassified=true
```

## Example Configuration

Complete `hexaglue.yaml` example:

```yaml
classification:
  # Exclude non-domain types
  exclude:
    - "*.util.*"
    - "**.*Exception"
    - "**.*Config"

  # Explicit classifications
  explicit:
    com.example.domain.OrderId: VALUE_OBJECT
    com.example.domain.Status: VALUE_OBJECT

  # Validation settings
  validation:
    failOnUnclassified: false  # Set true for CI/CD
    allowInferred: true
```

## What's Next?

- [Validation](VALIDATION.md) - Validate classification before generation
- [Configuration](CONFIGURATION.md) - All configuration options
- [JPA Generation](JPA_GENERATION.md) - Generate infrastructure from classified types

**Example:** See [tutorial-validation](../examples/tutorial-validation/) for a complete classification example.
