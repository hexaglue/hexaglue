/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.plugin.audit.domain.service;

import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.model.report.IssueEntry;
import io.hexaglue.plugin.audit.domain.model.report.SourceLocation;
import io.hexaglue.plugin.audit.domain.model.report.Suggestion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enriches violations with impact descriptions and remediation suggestions.
 *
 * <p>Each violation is transformed into an {@link IssueEntry} with mandatory
 * impact and suggestion fields, as required by the report specification.
 *
 * @since 5.0.0
 */
public class IssueEnricher {

    private final Map<String, IssueTemplate> templates;
    private final Map<String, AtomicInteger> idCounters = new HashMap<>();

    /**
     * Creates an IssueEnricher with default templates.
     */
    public IssueEnricher() {
        this.templates = buildDefaultTemplates();
    }

    /**
     * Creates an IssueEnricher with custom templates.
     *
     * @param templates custom issue templates
     */
    public IssueEnricher(Map<String, IssueTemplate> templates) {
        this.templates = new HashMap<>(templates);
        this.templates.putAll(buildDefaultTemplates()); // Ensure defaults are available
    }

    /**
     * Enriches a violation into a full IssueEntry.
     *
     * @param violation the violation to enrich
     * @return enriched issue entry
     */
    public IssueEntry enrich(Violation violation) {
        String constraintId = violation.constraintId().value();
        IssueTemplate template = templates.getOrDefault(constraintId, defaultTemplate());

        String id = generateId(violation);
        String title = extractTitle(violation);
        String message = violation.message();
        SourceLocation location = toSourceLocation(violation);
        String impact = template.impact(violation);
        Suggestion suggestion = template.suggestion(violation);

        return new IssueEntry(id, constraintId, violation.severity(), title, message, location, impact, suggestion);
    }

    /**
     * Generates a unique ID for a violation.
     *
     * @param violation the violation
     * @return unique ID
     */
    public String generateId(Violation violation) {
        String baseId = violation.constraintId().value().replace(":", "-");
        AtomicInteger counter = idCounters.computeIfAbsent(baseId, k -> new AtomicInteger(0));
        return baseId + "-" + counter.incrementAndGet();
    }

    /**
     * Extracts a title from a violation.
     *
     * @param violation the violation
     * @return title
     */
    public String extractTitle(Violation violation) {
        String constraintId = violation.constraintId().value();
        return switch (constraintId) {
            case "ddd:aggregate-cycle" -> "Circular dependency between aggregates";
            case "ddd:value-object-immutable" -> "Mutable Value Object";
            case "ddd:entity-identity" -> "Entity without identity";
            case "ddd:aggregate-boundary" -> "Aggregate boundary violation";
            case "ddd:aggregate-repository" -> "Aggregate without repository";
            case "ddd:domain-purity" -> "Domain purity violation";
            case "hexagonal:port-coverage" ->
                violation.message().startsWith("Driving")
                        ? "Driving port without adapter"
                        : "Driven port without adapter";
            case "hexagonal:port-direction" -> "Port direction violation";
            case "hexagonal:layer-isolation" -> "Layer isolation violation";
            case "hexagonal:dependency-inversion" -> "Dependency inversion violation";
            case "hexagonal:dependency-direction" -> "Dependency direction violation";
            case "hexagonal:port-interface" -> "Port is not an interface";
            case "hexagonal:application-purity" -> "Application layer purity violation";
            case "ddd:aggregate-consistency" -> "Aggregate consistency violation";
            case "ddd:event-naming" -> "Domain event naming violation";
            default -> humanize(constraintId);
        };
    }

    private SourceLocation toSourceLocation(Violation violation) {
        var loc = violation.location();
        String type = violation.affectedTypes().stream().findFirst().orElse("Unknown");
        String file = loc.filePath();
        Integer line = loc.lineStart() > 0 ? loc.lineStart() : null;
        return new SourceLocation(type, file, line);
    }

