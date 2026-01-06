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
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Detects CoreAppClass instances from the application graph.
 *
 * <p>A CoreAppClass is a DomainAnchor class that implements or depends on
 * at least one user-code interface. These classes are the "pivots" that
 * enable semantic port classification.
 *
 * <p>Detection logic:
 * <ol>
 *   <li>Only considers DomainAnchor classes (not INFRA or DRIVING anchors)</li>
 *   <li>Finds implemented user-code interfaces</li>
 *   <li>Finds depended user-code interfaces (via constructor/field injection)</li>
 *   <li>Creates CoreAppClass if at least one interface relationship exists</li>
 * </ol>
 */
public final class CoreAppClassDetector {

    private CoreAppClassDetector() {
        // Utility class
    }

    /**
     * Analyzes all types and returns a CoreAppClassIndex.
     *
     * @param graph the application graph
     * @param anchors the anchor context (must be computed first)
     * @return the index of all detected CoreAppClass instances
     */
    public static CoreAppClassIndex analyze(ApplicationGraph graph, AnchorContext anchors) {
        return analyze(graph.query(), anchors);
    }

    /**
     * Analyzes all types via query and returns a CoreAppClassIndex.
     *
     * @param query the graph query
     * @param anchors the anchor context (must be computed first)
     * @return the index of all detected CoreAppClass instances
     */
    public static CoreAppClassIndex analyze(GraphQuery query, AnchorContext anchors) {
        CoreAppClassIndex.Builder builder = CoreAppClassIndex.builder();

        // Only consider classes and records that are DomainAnchors
        query.types(t -> (t.form() == JavaForm.CLASS || t.form() == JavaForm.RECORD) && anchors.isDomainAnchor(t.id()))
                .forEach(type -> {
                    Optional<CoreAppClass> coreApp = detect(type, query, anchors);
                    coreApp.ifPresent(builder::put);
                });

        return builder.build();
    }

    /**
     * Detects if a type is a CoreAppClass.
     *
     * @param type the type to analyze
     * @param query the graph query
     * @param anchors the anchor context
     * @return the CoreAppClass if detected, empty otherwise
     */
    public static Optional<CoreAppClass> detect(TypeNode type, GraphQuery query, AnchorContext anchors) {
        // Must be a DomainAnchor (not infra, not driving)
        if (!anchors.isDomainAnchor(type.id())) {
            return Optional.empty();
        }

        // Find user-code interfaces this class implements
        Set<NodeId> implementedInterfaces = findImplementedUserCodeInterfaces(type, query);

        // Find user-code interfaces this class depends on (via fields)
        Set<NodeId> dependedInterfaces = findDependedUserCodeInterfaces(type, query);

        // Must implement OR depend on at least one user-code interface
        if (implementedInterfaces.isEmpty() && dependedInterfaces.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new CoreAppClass(type.id(), implementedInterfaces, dependedInterfaces));
    }

    /**
     * Finds user-code interfaces implemented by the given type.
     */
    private static Set<NodeId> findImplementedUserCodeInterfaces(TypeNode type, GraphQuery query) {
        Set<NodeId> result = new HashSet<>();

        for (TypeNode iface : query.interfacesOf(type)) {
            if (isUserCodeInterface(iface)) {
                result.add(iface.id());
            }
        }

        return result;
    }

    /**
     * Finds user-code interfaces that the given type depends on via fields.
     *
     * <p>This detects dependency injection patterns where interfaces are
     * injected as constructor parameters or fields.
     */
    private static Set<NodeId> findDependedUserCodeInterfaces(TypeNode type, GraphQuery query) {
        Set<NodeId> result = new HashSet<>();

        for (FieldNode field : query.fieldsOf(type)) {
            String fieldTypeName = field.type().rawQualifiedName();

            // Skip primitives, JDK types, framework types
            if (isExternalType(fieldTypeName)) {
                continue;
            }

            // Look up the field type in the graph
            Optional<TypeNode> fieldType = query.type(fieldTypeName);
            if (fieldType.isPresent() && fieldType.get().isInterface()) {
                if (isUserCodeInterface(fieldType.get())) {
                    result.add(fieldType.get().id());
                }
            }
        }

        return result;
    }

    /**
     * Returns true if the interface is a user-code interface (not from JDK/frameworks).
     */
    private static boolean isUserCodeInterface(TypeNode iface) {
        if (!iface.isInterface()) {
            return false;
        }
        return !isExternalType(iface.qualifiedName());
    }

    /**
     * Returns true if the type is external (JDK, frameworks, etc.).
     */
    private static boolean isExternalType(String qualifiedName) {
        if (qualifiedName == null) {
            return true;
        }
        return qualifiedName.startsWith("java.")
                || qualifiedName.startsWith("javax.")
                || qualifiedName.startsWith("jakarta.")
                || qualifiedName.startsWith("org.springframework.")
                || qualifiedName.startsWith("org.hibernate.")
                || qualifiedName.startsWith("com.fasterxml.")
                || qualifiedName.startsWith("org.slf4j.")
                || qualifiedName.startsWith("org.apache.")
                || qualifiedName.startsWith("com.google.")
                || qualifiedName.startsWith("io.micrometer.")
                || qualifiedName.startsWith("reactor.")
                || qualifiedName.startsWith("rx.")
                || qualifiedName.startsWith("kotlin.");
    }
}
