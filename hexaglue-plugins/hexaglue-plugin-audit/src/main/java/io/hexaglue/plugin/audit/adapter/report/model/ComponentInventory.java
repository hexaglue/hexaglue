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

package io.hexaglue.plugin.audit.adapter.report.model;

import java.util.List;
import java.util.Objects;

/**
 * Inventory of architectural components found in the codebase.
 *
 * <p>Provides counts of domain types and ports organized by their DDD/Hexagonal
 * classification, along with example names and bounded context breakdown.
 * Used in audit reports to give an overview of the application structure.
 *
 * @param aggregateRoots       number of aggregate root classes
 * @param entities             number of entity classes (non-root)
 * @param valueObjects         number of value object classes
 * @param domainEvents         number of domain event classes
 * @param domainServices       number of domain service classes
 * @param applicationServices  number of application service classes
 * @param drivingPorts         number of driving (inbound) ports
 * @param drivenPorts          number of driven (outbound) ports
 * @param totalDomainTypes     total count of domain types
 * @param totalPorts           total count of ports
 * @param aggregateExamples    example aggregate root names (up to 3)
 * @param entityExamples       example entity names (up to 3)
 * @param valueObjectExamples  example value object names (up to 3)
 * @param domainEventExamples  example domain event names (up to 3)
 * @param domainServiceExamples example domain service names (up to 3)
 * @param drivingPortExamples  example driving port names (up to 3)
 * @param drivenPortExamples   example driven port names (up to 3)
 * @param boundedContexts      bounded context breakdown statistics
 * @since 1.0.0
 */
