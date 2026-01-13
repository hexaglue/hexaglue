# HexaGlue Examples

This directory contains example applications demonstrating HexaGlue's code generation capabilities. Each example builds on the previous one, progressively introducing more advanced concepts.

## Learning Path

| Example | Complexity | What You'll Learn |
|---------|------------|-------------------|
| [minimal](./minimal/) | Beginner | Core concepts, basic entity generation |
| [coffeeshop](./coffeeshop/) | Intermediate | Value objects, enums, embedded collections |
| [ecommerce](./ecommerce/) | Advanced | Multiple aggregates, relationships, rich domain |
| [validation-demo](./validation-demo/) | Intermediate | Validation phase, hexaglue.yaml config, exclude patterns |

## Quick Start

```bash
# Clone and build HexaGlue first
cd hexaglue
mvn clean install -DskipTests

# Then try any example
cd examples/minimal
mvn clean compile
mvn spring-boot:run
```

## Example Overview

### 1. Minimal - Your First HexaGlue Project

**Perfect for**: Getting started, understanding the basics

```
minimal/
├── domain/
│   ├── Task.java          # Aggregate Root
│   └── TaskId.java        # Identifier
└── ports/
    ├── in/TaskUseCases.java    # Driving Port
    └── out/TaskRepository.java # Driven Port
```

**Generated**: 4 files (Entity, Repository, Mapper, Adapter)

**Key Concepts**:
- Aggregate root with identity
- Driving vs Driven ports
- Basic JPA entity generation

---

### 2. Coffeeshop - Value Objects & Collections

**Perfect for**: Understanding value objects and embedded types

```
coffeeshop/
├── domain/order/
│   ├── Order.java         # Aggregate Root
│   ├── OrderId.java       # Identifier
│   ├── LineItem.java      # Value Object (embedded)
│   ├── Location.java      # Enum
│   └── OrderStatus.java   # Enum
└── ports/
    ├── in/OrderingCoffee.java
    └── out/Orders.java
```

**Generated**: 6 files (Entity, Embeddable, Repository, 2 Mappers, Adapter)

**Key Concepts**:
- Value objects as `@Embeddable`
- `@ElementCollection` for collections of value objects
- `@Enumerated(EnumType.STRING)` for enums
- MapStruct mappers with nested type support

---

### 3. E-Commerce - Rich Domain Model

**Perfect for**: Real-world architecture patterns

```
ecommerce/
├── domain/
│   ├── order/      # Order aggregate (Order, OrderLine, Money, Address...)
│   ├── customer/   # Customer aggregate (Customer, Email...)
│   └── product/    # Product aggregate (Product, Quantity...)
└── ports/
    ├── in/         # 3 driving ports (use cases)
    └── out/        # 4 driven ports (repositories + gateway)
```

**Generated**: 25 files (4 Entities, 4 Embeddables, 4 Repositories, 8 Mappers, 5 Adapters)

**Key Concepts**:
- Multiple aggregates with separate identities
- Inter-aggregate references by ID (not direct references)
- Entity relationships (`@OneToMany`, `@ManyToOne`)
- Non-repository driven ports (PaymentGateway)
- Complex value object hierarchies

### 4. Validation Demo - Classification Control

**Perfect for**: Understanding the validation phase and classification configuration

```
validation-demo/
├── hexaglue.yaml          # Classification configuration
├── application/
│   └── OrderApplicationService.java  # APPLICATION_SERVICE
├── domain/
│   ├── Order.java         # @AggregateRoot (EXPLICIT)
│   ├── OrderId.java       # VALUE_OBJECT (via hexaglue.yaml)
│   └── OrderLine.java     # @Entity (EXPLICIT)
├── port/
│   ├── OrderService.java  # DRIVING port
│   └── OrderRepository.java # DRIVEN port
└── util/
    └── StringUtils.java   # EXCLUDED via pattern
```

**Key Concepts**:
- `hexaglue:validate` goal for classification validation
- `classification.exclude` patterns
- `classification.explicit` mappings
- `failOnUnclassified` for CI/CD

---

## What Gets Generated

For each example, HexaGlue generates:

| Component | Description | Location |
|-----------|-------------|----------|
| JPA Entities | `@Entity` classes with proper mappings | `target/generated-sources/hexaglue/.../persistence/` |
| Embeddables | `@Embeddable` for value objects | Same as above |
| Repositories | Spring Data `JpaRepository` interfaces | Same as above |
| Mappers | MapStruct interfaces (Domain ↔ Entity) | Same as above |
| Adapters | `@Component` implementing driven ports | Same as above |
| Documentation | Architecture docs with diagrams | `target/generated-sources/generated-docs/` |

## Project Structure After Generation

```
example-app/
├── src/main/java/
│   └── com/example/
│       ├── domain/           # Your domain code (untouched)
│       └── ports/            # Your port interfaces (untouched)
├── target/generated-sources/
│   ├── hexaglue/             # Generated JPA infrastructure
│   │   └── com/example/infrastructure/persistence/
│   │       ├── *Entity.java
│   │       ├── *Embeddable.java
│   │       ├── *JpaRepository.java
│   │       ├── *Mapper.java
│   │       └── *Adapter.java
│   └── generated-docs/       # Generated documentation
│       └── docs/architecture/
│           ├── README.md
│           ├── domain.md
│           ├── ports.md
│           └── diagrams.md
└── pom.xml
```

## Running the Examples

Each example is a standalone Spring Boot application:

```bash
cd examples/<example-name>
mvn clean compile    # Generate code
mvn spring-boot:run  # Run application
```

> **Note**: Applications start and exit immediately (no web server). This confirms Spring context initializes correctly with generated infrastructure.

## Next Steps

1. Start with [minimal](./minimal/) to understand the basics
2. Progress to [coffeeshop](./coffeeshop/) for value objects
3. Explore [ecommerce](./ecommerce/) for real-world patterns
4. Create your own project using these examples as templates

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
