# HexaGlue JPA Plugin

Generates Spring Data JPA infrastructure code from your domain model and repository ports.

## Features

- **JPA Entities** - Entity classes with proper annotations (`@Entity`, `@Table`, `@Id`, etc.)
- **Embeddables** - `@Embeddable` classes for value objects
- **Spring Data Repositories** - `JpaRepository` interfaces with custom query methods
- **MapStruct Mappers** - Bidirectional domain-to-entity mapping interfaces
- **Port Adapters** - `@Component` implementations of your repository ports
- **Auditing Support** - Optional `@CreatedDate` and `@LastModifiedDate` fields
- **Optimistic Locking** - Optional `@Version` field for concurrency control

## Installation

Add the plugin as a dependency to the HexaGlue Maven plugin:

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

## Generated Files

For each aggregate root with a repository port, the plugin generates:

```
target/generated-sources/hexaglue/
└── com/example/infrastructure/jpa/
    ├── OrderEntity.java           # JPA entity
    ├── OrderJpaRepository.java    # Spring Data repository interface
    ├── OrderMapper.java           # MapStruct mapper
    ├── OrderAdapter.java          # Port adapter implementation
    ├── AddressEmbeddable.java     # Embeddable for value objects
    └── AddressMapper.java         # Value object mapper
```

## Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `entitySuffix` | `Entity` | Suffix for generated JPA entity classes |
| `repositorySuffix` | `JpaRepository` | Suffix for Spring Data repository interfaces |
| `adapterSuffix` | `Adapter` | Suffix for port adapter classes |
| `mapperSuffix` | `Mapper` | Suffix for MapStruct mapper interfaces |
| `tablePrefix` | `""` | Prefix for database table names |
| `enableAuditing` | `false` | Add `@CreatedDate` and `@LastModifiedDate` fields |
| `enableOptimisticLocking` | `false` | Add `@Version` field for optimistic locking |
| `generateRepositories` | `true` | Generate Spring Data JPA repository interfaces |
| `generateMappers` | `true` | Generate MapStruct mapper interfaces |
| `generateAdapters` | `true` | Generate port adapter implementations |

## Sample Output

### JPA Entity (OrderEntity.java)

```java
@Generated("io.hexaglue.plugin.jpa")
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

### Spring Data Repository (OrderJpaRepository.java)

```java
@Generated("io.hexaglue.plugin.jpa")
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByCustomerName(String customerName);
}
```

### MapStruct Mapper (OrderMapper.java)

```java
@Generated("io.hexaglue.plugin.jpa")
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {AddressMapper.class, OrderLineMapper.class})
public interface OrderMapper {

    @Mapping(source = "id.value", target = "id")
    OrderEntity toEntity(Order domain);

    @Mapping(target = "id", expression = "java(new OrderId(entity.getId()))")
    Order toDomain(OrderEntity entity);

    List<OrderEntity> toEntityList(List<Order> domains);
    List<Order> toDomainList(List<OrderEntity> entities);
}
```

### Port Adapter (OrderAdapter.java)

```java
@Generated("io.hexaglue.plugin.jpa")
@Component
@Transactional
public class OrderAdapter implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderMapper orderMapper;

    public OrderAdapter(OrderJpaRepository orderJpaRepository, OrderMapper orderMapper) {
        this.orderJpaRepository = orderJpaRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    public Order save(Order order) {
        var entity = orderMapper.toEntity(order);
        var saved = orderJpaRepository.save(entity);
        return orderMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return orderJpaRepository.findById(id.value())
                .map(orderMapper::toDomain);
    }

    @Override
    public List<Order> findAll() {
        return orderMapper.toDomainList(orderJpaRepository.findAll());
    }

    @Override
    public void delete(Order order) {
        var entity = orderMapper.toEntity(order);
        orderJpaRepository.delete(entity);
    }
}
```

## Domain Mapping

The plugin automatically handles these domain patterns:

| Domain Pattern | JPA Mapping |
|----------------|-------------|
| Aggregate Root | `@Entity` with `@Table` |
| Entity | `@Entity` with `@Table` |
| Value Object | `@Embeddable` |
| Wrapped Identity (`OrderId`) | Unwrapped to raw type (`UUID`) |
| Composite Identity | `@EmbeddedId` |
| `List<Entity>` | `@OneToMany` |
| `List<ValueObject>` | `@ElementCollection` |
| Single Value Object | `@Embedded` |
| Aggregate Reference | Raw ID type (no `@ManyToOne`) |
| Enum | `@Enumerated(EnumType.STRING)` |
| `BigDecimal` (in Money) | `@Column(precision = 19, scale = 2)` |
| `byte[]` | `@Lob` |

## Required Dependencies

Add these dependencies to your project:

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

<!-- MapStruct Processor (for annotation processing) -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
```

## Plugin ID

`io.hexaglue.plugin.jpa`

---

**HexaGlue - Focus on business code, not infrastructure glue.**
