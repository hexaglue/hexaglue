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
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.syntax.TypeRef;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Builds {@link DomainEvent} instances from classification results.
 *
 * <p>This builder transforms a classified {@link TypeNode} into a {@link DomainEvent}
 * by constructing the type structure and classification trace.</p>
 *
 * <h2>Metadata Detection (since 5.0.0)</h2>
 * <p>The builder automatically detects common domain event metadata fields:</p>
 * <ul>
 *   <li>Aggregate ID fields: fields containing "aggregateId" or ending with "Id"
 *       (excluding "eventId", "id")</li>
 *   <li>Timestamp fields: fields named "timestamp", "occurredAt", "createdAt",
 *       "eventTime", "happenedAt"</li>
 *   <li>Source aggregate: inferred from field types matching classified aggregates</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DomainEventBuilder builder = new DomainEventBuilder(structureBuilder, traceConverter);
 * DomainEvent event = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 * @since 5.0.0 added metadata field detection
 */
public final class DomainEventBuilder {

    private static final Set<String> TIMESTAMP_FIELD_NAMES =
            Set.of("timestamp", "occurredat", "createdat", "eventtime", "happenedat", "recordedat", "emittedat");

    private static final Set<String> EXCLUDED_ID_FIELD_NAMES = Set.of("id", "eventid");

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;

    /**
     * Creates a new DomainEventBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @throws NullPointerException if any argument is null
     */
    public DomainEventBuilder(TypeStructureBuilder structureBuilder, ClassificationTraceConverter traceConverter) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
    }

    /**
     * Builds a DomainEvent from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built DomainEvent
     * @throws NullPointerException if any argument is null
     */
    public DomainEvent build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);

        Optional<Field> aggregateIdField = detectAggregateIdField(structure);
        Optional<Field> timestampField = detectTimestampField(structure);
        Optional<TypeRef> sourceAggregate = detectSourceAggregate(structure, aggregateIdField, context);

        return DomainEvent.of(id, structure, trace, aggregateIdField, timestampField, sourceAggregate);
    }

    /**
     * Detects the aggregate ID field in the event structure.
     *
     * <p>Detection rules:</p>
     * <ol>
     *   <li>Field named exactly "aggregateId"</li>
     *   <li>Field containing "aggregateid" (case-insensitive)</li>
     *   <li>Field ending with "Id" (excluding "id" and "eventId")</li>
     * </ol>
     *
     * @param structure the event structure
     * @return the detected aggregate ID field, or empty
     */
    private Optional<Field> detectAggregateIdField(TypeStructure structure) {
        // First, look for exact match "aggregateId"
        Optional<Field> exactMatch = structure.fields().stream()
                .filter(f -> f.name().equals("aggregateId"))
                .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        // Then, look for field containing "aggregateid" (case-insensitive)
        Optional<Field> containsMatch = structure.fields().stream()
                .filter(f -> f.name().toLowerCase().contains("aggregateid"))
                .findFirst();
        if (containsMatch.isPresent()) {
            return containsMatch;
        }

        // Finally, look for field ending with "Id" (but not "id" or "eventId")
        return structure.fields().stream()
                .filter(f -> f.name().endsWith("Id"))
                .filter(f -> !EXCLUDED_ID_FIELD_NAMES.contains(f.name().toLowerCase()))
                .findFirst();
    }

    /**
     * Detects the timestamp field in the event structure.
     *
     * <p>Looks for fields with common timestamp names:</p>
     * <ul>
     *   <li>timestamp</li>
     *   <li>occurredAt</li>
     *   <li>createdAt</li>
     *   <li>eventTime</li>
     *   <li>happenedAt</li>
     *   <li>recordedAt</li>
     *   <li>emittedAt</li>
     * </ul>
     *
     * @param structure the event structure
     * @return the detected timestamp field, or empty
     */
    private Optional<Field> detectTimestampField(TypeStructure structure) {
        return structure.fields().stream()
                .filter(f -> TIMESTAMP_FIELD_NAMES.contains(f.name().toLowerCase()))
                .findFirst();
    }

    /**
     * Detects the source aggregate from which this event was emitted.
     *
     * <p>Detection is based on the aggregate ID field type. If the field type
     * matches an identifier that is associated with a known aggregate, that
     * aggregate is returned as the source.</p>
     *
     * <p>For example, if the event has an "orderId" field of type "OrderId",
     * and "Order" is a classified aggregate root, then "Order" is the source aggregate.</p>
     *
     * @param structure the event structure
     * @param aggregateIdField the detected aggregate ID field
     * @param context the builder context
     * @return the source aggregate type, or empty
     */
    private Optional<TypeRef> detectSourceAggregate(
            TypeStructure structure, Optional<Field> aggregateIdField, BuilderContext context) {
        if (aggregateIdField.isEmpty()) {
            return Optional.empty();
        }

        Field idField = aggregateIdField.get();
        String fieldTypeName = idField.type().qualifiedName();

        // If the field type name ends with "Id", derive the aggregate name
        if (fieldTypeName.endsWith("Id")) {
            String aggregateName = fieldTypeName.substring(0, fieldTypeName.length() - 2);

            // Check if this aggregate is in the classification results
            if (context.isClassifiedAsAggregate(aggregateName)) {
                return Optional.of(new TypeRef(
                        aggregateName, extractSimpleName(aggregateName), java.util.List.of(), false, false, 0));
            }
        }

        return Optional.empty();
    }

    private static String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
