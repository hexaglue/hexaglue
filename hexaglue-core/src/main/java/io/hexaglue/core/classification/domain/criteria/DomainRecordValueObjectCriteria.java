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

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Matches Java records in domain packages that have no identity field and classifies
 * them as VALUE_OBJECT.
 *
 * <p>In Domain-Driven Design, value objects are immutable types defined by their
 * attributes rather than by an identity. Java records are naturally immutable and
 * structurally transparent, making them ideal value objects when they lack identity.
 *
 * <p>Detection conditions:
 * <ul>
 *   <li>Type is a Java record</li>
 *   <li>Name does NOT end with "Id" or "ID" (would be an IDENTIFIER)</li>
 *   <li>Name does NOT end with "Event" (would be a DOMAIN_EVENT)</li>
 *   <li>Has NO identity field ({@code id} or {@code <typeName>Id})</li>
 *   <li>Is referenced by at least one other type in the graph (domain signal)</li>
 * </ul>
 *
 * <p>Priority: 65 (below DomainEventNamingCriteria at 68 — naming conventions
 * should take precedence, but this is stronger than no heuristic)
 * <p>Confidence: MEDIUM (heuristic based on form + absence of identity)
 *
 * @since 5.0.0
 */
public final class DomainRecordValueObjectCriteria implements ClassificationCriteria<ElementKind>, IdentifiedCriteria {

    @Override
    public String id() {
        return "domain.structural.recordValueObject";
    }

    @Override
    public String name() {
        return "domain-record-value-object";
    }

    @Override
    public int priority() {
        return 65;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.VALUE_OBJECT;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a record
        if (node.form() != JavaForm.RECORD) {
            return MatchResult.noMatch();
        }

        String simpleName = node.simpleName();

        // Exclude identifier patterns (would be IDENTIFIER)
        if (simpleName.endsWith("Id") || simpleName.endsWith("ID")) {
            return MatchResult.noMatch();
        }

        // Exclude event patterns (would be DOMAIN_EVENT)
        if (simpleName.endsWith("Event")) {
            return MatchResult.noMatch();
        }

        // Must NOT have an identity field
        if (hasIdentityField(node, query)) {
            return MatchResult.noMatch();
        }

        // Must be referenced by at least one other type (domain signal)
        boolean isReferenced = !query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.FIELD_TYPE
                        || e.kind() == EdgeKind.USES_AS_COLLECTION_ELEMENT
                        || e.kind() == EdgeKind.PARAMETER_TYPE
                        || e.kind() == EdgeKind.RETURN_TYPE)
                .toList()
                .isEmpty();

        if (!isReferenced) {
            return MatchResult.noMatch();
        }

        return MatchResult.match(
                ConfidenceLevel.MEDIUM,
                "Record '%s' has no identity field and is referenced by other types".formatted(simpleName),
                List.of(Evidence.fromStructure("Immutable record without identity", List.of(node.id()))));
    }

    /**
     * Checks if a type has an identity field ({@code id} or {@code <typeName>Id}).
     */
    private boolean hasIdentityField(TypeNode type, GraphQuery query) {
        return query.fieldsOf(type).stream().anyMatch(field -> isIdentityField(type, field));
    }

    /**
     * Checks if a field represents the identity of the containing type.
     *
     * <p>Identity fields are detected by name convention:
     * <ul>
     *   <li>{@code id} - generic identity field name</li>
     *   <li>{@code <typeName>Id} - type-specific identity (e.g., {@code orderId} for {@code Order})</li>
     * </ul>
     */
    private boolean isIdentityField(TypeNode type, FieldNode field) {
        String fieldName = field.simpleName();

        if (fieldName.equals("id")) {
            return true;
        }

        String typeName = type.simpleName();
        String expectedIdFieldName = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1) + "Id";
        return fieldName.equals(expectedIdFieldName);
    }

    @Override
    public String description() {
        return "Classifies records without identity that are referenced by other types as VALUE_OBJECT";
    }
}
