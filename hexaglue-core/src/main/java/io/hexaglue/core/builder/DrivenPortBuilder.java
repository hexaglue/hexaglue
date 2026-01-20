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

package io.hexaglue.core.builder;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds {@link DrivenPort} instances from classification results.
 *
 * <p>This builder transforms a classified {@link TypeNode} into a {@link DrivenPort}
 * by constructing the type structure, classification trace, port type, and managed aggregate.</p>
 *
 * <p>The port type is determined from the classification kind:</p>
 * <ul>
 *   <li>REPOSITORY -> DrivenPortType.REPOSITORY</li>
 *   <li>GATEWAY -> DrivenPortType.GATEWAY</li>
 *   <li>EVENT_PUBLISHER -> DrivenPortType.EVENT_PUBLISHER</li>
 *   <li>GENERIC or others -> DrivenPortType.OTHER</li>
 * </ul>
 *
 * <p>For repository ports, the builder attempts to detect the managed aggregate
 * from method return types.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DrivenPortBuilder builder = new DrivenPortBuilder(structureBuilder, traceConverter);
 * DrivenPort port = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class DrivenPortBuilder {

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;

    /**
     * Creates a new DrivenPortBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @throws NullPointerException if any argument is null
     */
    public DrivenPortBuilder(TypeStructureBuilder structureBuilder, ClassificationTraceConverter traceConverter) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
    }

    /**
     * Builds a DrivenPort from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built DrivenPort
     * @throws NullPointerException if any argument is null
     */
    public DrivenPort build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);
        DrivenPortType portType = mapPortType(classification.kind());
        Optional<TypeRef> managedAggregate = detectManagedAggregate(typeNode, context);

        if (managedAggregate.isPresent() && portType == DrivenPortType.REPOSITORY) {
            return DrivenPort.repository(id, structure, trace, managedAggregate.get());
        }
        return DrivenPort.of(id, structure, trace, portType);
    }

    /**
     * Maps the classification kind to a DrivenPortType.
     *
     * @param kind the classification kind string
     * @return the corresponding DrivenPortType
     */
    private DrivenPortType mapPortType(String kind) {
        if (kind == null) {
            return DrivenPortType.OTHER;
        }
        return switch (kind.toUpperCase()) {
            case "REPOSITORY" -> DrivenPortType.REPOSITORY;
            case "GATEWAY" -> DrivenPortType.GATEWAY;
            case "EVENT_PUBLISHER" -> DrivenPortType.EVENT_PUBLISHER;
            case "NOTIFICATION" -> DrivenPortType.NOTIFICATION;
            default -> DrivenPortType.OTHER;
        };
    }

    /**
     * Detects the managed aggregate from method return types.
     *
     * <p>For repository ports, this looks at method return types to find
     * types that are classified as AGGREGATE_ROOT.</p>
     *
     * @param typeNode the type node
     * @param context the builder context
     * @return the managed aggregate if found
     */
    private Optional<TypeRef> detectManagedAggregate(TypeNode typeNode, BuilderContext context) {
        List<MethodNode> methods = context.graphQuery().methodsOf(typeNode);

        for (MethodNode method : methods) {
            io.hexaglue.core.frontend.TypeRef returnType = method.returnType();
            if (returnType == null) {
                continue;
            }

            // Check if return type is an Optional or collection and extract the element type
            String typeName = extractReturnTypeName(returnType);
            if (typeName != null && isAggregateRoot(typeName, context)) {
                return Optional.of(TypeRef.of(typeName));
            }
        }

        return Optional.empty();
    }

    /**
     * Extracts the relevant type name from a return type.
     *
     * <p>Handles common wrapper types like Optional and collections.</p>
     *
     * @param returnType the return type
     * @return the extracted type name, or null if not extractable
     */
    private String extractReturnTypeName(io.hexaglue.core.frontend.TypeRef returnType) {
        String rawName = returnType.rawQualifiedName();

        // If it's a generic type (Optional, List, etc.), get the type argument
        if (!returnType.arguments().isEmpty()) {
            io.hexaglue.core.frontend.TypeRef typeArg = returnType.arguments().get(0);
            return typeArg.rawQualifiedName();
        }

        // Skip common non-aggregate types
        if (rawName.startsWith("java.") || rawName.equals("void")) {
            return null;
        }

        return rawName;
    }

    /**
     * Checks if a type is classified as AGGREGATE_ROOT.
     *
     * @param qualifiedName the qualified name of the type
     * @param context the builder context
     * @return true if the type is an aggregate root
     */
    private boolean isAggregateRoot(String qualifiedName, BuilderContext context) {
        return context.classificationResults().stream()
                .filter(result -> matchesQualifiedName(result, qualifiedName))
                .anyMatch(result -> "AGGREGATE_ROOT".equals(result.kind()));
    }

    /**
     * Checks if a classification result matches the given qualified name.
     *
     * @param result the classification result
     * @param qualifiedName the qualified name to match
     * @return true if the result's subject ID contains the qualified name
     */
    private boolean matchesQualifiedName(ClassificationResult result, String qualifiedName) {
        // NodeId format is "type:com.example.Order" so we extract the qualified name part
        String nodeIdStr = result.subjectId().toString();
        return nodeIdStr.endsWith(qualifiedName) || nodeIdStr.contains(":" + qualifiedName);
    }
}
