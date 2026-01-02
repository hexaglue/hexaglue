# E-Commerce Example

A rich domain example demonstrating all HexaGlue capabilities with a complete e-commerce domain.

## Domain Model

### Aggregate Roots

- **Order** - Purchase order with line items and lifecycle management
- **Customer** - Customer profile with billing address
- **Product** - Product catalog with stock management

### Entities

- **OrderLine** - Line item within an order (quantity, price snapshot)

### Value Objects

- **Money** - Monetary amount with currency
- **Quantity** - Positive quantity validation
- **Email** - Email address with validation
- **Address** - Shipping and billing addresses
- **OrderStatus** - Order lifecycle states

### Identifiers

- **OrderId** - Order aggregate identifier (UUID)
- **CustomerId** - Customer aggregate identifier (UUID)
- **ProductId** - Product aggregate identifier (UUID)

## Ports

### Driving Ports (ports/in)

Primary ports for application use cases:

- **OrderingProducts** - Order creation and lifecycle management
- **ManagingCustomers** - Customer registration and profile management
- **ManagingProducts** - Product catalog and stock management

### Driven Ports (ports/out)

Secondary ports for infrastructure:

- **OrderRepository** - Order persistence
- **CustomerRepository** - Customer persistence
- **ProductRepository** - Product persistence
- **PaymentGateway** - External payment processing

## Structure

```
src/main/java/com/ecommerce/
├── domain/
│   ├── order/
│   │   ├── Order.java          # Aggregate root
│   │   ├── OrderId.java        # Identifier
│   │   ├── OrderLine.java      # Entity
│   │   ├── OrderStatus.java    # Value object (enum)
│   │   ├── Money.java          # Value object
│   │   ├── Quantity.java       # Value object
│   │   └── Address.java        # Value object
│   ├── customer/
│   │   ├── Customer.java       # Aggregate root
│   │   ├── CustomerId.java     # Identifier
│   │   └── Email.java          # Value object
│   └── product/
│       ├── Product.java        # Aggregate root
│       └── ProductId.java      # Identifier
└── ports/
    ├── in/
    │   ├── OrderingProducts.java
    │   ├── ManagingCustomers.java
    │   └── ManagingProducts.java
    └── out/
        ├── OrderRepository.java
        ├── CustomerRepository.java
        ├── ProductRepository.java
        └── PaymentGateway.java
```

## HexaGlue Configuration

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      basePackage: com.ecommerce.infrastructure.persistence
      idStrategy: ASSIGNED
      enableAuditing: true
      entitySuffix: Entity
      repositorySuffix: JpaRepository

    io.hexaglue.plugin.portdocs:
      outputDir: docs/ports/
      mergeMode: OVERWRITE
```

## Generated Code

HexaGlue will generate:

### JPA Entities

```java
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private UUID id;

    private UUID customerId;

    @Embedded
    private AddressEmbeddable shippingAddress;

    @ElementCollection
    @CollectionTable(name = "order_lines")
    private List<OrderLineEmbeddable> lines;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Version
    private Long version;
    // ...
}
```

### Spring Data Repositories

```java
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByCustomerId(UUID customerId);
}
```

### MapStruct Mappers

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(source = "id.value", target = "id")
    OrderEntity toEntity(Order domain);

    @Mapping(target = "id", expression = "java(new OrderId(entity.getId()))")
    Order toDomain(OrderEntity entity);
}
```

### Port Adapters

```java
@Component
@Transactional
public class OrderRepositoryAdapter implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain);
    }
    // ...
}
```

## Building

```bash
cd examples/ecommerce
mvn clean compile
```

Generated sources will be in `target/generated-sources/annotations/`.

## Key Patterns Demonstrated

1. **Rich Domain Model** - Aggregates with business logic and invariants
2. **Value Objects** - Immutable types with validation (Money, Email, Quantity)
3. **Identity Wrappers** - Type-safe identifiers (OrderId, CustomerId, ProductId)
4. **Hexagonal Ports** - Clean separation of driving (use cases) and driven (infrastructure) ports
5. **External Gateway** - PaymentGateway demonstrates non-repository driven ports
6. **Aggregate Relationships** - Order references Customer and Product by ID (not direct reference)
