# E-Commerce Audit Sample

This is a sample e-commerce application designed to test the **HexaGlue DDD Audit Plugin**. The project intentionally contains architectural violations and DDD anti-patterns to demonstrate the audit capabilities.

## Project Structure

```
com.example.ecommerce/
├── domain/
│   ├── shared/          # Base classes (AggregateRoot, Entity, DomainEvent)
│   ├── order/           # Order bounded context
│   ├── customer/        # Customer bounded context
│   ├── product/         # Product bounded context
│   └── inventory/       # Inventory bounded context
├── application/
│   ├── port/
│   │   ├── driving/     # Use case interfaces
│   │   └── driven/      # Repository and gateway interfaces
│   └── service/         # Application services
└── infrastructure/
    ├── persistence/     # Repository implementations
    ├── payment/         # Payment gateway implementation
    ├── notification/    # (Missing - intentional violation)
    └── web/             # Controllers
```

## Intentional Violations for Audit Testing

### DDD Violations

| Violation | Location | Description |
|-----------|----------|-------------|
| `ddd:entity-identity` | `OrderLine.java` | Entity without proper identity (getId() returns null) |
| `ddd:value-object-immutable` | `Money.java` | Value object with setter method |
| `ddd:event-naming` | `OrderPlaceEvent.java`, `OrderCancelEvent.java`, `ProductAddEvent.java` | Events not named in past tense |
| `ddd:domain-purity` | `Email.java` | Domain value object with Jakarta Validation annotations |
| `ddd:aggregate-cycle` | `Order.java`, `InventoryItem.java` | Bidirectional aggregate references |
| `ddd:aggregate-repository` | `Product.java` | Aggregate without corresponding repository port |

### Hexagonal Architecture Violations

| Violation | Location | Description |
|-----------|----------|-------------|
| `hex:port-interface` | `InventoryPort.java` | Port defined as concrete class instead of interface |
| `hex:port-coverage` | `NotificationService.java` | Port without adapter implementation |
| `hex:dependency-inversion` | `OrderApplicationService.java` | Application service depending on concrete `JpaOrderRepository` |
| `hex:dependency-direction` | `OrderApplicationService.java` | Import from infrastructure layer in application service |

### Expected Audit Results

The audit should detect:
- **3** event naming violations
- **1** entity identity violation
- **1** value object immutability violation
- **1** domain purity violation
- **2** aggregate cycle violations (bidirectional)
- **1** port interface violation
- **1** port coverage violation
- **1** dependency inversion violation

## Running the Audit

```bash
cd hexaglue/examples/ecommerce-audit-sample
mvn compile hexaglue:audit
```

## Bounded Contexts

1. **Order Context**: Orders, order lines, money values
2. **Customer Context**: Customers, addresses, emails
3. **Product Context**: Products, categories
4. **Inventory Context**: Inventory items, stock levels

## Architecture Overview

- **4 Aggregate Roots**: Order, Customer, Product, InventoryItem
- **5 Entities**: OrderLine, ProductCategory (+ aggregates)
- **7 Value Objects**: OrderId, CustomerId, ProductId, InventoryItemId, Money, Address, Email, StockLevel, OrderStatus
- **6 Domain Events**: OrderPlaceEvent, OrderCancelEvent, OrderShippedEvent, CustomerCreatedEvent, CustomerAddressChangedEvent, ProductAddEvent, ProductPriceChangedEvent
- **3 Driving Ports**: OrderUseCase, CustomerUseCase, InventoryPort
- **5 Driven Ports**: OrderRepository, CustomerRepository, InventoryRepository, PaymentGateway, NotificationService
- **5 Adapters**: JpaOrderRepository, JpaCustomerRepository, InMemoryInventoryRepository, StripePaymentGateway, (missing NotificationAdapter)

## Expected Health Score

Due to the intentional violations, the health score should be **below 70%**.