    private String humanize(String constraintId) {
        // Convert "ddd:aggregate-cycle" to "Aggregate cycle"
        String[] parts = constraintId.split(":");
        String name = parts.length > 1 ? parts[1] : parts[0];
        return capitalize(name.replace("-", " ").replace("_", " "));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private IssueTemplate defaultTemplate() {
        return new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "This issue may affect the architectural integrity of your application.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.simple("Review and fix this issue");
            }
        };
    }

    private Map<String, IssueTemplate> buildDefaultTemplates() {
        Map<String, IssueTemplate> map = new HashMap<>();

        // DDD: Aggregate Cycle
        map.put("ddd:aggregate-cycle", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Aggregates become tightly coupled, making it impossible to maintain "
                        + "independent consistency boundaries. This can cause cascade updates, "
                        + "transactional issues, and makes the domain model harder to evolve.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Replace direct references with domain events",
                        List.of(
                                "Remove the direct reference to the other aggregate",
                                "Create a domain event to communicate between aggregates",
                                "Have the other aggregate subscribe to the event"),
                        "// Instead of: order.reserveInventory(inventoryItem)\n"
                                + "// Use: domainEvents.publish(new OrderPlaced(orderId, items))",
                        "2 days");
            }
        });

        // DDD: Value Object Immutability
        map.put("ddd:value-object-immutable", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Value objects must be immutable to guarantee consistency. A mutable "
                        + "value object can be changed after being assigned to an entity, causing "
                        + "unexpected behavior and breaking domain invariants.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Remove setters, make fields final, use factory methods",
                        List.of(
                                "Make all fields final",
                                "Remove all setter methods",
                                "Add factory method or use record"),
                        "public record Money(BigDecimal amount, Currency currency) {\n"
                                + "    public Money add(Money other) {\n"
                                + "        return new Money(amount.add(other.amount), currency);\n"
                                + "    }\n"
                                + "}",
                        "0.5 days");
            }
        });

        // Hexagonal: Port Coverage
        map.put("hexagonal:port-coverage", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Ports without adapters cannot be used. The application cannot interact "
                        + "with external systems or be invoked by driving adapters.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                boolean isDriving = v.message().toLowerCase().contains("driving");
                if (isDriving) {
                    // Driving port: requires REST controller exposing the port as an API
                    // Can be automated by hexaglue-plugin-rest
                    return Suggestion.automatable(
                            "Create a driving adapter exposing this port as a REST API",
                            List.of(
                                    "Create a REST controller delegating to the driving port",
                                    "Create request DTOs for each endpoint",
                                    "Create response DTOs for each endpoint",
                                    "Map HTTP verbs to port methods",
                                    "Register the controller with your DI container"),
                            "@RestController\n"
                                    + "@RequestMapping(\"/api/orders\")\n"
                                    + "public class OrderController {\n"
                                    + "    private final OrderService orderService; // driving port\n"
                                    + "    \n"
                                    + "    @PostMapping\n"
                                    + "    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {\n"
                                    + "        Order order = orderService.placeOrder(request.toDomain());\n"
                                    + "        return ResponseEntity.ok(OrderResponse.from(order));\n"
                                    + "    }\n"
                                    + "}",
                            "2 days",
                            "hexaglue-plugin-rest");
                } else {
                    // Driven port: requires full persistence chain (adapter, entity, mapper, converters)
                    // Can be automated by hexaglue-plugin-jpa
                    return Suggestion.automatable(
                            "Create an infrastructure adapter implementing this port",
                            List.of(
                                    "Create JPA entity mapping the aggregate",
                                    "Create mapper to convert between domain and JPA entity",
                                    "Create JPA repository adapter implementing the port",
                                    "Add identity converters if using typed identifiers",
                                    "Register the implementation with your DI container"),
                            "@Repository\n" + "public class JpaOrderRepository implements OrderRepository {\n"
                                    + "    private final SpringDataOrderRepository springRepo;\n"
                                    + "    private final OrderMapper mapper;\n"
                                    + "    \n"
                                    + "    @Override\n"
                                    + "    public void save(Order order) {\n"
                                    + "        springRepo.save(mapper.toEntity(order));\n"
                                    + "    }\n"
                                    + "}",
                            "3 days",
                            "hexaglue-plugin-jpa");
                }
            }
        });

        // Hexagonal: Port Direction
        map.put("hexagonal:port-direction", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Ports should only be used in the intended direction. "
                        + "Driving ports should be called from external adapters, "
                        + "and driven ports should be called from the domain.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Correct the port usage direction",
                        List.of(
                                "Review which component is calling the port",
                                "Move the call to the appropriate layer",
                                "Consider if the port type is correct"),
                        null,
                        "0.5 days");
            }
        });

        // Hexagonal: Layer Isolation
        map.put("hexagonal:layer-isolation", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Layer isolation violations break the hexagonal architecture contract. "
                        + "When layers bypass ports to access each other directly, the architecture "
                        + "loses its ability to swap implementations and maintain testability.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Route the dependency through the appropriate port",
                        List.of(
                                "Identify the direct cross-layer dependency",
                                "Create or use an existing port interface for this interaction",
                                "Refactor the caller to depend on the port instead of the concrete class",
                                "Ensure the adapter implements the port on the infrastructure side"),
                        null,
                        "1 day");
            }
        });

        // DDD: Entity Identity
        map.put("ddd:entity-identity", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Entities are defined by their identity, not their attributes. Without an identity "
                        + "field, the entity cannot be uniquely identified, tracked, or persisted. Equality "
                        + "comparisons become attribute-based instead of identity-based, which breaks DDD "
                        + "semantics and can cause subtle bugs in collections and persistence operations.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Add a dedicated identity field to the entity",
                        List.of(
                                "Create a typed identifier (e.g., OrderId wrapping UUID) or use a simple ID type",
                                "Add the identity field to the entity class",
                                "Implement equals/hashCode based on the identity field (or use a record)"),
                        "// Create a typed identifier\n"
                                + "public record OrderId(UUID value) {\n"
                                + "    public static OrderId generate() {\n"
                                + "        return new OrderId(UUID.randomUUID());\n"
                                + "    }\n"
                                + "}\n\n"
                                + "// Add identity to the entity\n"
                                + "public class Order {\n"
                                + "    private final OrderId id;\n"
                                + "    // ... other fields\n"
                                + "}",
                        "0.5 days");
            }
        });

        // DDD: Aggregate Repository
        map.put("ddd:aggregate-repository", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Aggregate roots are the unit of retrieval and persistence in DDD. Without a "
                        + "repository interface, there is no standard way to load or save the aggregate, "
                        + "and the aggregate boundary cannot be enforced for persistence operations. "
                        + "This often leads to ad-hoc data access that bypasses aggregate invariants.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Create a driven port (repository interface) for the aggregate root",
                        List.of(
                                "Create a repository interface in the domain layer (driven port)",
                                "Define standard persistence methods (save, findById, delete)",
                                "Implement the repository in the infrastructure layer as an adapter"),
                        "// Domain layer: driven port\n"
                                + "public interface OrderRepository {\n"
                                + "    void save(Order order);\n"
                                + "    Optional<Order> findById(OrderId id);\n"
                                + "    void delete(OrderId id);\n"
                                + "}",
                        "1 day");
            }
        });

        // DDD: Aggregate Boundary
        map.put("ddd:aggregate-boundary", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Direct access to internal entities bypasses the aggregate root, which is "
                        + "responsible for enforcing invariants and maintaining consistency. External "
                        + "code modifying internal entities can break business rules and lead to "
                        + "inconsistent state that the aggregate root cannot prevent or detect.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Route all access to internal entities through the aggregate root",
                        List.of(
                                "Remove direct references to the internal entity from external code",
                                "Add methods on the aggregate root to expose the needed behavior",
                                "If external code needs read access, return value objects or DTOs instead",
                                "Ensure the internal entity is not directly retrievable from repositories"),
                        "// Before: external code accesses entity directly\n"
                                + "// orderItem.updateQuantity(5);\n\n"
                                + "// After: access through aggregate root\n"
                                + "order.updateItemQuantity(orderItemId, 5);",
                        "2 days");
            }
        });

        // DDD: Aggregate Consistency
        map.put("ddd:aggregate-consistency", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                boolean isMultiOwnership = v.message().contains("multiple aggregates");
                if (isMultiOwnership) {
                    return "An entity referenced by multiple aggregates creates unclear ownership "
                            + "and consistency boundaries. When two aggregates share an entity, it is "
                            + "unclear which aggregate is responsible for maintaining its invariants, "
                            + "and concurrent modifications can lead to data corruption.";
                }
                return "An oversized aggregate is harder to understand, maintain, and reason about. "
                        + "Large aggregates cause performance issues (loading too many objects), "
                        + "increase transaction contention, and often indicate that the aggregate "
                        + "is trying to enforce too many invariants at once.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                boolean isMultiOwnership = v.message().contains("multiple aggregates");
                if (isMultiOwnership) {
                    return Suggestion.complete(
                            "Assign the entity to a single aggregate or promote it to its own aggregate",
                            List.of(
                                    "Identify which aggregate is the true owner of the entity",
                                    "Remove the entity reference from the other aggregate(s)",
                                    "Replace removed references with the entity's identifier (TypeId) or a value object",
                                    "If the entity is truly shared, consider promoting it to its own aggregate root"),
                            "// Before: entity shared between aggregates\n"
                                    + "// Order -> OrderItem <- Inventory\n\n"
                                    + "// After: reference by ID instead\n"
                                    + "public class Inventory {\n"
                                    + "    private final List<OrderItemId> reservedItems; // reference by ID\n"
                                    + "}",
                            "1 day");
                }
                return Suggestion.complete(
                        "Split the aggregate into smaller, more focused aggregates",
                        List.of(
                                "Identify groups of entities that enforce related invariants",
                                "Extract each group into its own aggregate with a dedicated root",
                                "Replace direct entity references between aggregates with identifiers",
                                "Use domain events to coordinate between the new aggregates"),
                        null,
                        "3 days");
            }
        });

        // DDD: Domain Purity
        map.put("ddd:domain-purity", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Infrastructure imports in the domain layer (e.g., JPA annotations, Spring "
                        + "framework classes) couple the business logic to specific technologies. "
                        + "This makes the domain model harder to test in isolation, impossible to "
                        + "reuse with different frameworks, and blurs the separation between what "
                        + "the system does and how it does it.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Remove infrastructure imports and use domain-pure alternatives",
                        List.of(
                                "Identify all forbidden infrastructure imports in the domain type",
                                "Replace framework annotations with domain concepts (e.g., remove @Entity, @Column)",
                                "If infrastructure behavior is needed, define a port interface in the domain layer",
                                "Move framework-specific code to infrastructure adapters"),
                        "// Before: domain polluted with JPA\n"
                                + "@Entity\n"
                                + "public class Order { ... }\n\n"
                                + "// After: pure domain model\n"
                                + "public class Order {\n"
                                + "    private final OrderId id;\n"
                                + "    private final List<OrderItem> items;\n"
                                + "    // Pure business logic, no framework imports\n"
                                + "}",
                        "1 day");
            }
        });

        // DDD: Event Naming
        map.put("ddd:event-naming", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Domain events represent facts that have already happened in the business "
                        + "domain. A name not in past tense is misleading: it suggests a command "
                        + "(something to do) rather than an event (something that occurred). "
                        + "Consistent past-tense naming makes event flows easier to understand "
                        + "and aligns with event-sourcing and event-driven architecture conventions.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Rename the event class to use past tense",
                        List.of(
                                "Rename the event class to past tense (e.g., OrderPlace -> OrderPlaced)",
                                "Update all references to the renamed class"),
                        "// Before: imperative/present tense\n"
                                + "public record OrderPlace(OrderId orderId) {}\n\n"
                                + "// After: past tense\n"
                                + "public record OrderPlaced(OrderId orderId) {}",
                        "0.5 days");
            }
        });

        // Hexagonal: Port Interface
        map.put("hexagonal:port-interface", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Ports defined as classes instead of interfaces prevent the application from "
                        + "swapping implementations. The core principle of hexagonal architecture is "
                        + "that the application core depends on abstractions (interfaces), and adapters "
                        + "provide concrete implementations. A port as a class violates the Dependency "
                        + "Inversion Principle and makes testing with mocks or stubs impossible.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Convert the port from a class to an interface",
                        List.of(
                                "Extract an interface from the existing port class",
                                "Move the implementation to an adapter class in the infrastructure layer",
                                "Update all dependents to reference the interface instead of the class"),
                        "// Before: port as a class\n"
                                + "public class OrderRepository { ... }\n\n"
                                + "// After: port as an interface\n"
                                + "public interface OrderRepository {\n"
                                + "    void save(Order order);\n"
                                + "    Optional<Order> findById(OrderId id);\n"
                                + "}\n\n"
                                + "// Implementation in infrastructure layer\n"
                                + "public class JpaOrderRepository implements OrderRepository { ... }",
                        "1 day");
            }
        });

        // Hexagonal: Dependency Direction
        map.put("hexagonal:dependency-direction", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "The domain or application layer depends on infrastructure, which inverts "
                        + "the fundamental rule of hexagonal architecture: dependencies must flow "
                        + "inward toward the domain core. This makes the business logic tightly "
                        + "coupled to specific technology choices, impossible to test without "
                        + "infrastructure, and difficult to evolve independently.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Invert the dependency by introducing a port interface",
                        List.of(
                                "Identify the infrastructure type that the domain/application depends on",
                                "Create a port interface in the domain layer defining the needed behavior",
                                "Move the infrastructure implementation to an adapter that implements the port",
                                "Inject the port interface via constructor in the domain/application code"),
                        "// Before: domain depends on infrastructure\n"
                                + "public class OrderService {\n"
                                + "    private final JpaOrderDao dao; // infrastructure!\n"
                                + "}\n\n"
                                + "// After: domain depends on port (interface)\n"
                                + "public class OrderService {\n"
                                + "    private final OrderRepository repository; // port interface\n"
                                + "}",
                        "2 days");
            }
        });

        // Hexagonal: Dependency Inversion
        map.put("hexagonal:dependency-inversion", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "The application layer depends on a concrete infrastructure class instead "
                        + "of an abstraction (interface). This creates tight coupling to a specific "
                        + "implementation, making it impossible to swap adapters, test with mocks, "
                        + "or evolve the infrastructure independently of the application logic.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Replace the concrete dependency with a port interface",
                        List.of(
                                "Identify the concrete infrastructure class being referenced",
                                "Create or reuse a port interface that defines the needed contract",
                                "Change the application code to depend on the port interface",
                                "Ensure the concrete class implements the port interface"),
                        "// Before: depends on concrete class\n"
                                + "public class PlaceOrderUseCase {\n"
                                + "    private final StripePaymentGateway gateway; // concrete!\n"
                                + "}\n\n"
                                + "// After: depends on abstraction\n"
                                + "public class PlaceOrderUseCase {\n"
                                + "    private final PaymentGateway gateway; // port interface\n"
                                + "}",
                        "1 day");
            }
        });

        // Hexagonal: Application Purity
        map.put("hexagonal:application-purity", new IssueTemplate() {
            @Override
            public String impact(Violation v) {
                return "Infrastructure imports in the application layer (e.g., @Service, "
                        + "@Transactional, JPA annotations) tie the use-case orchestration to a "
                        + "specific framework. This makes application services non-portable, harder "
                        + "to test without the framework runtime, and blurs the boundary between "
                        + "application logic and infrastructure concerns.";
            }

            @Override
            public Suggestion suggestion(Violation v) {
                return Suggestion.complete(
                        "Remove infrastructure imports from application services",
                        List.of(
                                "Remove framework annotations (@Service, @Transactional, @Component)",
                                "Replace framework-specific types with domain or port interfaces",
                                "Configure dependency injection and transaction management externally (in the infrastructure layer)",
                                "If validation is needed, use domain-specific validation instead of javax/jakarta.validation"),
                        "// Before: framework-coupled application service\n"
                                + "@Service\n"
                                + "@Transactional\n"
                                + "public class PlaceOrderUseCase { ... }\n\n"
                                + "// After: pure application service\n"
                                + "public class PlaceOrderUseCase {\n"
                                + "    private final OrderRepository orderRepository; // port\n"
                                + "    private final PaymentGateway paymentGateway;   // port\n"
                                + "    // Pure use-case orchestration, no framework imports\n"
                                + "}",
                        "1 day");
            }
        });

        return map;
    }

    /**
     * Template for generating issue impact and suggestions.
     */
    public interface IssueTemplate {
        /**
         * Returns the impact description for a violation.
         */
        String impact(Violation violation);

        /**
         * Returns the suggestion for fixing a violation.
         */
        Suggestion suggestion(Violation violation);
    }
}
