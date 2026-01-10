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

package io.hexaglue.plugin.audit.adapter.analyzer;

import io.hexaglue.plugin.audit.domain.model.BoundedContext;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainModel;
import io.hexaglue.spi.ir.DomainType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Detects Bounded Contexts from the analyzed domain model.
 *
 * <p>This analyzer implements an automatic detection strategy for identifying
 * Bounded Contexts based on package structure and domain type distribution.
 * A Bounded Context is a central pattern in DDD that defines explicit boundaries
 * within which a particular domain model is defined and applicable.
 *
 * <p><strong>Detection heuristics:</strong>
 * <ol>
 *   <li><strong>Aggregate-based detection (primary):</strong> Each package containing
 *       at least one {@link DomainKind#AGGREGATE_ROOT} is considered a bounded context.
 *       The context name is derived from the package name.</li>
 *   <li><strong>Package hierarchy:</strong> All domain types within a context's package
 *       or its sub-packages belong to that context.</li>
 *   <li><strong>Type categorization:</strong> Domain types are categorized within their
 *       context based on their {@link DomainKind}.</li>
 * </ol>
 *
 * <p><strong>Package-to-context mapping:</strong><br>
 * Given the package structure:
 * <pre>{@code
 * com.example.order
 *   - Order (AggregateRoot)
 *   - OrderLine (Entity)
 *   - Money (ValueObject)
 * com.example.inventory
 *   - Product (AggregateRoot)
 *   - Stock (Entity)
 * }</pre>
 *
 * This detector will identify two bounded contexts:
 * <ul>
 *   <li>"order" containing Order, OrderLine, and Money</li>
 *   <li>"inventory" containing Product and Stock</li>
 * </ul>
 *
 * <p><strong>Edge cases:</strong>
 * <ul>
 *   <li><strong>No aggregate roots:</strong> If a package contains domain types but no
 *       aggregate roots, those types are not assigned to any context.</li>
 *   <li><strong>Nested packages:</strong> The most specific package with an aggregate root
 *       defines the context boundary. For example, if both {@code com.example.order} and
 *       {@code com.example.order.shipping} contain aggregate roots, they are treated as
 *       separate contexts.</li>
 *   <li><strong>Flat structure:</strong> If all aggregates are in the same package,
 *       a single bounded context is detected.</li>
 * </ul>
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * BoundedContextDetector detector = new BoundedContextDetector();
 * List<BoundedContext> contexts = detector.detect(irSnapshot.domain());
 *
 * contexts.forEach(ctx -> {
 *     System.out.println("Context: " + ctx.name());
 *     System.out.println("  Aggregates: " + ctx.aggregateRoots().size());
 *     System.out.println("  Entities: " + ctx.entities().size());
 *     System.out.println("  Value Objects: " + ctx.valueObjects().size());
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
public class BoundedContextDetector {

    /**
     * Detects bounded contexts from the given domain model.
     *
     * <p>The detection process:
     * <ol>
     *   <li>Identifies all packages containing aggregate roots</li>
     *   <li>Creates a bounded context for each such package</li>
     *   <li>Assigns all domain types to their respective contexts based on package membership</li>
     *   <li>Categorizes types within each context by their domain kind</li>
     * </ol>
     *
     * <p>If the domain model contains no aggregate roots, this method returns an empty list.
     * Domain types that don't belong to any aggregate-containing package are not included
     * in any context.
     *
     * @param domainModel the domain model to analyze
     * @return list of detected bounded contexts, or empty list if none found
     * @throws NullPointerException if domainModel is null
     */
    public List<BoundedContext> detect(DomainModel domainModel) {
        Objects.requireNonNull(domainModel, "domainModel required");

        List<DomainType> aggregateRoots = domainModel.aggregateRoots();

        if (aggregateRoots.isEmpty()) {
            return List.of();
        }

        // Group aggregate roots by their package to identify context boundaries
        Map<String, List<DomainType>> contextToAggregates = groupByPackage(aggregateRoots);

        // Build bounded contexts by assigning all domain types to their respective contexts
        List<BoundedContext> contexts = new ArrayList<>();

        for (Map.Entry<String, List<DomainType>> entry : contextToAggregates.entrySet()) {
            String contextPackage = entry.getKey();
            String contextName = deriveContextName(contextPackage);

            // Collect all types that belong to this context
            List<DomainType> contextAggregates = entry.getValue();
            List<DomainType> contextEntities =
                    findEntitiesInPackage(domainModel, contextPackage, contextToAggregates.keySet());
            List<DomainType> contextValueObjects =
                    findValueObjectsInPackage(domainModel, contextPackage, contextToAggregates.keySet());
            List<DomainType> contextEvents =
                    findDomainEventsInPackage(domainModel, contextPackage, contextToAggregates.keySet());
            List<DomainType> contextServices =
                    findDomainServicesInPackage(domainModel, contextPackage, contextToAggregates.keySet());

            BoundedContext context = new BoundedContext(
                    contextName,
                    contextAggregates,
                    contextEntities,
                    contextValueObjects,
                    contextEvents,
                    contextServices);

            contexts.add(context);
        }

        return contexts;
    }

    /**
     * Groups domain types by their package name.
     *
     * @param types the domain types to group
     * @return map of package name to list of types in that package
     */
    private Map<String, List<DomainType>> groupByPackage(List<DomainType> types) {
        Map<String, List<DomainType>> packageMap = new HashMap<>();

        for (DomainType type : types) {
            String packageName = type.packageName();
            packageMap.computeIfAbsent(packageName, k -> new ArrayList<>()).add(type);
        }

        return packageMap;
    }

    /**
     * Derives a context name from a package name.
     *
     * <p>The context name is the last segment of the package name. For example:
     * <ul>
     *   <li>{@code com.example.order} → "order"</li>
     *   <li>{@code com.example.inventory} → "inventory"</li>
     *   <li>{@code order} → "order"</li>
     * </ul>
     *
     * @param packageName the package name
     * @return the derived context name
     */
    private String deriveContextName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "default";
        }

        int lastDot = packageName.lastIndexOf('.');
        return lastDot >= 0 ? packageName.substring(lastDot + 1) : packageName;
    }

    /**
     * Finds all entities in a given package or its sub-packages.
     *
     * <p>Only non-aggregate-root entities are included (i.e., {@link DomainKind#ENTITY} only).
     * This method excludes entities from sub-packages that have their own aggregate roots
     * (and thus form their own bounded contexts).
     *
     * @param domainModel      the domain model
     * @param contextPackage   the package defining the context boundary
     * @param contextPackages  all packages that define bounded contexts
     * @return list of entities in this context
     */
    private List<DomainType> findEntitiesInPackage(
            DomainModel domainModel, String contextPackage, java.util.Set<String> contextPackages) {
        return domainModel.types().stream()
                .filter(type -> type.kind() == DomainKind.ENTITY)
                .filter(type -> belongsToContext(type, contextPackage, contextPackages))
                .toList();
    }

    /**
     * Finds all value objects in a given package or its sub-packages.
     *
     * <p>This method excludes value objects from sub-packages that have their own
     * aggregate roots (and thus form their own bounded contexts).
     *
     * @param domainModel      the domain model
     * @param contextPackage   the package defining the context boundary
     * @param contextPackages  all packages that define bounded contexts
     * @return list of value objects in this context
     */
    private List<DomainType> findValueObjectsInPackage(
            DomainModel domainModel, String contextPackage, java.util.Set<String> contextPackages) {
        return domainModel.types().stream()
                .filter(type -> type.kind() == DomainKind.VALUE_OBJECT)
                .filter(type -> belongsToContext(type, contextPackage, contextPackages))
                .toList();
    }

    /**
     * Finds all domain events in a given package or its sub-packages.
     *
     * <p>This method excludes domain events from sub-packages that have their own
     * aggregate roots (and thus form their own bounded contexts).
     *
     * @param domainModel      the domain model
     * @param contextPackage   the package defining the context boundary
     * @param contextPackages  all packages that define bounded contexts
     * @return list of domain events in this context
     */
    private List<DomainType> findDomainEventsInPackage(
            DomainModel domainModel, String contextPackage, java.util.Set<String> contextPackages) {
        return domainModel.types().stream()
                .filter(type -> type.kind() == DomainKind.DOMAIN_EVENT)
                .filter(type -> belongsToContext(type, contextPackage, contextPackages))
                .toList();
    }

    /**
     * Finds all domain services in a given package or its sub-packages.
     *
     * <p>This method excludes domain services from sub-packages that have their own
     * aggregate roots (and thus form their own bounded contexts).
     *
     * @param domainModel      the domain model
     * @param contextPackage   the package defining the context boundary
     * @param contextPackages  all packages that define bounded contexts
     * @return list of domain services in this context
     */
    private List<DomainType> findDomainServicesInPackage(
            DomainModel domainModel, String contextPackage, java.util.Set<String> contextPackages) {
        return domainModel.types().stream()
                .filter(type -> type.kind() == DomainKind.DOMAIN_SERVICE)
                .filter(type -> belongsToContext(type, contextPackage, contextPackages))
                .toList();
    }

    /**
     * Determines whether a domain type belongs to a specific bounded context.
     *
     * <p>A type belongs to a context if:
     * <ul>
     *   <li>Its package name equals the context package, OR</li>
     *   <li>Its package name starts with the context package followed by a dot (sub-package),
     *       AND no other bounded context exists in a more specific sub-package</li>
     * </ul>
     *
     * <p>This ensures that when we have nested packages with aggregate roots
     * (e.g., {@code com.example.order} and {@code com.example.order.shipping}),
     * types in the shipping package are assigned to the shipping context, not the order context.
     *
     * @param type            the domain type to check
     * @param contextPackage  the context package
     * @param contextPackages all packages that define bounded contexts
     * @return true if the type belongs to this specific context
     */
    private boolean belongsToContext(DomainType type, String contextPackage, java.util.Set<String> contextPackages) {
        String typePackage = type.packageName();

        // Type is in the exact context package
        if (typePackage.equals(contextPackage)) {
            return true;
        }

        // Type is in a sub-package of the context
        if (typePackage.startsWith(contextPackage + ".")) {
            // Check if there's a more specific context that owns this type
            for (String otherContextPackage : contextPackages) {
                // Skip the current context
                if (otherContextPackage.equals(contextPackage)) {
                    continue;
                }

                // If another context is a sub-package of the current context
                // and the type belongs to it, then the type doesn't belong to current context
                if (otherContextPackage.startsWith(contextPackage + ".")
                        && (typePackage.equals(otherContextPackage)
                                || typePackage.startsWith(otherContextPackage + "."))) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }
}
