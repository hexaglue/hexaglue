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

package io.hexaglue.arch.model.query;

import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.PortType;
import io.hexaglue.arch.model.index.PortIndex;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Fluent query builder for ports (driving and driven).
 *
 * <p>Provides chainable filters for finding ports matching specific criteria.
 * All filter operations return new query instances (immutable).</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Query driving ports with use cases
 * List<DrivingPort> useCasePorts = query.drivingPorts()
 *     .withUseCases()
 *     .toList();
 *
 * // Query repositories
 * List<DrivenPort> repositories = query.drivenPorts()
 *     .repositories()
 *     .toList();
 *
 * // Query all ports in a package
 * List<PortType> orderPorts = query.allPorts()
 *     .inPackage("com.example.order")
 *     .toList();
 * }</pre>
 *
 * @since 5.0.0
 */
public final class PortQuery {

    private final PortIndex portIndex;
    private final PortSource source;
    private final Predicate<PortType> filter;

    private enum PortSource {
        DRIVING,
        DRIVEN,
        ALL
    }

    private PortQuery(PortIndex portIndex, PortSource source, Predicate<PortType> filter) {
        this.portIndex = Objects.requireNonNull(portIndex, "portIndex must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.filter = Objects.requireNonNull(filter, "filter must not be null");
    }

    /**
     * Creates a query for driving ports.
     *
     * @param portIndex the port index
     * @return a new PortQuery for driving ports
     */
    static PortQuery drivingPorts(PortIndex portIndex) {
        return new PortQuery(portIndex, PortSource.DRIVING, p -> true);
    }

    /**
     * Creates a query for driven ports.
     *
     * @param portIndex the port index
     * @return a new PortQuery for driven ports
     */
    static PortQuery drivenPorts(PortIndex portIndex) {
        return new PortQuery(portIndex, PortSource.DRIVEN, p -> true);
    }

    /**
     * Creates a query for all ports.
     *
     * @param portIndex the port index
     * @return a new PortQuery for all ports
     */
    static PortQuery allPorts(PortIndex portIndex) {
        return new PortQuery(portIndex, PortSource.ALL, p -> true);
    }

    /**
     * Filters driving ports that have use cases defined.
     *
     * @return a new query with the filter applied
     */
    public PortQuery withUseCases() {
        return addFilter(p -> {
            if (p instanceof DrivingPort dp) {
                return dp.hasUseCases();
            }
            return false;
        });
    }

    /**
     * Filters driven ports that are repositories.
     *
     * @return a new query with the filter applied
     */
    public PortQuery repositories() {
        return addFilter(p -> {
            if (p instanceof DrivenPort dp) {
                return dp.portType() == DrivenPortType.REPOSITORY;
            }
            return false;
        });
    }

    /**
     * Filters driven ports that are gateways.
     *
     * @return a new query with the filter applied
     */
    public PortQuery gateways() {
        return addFilter(p -> {
            if (p instanceof DrivenPort dp) {
                return dp.portType() == DrivenPortType.GATEWAY;
            }
            return false;
        });
    }

    /**
     * Filters driven ports that are event publishers.
     *
     * @return a new query with the filter applied
     */
    public PortQuery eventPublishers() {
        return addFilter(p -> {
            if (p instanceof DrivenPort dp) {
                return dp.portType() == DrivenPortType.EVENT_PUBLISHER;
            }
            return false;
        });
    }

    /**
     * Filters ports in a specific package.
     *
     * @param packageName the package name to match (exact match)
     * @return a new query with the filter applied
     */
    public PortQuery inPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName must not be null");
        return addFilter(p -> packageName.equals(p.packageName()));
    }

    /**
     * Filters ports in a package or its subpackages.
     *
     * @param packagePrefix the package prefix to match
     * @return a new query with the filter applied
     */
    public PortQuery inPackageTree(String packagePrefix) {
        Objects.requireNonNull(packagePrefix, "packagePrefix must not be null");
        return addFilter(p -> {
            String pkg = p.packageName();
            return pkg.equals(packagePrefix) || pkg.startsWith(packagePrefix + ".");
        });
    }

    /**
     * Filters ports whose name matches a pattern.
     *
     * @param namePattern a regex pattern to match against simple name
     * @return a new query with the filter applied
     */
    public PortQuery nameMatches(String namePattern) {
        Objects.requireNonNull(namePattern, "namePattern must not be null");
        return addFilter(p -> p.simpleName().matches(namePattern));
    }

    /**
     * Applies a custom filter predicate.
     *
     * @param predicate the filter predicate
     * @return a new query with the filter applied
     */
    public PortQuery where(Predicate<PortType> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return addFilter(predicate);
    }

    /**
     * Returns a stream of matching ports.
     *
     * @return a stream of ports matching all filters
     */
    public Stream<PortType> stream() {
        return sourceStream().filter(filter);
    }

    /**
     * Returns a list of matching ports.
     *
     * @return an unmodifiable list of matching ports
     */
    public List<PortType> toList() {
        return stream().toList();
    }

    /**
     * Returns the count of matching ports.
     *
     * @return the count
     */
    public long count() {
        return stream().count();
    }

    /**
     * Returns whether any port matches the filters.
     *
     * @return true if at least one port matches
     */
    public boolean exists() {
        return stream().findAny().isPresent();
    }

    /**
     * Returns whether no port matches the filters.
     *
     * @return true if no port matches
     */
    public boolean isEmpty() {
        return !exists();
    }

    private Stream<PortType> sourceStream() {
        return switch (source) {
            case DRIVING -> portIndex.drivingPorts().map(p -> (PortType) p);
            case DRIVEN -> portIndex.drivenPorts().map(p -> (PortType) p);
            case ALL ->
                Stream.concat(
                        portIndex.drivingPorts().map(p -> (PortType) p),
                        portIndex.drivenPorts().map(p -> (PortType) p));
        };
    }

    private PortQuery addFilter(Predicate<PortType> additional) {
        return new PortQuery(portIndex, source, filter.and(additional));
    }
}
