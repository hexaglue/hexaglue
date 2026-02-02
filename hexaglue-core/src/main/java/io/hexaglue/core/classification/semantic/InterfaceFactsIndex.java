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

import io.hexaglue.core.classification.anchor.AnchorContext;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Computes and indexes InterfaceFacts for all user-code interfaces.
 *
 * <p>This is the key component for semantic port classification. It computes
 * facts about each interface based on:
 * <ul>
 *   <li>Implementation status (missing, internal-only, etc.)</li>
 *   <li>Usage by CoreAppClasses (implemented, depended)</li>
 *   <li>Presence of port annotations (jMolecules only)</li>
 * </ul>
 */
public final class InterfaceFactsIndex {

    /**
     * jMolecules port-related annotation qualified names.
     *
     * <p>Only jMolecules annotations are supported:
     * <ul>
     *   <li>{@code @PrimaryPort} - Marks a driving (inbound) port</li>
     *   <li>{@code @SecondaryPort} - Marks a driven (outbound) port</li>
     *   <li>{@code @Repository} - Marks a repository port</li>
     * </ul>
     */
    private static final Set<String> PORT_ANNOTATIONS = Set.of(
            "org.jmolecules.architecture.hexagonal.PrimaryPort",
            "org.jmolecules.architecture.hexagonal.SecondaryPort",
            "org.jmolecules.ddd.annotation.Repository");

    private final Map<NodeId, InterfaceFacts> facts;

    private InterfaceFactsIndex(Map<NodeId, InterfaceFacts> facts) {
        this.facts = Map.copyOf(facts);
    }

    /**
     * Builds an InterfaceFactsIndex by analyzing all interfaces in the graph.
     *
     * @param graph the application graph
     * @param anchors the anchor context
     * @param coreIndex the CoreAppClass index
     * @return the built index
     */
    public static InterfaceFactsIndex build(
            ApplicationGraph graph, AnchorContext anchors, CoreAppClassIndex coreIndex) {
        return build(graph.query(), anchors, coreIndex);
    }

    /**
     * Builds an InterfaceFactsIndex by analyzing all interfaces via query.
     *
     * @param query the graph query
     * @param anchors the anchor context
     * @param coreIndex the CoreAppClass index
     * @return the built index
     */
    public static InterfaceFactsIndex build(GraphQuery query, AnchorContext anchors, CoreAppClassIndex coreIndex) {
        Map<NodeId, InterfaceFacts> facts = new HashMap<>();

        query.types(t -> t.form() == JavaForm.INTERFACE).forEach(iface -> {
            // Skip external interfaces (JDK, frameworks)
            if (isExternalInterface(iface)) {
                return;
            }

            InterfaceFacts interfaceFacts = computeFacts(iface, query, anchors, coreIndex);
            facts.put(iface.id(), interfaceFacts);
        });

        return new InterfaceFactsIndex(facts);
    }

    /**
     * Computes InterfaceFacts for a single interface.
     */
    private static InterfaceFacts computeFacts(
            TypeNode iface, GraphQuery query, AnchorContext anchors, CoreAppClassIndex coreIndex) {

        NodeId interfaceId = iface.id();

        // Count production implementations (exclude test code)
        // Note: In a real implementation, we'd filter by source set (prod vs test)
        // For now, we count all implementors that are not in test packages
        long implsProdCount = query.implementorsOf(iface).stream()
                .filter(impl -> !isTestCode(impl))
                .count();

        // Check if all implementations are domain anchors (internal implementations)
        boolean internalImplOnly = implsProdCount > 0
                && query.implementorsOf(iface).stream()
                        .filter(impl -> !isTestCode(impl))
                        .allMatch(impl -> anchors.isDomainAnchor(impl.id()));

        // Check if all implementations are infrastructure anchors (adapters)
        boolean infraImplOnly = implsProdCount > 0
                && query.implementorsOf(iface).stream()
                        .filter(impl -> !isTestCode(impl))
                        .allMatch(impl -> anchors.isInfraAnchor(impl.id()));

        // Check CoreAppClass relationships
        boolean usedByCore = coreIndex.isUsedByCore(interfaceId);
        boolean implementedByCore = coreIndex.isImplementedByCore(interfaceId);

        // Check for jMolecules port annotations
        boolean hasPortAnnotation = hasPortAnnotation(iface);

        return new InterfaceFacts(
                interfaceId,
                (int) implsProdCount,
                implsProdCount == 0,
                internalImplOnly,
                infraImplOnly,
                usedByCore,
                implementedByCore,
                hasPortAnnotation);
    }

    /**
     * Port naming patterns for safety check (fallback when no jMolecules annotations).
     *
     * <p>These patterns indicate that an interface is likely a port:
     * <ul>
     *   <li>Repository, Saver, Fetcher, Finder - data access patterns</li>
     *   <li>Gateway, Client, Adapter - external system patterns</li>
     *   <li>Publisher, Sender, Emitter - event patterns</li>
     * </ul>
     */
    private static final Set<String> PORT_NAME_PATTERNS = Set.of(
            "Repository",
            "Saver",
            "Fetcher",
            "Finder",
            "Loader",
            "Store",
            "Gateway",
            "Client",
            "Adapter",
            "Publisher",
            "Sender",
            "Emitter",
            "Port");

