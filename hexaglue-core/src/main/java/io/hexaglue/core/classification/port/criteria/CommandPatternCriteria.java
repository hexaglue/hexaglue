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
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
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
 * Matches interfaces that use Command pattern (CQRS style write operations).
 *
 * <p>This criteria detects driving (inbound) ports by analyzing:
 * <ul>
 *   <li>Methods with parameters ending in "Command"</li>
 *   <li>Methods with command-style names (create*, update*, delete*, etc.)</li>
 * </ul>
 *
 * <p>Priority: 75 (between naming and signature-based criteria)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVING
 */
public final class CommandPatternCriteria implements PortClassificationCriteria, IdentifiedCriteria {

    @Override
    public String id() {
        return "port.pattern.command";
    }

    @Override
    public String name() {
        return "command-pattern";
    }

    @Override
    public int priority() {
        return 75;
    }

    @Override
    public PortKind targetKind() {
        return PortKind.COMMAND;
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

        // Find methods with Command parameters
        List<MethodNode> commandParamMethods =
                methods.stream().filter(this::hasCommandParameter).toList();

        // Find methods with command-style names
        List<MethodNode> commandStyleMethods = methods.stream()
                .filter(m -> isCommandStyleMethod(m.simpleName()))
                .toList();

        // Check if there are more query-style methods (if so, let QueryPatternCriteria handle it)
        long queryStyleCount = methods.stream()
                .filter(m -> isQueryStyleMethod(m.simpleName()) || hasQueryParameter(m))
                .count();

        long commandStyleCount = commandParamMethods.size()
                + commandStyleMethods.stream()
                        .filter(m -> !commandParamMethods.contains(m))
                        .count();

        if (commandStyleCount == 0 || queryStyleCount > commandStyleCount) {
            return MatchResult.noMatch();
        }

        List<Evidence> evidences = new ArrayList<>();

        if (!commandParamMethods.isEmpty()) {
            String methodNames =
                    commandParamMethods.stream().map(MethodNode::simpleName).collect(joining(", "));
            evidences.add(Evidence.fromStructure(
                    "Command parameters in: " + methodNames,
                    commandParamMethods.stream().map(MethodNode::id).toList()));
        }

        if (!commandStyleMethods.isEmpty()) {
            String methodNames =
                    commandStyleMethods.stream().map(MethodNode::simpleName).collect(joining(", "));
            evidences.add(Evidence.fromNaming("Command style methods", methodNames));
        }

        return MatchResult.match(ConfidenceLevel.HIGH, "Interface uses command pattern", evidences);
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

    private boolean hasCommandParameter(MethodNode method) {
        return method.parameters().stream().anyMatch(p -> p.type().simpleName().endsWith("Command"));
    }

    private boolean hasQueryParameter(MethodNode method) {
        return method.parameters().stream().anyMatch(p -> p.type().simpleName().endsWith("Query"));
    }

    private boolean isCommandStyleMethod(String name) {
        return name.matches(
                "^(create|place|cancel|submit|process|handle|execute|update|delete|remove|add|register|save|insert).*");
    }

    private boolean isQueryStyleMethod(String name) {
        return name.matches("^(get|find|search|list|fetch|load|retrieve|query|count|exists).*");
    }
}
