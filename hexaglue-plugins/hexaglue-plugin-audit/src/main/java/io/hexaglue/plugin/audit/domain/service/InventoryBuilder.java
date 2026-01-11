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
import io.hexaglue.plugin.audit.adapter.report.model.PortMatrixEntry;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
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

    /**
     * Builds a component inventory from the IR snapshot.
     *
     * @param ir the IR snapshot to analyze
     * @return a ComponentInventory with counts by type
     */
    public ComponentInventory build(IrSnapshot ir) {
        Objects.requireNonNull(ir, "ir required");

        // Count domain types by kind
        Map<DomainKind, Long> domainCounts = ir.domain().types().stream()
                .collect(Collectors.groupingBy(t -> t.kind(), Collectors.counting()));

        // Count ports by direction
        Map<PortDirection, Long> portCounts = ir.ports().ports().stream()
                .collect(Collectors.groupingBy(Port::direction, Collectors.counting()));

        return ComponentInventory.builder()
                .aggregateRoots(count(domainCounts, DomainKind.AGGREGATE_ROOT))
                .entities(count(domainCounts, DomainKind.ENTITY))
                .valueObjects(count(domainCounts, DomainKind.VALUE_OBJECT))
                .domainEvents(count(domainCounts, DomainKind.DOMAIN_EVENT))
                .domainServices(count(domainCounts, DomainKind.DOMAIN_SERVICE))
                .applicationServices(count(domainCounts, DomainKind.APPLICATION_SERVICE)
                        + count(domainCounts, DomainKind.INBOUND_ONLY)
                        + count(domainCounts, DomainKind.OUTBOUND_ONLY)
                        + count(domainCounts, DomainKind.SAGA))
                .drivingPorts(count(portCounts, PortDirection.DRIVING))
                .drivenPorts(count(portCounts, PortDirection.DRIVEN))
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

    private static int count(Map<DomainKind, Long> counts, DomainKind kind) {
        return counts.getOrDefault(kind, 0L).intValue();
    }

    private static int count(Map<PortDirection, Long> counts, PortDirection direction) {
        return counts.getOrDefault(direction, 0L).intValue();
    }
}