    /**
     * Returns true if the interface has port indicators for the safety check.
     *
     * <p>Port indicators include (in order of priority):
     * <ol>
     *   <li>jMolecules annotations (@PrimaryPort, @SecondaryPort, @Repository)</li>
     *   <li>Package name contains "port" (e.g., ports.in, ports.out, port.driven)</li>
     *   <li>Interface name contains port patterns (Repository, Gateway, etc.)</li>
     * </ol>
     *
     * <p>Supported annotations:
     * <ul>
     *   <li>{@code @org.jmolecules.architecture.hexagonal.PrimaryPort}</li>
     *   <li>{@code @org.jmolecules.architecture.hexagonal.SecondaryPort}</li>
     *   <li>{@code @org.jmolecules.ddd.annotation.Repository}</li>
     * </ul>
     */
    private static boolean hasPortAnnotation(TypeNode iface) {
        // 1. Check for jMolecules annotations (highest priority)
        for (AnnotationRef annotation : iface.annotations()) {
            if (PORT_ANNOTATIONS.contains(annotation.qualifiedName())) {
                return true;
            }
        }

        // 2. Check if package contains "port" (e.g., ports.in, ports.out)
        String packageName = iface.packageName().toLowerCase();
        if (packageName.contains("port")) {
            return true;
        }

        // 3. Check if interface name contains port patterns (fallback)
        String interfaceName = iface.simpleName();
        for (String pattern : PORT_NAME_PATTERNS) {
            if (interfaceName.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the type is in test code.
     */
    private static boolean isTestCode(TypeNode type) {
        String pkg = type.packageName();
        return pkg.contains(".test.") || pkg.endsWith(".test") || pkg.contains(".tests.") || pkg.endsWith(".tests");
    }

    /**
     * Returns true if the interface is external (JDK, frameworks).
     */
    private static boolean isExternalInterface(TypeNode iface) {
        String name = iface.qualifiedName();
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jakarta.")
                || name.startsWith("org.springframework.")
                || name.startsWith("org.hibernate.");
    }

    /**
     * Creates an empty index.
     */
    public static InterfaceFactsIndex empty() {
        return new InterfaceFactsIndex(Map.of());
    }

    /**
     * Creates an InterfaceFactsIndex from a list of facts.
     *
     * <p>Useful for testing scenarios where the full graph analysis is not needed.
     *
     * @param facts the list of InterfaceFacts
     * @return the built index
     * @since 5.0.0
     */
    public static InterfaceFactsIndex fromFacts(java.util.List<InterfaceFacts> facts) {
        Map<NodeId, InterfaceFacts> factsMap = new HashMap<>();
        for (InterfaceFacts f : facts) {
            factsMap.put(f.interfaceId(), f);
        }
        return new InterfaceFactsIndex(factsMap);
    }

    /**
     * Returns the InterfaceFacts for the given interface, if any.
     */
    public Optional<InterfaceFacts> get(NodeId interfaceId) {
        return Optional.ofNullable(facts.get(interfaceId));
    }

    /**
     * Returns all InterfaceFacts.
     */
    public Stream<InterfaceFacts> all() {
        return facts.values().stream();
    }

    /**
     * Returns all interfaces that are DRIVING port candidates.
     */
    public Stream<InterfaceFacts> drivingPorts() {
        return all().filter(InterfaceFacts::isDrivingPortCandidate);
    }

    /**
     * Returns all interfaces that are DRIVEN port candidates.
     */
    public Stream<InterfaceFacts> drivenPorts() {
        return all().filter(InterfaceFacts::isDrivenPortCandidate);
    }

    /**
     * Returns all interfaces that are DRIVEN port candidates (without annotation check).
     */
    public Stream<InterfaceFacts> drivenPortsWithoutAnnotationCheck() {
        return all().filter(InterfaceFacts::isDrivenPortCandidateWithoutAnnotationCheck);
    }

    /**
     * Returns all internal interfaces (implemented by domain code but not ports).
     */
    public Stream<InterfaceFacts> internalInterfaces() {
        return all().filter(InterfaceFacts::isInternalInterface);
    }

    /**
     * Returns all undecided interfaces.
     */
    public Stream<InterfaceFacts> undecidedInterfaces() {
        return all().filter(InterfaceFacts::isUndecided);
    }

    /**
     * Returns the number of interfaces in the index.
     */
    public int size() {
        return facts.size();
    }

    /**
     * Returns the number of DRIVING port candidates.
     */
    public long drivingPortCount() {
        return drivingPorts().count();
    }

    /**
     * Returns the number of DRIVEN port candidates.
     */
    public long drivenPortCount() {
        return drivenPorts().count();
    }
}
