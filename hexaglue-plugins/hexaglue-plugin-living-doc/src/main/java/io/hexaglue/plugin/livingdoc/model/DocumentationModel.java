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

package io.hexaglue.plugin.livingdoc.model;

import java.util.List;
import java.util.Objects;

/**
 * A simplified model for documentation generation.
 *
 * <p>This abstraction allows generators to work with both legacy {@code IrSnapshot}
 * and the new v4 {@code ArchitecturalModel}. Use {@link DocumentationModelFactory}
 * to create instances.
 *
 * @param aggregateRoots the aggregate roots in the domain
 * @param entities the entities (non-aggregate-root)
 * @param valueObjects the value objects
 * @param drivingPorts the driving ports (use cases)
 * @param drivenPorts the driven ports (repositories, gateways)
 * @since 4.0.0
 */
public record DocumentationModel(
        List<DocType> aggregateRoots,
        List<DocType> entities,
        List<DocType> valueObjects,
        List<DocPort> drivingPorts,
        List<DocPort> drivenPorts) {

    public DocumentationModel {
        Objects.requireNonNull(aggregateRoots, "aggregateRoots must not be null");
        Objects.requireNonNull(entities, "entities must not be null");
        Objects.requireNonNull(valueObjects, "valueObjects must not be null");
        Objects.requireNonNull(drivingPorts, "drivingPorts must not be null");
        Objects.requireNonNull(drivenPorts, "drivenPorts must not be null");
        aggregateRoots = List.copyOf(aggregateRoots);
        entities = List.copyOf(entities);
        valueObjects = List.copyOf(valueObjects);
        drivingPorts = List.copyOf(drivingPorts);
        drivenPorts = List.copyOf(drivenPorts);
    }

    /**
     * Returns whether the model is empty.
     *
     * @return true if no types or ports exist
     */
    public boolean isEmpty() {
        return aggregateRoots.isEmpty()
                && entities.isEmpty()
                && valueObjects.isEmpty()
                && drivingPorts.isEmpty()
                && drivenPorts.isEmpty();
    }

    /**
     * Returns all domain types (aggregates, entities, value objects).
     *
     * @return list of all domain types
     */
    public List<DocType> allDomainTypes() {
        return java.util.stream.Stream.of(aggregateRoots.stream(), entities.stream(), valueObjects.stream())
                .flatMap(s -> s)
                .toList();
    }

    /**
     * Returns all ports (driving and driven).
     *
     * @return list of all ports
     */
    public List<DocPort> allPorts() {
        return java.util.stream.Stream.concat(drivingPorts.stream(), drivenPorts.stream())
                .toList();
    }

    /**
     * A simplified domain type for documentation.
     *
     * @param simpleName the simple class name
     * @param packageName the package name
     * @param qualifiedName the fully qualified name
     * @param kind the kind description (e.g., "Aggregate Root", "Entity")
     * @param construct the Java construct (CLASS, RECORD, ENUM)
     * @param propertyCount the number of properties/fields
     * @param classificationReason explanation of why this type was classified
     */
    public record DocType(
            String simpleName,
            String packageName,
            String qualifiedName,
            String kind,
            String construct,
            int propertyCount,
            String classificationReason) {

        public DocType {
            Objects.requireNonNull(simpleName, "simpleName must not be null");
            Objects.requireNonNull(packageName, "packageName must not be null");
            Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
            Objects.requireNonNull(kind, "kind must not be null");
            Objects.requireNonNull(construct, "construct must not be null");
        }
    }

    /**
     * A simplified port for documentation.
     *
     * @param simpleName the simple interface name
     * @param packageName the package name
     * @param qualifiedName the fully qualified name
     * @param kind the kind description (e.g., "Repository", "Use Case")
     * @param direction "DRIVING" or "DRIVEN"
     * @param methodCount the number of methods
     * @param classificationReason explanation of why this port was classified
     */
    public record DocPort(
            String simpleName,
            String packageName,
            String qualifiedName,
            String kind,
            String direction,
            int methodCount,
            String classificationReason) {

        public DocPort {
            Objects.requireNonNull(simpleName, "simpleName must not be null");
            Objects.requireNonNull(packageName, "packageName must not be null");
            Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
            Objects.requireNonNull(kind, "kind must not be null");
            Objects.requireNonNull(direction, "direction must not be null");
        }
    }
}
