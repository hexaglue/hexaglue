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
| `embeddableSuffix` | `Embeddable` | Suffix for `@Embeddable` value object classes |
| `generateEmbeddables` | `true` | Generate `@Embeddable` classes for value objects |
| `infrastructurePackage` | `{basePackage}.infrastructure.persistence` | Target package for generated JPA artifacts |
| `outputDirectory` | (global default) | Per-plugin override for the output directory. Set to `src/main/java` to write JPA artifacts directly into your source tree instead of `target/`. |
| `overwrite` | `always` | Controls overwriting of existing files: `always` (overwrite unconditionally), `if-unchanged` (overwrite only if the file was not manually edited), `never` (never overwrite, for one-time scaffolding) |
| `targetModule` | `null` | Multi-module: target module ID for routing generated artifacts |

### Maven Parameters

These parameters are set in the `<configuration>` block of the Maven plugin and control the generation behavior:

| Parameter | Type | Default | Property | Description |
|-----------|------|---------|----------|-------------|
| `basePackage` | string | (required) | `hexaglue.basePackage` | Base package to analyze. JPA artifacts are generated from domain types in this package. |
| `outputDirectory` | string | `target/hexaglue/generated-sources` | `hexaglue.outputDirectory` | Directory where JPA entities, repositories, and mappers are written |
| `skip` | boolean | `false` | `hexaglue.skip` | Skip HexaGlue execution entirely |
| `skipValidation` | boolean | `false` | `hexaglue.skipValidation` | Skip classification validation before generation |
| `staleFilePolicy` | WARN / DELETE / FAIL | `WARN` | `hexaglue.staleFilePolicy` | How to handle previously generated files no longer needed |
| `failOnUnclassified` | boolean | `false` | `hexaglue.failOnUnclassified` | Fail the build if domain types cannot be classified |

### YAML Configuration

```yaml
plugins:
  io.hexaglue.plugin.jpa:
    entitySuffix: "Entity"
    embeddableSuffix: "Embeddable"
    repositorySuffix: "JpaRepository"
    adapterSuffix: "Adapter"
    mapperSuffix: "Mapper"
    tablePrefix: "app_"
    enableAuditing: true
    enableOptimisticLocking: true
    generateRepositories: true
    generateMappers: true
    generateAdapters: true
    generateEmbeddables: true
    outputDirectory: "src/main/java"    # Write to source tree instead of target/
    overwrite: "if-unchanged"           # Protect manually edited files
```

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
    <version>1.6.3</version>
</dependency>

<!-- MapStruct Processor (for annotation processing) -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.6.3</version>
    <scope>provided</scope>
</dependency>
```

## Plugin ID

`io.hexaglue.plugin.jpa`

## Version History

### v5.0.0 (2026-01-28)

- Multi-module support with `targetModule` routing via `ModuleIndex`
- Automatic routing to `INFRASTRUCTURE` role modules
- Enhanced embeddable generation for nested value objects
- MapStruct dependency updated to 1.6.3

### v4.1.0 (2026-01-20)

- Migrated to use `model.registry().all(Type.class)` pattern
- Added support for enriched `DomainEntity` with `identityField()` access
- Improved type structure detection with `FieldRole` support

### v4.0.0 (2026-01-16)

- Initial release with `ArchitecturalModel` support
- Full JPA entity generation from domain model
- Spring Data repository and MapStruct mapper generation

---

**HexaGlue - Focus on business code, not infrastructure glue.**
