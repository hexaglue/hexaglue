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
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.arch.model.ApplicationType;
import io.hexaglue.arch.model.CommandHandler;
import io.hexaglue.arch.model.QueryHandler;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.Objects;

/**
 * Builds {@link ApplicationType} instances from classification results.
 *
 * <p>This builder transforms a classified {@link TypeNode} into the appropriate
 * {@link ApplicationType} implementation based on the classification kind:</p>
 * <ul>
 *   <li>APPLICATION_SERVICE -> {@link ApplicationService}</li>
 *   <li>COMMAND_HANDLER -> {@link CommandHandler}</li>
 *   <li>QUERY_HANDLER -> {@link QueryHandler}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ApplicationTypeBuilder builder = new ApplicationTypeBuilder(structureBuilder, traceConverter);
 * ApplicationType appType = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class ApplicationTypeBuilder {

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;

    /**
     * Creates a new ApplicationTypeBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @throws NullPointerException if any argument is null
     */
    public ApplicationTypeBuilder(TypeStructureBuilder structureBuilder, ClassificationTraceConverter traceConverter) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
    }

    /**
     * Builds an ApplicationType from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built ApplicationType (ApplicationService, CommandHandler, or QueryHandler)
     * @throws NullPointerException if any argument is null
     */
    public ApplicationType build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);

        return createApplicationType(id, structure, trace, classification.kind());
    }

    /**
     * Creates the appropriate ApplicationType based on the classification kind.
     *
     * @param id the type id
     * @param structure the type structure
     * @param trace the classification trace
     * @param kind the classification kind
     * @return the appropriate ApplicationType implementation
     */
    private ApplicationType createApplicationType(
            TypeId id, TypeStructure structure, ClassificationTrace trace, String kind) {
        if (kind == null) {
            return ApplicationService.of(id, structure, trace);
        }
        return switch (kind.toUpperCase()) {
            case "COMMAND_HANDLER" -> CommandHandler.of(id, structure, trace);
            case "QUERY_HANDLER" -> QueryHandler.of(id, structure, trace);
            default -> ApplicationService.of(id, structure, trace);
        };
    }
}
