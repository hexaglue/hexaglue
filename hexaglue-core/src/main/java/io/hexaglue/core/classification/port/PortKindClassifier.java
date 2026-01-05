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

package io.hexaglue.core.classification.port;

import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Set;

/**
 * Classifies DRIVEN ports into specific kinds based on method signatures.
 *
 * <p>This classifier analyzes the methods of a port interface to determine
 * whether it's a REPOSITORY, GATEWAY, or EVENT_PUBLISHER:
 *
 * <ul>
 *   <li><b>REPOSITORY</b>: Has CRUD methods (save, find, delete, etc.) that
 *       work with domain types</li>
 *   <li><b>EVENT_PUBLISHER</b>: Has publish/emit methods with Event parameters</li>
 *   <li><b>GATEWAY</b>: Default for external system integration (HTTP, messaging, etc.)</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * PortKind kind = PortKindClassifier.classify(portInterface, query);
 * }</pre>
 */
public final class PortKindClassifier {

    // CRUD method prefixes for REPOSITORY detection
    private static final Set<String> REPOSITORY_METHOD_PREFIXES = Set.of(
            "save", "find", "delete", "remove", "get", "exists", "count", "update",
            "persist", "store", "load", "fetch", "retrieve", "add", "insert");

    // Event publisher method prefixes
    private static final Set<String> EVENT_PUBLISHER_METHOD_PREFIXES = Set.of(
            "publish", "emit", "send", "dispatch", "fire", "broadcast", "notify");

    // Event type name patterns
    private static final Set<String> EVENT_TYPE_PATTERNS = Set.of(
            "Event", "DomainEvent", "IntegrationEvent", "Message");

    private PortKindClassifier() {
        // Utility class
    }

    /**
     * Classifies a DRIVEN port into a specific PortKind based on method signatures.
     *
     * @param port the port interface to classify
     * @param query the graph query for method lookups
     * @return the detected PortKind
     */
    public static PortKind classify(TypeNode port, GraphQuery query) {
        List<MethodNode> methods = query.methodsOf(port);

        if (methods.isEmpty()) {
            return PortKind.GENERIC;
        }

        // Check for event publisher first (most specific)
        if (isEventPublisher(methods)) {
            return PortKind.EVENT_PUBLISHER;
        }

        // Check for repository (CRUD operations)
        if (isRepository(methods)) {
            return PortKind.REPOSITORY;
        }

        // Default to GATEWAY for other driven ports
        return PortKind.GATEWAY;
    }

    /**
     * Checks if the port is an event publisher based on method signatures.
     *
     * <p>An event publisher has methods that:
     * <ul>
     *   <li>Start with publish/emit/send/dispatch/fire/broadcast/notify</li>
     *   <li>Have parameters with "Event" in the type name</li>
     * </ul>
     */
    private static boolean isEventPublisher(List<MethodNode> methods) {
        for (MethodNode method : methods) {
            String methodName = method.simpleName().toLowerCase();

            // Check for event publisher method prefixes
            boolean hasPublishPrefix = EVENT_PUBLISHER_METHOD_PREFIXES.stream()
                    .anyMatch(methodName::startsWith);

            if (hasPublishPrefix) {
                // Check if any parameter is an Event type
                for (var param : method.parameters()) {
                    String paramType = param.type().rawQualifiedName();
                    if (isEventType(paramType)) {
                        return true;
                    }
                }
                // Even without Event parameter, publish/emit methods indicate event publisher
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the port is a repository based on method signatures.
     *
     * <p>A repository has CRUD-like methods (save, find, delete, etc.).
     */
    private static boolean isRepository(List<MethodNode> methods) {
        int crudMethodCount = 0;

        for (MethodNode method : methods) {
            String methodName = method.simpleName().toLowerCase();

            boolean hasCrudPrefix = REPOSITORY_METHOD_PREFIXES.stream()
                    .anyMatch(methodName::startsWith);

            if (hasCrudPrefix) {
                crudMethodCount++;
            }
        }

        // At least one CRUD method to be considered a repository
        return crudMethodCount > 0;
    }

    /**
     * Checks if a type name represents an event type.
     */
    private static boolean isEventType(String typeName) {
        if (typeName == null) {
            return false;
        }
        String simpleName = typeName.contains(".")
                ? typeName.substring(typeName.lastIndexOf('.') + 1)
                : typeName;

        return EVENT_TYPE_PATTERNS.stream()
                .anyMatch(simpleName::contains);
    }

    /**
     * Returns true if the method name suggests a CRUD operation.
     */
    public static boolean isCrudMethod(String methodName) {
        String lower = methodName.toLowerCase();
        return REPOSITORY_METHOD_PREFIXES.stream().anyMatch(lower::startsWith);
    }

    /**
     * Returns true if the method name suggests an event publishing operation.
     */
    public static boolean isEventPublishMethod(String methodName) {
        String lower = methodName.toLowerCase();
        return EVENT_PUBLISHER_METHOD_PREFIXES.stream().anyMatch(lower::startsWith);
    }
}
