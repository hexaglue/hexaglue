# HexaGlue

***Focus on business code, not infrastructure glue.***

<div align="center">

  <img src="docs/assets/logo-hexaglue.png" alt="HexaGlue" width="400">

[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![Maven 3.8+](https://img.shields.io/badge/Maven-3.8%2B-blue.svg)](https://maven.apache.org/)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

</div>

**HexaGlue automatically generates the infrastructure code around your hexagonal architecture, so you can focus on what matters: your business logic.**



## What is HexaGlue?

HexaGlue is a compile-time tool that analyzes your domain code and generates all the infrastructure adapters you need. Write your ports once, and let HexaGlue create the database repositories, REST controllers, message handlers, and more.

```text
┌─────────────────┐                    ┌─────────────────────────────────┐
│  Domain Code    │                    │  Complete Application           │
│                 │     HexaGlue       │                                 │
│  • Ports        │  ──────────────>   │  • Domain Code (unchanged)      │
│  • Use Cases    │    mvn compile     │  • REST Controllers (generated) │
│  • Entities     │                    │  • Repositories (generated)     │
│                 │                    │  • Message Handlers (generated) │
└─────────────────┘                    └─────────────────────────────────┘

 [You write THIS]                           [HexaGlue generates THAT]
```

## How It Works

HexaGlue works in three simple steps during your build:

1. **Analyze** - Scans your domain code and identifies ports (interfaces your business needs)
2. **Delegate** - Passes the domain model to specialized plugins via a stable SPI
3. **Generate** - Creates infrastructure adapters (REST, database, messaging, etc.) automatically

## Why Use HexaGlue?

✅ **Pure domain code** - Your business logic stays clean, with **zero** infrastructure dependencies

✅ **No boilerplate** - Write a port interface once, get production-ready adapters in **seconds**

✅ **Type-safe** - Generation happens at **compile** time with full type checking

✅ **Flexible infrastructure** - Swap technologies (REST to GraphQL, MySQL to MongoDB) by changing **plugins**, not code

✅ **Extensible** - Add support for **any** technology through the plugin system

---

## Quick Start

### 1. Add the HexaGlue Maven Plugin

```xml
<build>
    <plugins>
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
            <dependencies>
                <!-- Add the plugins you need -->
                <dependency>
                    <groupId>io.hexaglue.plugins</groupId>
                    <artifactId>hexaglue-plugin-jpa</artifactId>
                    <version>${hexaglue.plugins.version}</version>
                    </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

### 2. Write Your Domain

```java
// Domain model - pure business logic
package com.example.domain;

public class Order {
    private OrderId id;
    private String customerName;
    private List<LineItem> items;
    private Money total;
}

public record OrderId(UUID value) {}

public record Money(BigDecimal amount, Currency currency) {}

...
```

### 3. Define Your Ports

```java
// Driven port - what your business needs from infrastructure
package com.example.ports.out;

public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    Order save(Order order);
    void delete(OrderId id);
}
```

### 4. Generate

```bash
mvn compile
```

HexaGlue generates the infrastructure code in `target/generated-sources/`:

```
target/generated-sources/hexaglue/
├── OrderEntity.java           # JPA entity
├── OrderJpaRepository.java    # Spring Data repository
├── OrderMapper.java           # MapStruct mapper
└── OrderAdapter.java          # Port implementation
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [Getting Started](docs/GETTING_STARTED.md) | Progressive tutorial from simple to advanced |
| [User Guide](docs/USER_GUIDE.md) | Complete concepts and features reference |
| [Configuration](docs/CONFIGURATION.md) | `hexaglue.yaml` reference |
| [SPI Reference](docs/SPI_REFERENCE.md) | Plugin development API |
| [Plugin Development](docs/PLUGIN_DEVELOPMENT.md) | Create your own plugins |

---

## Plugins

HexaGlue's plugin architecture allows generating any type of infrastructure code. Plugins are specialized for different technologies:

### Official Plugins

| Plugin | Description | Status |
|--------|-------------|--------|
| **Living Documentation** | Generates Markdown documentation of your architecture | ✅ Available |
| **JPA Repository** | Spring Data JPA entities, repositories, mappers, adapters | ✅ Available |
| **REST API** | Spring MVC controllers from driving ports | 📅 Planned |
| **OpenAPI** | OpenAPI specification from ports | 📅 Planned |
| **Kafka** | Kafka producers and consumers | 📅 Planned |
| **GraphQL** | GraphQL schema and resolvers | 📅 Planned |

---

## Optional jMolecules Integration

HexaGlue can also leverage **jMolecules annotations** to better understand your enriched domain model:

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

**jMolecules is not required**: HexaGlue discovers domain concepts automatically through smart heuristics. When present, annotations simply make domain intent explicit.

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

### Domain Events (📅 Planned )

| Annotation | Purpose |
|------------|---------|
| `@DomainEvent` | Marks a domain event |
| `@Externalized` | Marks an event for external publication |


---

## Project Structure

```
hexaglue/
├── hexaglue-maven-plugin/ # Maven plugin wrapper for core library
├── hexaglue-core/         # Core library: analyze and classify
├── hexaglue-spi/          # Stable API for plugins (JDK-only)
├── hexaglue-testing/      # Test utilities
├── hexaglue-plugins/      # Official code generation plugins
│   ├── hexaglue-plugin-jpa/
│   └── hexaglue-plugin-living-doc/
├── examples/              # Working examples
└── docs/                  # Documentation
```

---

## Example Applications

| Example | Description |
|---------|-------------|
| [minimal](examples/minimal/) | Very simple example |
| [coffeeshop](examples/coffeeshop/) | Coffee ordering application |
| [ecommerce](examples/ecommerce/) | Rich domain with relationships |

---

## Prerequisites

- **Java**: 17 or later
- **Maven**: 3.8 or later

---

## License

HexaGlue is distributed under the **Mozilla Public License 2.0 (MPL-2.0)**.

- ✅ May be used in commercial and proprietary products
- ✅ Your application code remains your own
- ✅ Generated code belongs to you without restriction
- ⚠️ Modifications to HexaGlue source files must be shared under MPL-2.0

[Learn more about MPL-2.0](https://www.mozilla.org/MPL/2.0/)

---

## Support

- [GitHub Issues](https://github.com/hexaglue/hexaglue/issues): Report bugs or request features
- [GitHub Discussions](https://github.com/hexaglue/hexaglue/discussions): Ask questions and share ideas

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
