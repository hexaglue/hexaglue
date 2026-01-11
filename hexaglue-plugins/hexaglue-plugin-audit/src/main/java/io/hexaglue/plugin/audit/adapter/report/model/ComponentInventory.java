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

/**
 * Inventory of architectural components found in the codebase.
 *
 * <p>Provides counts of domain types and ports organized by their DDD/Hexagonal
 * classification. Used in audit reports to give an overview of the application
 * structure.
 *
 * @param aggregateRoots      number of aggregate root classes
 * @param entities            number of entity classes (non-root)
 * @param valueObjects        number of value object classes
 * @param domainEvents        number of domain event classes
 * @param domainServices      number of domain service classes
 * @param applicationServices number of application service classes
 * @param drivingPorts        number of driving (inbound) ports
 * @param drivenPorts         number of driven (outbound) ports
 * @param totalDomainTypes    total count of domain types
 * @param totalPorts          total count of ports
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
        int totalPorts) {

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
        return new ComponentInventory(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
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
                    totalP);
        }
    }
}
