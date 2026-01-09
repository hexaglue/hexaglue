# Medium Test Corpus (~200 types)

## Description

Extended e-commerce Order Management System with multiple bounded contexts.

## Domain Structure

### Core Domains

1. **Order Management** (from small corpus)
   - Aggregates: Order, Customer
   - Value Objects: OrderId, CustomerId, Address, Email, etc.

2. **Product Catalog**
   - Aggregates: Product
   - Value Objects: ProductId, SKU, Category, Dimensions, Weight

3. **Inventory Management**
   - Aggregates: Inventory
   - Value Objects: InventoryId, Quantity
   - Entities: InventoryTransaction

4. **Payment** (to be added)
   - Aggregates: Payment
   - Value Objects: PaymentId, PaymentMethod, CardDetails

5. **Shipping** (to be added)
   - Aggregates: Shipment
   - Value Objects: ShipmentId, TrackingNumber, Carrier

6. **Reviews & Ratings** (to be added)
   - Aggregates: ProductReview
   - Value Objects: Rating, ReviewId

## Ports

### Driving Ports (Primary)
- OrderService
- CustomerService
- ProductService
- InventoryService
- PaymentService
- ShippingService

### Driven Ports (Secondary)
- OrderRepository
- CustomerRepository
- ProductRepository
- InventoryRepository
- PaymentGateway
- ShippingProvider
- NotificationService
- EventPublisher

## Target: ~200 Types

- Aggregates: ~10
- Entities: ~20
- Value Objects: ~80
- Ports: ~20
- Use Cases: ~30
- Domain Services: ~10
- Exceptions: ~15
- Validators: ~10
- Specifications: ~5
- Events: ~10

## Build

```bash
cd test-corpus/medium
mvn clean compile
```
