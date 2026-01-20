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
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.Objects;

/**
 * Builds {@link UnclassifiedType} instances from classification results.
 *
 * <p>This builder transforms a classified {@link TypeNode} with an UNCLASSIFIED
 * status into an {@link UnclassifiedType} by constructing the type structure,
 * classification trace, and detecting the appropriate unclassification category.</p>
 *
 * <p>The category is determined by the {@link UnclassifiedCategoryDetector} based on:</p>
 * <ul>
 *   <li>UTILITY - type name ends with Utils, Helper, etc.</li>
 *   <li>OUT_OF_SCOPE - type is in test, mock, or stub package</li>
 *   <li>TECHNICAL - type has framework annotations (@Configuration, @Component)</li>
 *   <li>AMBIGUOUS - criteria were evaluated but no match</li>
 *   <li>UNKNOWN - no conditions matched</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * UnclassifiedTypeBuilder builder = new UnclassifiedTypeBuilder(
 *     structureBuilder, traceConverter, categoryDetector);
 * UnclassifiedType unclassified = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class UnclassifiedTypeBuilder {

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;
    private final UnclassifiedCategoryDetector categoryDetector;

    /**
     * Creates a new UnclassifiedTypeBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @param categoryDetector the detector for unclassified categories
     * @throws NullPointerException if any argument is null
     */
    public UnclassifiedTypeBuilder(
            TypeStructureBuilder structureBuilder,
            ClassificationTraceConverter traceConverter,
            UnclassifiedCategoryDetector categoryDetector) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
        this.categoryDetector = Objects.requireNonNull(categoryDetector, "categoryDetector must not be null");
    }

    /**
     * Builds an UnclassifiedType from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built UnclassifiedType
     * @throws NullPointerException if any argument is null
     */
    public UnclassifiedType build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);
        UnclassifiedCategory category = categoryDetector.detect(typeNode, classification);
        String reason = classification.justification();

        return UnclassifiedType.of(id, structure, trace, category, reason);
    }
}
