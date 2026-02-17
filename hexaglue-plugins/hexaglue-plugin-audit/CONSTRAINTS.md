# Architectural Constraints Reference

This document provides detailed information about each constraint validated by the DDD Audit Plugin.

## Table of Contents

- [DDD Constraints](#ddd-constraints)
  - [Entity Identity](#entity-identity)
  - [Aggregate Repository](#aggregate-repository)
  - [Value Object Immutability](#value-object-immutability)
  - [Aggregate Cycles](#aggregate-cycles)
  - [Aggregate Consistency](#aggregate-consistency)
  - [Domain Purity](#domain-purity)
  - [Event Naming](#event-naming)
- [Hexagonal Architecture Constraints](#hexagonal-architecture-constraints)
  - [Port Interface](#port-interface)
  - [Port Coverage](#port-coverage)
  - [Dependency Direction](#dependency-direction)
  - [Layer Isolation](#layer-isolation)

---

## DDD Constraints

### Entity Identity

**Constraint ID**: `ddd:entity-identity`
**Default Severity**: CRITICAL
**Category**: Domain-Driven Design

#### Description

Entities are defined by their identity, not their attributes. Every entity (including aggregate roots) must have a stable identity field that distinguishes it from other instances throughout its lifecycle.

#### Rationale

Without identity:
- You cannot track entity state changes
- Equality comparisons become unreliable
- Persistence and retrieval operations fail
- References between entities become ambiguous

#### What is Validated

The validator checks that:
- Each entity has at least one field marked as identity
- Identity is indicated by annotations like `@Id`, `@EmbeddedId`, or framework-specific markers
- Aggregate roots (special entities) also have identity

#### Examples

**PASS - Entity with ID field**
```java
@Entity
public class Customer {
    @Id
    private Long id;  // Clear identity field

    private String name;
    private String email;
}
```

**PASS - Composite identity**
```java
@Entity
public class OrderLine {
    @EmbeddedId
    private OrderLineId id;  // Composite key

    private int quantity;
    private Money unitPrice;
}
```

**FAIL - No identity field**
```java
@Entity
public class Product {
    // Missing @Id annotation!
    private String sku;
    private String name;
    private BigDecimal price;
}
```

#### How to Fix

1. Add an `@Id` annotation to your identity field
2. For natural keys, use `@EmbeddedId` with a composite key class
3. Ensure every entity and aggregate root has identity

---

### Aggregate Repository

**Constraint ID**: `ddd:aggregate-repository`
**Default Severity**: MAJOR
**Category**: Domain-Driven Design

#### Description

Aggregates are the unit of retrieval in DDD. Each aggregate root should have a dedicated repository interface for persistence and retrieval operations. Repositories provide the abstraction between the domain model and data storage.

#### Rationale

Without repositories:
- Persistence logic leaks into domain model
- Testing becomes difficult (can't mock persistence)
- Aggregate boundaries become unclear
- Direct database access violates separation of concerns

#### What is Validated

The validator checks that:
- Each aggregate root has a corresponding repository interface
- Repository naming follows convention: `{AggregateName}Repository`
- Repository is an interface (not a concrete class)

#### Examples

**PASS - Aggregate with repository**
```java
// Aggregate Root
@AggregateRoot
public class Order {
    @Id
    private OrderId id;
    // ... domain logic
}

// Repository Interface
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
    void delete(Order order);
}
```

**FAIL - Aggregate without repository**
```java
// Aggregate Root
@AggregateRoot
public class Customer {
    @Id
    private CustomerId id;
    // ... but no CustomerRepository interface exists!
}
```

**FAIL - Repository is a class, not interface**
```java
// WRONG: Repository should be an interface
public class ProductRepository {
    public void save(Product product) { /* ... */ }
}
```

#### How to Fix

1. Create a repository interface for each aggregate root
2. Name it `{AggregateName}Repository`
3. Define methods for persistence operations
4. Implement the interface in the infrastructure layer

---

### Value Object Immutability

**Constraint ID**: `ddd:value-object-immutable`
**Default Severity**: CRITICAL
**Category**: Domain-Driven Design

#### Description

Value Objects represent descriptive aspects with no conceptual identity. They should be immutable - once created, their state cannot change. This ensures thread-safety, enables safe sharing, and provides proper value semantics.

#### Rationale

Mutable value objects cause:
- Unexpected side effects when shared
- Thread-safety issues in concurrent systems
- Violation of value semantics (equality based on state)
- Difficulty in reasoning about code behavior

#### What is Validated

The validator checks that:
- Value objects have no setter methods
- No methods that mutate internal state
- Fields should be final (though not enforced by this validator)

#### Examples

**PASS - Immutable value object**
```java
@ValueObject
public class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    // Only getters, no setters
    public BigDecimal amount() { return amount; }
    public Currency currency() { return currency; }

    // Operations return new instances
    public Money add(Money other) {
        // validate same currency
        return new Money(amount.add(other.amount), currency);
    }
}
```

**PASS - Record-based value object (Java 14+)**
```java
@ValueObject
public record Address(String street, String city, String zipCode) {
    // Records are immutable by default
}
```

**FAIL - Mutable value object**
```java
@ValueObject
public class EmailAddress {
    private String value;

    // WRONG: Setter violates immutability!
    public void setValue(String value) {
        this.value = value;
    }
}
```

#### How to Fix

1. Remove all setter methods from value objects
2. Make fields final
3. Return new instances instead of mutating state
4. Consider using Java records for immutable value objects

---

### Aggregate Cycles

**Constraint ID**: `ddd:aggregate-cycle`
**Default Severity**: BLOCKER
**Category**: Domain-Driven Design

#### Description

Aggregates are consistency boundaries and must be independent units. Circular dependencies between aggregate roots indicate poor boundary design and violate the fundamental principle that aggregates should be independently modifiable and persistable.

#### Rationale

Circular aggregate dependencies cause:
- Difficulty in understanding the domain model
- Problems with transaction boundaries
- Challenges in implementing eventual consistency
- Tight coupling that prevents independent evolution
- Potential database deadlocks during concurrent modifications

#### What is Validated

The validator uses graph cycle detection to find all cycles in the aggregate dependency graph. Each aggregate root is a node, and edges represent direct dependencies (through references or relations).

#### Examples

**PASS - Acyclic aggregate dependencies**
```java
@AggregateRoot
public class Order {
    private CustomerId customerId;  // Reference by ID only
    private List<OrderLine> lines;
}

@AggregateRoot
public class Customer {
    private CustomerId id;
    // No reference back to Order
}
```

**FAIL - Circular dependency**
```java
@AggregateRoot
public class Order {
    private Customer customer;  // Direct reference
}

@AggregateRoot
public class Customer {
    private List<Order> orders;  // Circular reference!
}
```

**FAIL - Three-way cycle**
```java
Order → Product → Supplier → Order
```

#### How to Fix

1. **Reference by ID**: Store only the ID, not the full object reference
```java
private CustomerId customerId;  // Not: private Customer customer;
```

2. **Use Domain Events**: Communicate between aggregates via events instead of direct references

3. **Refactor Boundaries**: If aggregates are tightly coupled, reconsider the aggregate boundaries. Perhaps they should be one aggregate?

4. **Query Pattern**: Load related aggregates separately via repository queries

---

### Aggregate Consistency

**Constraint ID**: `ddd:aggregate-consistency`
**Default Severity**: MAJOR
**Category**: Domain-Driven Design

#### Description

Aggregates must maintain proper boundaries to protect invariants. This validator checks three essential rules:

1. **Single Ownership**: An entity should belong to only ONE aggregate
2. **Size Limit**: Aggregates should not exceed 7 entities (configurable)
3. **Boundary Respect**: External types should not reference internal entities directly

#### Rationale

Poor aggregate boundaries cause:
- Unclear ownership and consistency responsibilities
- Performance issues with large aggregates
- Invariant violations through direct entity access
- Difficulty in distributing transactions
- Concurrency conflicts

#### What is Validated

**Rule 1: Single Ownership**
Checks that each entity is referenced by only one aggregate root. Multiple ownership indicates unclear boundaries.

**Rule 2: Size Limit**
Counts entities within each aggregate. Large aggregates (>7 entities) are harder to maintain and can cause performance issues.

**Rule 3: Boundary Respect**
Ensures external types (from other layers or aggregates) don't bypass the aggregate root to access internal entities.

#### Examples

**PASS - Well-bounded aggregate**
```java
@AggregateRoot
public class Order {
    @Id
    private OrderId id;
    private List<OrderLine> lines;  // Internal entities
    private OrderStatus status;

    // Invariant enforcement
    public void addLine(OrderLine line) {
        validateNotClosed();
        lines.add(line);
    }
}

@Entity
class OrderLine {  // Package-private, owned by Order only
    private Product productRef;  // Reference by ID
    private int quantity;
}
```

**FAIL - Multiple ownership**
```java
@AggregateRoot
public class Order {
    private Address shippingAddress;  // References Address
}

@AggregateRoot
public class Customer {
    private Address billingAddress;  // Also references Address - violation!
}

@Entity
public class Address {
    // Owned by both Order and Customer!
}
```

**FAIL - Too large**
```java
@AggregateRoot
public class HugeAggregate {
    private List<Entity1> entities1;
    private List<Entity2> entities2;
    private List<Entity3> entities3;
    // ... 15 more entity collections - too large!
}
```

**FAIL - Boundary breach**
```java
// Infrastructure layer
public class OrderController {
    public void updateLine(OrderLineId lineId) {
        OrderLine line = orderLineRepository.findById(lineId);
        line.setQuantity(10);  // WRONG: Bypassing Order aggregate!
    }
}
```

#### How to Fix

**For Multiple Ownership:**
- Make Address a value object instead of entity
- Or copy Address per aggregate (denormalize)
- Or use address references by ID

**For Large Aggregates:**
- Split into multiple aggregates
- Move entities to separate aggregates
- Use domain events for coordination

**For Boundary Breaches:**
- Always access entities through the aggregate root
- Make internal entities package-private
- Don't expose entity collections directly

---

### Domain Purity

**Constraint ID**: `ddd:domain-purity`
**Default Severity**: CRITICAL
**Category**: Domain-Driven Design

#### Description

The domain layer must remain pure and free from infrastructure concerns. Domain types should not import or depend on infrastructure classes (database drivers, web frameworks, external libraries).

#### Rationale

Impure domain code causes:
- Technology lock-in
- Difficulty in testing domain logic
- Business logic contaminated with technical details
- Reduced portability and reusability

#### What is Validated

The validator checks that domain types do not:
- Import infrastructure packages (javax.persistence, org.springframework, etc.)
- Depend on external framework annotations
- Reference database-specific types

#### Examples

**PASS - Pure domain**
```java
@AggregateRoot
public class Order {
    private OrderId id;
    private List<OrderLine> lines;
    private Money totalAmount;

    public void addLine(Product product, int quantity) {
        // Pure business logic
        lines.add(new OrderLine(product.id(), quantity, product.price()));
        recalculateTotal();
    }
}
```

**FAIL - Impure domain**
```java
@AggregateRoot
@Entity  // JPA annotation in domain!
@Table(name = "orders")  // Infrastructure concern!
public class Order {
    @Id
    @GeneratedValue  // Database detail in domain!
    private Long id;

    @Autowired  // Spring dependency in domain!
    private EmailService emailService;
}
```

#### How to Fix

1. Remove all infrastructure annotations from domain classes
2. Use separate JPA entities in the infrastructure layer
3. Define ports for external dependencies
4. Keep domain classes as pure POJOs or records

---

### Event Naming

**Constraint ID**: `ddd:event-naming`
**Default Severity**: MINOR
**Category**: Domain-Driven Design

#### Description

Domain events represent something that has happened in the past. Their names should use past tense to clearly communicate this temporal aspect. Well-named events improve code readability and domain understanding.

#### Rationale

Improper event naming causes:
- Confusion about whether something has happened or will happen
- Difficulty understanding the domain flow
- Inconsistent ubiquitous language

#### What is Validated

The validator checks that domain event names:
- End with past tense verbs (Created, Updated, Deleted, Placed, etc.)
- Follow the pattern `{Subject}{PastTenseVerb}` or `{Subject}{PastTenseVerb}Event`

#### Examples

**PASS - Past tense naming**
```java
@DomainEvent
public record OrderPlaced(OrderId orderId, CustomerId customerId, Instant occurredAt) {}

@DomainEvent
public record PaymentReceived(PaymentId paymentId, Money amount) {}

@DomainEvent
public record CustomerRegistered(CustomerId customerId, Email email) {}
```

**FAIL - Imperative or present tense naming**
```java
@DomainEvent
public record PlaceOrder(OrderId orderId) {}  // Imperative - sounds like command

@DomainEvent
public record OrderPlacing(OrderId orderId) {}  // Present tense - sounds ongoing

@DomainEvent
public record OrderEvent(OrderId orderId) {}  // Vague - what happened?
```

#### How to Fix

1. Rename events to past tense: `PlaceOrder` → `OrderPlaced`
2. Use clear past participles: `Created`, `Updated`, `Deleted`, `Sent`, `Received`
3. Follow pattern: `{WhatHappened}` not `{WhatToDo}`

---

## Hexagonal Architecture Constraints

### Port Interface

**Constraint ID**: `hexagonal:port-interface`
**Default Severity**: CRITICAL
**Category**: Hexagonal Architecture

#### Description

Ports define the boundaries between the application core and external adapters. They must be interfaces (not concrete classes) to allow multiple implementations (adapters) and maintain the Dependency Inversion Principle.

#### Rationale

Concrete port implementations cause:
- Tight coupling to specific technologies
- Inability to swap implementations
- Difficulty in testing (can't mock easily)
- Violation of Dependency Inversion Principle

#### What is Validated

The validator checks that:
- All types classified as `PORT` are interfaces
- Not abstract or concrete classes

#### Examples

**PASS - Port as interface**
```java
// Port interface
public interface NotificationPort {
    void send(Notification notification);
}

// Adapter implementation (infrastructure layer)
public class EmailNotificationAdapter implements NotificationPort {
    @Override
    public void send(Notification notification) {
        // Email-specific implementation
    }
}
```

**FAIL - Port as class**
```java
// WRONG: Port should be an interface
public class PaymentPort {
    public void processPayment(Payment payment) {
        // ...
    }
}
```

#### How to Fix

1. Convert concrete port classes to interfaces
2. Move implementation details to adapter classes in infrastructure layer
3. Use dependency injection to wire adapters at runtime

---

### Port Coverage

**Constraint ID**: `hexagonal:port-coverage`
**Default Severity**: MAJOR
**Category**: Hexagonal Architecture

#### Description

Every port (interface defining a boundary contract) should have at least one adapter implementation. Uncovered ports indicate incomplete architecture - either the port is unused or the adapter is missing.

#### Rationale

Uncovered ports cause:
- Runtime failures when the port is invoked
- Incomplete system functionality
- Dead code that confuses developers
- False sense of abstraction without implementation

#### What is Validated

The validator checks that:
- Each driven port has at least one implementing adapter
- Each driving port has at least one adapter using it
- No orphan ports exist without corresponding adapters

#### Examples

**PASS - Port with adapter**
```java
// Port (in domain/application layer)
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
}

// Adapter (in infrastructure layer)
@Repository
public class JpaOrderRepository implements OrderRepository {
    @Override
    public Optional<Order> findById(OrderId id) {
        // JPA implementation
    }

    @Override
    public void save(Order order) {
        // JPA implementation
    }
}
```

**FAIL - Port without adapter**
```java
// Port defined but never implemented
public interface NotificationPort {
    void sendNotification(Notification notification);
}
// No class implements NotificationPort!
```

#### How to Fix

1. Create an adapter class implementing the port
2. If the port is truly unused, remove it
3. For testing, create a mock/stub adapter
4. Ensure adapters are in the infrastructure layer

---

### Dependency Direction

**Constraint ID**: `hexagonal:dependency-direction`
**Default Severity**: BLOCKER
**Category**: Hexagonal Architecture

#### Description

The domain layer (business logic) must not depend on infrastructure concerns. Dependencies should always flow inward toward the domain core. This ensures the domain remains independent, testable, and technology-agnostic.

#### Rationale

Domain → Infrastructure dependencies cause:
- Technology lock-in
- Difficulty in testing domain logic
- Cannot change infrastructure without touching domain
- Business logic contaminated with technical concerns

#### What is Validated

The validator detects when domain layer types directly reference infrastructure layer types.

#### Examples

**PASS - Correct dependency direction**
```java
// Domain layer
public class OrderService {
    private final OrderRepository repository;  // Port (domain interface)

    public void placeOrder(Order order) {
        // Business logic
        repository.save(order);
    }
}

// Infrastructure layer
public class JpaOrderRepository implements OrderRepository {
    // Infrastructure details here
}
```

**FAIL - Wrong dependency direction**
```java
// Domain layer - WRONG!
public class OrderService {
    private final JpaOrderRepository jpaRepo;  // Direct infrastructure dependency!

    public void placeOrder(Order order) {
        jpaRepo.save(order);
    }
}
```

#### How to Fix

1. Define port interfaces in the domain layer
2. Implement ports in the infrastructure layer
3. Use dependency injection to wire implementations
4. Never import infrastructure classes in domain code

---

### Layer Isolation

**Constraint ID**: `hexagonal:layer-isolation`
**Default Severity**: MAJOR
**Category**: Hexagonal Architecture

#### Description

Layers should have clear boundaries and respect dependency rules:
- **Domain**: Depends on nothing external (pure business logic)
- **Application**: Depends only on Domain (use cases, orchestration)
- **Infrastructure**: Can depend on both Domain and Application

#### Rationale

Improper layer dependencies cause:
- Tangled architecture
- Difficulty in understanding system structure
- Circular dependencies
- Reduced maintainability and testability

#### What is Validated

The validator checks for violations of layer dependency rules:
- Application → Infrastructure (should use ports)
- Domain → Application (business logic shouldn't know about use cases)
- Other forbidden cross-layer dependencies

#### Examples

**PASS - Proper layer isolation**
```
Infrastructure → Application → Domain
     ↓
  Ports (in Domain)
```

**FAIL - Application depends on Infrastructure**
```java
// Application layer - WRONG!
public class OrderApplicationService {
    private final JpaRepository jpaRepo;  // Infrastructure dependency!
}
```

**FAIL - Domain depends on Application**
```java
// Domain layer - WRONG!
public class Order {
    private final OrderApplicationService appService;  // Should not know about app layer!
}
```

#### How to Fix

1. Use port interfaces in Application layer
2. Keep Domain layer pure (no external dependencies)
3. Implement adapters in Infrastructure layer
4. Follow dependency inversion principle

---

## Configuration Examples

### Adjust Severity Levels

```xml
<config>
    <severity>
        <ddd:aggregate-repository>CRITICAL</ddd:aggregate-repository>
        <hexagonal:layer-isolation>BLOCKER</hexagonal:layer-isolation>
    </severity>
</config>
```

### Enable CRITICAL as errors (strict mode)

```xml
<configuration>
    <errorOnCritical>true</errorOnCritical>
</configuration>
```

Or via `hexaglue.yaml`:

```yaml
plugins:
  io.hexaglue.plugin.audit:
    errorOnCritical: true
```

### Disable build failure (for migration)

```xml
<configuration>
    <failOnError>false</failOnError>
</configuration>
```

---

## Visual Violation Types

When violations are detected, they are visually highlighted in Mermaid diagrams with distinct color styles:

| Violation Type | Constraint | Color | Style Name |
|----------------|------------|-------|------------|
| `CYCLE` | ddd:aggregate-cycle | Red `#FF5978` | Alert |
| `MUTABLE_VALUE_OBJECT` | ddd:value-object-immutable | Orange `#FF9800` | MutableWarning |
| `IMPURE_DOMAIN` | ddd:domain-purity | Purple `#9C27B0` | ImpurityWarning |
| `BOUNDARY_VIOLATION` | ddd:aggregate-consistency | Red `#E53935` | BoundaryWarning |
| `MISSING_IDENTITY` | ddd:entity-identity | Yellow `#FBC02D` | MissingIdentityWarning |
| `MISSING_REPOSITORY` | ddd:aggregate-repository | Blue `#1976D2` | MissingRepositoryInfo |
| `EVENT_NAMING` | ddd:event-naming | Cyan `#00ACC1` | EventNamingWarning |
| `PORT_UNCOVERED` | hexagonal:port-coverage | Teal `#00897B` | PortUncoveredWarning |
| `DEPENDENCY_INVERSION` | hexagonal:dependency-inversion | Amber `#FFB300` | DependencyInversionWarning |
| `LAYER_VIOLATION` | hexagonal:layer-isolation | Grey `#616161` | LayerViolationWarning |
| `PORT_NOT_INTERFACE` | hexagonal:port-interface | Brown `#8D6E63` | PortNotInterfaceWarning |

These styles are applied automatically in the generated diagrams (domainModel, applicationLayer, portsLayer, fullArchitecture) to help quickly identify problematic types.

---

## Additional Resources

- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design by Vaughn Vernon](https://vaughnvernon.com/)
- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
