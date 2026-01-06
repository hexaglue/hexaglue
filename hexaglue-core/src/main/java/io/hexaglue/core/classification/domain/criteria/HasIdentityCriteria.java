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

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Optional;

/**
 * Matches types that have an identity field, suggesting they are entities.
 *
 * <p>Identity field patterns:
 * <ul>
 *   <li>Field named exactly "id"</li>
 *   <li>Field with name ending in "Id" (e.g., orderId, customerId)</li>
 * </ul>
 *
 * <p>Priority: 60 (medium heuristic - needs more context for higher confidence)
 * <p>Confidence: MEDIUM
 */
public final class HasIdentityCriteria implements ClassificationCriteria<DomainKind>, IdentifiedCriteria {

    @Override
    public String id() {
        return "domain.structural.hasIdentity";
    }

    @Override
    public String name() {
        return "has-identity";
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.ENTITY;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Only apply to classes (not interfaces, records, enums)
        if (node.form() != JavaForm.CLASS) {
            return MatchResult.noMatch();
        }

        // Find identity field
        Optional<FieldNode> identityField = findIdentityField(node, query);
        if (identityField.isEmpty()) {
            return MatchResult.noMatch();
        }

        FieldNode field = identityField.get();
        Evidence evidence = Evidence.fromStructure(
                "Has identity field '%s' of type %s"
                        .formatted(field.simpleName(), field.type().simpleName()),
                List.of(field.id()));

        return MatchResult.match(
                ConfidenceLevel.MEDIUM, "Has identity field '%s'".formatted(field.simpleName()), evidence);
    }

    private Optional<FieldNode> findIdentityField(TypeNode node, GraphQuery query) {
        List<FieldNode> fields = query.fieldsOf(node);

        // Priority 1: Look for explicit @Identity annotation
        Optional<FieldNode> annotatedId =
                fields.stream().filter(this::hasIdentityAnnotation).findFirst();
        if (annotatedId.isPresent()) {
            return annotatedId;
        }

        // Priority 2: Look for field named exactly "id"
        Optional<FieldNode> idField =
                fields.stream().filter(f -> f.simpleName().equals("id")).findFirst();
        if (idField.isPresent()) {
            return idField;
        }

        // Priority 3: Look for fields ending with "Id"
        return fields.stream().filter(f -> f.simpleName().endsWith("Id")).findFirst();
    }

    private boolean hasIdentityAnnotation(FieldNode field) {
        return field.annotations().stream()
                .anyMatch(a -> a.qualifiedName().equals("org.jmolecules.ddd.annotation.Identity")
                        || a.simpleName().equals("Identity")
                        || a.simpleName().equals("Id"));
    }
}
