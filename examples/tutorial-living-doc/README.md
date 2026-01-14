# Tutorial: Living Documentation

This example demonstrates the HexaGlue **Living Documentation** plugin.

## What This Example Shows

- Generating architecture documentation from domain code
- Mermaid diagrams for visualization
- Multi-aggregate domain model documentation

## Domain Model

A simple e-commerce domain with two aggregates:

**Order Aggregate**
- `Order` - Aggregate root with order lines
- `OrderLine` - Entity within the Order aggregate
- `OrderId` - Typed identifier
- `OrderStatus` - Enum for order lifecycle

**Product Aggregate**
- `Product` - Aggregate root for catalog items
- `ProductId` - Typed identifier

**Shared Value Objects**
- `Money` - Monetary amounts with currency
- `Quantity` - Item quantities

**Ports**
- `OrderUseCases` - Driving port for order operations
- `ProductUseCases` - Driving port for product operations
- `OrderRepository` - Driven port for order persistence
- `ProductRepository` - Driven port for product persistence

## Running the Example

```bash
mvn compile
```

## Exploring Generated Documentation

After running `mvn compile`, explore the generated documentation:

```
target/hexaglue/living-doc/
├── README.md      # Architecture overview with metrics and diagram
├── domain.md      # Complete domain model documentation
├── ports.md       # Driving and driven ports documentation
└── diagrams.md    # Mermaid architecture diagrams
```

### View the Documentation

```bash
# Open the overview
open target/hexaglue/living-doc/README.md

# Or use a local Markdown server
npx markserv target/hexaglue/living-doc/
```

### What You'll See

**README.md** - Architecture overview with:
- Summary metrics (aggregates, entities, value objects, ports)
- Hexagonal architecture flow diagram
- Domain and ports summaries

**domain.md** - Domain model details:
- Aggregate Roots with properties
- Entities and their relationships
- Value Objects with their fields
- Identifiers

**ports.md** - Port documentation:
- Driving ports (use cases) with methods
- Driven ports (repositories) with methods

**diagrams.md** - Mermaid diagrams:
- Domain model class diagram
- Per-aggregate diagrams
- Ports flow diagram
- Dependencies diagram

## Configuration

This example uses the simplest configuration:

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
            <artifactId>hexaglue-plugin-living-doc</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</plugin>
```

## Related Documentation

- [Living Documentation Tutorial](../../docs/LIVING_DOCUMENTATION.md)
- [Configuration Reference](../../docs/CONFIGURATION.md)
