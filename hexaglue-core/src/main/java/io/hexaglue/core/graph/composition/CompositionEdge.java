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

package io.hexaglue.core.graph.composition;

import java.util.Objects;

/**
 * An edge in the composition graph representing a relationship between domain types.
 *
 * <p>Each edge represents a field in the source type that references or contains
 * the target type. The edge captures the nature of this relationship (composition,
 * reference by ID, or direct reference) and its cardinality.
 *
 * <p>Edges are immutable and directional (source → target).
 *
 * @param source      the qualified name of the source type (container)
 * @param target      the qualified name of the target type (contained/referenced)
 * @param type        the type of relationship
 * @param cardinality the cardinality of the relationship
 * @param fieldName   the name of the field establishing this relationship
 * @since 3.0.0
 */
public record CompositionEdge(
        String source, String target, RelationType type, Cardinality cardinality, String fieldName) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any parameter is null
     */
    public CompositionEdge {
        Objects.requireNonNull(source, "source required");
        Objects.requireNonNull(target, "target required");
        Objects.requireNonNull(type, "type required");
        Objects.requireNonNull(cardinality, "cardinality required");
        Objects.requireNonNull(fieldName, "fieldName required");
    }

    /**
     * Creates a composition edge for a single embedded field.
     *
     * @param source    the source type qualified name
     * @param target    the target type qualified name
     * @param fieldName the field name
     * @return new edge with COMPOSITION and ONE cardinality
     */
    public static CompositionEdge composition(String source, String target, String fieldName) {
        return new CompositionEdge(source, target, RelationType.COMPOSITION, Cardinality.ONE, fieldName);
    }

    /**
     * Creates a composition edge for a collection of embedded objects.
     *
     * @param source    the source type qualified name
     * @param target    the target type qualified name
     * @param fieldName the field name
     * @return new edge with COMPOSITION and MANY cardinality
     */
    public static CompositionEdge compositionMany(String source, String target, String fieldName) {
        return new CompositionEdge(source, target, RelationType.COMPOSITION, Cardinality.MANY, fieldName);
    }

    /**
     * Creates a reference-by-id edge for a single ID field.
     *
     * @param source    the source type qualified name
     * @param target    the target type qualified name
     * @param fieldName the field name
     * @return new edge with REFERENCE_BY_ID and ONE cardinality
     */
    public static CompositionEdge referenceById(String source, String target, String fieldName) {
        return new CompositionEdge(source, target, RelationType.REFERENCE_BY_ID, Cardinality.ONE, fieldName);
    }

    /**
     * Creates a reference-by-id edge for a collection of IDs.
     *
     * @param source    the source type qualified name
     * @param target    the target type qualified name
     * @param fieldName the field name
     * @return new edge with REFERENCE_BY_ID and MANY cardinality
     */
    public static CompositionEdge referenceByIdMany(String source, String target, String fieldName) {
        return new CompositionEdge(source, target, RelationType.REFERENCE_BY_ID, Cardinality.MANY, fieldName);
    }

    /**
     * Creates a direct reference edge (design smell).
     *
     * @param source    the source type qualified name
     * @param target    the target type qualified name
     * @param fieldName the field name
     * @return new edge with DIRECT_REFERENCE and ONE cardinality
     */
    public static CompositionEdge directReference(String source, String target, String fieldName) {
        return new CompositionEdge(source, target, RelationType.DIRECT_REFERENCE, Cardinality.ONE, fieldName);
    }

    /**
     * Creates a direct reference edge for a collection (design smell).
     *
     * @param source    the source type qualified name
     * @param target    the target type qualified name
     * @param fieldName the field name
     * @return new edge with DIRECT_REFERENCE and MANY cardinality
     */
    public static CompositionEdge directReferenceMany(String source, String target, String fieldName) {
        return new CompositionEdge(source, target, RelationType.DIRECT_REFERENCE, Cardinality.MANY, fieldName);
    }

    /**
     * Returns true if this edge represents composition within an aggregate.
     *
     * @return true if type is COMPOSITION
     */
    public boolean isComposition() {
        return type == RelationType.COMPOSITION;
    }

    /**
     * Returns true if this edge represents a reference by ID.
     *
     * @return true if type is REFERENCE_BY_ID
     */
    public boolean isReferenceById() {
        return type == RelationType.REFERENCE_BY_ID;
    }

    /**
     * Returns true if this edge represents a direct reference (smell).
     *
     * @return true if type is DIRECT_REFERENCE
     */
    public boolean isDirectReference() {
        return type == RelationType.DIRECT_REFERENCE;
    }

    /**
     * Returns true if this edge has MANY cardinality.
     *
     * @return true if cardinality is MANY
     */
    public boolean isCollection() {
        return cardinality == Cardinality.MANY;
    }

    /**
     * Returns true if this edge crosses aggregate boundaries.
     *
     * @return true if the relationship type crosses boundaries
     */
    public boolean crossesBoundary() {
        return type.crossesAggregateBoundary();
    }

    /**
     * Returns a human-readable string representation for debugging.
     *
     * @return formatted string showing the relationship
     */
    @Override
    public String toString() {
        String sourceSimple = source.substring(source.lastIndexOf('.') + 1);
        String targetSimple = target.substring(target.lastIndexOf('.') + 1);
        String cardinalityStr = cardinality == Cardinality.MANY ? "[]" : "";
        return String.format("%s.%s -> %s%s [%s]", sourceSimple, fieldName, targetSimple, cardinalityStr, type);
    }
}
