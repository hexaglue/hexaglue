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

package io.hexaglue.core.classification.port.criteria;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Set;

/**
 * Matches interfaces with naming pattern *Repository and CRUD-like methods.
 *
 * <p>Naming patterns:
 * <ul>
 *   <li>Name ends with "Repository" or "Repositories"</li>
 *   <li>Plural name with CRUD methods (e.g., "Orders" with save/find methods)</li>
 * </ul>
 *
 * <p>Priority: 80 (strong heuristic)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVEN
 */
public final class NamingRepositoryCriteria implements PortClassificationCriteria {

    private static final Set<String> CRUD_METHOD_PREFIXES =
            Set.of("save", "find", "delete", "remove", "get", "exists", "count", "update");

    @Override
    public String name() {
        return "naming-repository";
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public PortKind targetKind() {
        return PortKind.REPOSITORY;
    }

    @Override
    public PortDirection targetDirection() {
        return PortDirection.DRIVEN;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be an interface
        if (node.form() != JavaForm.INTERFACE) {
            return MatchResult.noMatch();
        }

        String name = node.simpleName();
        List<MethodNode> methods = query.methodsOf(node);
        List<String> crudMethods = methods.stream()
                .map(MethodNode::simpleName)
                .filter(this::isCrudMethod)
                .toList();

        // Check explicit repository naming pattern
        boolean hasRepositoryName = name.endsWith("Repository") || name.endsWith("Repositories");
        if (hasRepositoryName) {
            if (crudMethods.isEmpty()) {
                // Has repository name but no CRUD methods - still matches with lower confidence
                return MatchResult.match(
                        ConfidenceLevel.MEDIUM,
                        "Interface name ends with 'Repository'",
                        List.of(Evidence.fromNaming("*Repository", name)));
            }

            return MatchResult.match(
                    ConfidenceLevel.HIGH,
                    "Interface '%s' with CRUD methods: %s".formatted(name, String.join(", ", crudMethods)),
                    List.of(
                            Evidence.fromNaming("*Repository", name),
                            Evidence.fromStructure(
                                    "Has CRUD methods: " + String.join(", ", crudMethods), List.of(node.id()))));
        }

        // Check if it's a plural name with CRUD methods (e.g., "Orders" with save/find)
        // This is a common DDD pattern where the repository is named after the plural aggregate
        if (!crudMethods.isEmpty() && isPluralName(name)) {
            return MatchResult.match(
                    ConfidenceLevel.HIGH,
                    "Interface '%s' is a plural name with CRUD methods: %s"
                            .formatted(name, String.join(", ", crudMethods)),
                    List.of(
                            Evidence.fromNaming("Plural + CRUD", name),
                            Evidence.fromStructure(
                                    "Has CRUD methods: " + String.join(", ", crudMethods), List.of(node.id()))));
        }

        return MatchResult.noMatch();
    }

    private boolean isCrudMethod(String methodName) {
        String lowerName = methodName.toLowerCase();
        return CRUD_METHOD_PREFIXES.stream().anyMatch(lowerName::startsWith);
    }

    private boolean isPluralName(String name) {
        // Simple heuristic: plural names typically end with 's' and start with uppercase
        // Examples: Orders, Customers, Products
        return name.length() > 1
                && Character.isUpperCase(name.charAt(0))
                && name.endsWith("s")
                && !name.endsWith("ss"); // Exclude words like "Address"
    }
}
