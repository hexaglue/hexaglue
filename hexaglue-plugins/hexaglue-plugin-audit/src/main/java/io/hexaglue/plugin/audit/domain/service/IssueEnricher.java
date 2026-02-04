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
                    return Suggestion.complete(
                            "Create an application service implementing this port",
                            List.of(
                                    "Create a new class implementing the port interface",
                                    "Implement all required methods",
                                    "Register the implementation with your DI container"),
                            "@Service\n" + "public class PortImplementation implements PortInterface {\n"
                                    + "    @Override\n"
                                    + "    public void method() {\n"
                                    + "        // implementation\n"
                                    + "    }\n"
                                    + "}",
                            "1 day");
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
                        "0.25 days");
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
                        "0.5 days");
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
