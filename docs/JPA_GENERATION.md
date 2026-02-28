# JPA Generation

Generate Spring Data JPA infrastructure code from your domain model. This tutorial covers entity generation, repositories, mappers, and adapters.

## What Gets Generated

For each aggregate root with a repository port, HexaGlue generates:

| File | Description |
|------|-------------|
| `*Entity.java` | JPA entity with proper annotations |
| `*JpaRepository.java` | Spring Data repository interface |
| `*Mapper.java` | MapStruct mapper (domain <-> entity) |
| `*Adapter.java` | Port implementation using repository + mapper |
| `*Embeddable.java` | Embeddable classes for value objects |

## Setup

### 1. Add the JPA Plugin

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
            <artifactId>hexaglue-plugin-jpa</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

### 2. Add Required Dependencies

```xml
<dependencies>
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.6.3</version>
    </dependency>
</dependencies>
```

### 3. Configure MapStruct Processor

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.6.3</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### 4. Generate

```bash
mvn compile
```

Generated files appear in `target/generated-sources/hexaglue/`.

## Domain to JPA Mapping

HexaGlue maps domain patterns to JPA annotations:

| Domain Pattern | JPA Mapping |
|----------------|-------------|
| Aggregate Root | `@Entity` + `@Table` |
| Entity | `@Entity` + `@Table` |
| Value Object | `@Embeddable` |
| Identifier (`OrderId`) | Unwrapped to raw type (`UUID`) |
| Composite Identity | `@EmbeddedId` |
| `List<Entity>` | `@OneToMany` |
| `List<ValueObject>` | `@ElementCollection` |
| Single Value Object | `@Embedded` |
| Enum | `@Enumerated(EnumType.STRING)` |

## Generated Code Examples

### Domain Model (Input)

```java
// Aggregate Root
public class Order {
    private final OrderId id;
    private String customerName;
    private Address shippingAddress;
    private List<OrderLine> lines;
    // ...
}

// Value Object
public record OrderId(UUID value) {}

// Value Object
public record Address(String street, String city, String zip) {}

// Entity
public class OrderLine {
    private String productId;
    private int quantity;
    private Money price;
}
```

### Generated Entity

```java
@Entity
@Table(name = "order")
public class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "customer_name")
    private String customerName;

    @Embedded
    private AddressEmbeddable shippingAddress;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineEntity> lines = new ArrayList<>();

    // Constructors, getters, setters...
}
```

### Generated Embeddable

```java
@Embeddable
public class AddressEmbeddable {
    private String street;
    private String city;
    private String zip;
    // ...
}
```

### Generated Repository

```java
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
}
```

### Generated Mapper

```java
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {AddressMapper.class, OrderLineMapper.class})
public interface OrderMapper {

    @Mapping(source = "id.value", target = "id")
    OrderEntity toEntity(Order domain);

    @Mapping(target = "id", expression = "java(new OrderId(entity.getId()))")
    Order toDomain(OrderEntity entity);
}
```

### Generated Adapter

```java
@Component
@Transactional
public class OrderAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Order save(Order order) {
        var entity = mapper.toEntity(order);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value())
                .map(mapper::toDomain);
    }
}
```

## Output Directory Structure

```
target/generated-sources/hexaglue/
└── com/example/infrastructure/
    ├── persistence/
    │   ├── OrderEntity.java
    │   ├── OrderLineEntity.java
    │   ├── AddressEmbeddable.java
    │   ├── OrderJpaRepository.java
    │   ├── OrderMapper.java
    │   └── AddressMapper.java
    └── adapters/
        └── OrderAdapter.java
```

## Configuration Options

Configure via `hexaglue.yaml`:

```yaml
plugins:
  io.hexaglue.plugin.jpa:
    entitySuffix: "Entity"           # Default
    repositorySuffix: "JpaRepository" # Default
    adapterSuffix: "Adapter"         # Default
    mapperSuffix: "Mapper"           # Default
    tablePrefix: "app_"              # Prefix for table names
    enableAuditing: true             # Add @CreatedDate, @LastModifiedDate
    enableOptimisticLocking: true    # Add @Version field
```

### Auditing Fields

When `enableAuditing: true`:

```java
@Entity
public class OrderEntity {
    // ...
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
```

### Optimistic Locking

When `enableOptimisticLocking: true`:

```java
@Entity
public class OrderEntity {
    // ...
    @Version
    private Long version;
}
```

## Best Practices

1. **Keep domain pure** - No JPA annotations in domain classes
2. **Use repository ports** - Define repository interfaces in your domain
3. **Let HexaGlue wire** - Generated adapters connect domain to infrastructure
4. **Add database** - Include H2 or PostgreSQL dependency for runtime

## What's Next?

- [Living Documentation](LIVING_DOCUMENTATION.md) - Generate architecture docs
- [Configuration](CONFIGURATION.md) - All plugin configuration options
- [Classification](CLASSIFICATION.md) - How types are mapped to JPA

**Example:** See [sample-value-objects](../examples/sample-value-objects/) for a complete JPA generation example.
