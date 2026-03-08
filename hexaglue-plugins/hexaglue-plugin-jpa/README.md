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

<!-- GENERATED:CONFIG:START -->
| Option | Default | Description |
|--------|---------|-------------|
| `entitySuffix` | `Entity` | suffix for generated JPA entity classes (default: "Entity") |
| `embeddableSuffix` | `Embeddable` | suffix for generated JPA embeddable classes (default: "Embeddable") |
| `repositorySuffix` | `JpaRepository` | suffix for Spring Data repository interfaces (default: "JpaRepository") |
| `adapterSuffix` | `Adapter` | suffix for port adapter classes (default: "Adapter") |
| `mapperSuffix` | `Mapper` | suffix for MapStruct mapper interfaces (default: "Mapper") |
| `tablePrefix` | `""` | prefix for database table names (default: "") |
| `idStrategy` | `ASSIGNED` | fallback identity generation strategy when domain fields lack |
| `targetModule` | `null` | target module for multi-module routing (null = no routing) |
| `enableAuditing` | `false` | true to add JPA auditing annotations (createdDate, lastModifiedDate) |
| `enableOptimisticLocking` | `false` | true to add |
| `generateRepositories` | `true` | true to generate Spring Data JPA repository interfaces |
| `generateMappers` | `true` | true to generate MapStruct mapper interfaces |
| `generateAdapters` | `true` | true to generate port adapter implementations |
| `generateEmbeddables` | `true` | true to generate JPA embeddable classes for value objects |
| `infrastructurePackage` | `basePackage + ".infrastructure.persistence` |  |
<!-- GENERATED:CONFIG:END -->

### Maven Parameters

These parameters are set in the `<configuration>` block of the Maven plugin and control the generation behavior:

<!-- GENERATED:MAVEN:START -->
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `basePackage` | string | (required) | The base package to analyze. Types outside this package are ignored |
| `failOnUnclassified` | boolean | `false` | Whether to fail the build if unclassified types remain. When enabled, the build will fail if any domain types cannot be classified with sufficient confidence |
| `outputDirectory` | string | `${project.build.directory}/generated-sources/hexaglue` | Output directory for generated sources |
| `skip` | boolean | `false` | Skip HexaGlue execution |
| `skipValidation` | boolean | `false` | Skip validation step before generation. When true, generation will proceed even if there are unclassified types |
| `tolerantResolution` | boolean | `false` | Enable tolerant type resolution for projects using annotation processors. When enabled, HexaGlue accepts unresolved types during analysis instead of failing |
<!-- GENERATED:MAVEN:END -->

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
