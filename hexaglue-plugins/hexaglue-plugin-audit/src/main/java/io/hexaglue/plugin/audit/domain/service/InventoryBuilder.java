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
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.SourceRef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
 * @since 1.0.0
 */
public class InventoryBuilder {

    private static final int MAX_EXAMPLES = 3;

    /**
     * Builds a component inventory from the IR snapshot.
     *
     * @param ir the IR snapshot to analyze
     * @return a ComponentInventory with counts by type
     */
    public ComponentInventory build(IrSnapshot ir) {
        Objects.requireNonNull(ir, "ir required");

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

        // Build bounded context statistics
        List<BoundedContextStats> bcStats = buildBoundedContextStats(ir);

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
     * Builds bounded context statistics by inferring BC names from package structure.
     *
     * <p>The bounded context is inferred from the package structure,
     * assuming patterns like: com.example.{bc}.domain.model.Entity
     */
    private List<BoundedContextStats> buildBoundedContextStats(IrSnapshot ir) {
        // Group types by inferred bounded context
        Map<String, List<DomainType>> typesByBc = new HashMap<>();
        for (DomainType type : ir.domain().types()) {
            String bc = inferBoundedContext(type.packageName());
            typesByBc.computeIfAbsent(bc, k -> new ArrayList<>()).add(type);
        }

        // Group ports by inferred bounded context
        Map<String, List<Port>> portsByBc = new HashMap<>();
        for (Port port : ir.ports().ports()) {
            String bc = inferBoundedContext(port.packageName());
            portsByBc.computeIfAbsent(bc, k -> new ArrayList<>()).add(port);
        }

        // Build stats for each bounded context
        List<BoundedContextStats> stats = new ArrayList<>();
        Set<String> allBcs = new HashSet<>(typesByBc.keySet());
        allBcs.addAll(portsByBc.keySet());

        for (String bc : allBcs) {
            List<DomainType> bcTypes = typesByBc.getOrDefault(bc, List.of());
            List<Port> bcPorts = portsByBc.getOrDefault(bc, List.of());

            int aggregates = (int) bcTypes.stream().filter(t -> t.kind() == DomainKind.AGGREGATE_ROOT).count();
            int entities = (int) bcTypes.stream().filter(t -> t.kind() == DomainKind.ENTITY).count();
            int vos = (int) bcTypes.stream().filter(t -> t.kind() == DomainKind.VALUE_OBJECT).count();
            int ports = bcPorts.size();

            // Calculate actual LOC from SourceRef
            int typesLoc = bcTypes.stream().mapToInt(this::calculateLoc).sum();
            int portsLoc = bcPorts.stream().mapToInt(this::calculatePortLoc).sum();
            int totalLoc = typesLoc + portsLoc;

            stats.add(new BoundedContextStats(bc, aggregates, entities, vos, ports, totalLoc));
        }

        // Sort by name
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
     * Infers the bounded context name from a package name.
     *
     * <p>Heuristic: looks for a segment before "domain", "model", "application", or "port".
     * Examples:
     * <ul>
     *   <li>com.example.order.domain.model → Order</li>
     *   <li>com.example.order.domain → Order</li>
     *   <li>com.example.catalog.model → Catalog</li>
     * </ul>
     */
    private String inferBoundedContext(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "Unknown";
        }

        String[] parts = packageName.split("\\.");
        if (parts.length < 2) {
            return capitalize(packageName);
        }

        // Look for pattern: *.{bc}.domain.* or *.{bc}.model.* or *.{bc}.application.*
        for (int i = 0; i < parts.length - 1; i++) {
            String next = parts[i + 1];
            if ("domain".equals(next) || "model".equals(next) || "application".equals(next)
                    || "port".equals(next) || "ports".equals(next)) {
                return capitalize(parts[i]);
            }
        }

        // Fallback: use second-to-last segment (before domain layer package)
        return capitalize(parts[Math.max(0, parts.length - 2)]);
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
