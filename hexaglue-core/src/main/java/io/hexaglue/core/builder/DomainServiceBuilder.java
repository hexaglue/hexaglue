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
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.Objects;

/**
 * Builds {@link DomainService} instances from classification results.
 *
 * <p>This builder transforms a classified {@link TypeNode} into a {@link DomainService}
 * by constructing the type structure and classification trace.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DomainServiceBuilder builder = new DomainServiceBuilder(structureBuilder, traceConverter);
 * DomainService service = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class DomainServiceBuilder {

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;

    /**
     * Creates a new DomainServiceBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @throws NullPointerException if any argument is null
     */
    public DomainServiceBuilder(TypeStructureBuilder structureBuilder, ClassificationTraceConverter traceConverter) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
    }

    /**
     * Builds a DomainService from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built DomainService
     * @throws NullPointerException if any argument is null
     */
    public DomainService build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);

        return DomainService.of(id, structure, trace);
    }
}