public record ComponentInventory(
        int aggregateRoots,
        int entities,
        int valueObjects,
        int domainEvents,
        int domainServices,
        int applicationServices,
        int drivingPorts,
        int drivenPorts,
        int totalDomainTypes,
        int totalPorts,
        List<String> aggregateExamples,
        List<String> entityExamples,
        List<String> valueObjectExamples,
        List<String> domainEventExamples,
        List<String> domainServiceExamples,
        List<String> drivingPortExamples,
        List<String> drivenPortExamples,
        List<BoundedContextStats> boundedContexts) {

    public ComponentInventory {
        validateNonNegative("aggregateRoots", aggregateRoots);
        validateNonNegative("entities", entities);
        validateNonNegative("valueObjects", valueObjects);
        validateNonNegative("domainEvents", domainEvents);
        validateNonNegative("domainServices", domainServices);
        validateNonNegative("applicationServices", applicationServices);
        validateNonNegative("drivingPorts", drivingPorts);
        validateNonNegative("drivenPorts", drivenPorts);
        validateNonNegative("totalDomainTypes", totalDomainTypes);
        validateNonNegative("totalPorts", totalPorts);
        aggregateExamples = aggregateExamples != null ? List.copyOf(aggregateExamples) : List.of();
        entityExamples = entityExamples != null ? List.copyOf(entityExamples) : List.of();
        valueObjectExamples = valueObjectExamples != null ? List.copyOf(valueObjectExamples) : List.of();
        domainEventExamples = domainEventExamples != null ? List.copyOf(domainEventExamples) : List.of();
        domainServiceExamples = domainServiceExamples != null ? List.copyOf(domainServiceExamples) : List.of();
        drivingPortExamples = drivingPortExamples != null ? List.copyOf(drivingPortExamples) : List.of();
        drivenPortExamples = drivenPortExamples != null ? List.copyOf(drivenPortExamples) : List.of();
        boundedContexts = boundedContexts != null ? List.copyOf(boundedContexts) : List.of();
    }

    private static void validateNonNegative(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative: " + value);
        }
    }

    /**
     * Returns an empty inventory with all counts at zero.
     *
     * @return a ComponentInventory with all values set to 0
     */
    public static ComponentInventory empty() {
        return new ComponentInventory(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Creates a builder for constructing a ComponentInventory.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Formats a list of examples as a comma-separated string with ellipsis if truncated.
     *
     * @param examples the list of examples
     * @param total    the total count
     * @return formatted string like "Order, Product, Customer, ..."
     */
    public static String formatExamples(List<String> examples, int total) {
        if (examples.isEmpty()) {
            return "–";
        }
        String joined = String.join(", ", examples);
        if (total > examples.size()) {
            return joined + ", ...";
        }
        return joined;
    }

    /**
     * Statistics for a single bounded context.
     *
     * @param name        the bounded context name
     * @param aggregates  number of aggregate roots
     * @param entities    number of entities
     * @param valueObjects number of value objects
     * @param ports       number of ports
     * @param estimatedLoc estimated lines of code
     */
    public record BoundedContextStats(
            String name,
            int aggregates,
            int entities,
            int valueObjects,
            int ports,
            int estimatedLoc) {

        public BoundedContextStats {
            Objects.requireNonNull(name, "name required");
        }
    }

    /**
     * Builder for ComponentInventory.
     */
    public static final class Builder {
        private int aggregateRoots;
        private int entities;
        private int valueObjects;
        private int domainEvents;
        private int domainServices;
        private int applicationServices;
        private int drivingPorts;
        private int drivenPorts;
        private List<String> aggregateExamples = List.of();
        private List<String> entityExamples = List.of();
        private List<String> valueObjectExamples = List.of();
        private List<String> domainEventExamples = List.of();
        private List<String> domainServiceExamples = List.of();
        private List<String> drivingPortExamples = List.of();
        private List<String> drivenPortExamples = List.of();
        private List<BoundedContextStats> boundedContexts = List.of();

        public Builder aggregateRoots(int count) {
            this.aggregateRoots = count;
            return this;
        }

        public Builder entities(int count) {
            this.entities = count;
            return this;
        }

        public Builder valueObjects(int count) {
            this.valueObjects = count;
            return this;
        }

        public Builder domainEvents(int count) {
            this.domainEvents = count;
            return this;
        }

        public Builder domainServices(int count) {
            this.domainServices = count;
            return this;
        }

        public Builder applicationServices(int count) {
            this.applicationServices = count;
            return this;
        }

        public Builder drivingPorts(int count) {
            this.drivingPorts = count;
            return this;
        }

        public Builder drivenPorts(int count) {
            this.drivenPorts = count;
            return this;
        }

        public Builder aggregateExamples(List<String> examples) {
            this.aggregateExamples = examples;
            return this;
        }

        public Builder entityExamples(List<String> examples) {
            this.entityExamples = examples;
            return this;
        }

        public Builder valueObjectExamples(List<String> examples) {
            this.valueObjectExamples = examples;
            return this;
        }

        public Builder domainEventExamples(List<String> examples) {
            this.domainEventExamples = examples;
            return this;
        }

        public Builder domainServiceExamples(List<String> examples) {
            this.domainServiceExamples = examples;
            return this;
        }

        public Builder drivingPortExamples(List<String> examples) {
            this.drivingPortExamples = examples;
            return this;
        }

        public Builder drivenPortExamples(List<String> examples) {
            this.drivenPortExamples = examples;
            return this;
        }

        public Builder boundedContexts(List<BoundedContextStats> contexts) {
            this.boundedContexts = contexts;
            return this;
        }

        /**
         * Builds the ComponentInventory, computing totals automatically.
         *
         * @return a new ComponentInventory
         */
        public ComponentInventory build() {
            int totalDomain = aggregateRoots + entities + valueObjects + domainEvents + domainServices
                    + applicationServices;
            int totalP = drivingPorts + drivenPorts;
            return new ComponentInventory(
                    aggregateRoots,
                    entities,
                    valueObjects,
                    domainEvents,
                    domainServices,
                    applicationServices,
                    drivingPorts,
                    drivenPorts,
                    totalDomain,
                    totalP,
                    aggregateExamples,
                    entityExamples,
                    valueObjectExamples,
                    domainEventExamples,
                    domainServiceExamples,
                    drivingPortExamples,
                    drivenPortExamples,
                    boundedContexts);
        }
    }
}
