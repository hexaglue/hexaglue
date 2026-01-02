# Coffee Shop Example

A complete example demonstrating HexaGlue's code generation capabilities with a simple coffee shop domain.

## Domain Overview

This example models a coffee shop ordering system using Hexagonal Architecture (Ports & Adapters):

```
com.coffeeshop/
├── domain/order/          # Domain layer
│   ├── Order.java         # Aggregate Root
│   ├── OrderId.java       # Identifier (Value Object)
│   ├── LineItem.java      # Value Object
│   ├── Location.java      # Enum
│   └── OrderStatus.java   # Enum
├── ports/in/              # Driving (Primary) Ports
│   └── OrderingCoffee.java
└── ports/out/             # Driven (Secondary) Ports
    └── Orders.java        # Repository interface
```

## Domain Model

### Order (Aggregate Root)

The `Order` aggregate manages the order lifecycle:

- **Identity**: `OrderId` (wraps UUID)
- **Properties**: customerName, location, items, createdAt, status
- **Behavior**: addItem(), submit(), complete(), cancel(), totalAmount()

### LineItem (Value Object)

Represents a product in the order:
- productName, quantity, unitPrice
- Calculated: totalPrice()

## HexaGlue Configuration

The project uses two HexaGlue plugins:

### Living Documentation Plugin

Generates architecture documentation in `target/generated-sources/generated-docs/`:
- `docs/architecture/README.md` - Overview with Mermaid diagram
- `docs/architecture/domain.md` - Domain model documentation
- `docs/architecture/ports.md` - Ports documentation
- `docs/architecture/diagrams.md` - Class and flow diagrams

### JPA Plugin

Generates JPA infrastructure in `target/generated-sources/hexaglue/`:
- `OrderEntity.java` - JPA entity with `@Entity`, `@Table`, `@Id`
- `OrderJpaRepository.java` - Spring Data repository
- `OrderMapper.java` - MapStruct mapper (Domain ↔ Entity)
- `OrderAdapter.java` - Repository port implementation
- `LineItemEmbeddable.java` - JPA embeddable for value object
- `LineItemMapper.java` - MapStruct mapper for LineItem

## Building

```bash
# Generate code and compile
mvn clean compile

# View generated files
ls target/generated-sources/hexaglue/
ls target/generated-sources/generated-docs/
```

## Generated Code Example

### OrderEntity.java

```java
@Entity
@Table(name = "order")
public class OrderEntity {
    @Id
    private UUID id;
    private String customerName;
    private Location location;

    @ElementCollection
    @CollectionTable(name = "items")
    private List<LineItemEmbeddable> items;
    // ...
}
```

### OrderAdapter.java

```java
@Component
@Transactional
public class OrderAdapter implements Orders {
    private final OrderJpaRepository orderJpaRepository;
    private final OrderMapper orderMapper;

    @Override
    public Order save(Order order) {
        var entity = orderMapper.toEntity(order);
        var saved = orderJpaRepository.save(entity);
        return orderMapper.toDomain(saved);
    }
    // ...
}
```

## Configuration Options

You can customize generation by adding a `hexaglue.yaml` file:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      entitySuffix: Entity
      repositorySuffix: JpaRepository
      enableAuditing: true
      enableOptimisticLocking: true
    io.hexaglue.plugin.livingdoc:
      outputDir: docs/architecture/
      generateDiagrams: true
      diagramFormat: mermaid
```

## Project Structure After Generation

```
coffeeshop/
├── src/main/java/
│   └── com/coffeeshop/          # Your domain code
├── target/generated-sources/
│   ├── hexaglue/                # Generated JPA infrastructure
│   │   └── com/coffeeshop/.../persistence/
│   │       ├── OrderEntity.java
│   │       ├── OrderJpaRepository.java
│   │       ├── OrderMapper.java
│   │       ├── OrderAdapter.java
│   │       ├── LineItemEmbeddable.java
│   │       └── LineItemMapper.java
│   └── generated-docs/          # Generated documentation
│       └── docs/architecture/
│           ├── README.md
│           ├── domain.md
│           ├── ports.md
│           └── diagrams.md
└── pom.xml
```

## Learn More

- [HexaGlue Documentation](../../docs/)
- [JPA Plugin Reference](../../hexaglue-plugins/hexaglue-plugin-jpa/)
- [Living Doc Plugin Reference](../../hexaglue-plugins/hexaglue-plugin-living-doc/)
