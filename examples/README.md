# HexaGlue Examples

This directory contains example applications demonstrating HexaGlue's capabilities.

## Example Categories

### Tutorial Examples (`tutorial-*`)

Focused examples demonstrating specific features:

| Example | Feature | Tutorial |
|---------|---------|----------|
| [tutorial-validation](./tutorial-validation/) | Classification validation | [VALIDATION.md](../docs/VALIDATION.md) |
| [tutorial-living-doc](./tutorial-living-doc/) | Living Documentation | [LIVING_DOCUMENTATION.md](../docs/LIVING_DOCUMENTATION.md) |
| [tutorial-audit](./tutorial-audit/) | Architecture Audit (clean) | [ARCHITECTURE_AUDIT.md](../docs/ARCHITECTURE_AUDIT.md) |

### Sample Applications (`sample-*`)

Complete applications showcasing various domain complexities:

| Example | Complexity | What It Demonstrates |
|---------|------------|---------------------|
| [sample-basic](./sample-basic/) | Beginner | Single aggregate, basic entity generation |
| [sample-value-objects](./sample-value-objects/) | Intermediate | Value objects, enums, embedded collections |
| [sample-multi-aggregate](./sample-multi-aggregate/) | Advanced | Multiple aggregates, relationships, rich domain |
| [sample-audit-violations](./sample-audit-violations/) | Advanced | Intentional violations for audit testing |
| [sample-starwars](./sample-starwars/) | Intermediate | Complete hexagonal app with Star Wars theme |
| [sample-pokedex](./sample-pokedex/) | Intermediate | Complete hexagonal app with Pokemon theme |

## Quick Start

```bash
# Build HexaGlue first
cd hexaglue
make install

# Run any example
cd examples/sample-basic
mvn compile
```

## Learning Paths

### For Java Developers

1. **Start here**: [sample-basic](./sample-basic/) - Basic aggregate with JPA generation
2. **Next**: [sample-value-objects](./sample-value-objects/) - Value objects and collections
3. **Then**: [tutorial-validation](./tutorial-validation/) - Classification control
4. **Advanced**: [sample-multi-aggregate](./sample-multi-aggregate/) - Real-world patterns

### For Architects

1. **Start here**: [tutorial-validation](./tutorial-validation/) - Classification system
2. **Next**: [tutorial-audit](./tutorial-audit/) - Architecture constraints
3. **Then**: [tutorial-living-doc](./tutorial-living-doc/) - Architecture documentation
4. **Study**: [sample-audit-violations](./sample-audit-violations/) - What violations look like

## Example Details

### sample-basic

The simplest possible HexaGlue example. One aggregate, one repository.

```
sample-basic/
├── domain/
│   ├── Task.java          # Aggregate Root
│   └── TaskId.java        # Identifier
└── ports/
    ├── in/TaskUseCases.java
    └── out/TaskRepository.java
```

**Plugins**: Living Doc, Audit

---

### sample-value-objects

Demonstrates value objects, enums, and embedded collections.

```
sample-value-objects/
├── domain/
│   ├── Order.java         # Aggregate Root
│   ├── OrderLine.java     # Value Object (embedded)
│   └── OrderStatus.java   # Enum
└── ports/
    ├── in/OrderingCoffee.java
    └── out/Orders.java
```

**Key Concepts**: `@Embeddable`, `@ElementCollection`, `@Enumerated`

---

### sample-multi-aggregate

Rich domain model with multiple aggregates and inter-aggregate references.

```
sample-multi-aggregate/
├── domain/
│   ├── order/      # Order aggregate
│   ├── customer/   # Customer aggregate
│   └── product/    # Product aggregate
└── ports/
    ├── in/         # 3 driving ports
    └── out/        # 4 driven ports
```

**Key Concepts**: Multiple aggregates, references by ID, entity relationships

---

### tutorial-validation

Demonstrates the classification validation phase.

```
tutorial-validation/
├── hexaglue.yaml           # Classification config
├── domain/
│   ├── Order.java          # @AggregateRoot
│   └── OrderId.java        # Explicit in yaml
└── util/
    └── StringUtils.java    # EXCLUDED
```

**Key Concepts**: `classification.exclude`, `classification.explicit`, `failOnUnclassified`

---

### tutorial-living-doc

Focused demonstration of the Living Documentation plugin.

```
tutorial-living-doc/
├── domain/
│   ├── Order.java          # Aggregate Root
│   ├── Product.java        # Aggregate Root
│   └── ...                 # Value objects
└── ports/
    ├── in/                 # Driving ports
    └── out/                # Driven ports
```

**Generates**: README.md, domain.md, ports.md, diagrams.md

---

### tutorial-audit

Clean architecture example that passes all audit constraints.

```
tutorial-audit/
├── domain/
│   ├── Customer.java       # Aggregate with identity
│   ├── CustomerId.java     # Immutable record
│   └── ...                 # Value objects
└── ports/
    ├── in/CustomerUseCases.java
    └── out/CustomerRepository.java
```

**Demonstrates**: All DDD and hexagonal best practices

---

### sample-audit-violations

Intentional architecture violations for testing the audit plugin.

**Use for**: Understanding what violations look like, testing CI integration

---

## Generated Output Structure

```
target/hexaglue/
├── generated-sources/      # Generated Java code
│   └── com/example/infrastructure/
│       ├── persistence/
│       │   ├── *Entity.java
│       │   ├── *JpaRepository.java
│       │   └── *Mapper.java
│       └── adapters/
│           └── *Adapter.java
├── living-doc/             # Living Documentation
│   ├── README.md
│   ├── domain.md
│   ├── ports.md
│   └── diagrams.md
├── audit/                  # Audit reports
│   ├── audit-report.json
│   ├── audit-report.html
│   └── audit-report.md
└── reports/
    └── validation/         # Classification report
        └── validation-report.md
```

## Related Documentation

- [Quick Start](../docs/QUICK_START.md) - Get started in 10 minutes
- [Classification](../docs/CLASSIFICATION.md) - How types are classified
- [Configuration](../docs/CONFIGURATION.md) - All configuration options

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with love by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
