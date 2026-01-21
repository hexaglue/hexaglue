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
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.MethodRole;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds {@link DrivingPort} instances from classification results.
 *
 * <p>This builder transforms a classified {@link TypeNode} into a {@link DrivingPort}
 * by constructing the type structure and classification trace.</p>
 *
 * <h2>Use Case Detection (since 5.0.0)</h2>
 * <p>The builder extracts use cases from interface methods by:</p>
 * <ul>
 *   <li>Filtering out accessors (getters/setters) and object methods</li>
 *   <li>Deriving use case type (COMMAND, QUERY, COMMAND_QUERY) from method signature</li>
 * </ul>
 *
 * <h2>Input/Output Type Detection (since 5.0.0)</h2>
 * <p>The builder collects all unique types used as:</p>
 * <ul>
 *   <li>Method parameters (input types)</li>
 *   <li>Method return types (output types, excluding void)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DrivingPortBuilder builder = new DrivingPortBuilder(structureBuilder, traceConverter);
 * DrivingPort port = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 * @since 5.0.0 added use case and type detection
 */
public final class DrivingPortBuilder {

    private static final Set<MethodRole> EXCLUDED_ROLES =
            Set.of(MethodRole.GETTER, MethodRole.SETTER, MethodRole.OBJECT_METHOD);

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;

    /**
     * Creates a new DrivingPortBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @throws NullPointerException if any argument is null
     */
    public DrivingPortBuilder(TypeStructureBuilder structureBuilder, ClassificationTraceConverter traceConverter) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
    }

    /**
     * Builds a DrivingPort from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built DrivingPort
     * @throws NullPointerException if any argument is null
     */
    public DrivingPort build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);

        List<UseCase> useCases = extractUseCases(structure);
        List<TypeRef> inputTypes = extractInputTypes(structure);
        List<TypeRef> outputTypes = extractOutputTypes(structure);

        return DrivingPort.of(id, structure, trace, useCases, inputTypes, outputTypes);
    }

    /**
     * Extracts use cases from the interface methods.
     *
     * <p>Filters out accessors and object methods, then derives use case type
     * from each remaining method.</p>
     *
     * @param structure the type structure
     * @return list of use cases
     * @since 5.0.0
     */
    private List<UseCase> extractUseCases(TypeStructure structure) {
        return structure.methods().stream()
                .filter(this::isUseCaseMethod)
                .map(UseCase::from)
                .toList();
    }

    /**
     * Checks if a method represents a use case.
     *
     * @param method the method to check
     * @return true if the method is a use case
     */
    private boolean isUseCaseMethod(Method method) {
        // Exclude static methods (not interface contract)
        if (method.isStatic()) {
            return false;
        }

        // Exclude getters, setters, and object methods
        return method.roles().stream().noneMatch(EXCLUDED_ROLES::contains);
    }

    /**
     * Extracts all unique input types from method parameters.
     *
     * @param structure the type structure
     * @return list of unique input types
     * @since 5.0.0
     */
    private List<TypeRef> extractInputTypes(TypeStructure structure) {
        return structure.methods().stream()
                .filter(this::isUseCaseMethod)
                .flatMap(m -> m.parameters().stream())
                .map(Parameter::type)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Extracts all unique output types from method return types.
     *
     * <p>Excludes void return types.</p>
     *
     * @param structure the type structure
     * @return list of unique output types
     * @since 5.0.0
     */
    private List<TypeRef> extractOutputTypes(TypeStructure structure) {
        return structure.methods().stream()
                .filter(this::isUseCaseMethod)
                .map(Method::returnType)
                .filter(type -> !isVoidType(type))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Checks if a type is void.
     *
     * @param type the type to check
     * @return true if the type is void
     */
    private boolean isVoidType(TypeRef type) {
        String name = type.qualifiedName();
        return "void".equals(name) || "java.lang.Void".equals(name);
    }
}
