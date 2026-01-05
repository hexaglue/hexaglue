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

package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Matches classes that are stateless (or minimal state) with no port dependencies.
 *
 * <p>This criteria detects domain services by analyzing the structure
 * of a class. A domain service:
 * <ul>
 *   <li>Has no port dependencies (no interface-typed fields)</li>
 *   <li>Is stateless or has only final fields (configuration)</li>
 *   <li>Has methods that perform domain logic</li>
 *   <li>Has a name ending with "Service", "Calculator", "Validator", etc.</li>
 * </ul>
 *
 * <p>Key distinction from APPLICATION_SERVICE: domain services have
 * NO port dependencies.
 *
 * <p>Priority: 55 (heuristic, lower than HasPortDependenciesCriteria)
 * <p>Confidence: MEDIUM
 */
public final class StatelessNoDependenciesCriteria implements ClassificationCriteria<DomainKind> {

    @Override
    public String name() {
        return "stateless-no-dependencies";
    }

    @Override
    public int priority() {
        return 55;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.DOMAIN_SERVICE;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a concrete class (not interface, not record, not enum, not abstract)
        if (node.form() != JavaForm.CLASS) {
            return MatchResult.noMatch();
        }

        // Skip abstract classes (they're base classes, not concrete services)
        if (node.isAbstract()) {
            return MatchResult.noMatch();
        }

        // Skip types that look like domain objects (have identity field)
        if (hasIdentityField(node, query)) {
            return MatchResult.noMatch();
        }

        // Must not have port dependencies (interface-typed fields)
        if (hasPortDependencies(node, query)) {
            return MatchResult.noMatch();
        }

        // Must look like a service (naming pattern)
        if (!looksLikeService(node)) {
            return MatchResult.noMatch();
        }

        // Must have methods (contain logic)
        List<MethodNode> methods = query.methodsOf(node);
        if (methods.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Count non-static, non-getter/setter methods (domain logic)
        long logicMethods = methods.stream()
                .filter(m -> !m.isStatic())
                .filter(m -> !m.looksLikeGetter())
                .filter(m -> !m.looksLikeSetter())
                .count();

        if (logicMethods == 0) {
            return MatchResult.noMatch();
        }

        return MatchResult.match(
                ConfidenceLevel.MEDIUM,
                "Stateless class with domain logic and no port dependencies",
                List.of(Evidence.fromStructure(
                        "No port dependencies, has " + logicMethods + " domain methods",
                        List.of(node.id()))));
    }

    private boolean hasIdentityField(TypeNode node, GraphQuery query) {
        return query.fieldsOf(node).stream()
                .anyMatch(f -> f.simpleName().equals("id") || f.simpleName().endsWith("Id"));
    }

    private boolean hasPortDependencies(TypeNode node, GraphQuery query) {
        List<FieldNode> fields = query.fieldsOf(node);

        return fields.stream().anyMatch(field -> {
            // Check if field type is an interface
            return query.graph().edgesFrom(field.id()).stream()
                    .filter(e -> e.kind() == EdgeKind.FIELD_TYPE)
                    .map(e -> query.type(e.to()))
                    .flatMap(java.util.Optional::stream)
                    .anyMatch(t -> t.form() == JavaForm.INTERFACE);
        });
    }

    private boolean looksLikeService(TypeNode node) {
        String name = node.simpleName();
        return name.endsWith("Service")
                || name.endsWith("Calculator")
                || name.endsWith("Validator")
                || name.endsWith("Resolver")
                || name.endsWith("Policy")
                || name.endsWith("Strategy")
                || name.endsWith("Factory")
                || name.endsWith("Builder")
                || name.endsWith("Handler")
                || name.endsWith("Processor");
    }
}
