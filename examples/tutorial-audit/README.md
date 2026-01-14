# Tutorial: Architecture Audit (Clean)

This example demonstrates a **well-structured hexagonal architecture** that passes all HexaGlue audit constraints.

## What This Example Shows

- Proper DDD patterns that satisfy all DDD constraints
- Clean hexagonal architecture satisfying all hexagonal constraints
- Best practices for domain modeling

## Domain Model

**Customer Aggregate**
- `Customer` - Aggregate root with identity
- `CustomerId` - Typed identifier (immutable record)
- `Email` - Validated value object (immutable record)
- `Address` - Multi-field value object (immutable record)

**Ports**
- `CustomerUseCases` - Driving port (interface)
- `CustomerRepository` - Driven port (interface)

## DDD Best Practices Demonstrated

| Constraint | How This Example Satisfies It |
|------------|------------------------------|
| `ddd:entity-identity` | Customer has CustomerId identity field |
| `ddd:aggregate-repository` | CustomerRepository exists for Customer aggregate |
| `ddd:value-object-immutable` | Email, Address, CustomerId are records |
| `ddd:domain-purity` | No framework dependencies in domain |

## Hexagonal Best Practices Demonstrated

| Constraint | How This Example Satisfies It |
|------------|------------------------------|
| `hexagonal:port-interface` | CustomerUseCases and CustomerRepository are interfaces |
| `hexagonal:dependency-direction` | Domain has no infrastructure imports |

## Running the Example

```bash
mvn compile
```

## Expected Output

```
========================================
  DDD AUDIT REPORT
========================================

PROJECT: tutorial-audit
ANALYZED: 5 types, 2 ports
RESULT: PASSED

SUMMARY
-------
Total Constraints: 14
Passed: 14
Failed: 0
```

## Exploring the Reports

After running, check `target/hexaglue/audit/`:

- `audit-report.json` - Machine-readable results
- `audit-report.html` - Visual report
- `audit-report.md` - Markdown documentation

## Contrast with sample-audit-violations

The `sample-audit-violations` example intentionally contains violations to demonstrate what the audit catches. This example (`tutorial-audit`) shows the correct patterns.

## Related Documentation

- [Architecture Audit Tutorial](../../docs/ARCHITECTURE_AUDIT.md)
- [Classification System](../../docs/CLASSIFICATION.md)
