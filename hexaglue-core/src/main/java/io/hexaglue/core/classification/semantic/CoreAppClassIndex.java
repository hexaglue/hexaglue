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

package io.hexaglue.core.classification.semantic;

import io.hexaglue.core.graph.model.NodeId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Index of CoreAppClass instances for fast lookups.
 *
 * <p>Provides efficient queries for:
 * <ul>
 *   <li>Finding CoreAppClasses by their class id</li>
 *   <li>Finding CoreAppClasses that implement a given interface</li>
 *   <li>Finding CoreAppClasses that depend on a given interface</li>
 * </ul>
 */
public final class CoreAppClassIndex {

    private final Map<NodeId, CoreAppClass> coreAppClasses;
    private final Map<NodeId, Set<NodeId>> implementorsOfInterface;
    private final Map<NodeId, Set<NodeId>> usersOfInterface;

    private CoreAppClassIndex(Map<NodeId, CoreAppClass> coreAppClasses) {
        this.coreAppClasses = Map.copyOf(coreAppClasses);

        // Build reverse indexes
        this.implementorsOfInterface = buildImplementorsIndex(coreAppClasses);
        this.usersOfInterface = buildUsersIndex(coreAppClasses);
    }

    private static Map<NodeId, Set<NodeId>> buildImplementorsIndex(Map<NodeId, CoreAppClass> coreAppClasses) {
        return coreAppClasses.values().stream()
                .flatMap(core -> core.implementedInterfaces().stream()
                        .map(iface -> Map.entry(iface, core.classId())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toUnmodifiableSet())));
    }

    private static Map<NodeId, Set<NodeId>> buildUsersIndex(Map<NodeId, CoreAppClass> coreAppClasses) {
        return coreAppClasses.values().stream()
                .flatMap(core -> core.dependedInterfaces().stream()
                        .map(iface -> Map.entry(iface, core.classId())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toUnmodifiableSet())));
    }

    /**
     * Creates an empty index.
     */
    public static CoreAppClassIndex empty() {
        return new CoreAppClassIndex(Map.of());
    }

    /**
     * Creates a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the CoreAppClass for the given class id, if any.
     */
    public Optional<CoreAppClass> get(NodeId classId) {
        return Optional.ofNullable(coreAppClasses.get(classId));
    }

    /**
     * Returns true if the given class is a CoreAppClass.
     */
    public boolean isCoreAppClass(NodeId classId) {
        return coreAppClasses.containsKey(classId);
    }

    /**
     * Returns the CoreAppClasses that implement the given interface.
     *
     * <p>This is used to determine DRIVING ports: if an interface is
     * implemented by a CoreAppClass, it's a potential DRIVING port.
     */
    public Set<NodeId> coreImplementorsOf(NodeId interfaceId) {
        return implementorsOfInterface.getOrDefault(interfaceId, Set.of());
    }

    /**
     * Returns the CoreAppClasses that depend on (use) the given interface.
     *
     * <p>This is used to determine DRIVEN ports: if an interface is
     * used by a CoreAppClass, it's a potential DRIVEN port.
     */
    public Set<NodeId> coreUsersOf(NodeId interfaceId) {
        return usersOfInterface.getOrDefault(interfaceId, Set.of());
    }

    /**
     * Returns true if the interface is implemented by at least one CoreAppClass.
     */
    public boolean isImplementedByCore(NodeId interfaceId) {
        return !coreImplementorsOf(interfaceId).isEmpty();
    }

    /**
     * Returns true if the interface is used by at least one CoreAppClass.
     */
    public boolean isUsedByCore(NodeId interfaceId) {
        return !coreUsersOf(interfaceId).isEmpty();
    }

    /**
     * Returns all CoreAppClass instances.
     */
    public Stream<CoreAppClass> all() {
        return coreAppClasses.values().stream();
    }

    /**
     * Returns all pivot CoreAppClasses (implement AND depend on interfaces).
     */
    public Stream<CoreAppClass> pivots() {
        return all().filter(CoreAppClass::isPivot);
    }

    /**
     * Returns all inbound-only CoreAppClasses (implement but don't depend).
     */
    public Stream<CoreAppClass> inboundOnly() {
        return all().filter(CoreAppClass::isInboundOnly);
    }

    /**
     * Returns all outbound-only CoreAppClasses (depend but don't implement).
     */
    public Stream<CoreAppClass> outboundOnly() {
        return all().filter(CoreAppClass::isOutboundOnly);
    }

    /**
     * Returns the total number of CoreAppClasses.
     */
    public int size() {
        return coreAppClasses.size();
    }

    /**
     * Returns the number of interfaces that are implemented by CoreAppClasses.
     */
    public int implementedInterfaceCount() {
        return implementorsOfInterface.size();
    }

    /**
     * Returns the number of interfaces that are used by CoreAppClasses.
     */
    public int usedInterfaceCount() {
        return usersOfInterface.size();
    }

    /**
     * Builder for CoreAppClassIndex.
     */
    public static final class Builder {

        private final Map<NodeId, CoreAppClass> coreAppClasses = new HashMap<>();

        private Builder() {}

        /**
         * Adds a CoreAppClass to the index.
         */
        public Builder put(CoreAppClass coreAppClass) {
            Objects.requireNonNull(coreAppClass, "coreAppClass cannot be null");
            coreAppClasses.put(coreAppClass.classId(), coreAppClass);
            return this;
        }

        /**
         * Builds the index.
         */
        public CoreAppClassIndex build() {
            return new CoreAppClassIndex(coreAppClasses);
        }
    }
}
