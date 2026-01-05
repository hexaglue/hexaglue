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

import static java.util.stream.Collectors.joining;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Matches interfaces that use Query pattern (CQRS style read operations).
 *
 * <p>This criteria detects driving (inbound) ports by analyzing:
 * <ul>
 *   <li>Methods with parameters ending in "Query"</li>
 *   <li>Methods with query-style names (get*, find*, list*, etc.)</li>
 * </ul>
 *
 * <p>Priority: 75 (between naming and signature-based criteria)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVING
 */
public final class QueryPatternCriteria implements PortClassificationCriteria {

    @Override
    public String name() {
        return "query-pattern";
    }

    @Override
    public int priority() {
        return 75;
    }

    @Override
    public PortKind targetKind() {
        return PortKind.QUERY;
    }

    @Override
    public PortDirection targetDirection() {
        return PortDirection.DRIVING;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be an interface
        if (node.form() != JavaForm.INTERFACE) {
            return MatchResult.noMatch();
        }

        // Skip interfaces that look like driven ports
        if (looksLikeDrivenPort(node)) {
            return MatchResult.noMatch();
        }

        List<MethodNode> methods = query.methodsOf(node);
        if (methods.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Find methods with Query parameters
        List<MethodNode> queryParamMethods =
                methods.stream().filter(this::hasQueryParameter).toList();

        // Find methods with query-style names
        List<MethodNode> queryStyleMethods =
                methods.stream().filter(m -> isQueryStyleMethod(m.simpleName())).toList();

        // Check if there are more command-style methods (if so, let CommandPatternCriteria handle it)
        long commandStyleCount = methods.stream()
                .filter(m -> isCommandStyleMethod(m.simpleName()) || hasCommandParameter(m))
                .count();

        long queryStyleCount = queryParamMethods.size()
                + queryStyleMethods.stream()
                        .filter(m -> !queryParamMethods.contains(m))
                        .count();

        if (queryStyleCount == 0 || commandStyleCount >= queryStyleCount) {
            return MatchResult.noMatch();
        }

        List<Evidence> evidences = new ArrayList<>();

        if (!queryParamMethods.isEmpty()) {
            String methodNames =
                    queryParamMethods.stream().map(MethodNode::simpleName).collect(joining(", "));
            evidences.add(Evidence.fromStructure(
                    "Query parameters in: " + methodNames,
                    queryParamMethods.stream().map(MethodNode::id).toList()));
        }

        if (!queryStyleMethods.isEmpty()) {
            String methodNames =
                    queryStyleMethods.stream().map(MethodNode::simpleName).collect(joining(", "));
            evidences.add(Evidence.fromNaming("Query style methods", methodNames));
        }

        return MatchResult.match(ConfidenceLevel.HIGH, "Interface uses query pattern", evidences);
    }

    private boolean looksLikeDrivenPort(TypeNode node) {
        String name = node.simpleName();
        return name.endsWith("Repository")
                || name.endsWith("Gateway")
                || name.endsWith("Client")
                || name.endsWith("Adapter")
                || name.endsWith("Fetcher")
                || name.endsWith("Loader")
                || name.endsWith("Saver")
                || name.endsWith("Persister")
                || name.endsWith("Store");
    }

    private boolean hasQueryParameter(MethodNode method) {
        return method.parameters().stream().anyMatch(p -> p.type().simpleName().endsWith("Query"));
    }

    private boolean hasCommandParameter(MethodNode method) {
        return method.parameters().stream().anyMatch(p -> p.type().simpleName().endsWith("Command"));
    }

    private boolean isQueryStyleMethod(String name) {
        return name.matches("^(get|find|search|list|fetch|load|retrieve|query|count|exists).*");
    }

    private boolean isCommandStyleMethod(String name) {
        return name.matches(
                "^(create|place|cancel|submit|process|handle|execute|update|delete|remove|add|register|save|insert).*");
    }
}
