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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.DomainEvent;
import io.hexaglue.arch.domain.DomainService;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.ports.ApplicationService;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory.BoundedContextStats;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.BoundedContextInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Builds component inventory from the architectural model.
 *
 * <p>This service analyzes the ArchitecturalModel to count and categorize all domain
 * types and ports, producing a ComponentInventory for reporting.
 *
 * <p>Bounded contexts are obtained from the core's {@link ArchitectureQuery#findBoundedContexts()},
 * ensuring consistent detection across the entire HexaGlue pipeline. This follows the architectural
 * principle: "the core generates data, plugins consume it".
 *
 * @since 1.0.0
 * @since 4.0.0 - Migrated from IrSnapshot to ArchitecturalModel
 */
public class InventoryBuilder {

    private static final int MAX_EXAMPLES = 3;

    /**
     * Builds a component inventory from the architectural model using the architecture query.
     *
     * <p>Bounded contexts are obtained from {@link ArchitectureQuery#findBoundedContexts()},
     * ensuring consistency with the core's analysis. Uses {@link ArchitecturalModel#registry()}
     * instead of deprecated convenience methods.</p>
     *
     * @param model             the architectural model to analyze
     * @param architectureQuery the architecture query for bounded context detection
     * @return a ComponentInventory with counts by type
     * @throws NullPointerException if model or architectureQuery is null
     * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
     */
    public ComponentInventory build(ArchitecturalModel model, ArchitectureQuery architectureQuery) {
        Objects.requireNonNull(model, "model required");
        Objects.requireNonNull(architectureQuery, "architectureQuery required");

        var registry = model.registry();

        // Collect domain elements from registry
        List<DomainEntity> aggregates = registry.all(DomainEntity.class)
                .filter(DomainEntity::isAggregateRoot)
                .toList();
        List<DomainEntity> entities = registry.all(DomainEntity.class)
                .filter(e -> !e.isAggregateRoot())
                .toList();
        List<ValueObject> valueObjects = registry.all(ValueObject.class).toList();
        List<DomainEvent> domainEvents = registry.all(DomainEvent.class).toList();
        List<DomainService> domainServices = registry.all(DomainService.class).toList();
        List<ApplicationService> appServices = registry.all(ApplicationService.class).toList();
        List<DrivingPort> drivingPorts = registry.all(DrivingPort.class).toList();
        List<DrivenPort> drivenPorts = registry.all(DrivenPort.class).toList();

        // Extract examples (simple names, limited)
        List<String> aggregateExamples = extractExamples(aggregates.stream().map(e -> e.id().simpleName()));
        List<String> entityExamples = extractExamples(entities.stream().map(e -> e.id().simpleName()));
        List<String> voExamples =
                extractExamples(valueObjects.stream().map(vo -> vo.id().simpleName()));
        List<String> eventExamples =
                extractExamples(domainEvents.stream().map(ev -> ev.id().simpleName()));
        List<String> serviceExamples = extractExamples(domainServices.stream().map(s -> s.id().simpleName()));
        List<String> drivingExamples = extractExamples(drivingPorts.stream().map(p -> p.id().simpleName()));
        List<String> drivenExamples = extractExamples(drivenPorts.stream().map(p -> p.id().simpleName()));

        // Build bounded context statistics from architecture query
        List<BoundedContextStats> bcStats = buildBoundedContextStats(model, architectureQuery);

        return ComponentInventory.builder()
                .aggregateRoots(aggregates.size())
                .entities(entities.size())
                .valueObjects(valueObjects.size())
                .domainEvents(domainEvents.size())
                .domainServices(domainServices.size())
                .applicationServices(appServices.size())
                .drivingPorts(drivingPorts.size())
                .drivenPorts(drivenPorts.size())
                .aggregateExamples(aggregateExamples)
                .entityExamples(entityExamples)
                .valueObjectExamples(voExamples)
                .domainEventExamples(eventExamples)
                .domainServiceExamples(serviceExamples)
                .drivingPortExamples(drivingExamples)
                .drivenPortExamples(drivenExamples)
                .boundedContexts(bcStats)
                .build();
    }

    /**
     * Extracts up to MAX_EXAMPLES simple names from a stream.
     */
    private List<String> extractExamples(Stream<String> names) {
        return names.sorted().limit(MAX_EXAMPLES).toList();
    }

    /**
     * Builds bounded context statistics using the core's ArchitectureQuery.
     *
     * <p>This method obtains bounded contexts from {@link ArchitectureQuery#findBoundedContexts()}
     * and correlates them with domain elements from the architectural model.
     * Uses {@link ArchitecturalModel#registry()} instead of deprecated convenience methods.</p>
     *
     * @param model             the architectural model
     * @param architectureQuery the architecture query
     * @return list of bounded context statistics
     * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
     */
    private List<BoundedContextStats> buildBoundedContextStats(
            ArchitecturalModel model, ArchitectureQuery architectureQuery) {
        List<BoundedContextInfo> boundedContexts = architectureQuery.findBoundedContexts();

        List<BoundedContextStats> stats = new ArrayList<>();
        var registry = model.registry();

        for (BoundedContextInfo bcInfo : boundedContexts) {
            // Count aggregates in this bounded context
            int aggregateCount = (int) registry.all(DomainEntity.class)
                    .filter(DomainEntity::isAggregateRoot)
                    .filter(e -> bcInfo.typeNames().contains(e.id().qualifiedName()))
                    .count();

            // Count entities (non-aggregate roots)
            int entityCount = (int) registry.all(DomainEntity.class)
                    .filter(e -> !e.isAggregateRoot())
                    .filter(e -> bcInfo.typeNames().contains(e.id().qualifiedName()))
                    .count();

            // Count value objects
            int voCount = (int) registry.all(ValueObject.class)
                    .filter(vo -> bcInfo.typeNames().contains(vo.id().qualifiedName()))
                    .count();

            // Count ports in this bounded context (by package)
            int portCount = (int) Stream.concat(
                            registry.all(DrivingPort.class).map(p -> p.id().qualifiedName()),
                            registry.all(DrivenPort.class).map(p -> p.id().qualifiedName()))
                    .filter(name -> bcInfo.containsPackage(extractPackage(name)))
                    .count();

            // LOC calculation (simplified - use 0 for now as TypeSyntax doesn't expose source location)
            int totalLoc = 0;

            // Capitalize the context name for display
            String displayName = capitalize(bcInfo.name());
            stats.add(new BoundedContextStats(displayName, aggregateCount, entityCount, voCount, portCount, totalLoc));
        }

        // Sort by name for deterministic output
        stats.sort(Comparator.comparing(BoundedContextStats::name));
        return stats;
    }

    /**
     * Extracts the package name from a qualified name.
     */
    private String extractPackage(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
