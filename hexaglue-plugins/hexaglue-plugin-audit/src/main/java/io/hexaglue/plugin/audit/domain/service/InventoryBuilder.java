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

import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory.BoundedContextStats;
import io.hexaglue.plugin.audit.adapter.report.model.PortMatrixEntry;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.BoundedContextInfo;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.SourceRef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds component inventory from the IR snapshot.
 *
 * <p>This service analyzes the IrSnapshot to count and categorize all domain
 * types and ports, producing a ComponentInventory for reporting.
 *
 * <p>Bounded contexts are obtained from the core's {@link ArchitectureQuery#findBoundedContexts()},
 * ensuring consistent detection across the entire HexaGlue pipeline. This follows the architectural
 * principle: "the core generates data, plugins consume it".
 *
 * @since 1.0.0
 */
public class InventoryBuilder {

    private static final int MAX_EXAMPLES = 3;

    /**
     * Builds a component inventory from the IR snapshot using the architecture query.
     *
     * <p>Bounded contexts are obtained from {@link ArchitectureQuery#findBoundedContexts()},
     * ensuring consistency with the core's analysis.
     *
     * @param ir                the IR snapshot to analyze
     * @param architectureQuery the architecture query for bounded context detection
     * @return a ComponentInventory with counts by type
     * @throws NullPointerException if ir or architectureQuery is null
     */
    public ComponentInventory build(IrSnapshot ir, ArchitectureQuery architectureQuery) {
        Objects.requireNonNull(ir, "ir required");
        Objects.requireNonNull(architectureQuery, "architectureQuery required");

        // Group domain types by kind
        Map<DomainKind, List<DomainType>> typesByKind = ir.domain().types().stream()
                .collect(Collectors.groupingBy(DomainType::kind));

        // Group ports by direction
        Map<PortDirection, List<Port>> portsByDirection = ir.ports().ports().stream()
                .collect(Collectors.groupingBy(Port::direction));

        // Extract examples (simple names, limited)
        List<String> aggregateExamples = extractTypeExamples(typesByKind.get(DomainKind.AGGREGATE_ROOT));
        List<String> entityExamples = extractTypeExamples(typesByKind.get(DomainKind.ENTITY));
        List<String> voExamples = extractTypeExamples(typesByKind.get(DomainKind.VALUE_OBJECT));
        List<String> eventExamples = extractTypeExamples(typesByKind.get(DomainKind.DOMAIN_EVENT));
        List<String> serviceExamples = extractTypeExamples(typesByKind.get(DomainKind.DOMAIN_SERVICE));
        List<String> drivingExamples = extractPortExamples(portsByDirection.get(PortDirection.DRIVING));
        List<String> drivenExamples = extractPortExamples(portsByDirection.get(PortDirection.DRIVEN));

        // Build bounded context statistics from architecture query
        List<BoundedContextStats> bcStats = buildBoundedContextStats(ir, architectureQuery);

        return ComponentInventory.builder()
                .aggregateRoots(countTypes(typesByKind, DomainKind.AGGREGATE_ROOT))
                .entities(countTypes(typesByKind, DomainKind.ENTITY))
                .valueObjects(countTypes(typesByKind, DomainKind.VALUE_OBJECT))
                .domainEvents(countTypes(typesByKind, DomainKind.DOMAIN_EVENT))
                .domainServices(countTypes(typesByKind, DomainKind.DOMAIN_SERVICE))
                .applicationServices(
                        countTypes(typesByKind, DomainKind.APPLICATION_SERVICE)
                                + countTypes(typesByKind, DomainKind.INBOUND_ONLY)
                                + countTypes(typesByKind, DomainKind.OUTBOUND_ONLY)
                                + countTypes(typesByKind, DomainKind.SAGA))
                .drivingPorts(countPorts(portsByDirection, PortDirection.DRIVING))
                .drivenPorts(countPorts(portsByDirection, PortDirection.DRIVEN))
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
     * Builds a port matrix with adapter coverage information.
     *
     * @param ir              the IR snapshot to analyze
     * @param portsWithAdapter set of port qualified names that have adapters
     * @return list of port matrix entries
     */
    public List<PortMatrixEntry> buildPortMatrix(IrSnapshot ir, Set<String> portsWithAdapter) {
        Objects.requireNonNull(ir, "ir required");
        Set<String> adapters = portsWithAdapter != null ? portsWithAdapter : Set.of();

        return ir.ports().ports().stream()
                .map(port -> PortMatrixEntry.from(port, adapters.contains(port.qualifiedName())))
                .toList();
    }

    /**
     * Extracts up to MAX_EXAMPLES simple names from a list of domain types.
     */
    private List<String> extractTypeExamples(List<DomainType> types) {
        if (types == null || types.isEmpty()) {
            return List.of();
        }
        return types.stream()
                .map(DomainType::simpleName)
                .sorted()
                .limit(MAX_EXAMPLES)
                .toList();
    }

    /**
     * Extracts up to MAX_EXAMPLES simple names from a list of ports.
     */
    private List<String> extractPortExamples(List<Port> ports) {
        if (ports == null || ports.isEmpty()) {
            return List.of();
        }
        return ports.stream()
                .map(Port::simpleName)
                .sorted()
                .limit(MAX_EXAMPLES)
                .toList();
    }

    /**
     * Builds bounded context statistics using the core's ArchitectureQuery.
     *
     * <p>This method obtains bounded contexts from {@link ArchitectureQuery#findBoundedContexts()}
     * and correlates them with domain types and ports from the IR snapshot.
     *
     * @param ir                the IR snapshot
     * @param architectureQuery the architecture query
     * @return list of bounded context statistics
     */
    private List<BoundedContextStats> buildBoundedContextStats(IrSnapshot ir, ArchitectureQuery architectureQuery) {
        List<BoundedContextInfo> boundedContexts = architectureQuery.findBoundedContexts();

        // Create a lookup map: qualified type name -> DomainType
        Map<String, DomainType> typeByName = ir.domain().types().stream()
                .collect(Collectors.toMap(DomainType::qualifiedName, t -> t, (a, b) -> a));

        List<BoundedContextStats> stats = new ArrayList<>();

        for (BoundedContextInfo bcInfo : boundedContexts) {
            // Collect domain types that belong to this bounded context
            List<DomainType> bcTypes = bcInfo.typeNames().stream()
                    .map(typeByName::get)
                    .filter(Objects::nonNull)
                    .toList();

            // Collect ports that belong to this bounded context (by package)
            List<Port> bcPorts = ir.ports().ports().stream()
                    .filter(port -> bcInfo.containsPackage(port.packageName()))
                    .toList();

            int aggregates = (int) bcTypes.stream().filter(t -> t.kind() == DomainKind.AGGREGATE_ROOT).count();
            int entities = (int) bcTypes.stream().filter(t -> t.kind() == DomainKind.ENTITY).count();
            int vos = (int) bcTypes.stream().filter(t -> t.kind() == DomainKind.VALUE_OBJECT).count();
            int ports = bcPorts.size();

            // Calculate actual LOC from SourceRef
            int typesLoc = bcTypes.stream().mapToInt(this::calculateLoc).sum();
            int portsLoc = bcPorts.stream().mapToInt(this::calculatePortLoc).sum();
            int totalLoc = typesLoc + portsLoc;

            // Capitalize the context name for display
            String displayName = capitalize(bcInfo.name());
            stats.add(new BoundedContextStats(displayName, aggregates, entities, vos, ports, totalLoc));
        }

        // Sort by name for deterministic output
        stats.sort(Comparator.comparing(BoundedContextStats::name));
        return stats;
    }

    /**
     * Calculates lines of code for a domain type from its SourceRef.
     */
    private int calculateLoc(DomainType type) {
        SourceRef ref = type.sourceRef();
        if (ref == null || ref.isUnknown()) {
            return 0;
        }
        return Math.max(0, ref.lineEnd() - ref.lineStart() + 1);
    }

    /**
     * Calculates lines of code for a port from its SourceRef.
     */
    private int calculatePortLoc(Port port) {
        SourceRef ref = port.sourceRef();
        if (ref == null || ref.isUnknown()) {
            return 0;
        }
        return Math.max(0, ref.lineEnd() - ref.lineStart() + 1);
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

    private int countTypes(Map<DomainKind, List<DomainType>> map, DomainKind kind) {
        List<DomainType> list = map.get(kind);
        return list != null ? list.size() : 0;
    }

    private int countPorts(Map<PortDirection, List<Port>> map, PortDirection direction) {
        List<Port> list = map.get(direction);
        return list != null ? list.size() : 0;
    }
}
