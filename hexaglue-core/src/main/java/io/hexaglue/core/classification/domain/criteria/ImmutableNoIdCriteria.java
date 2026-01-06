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
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Matches immutable types without an identity field, suggesting they are value objects.
 *
 * <p>Immutability patterns:
 * <ul>
 *   <li>Records (always immutable by design)</li>
 *   <li>Classes with all fields private final and no setters</li>
 * </ul>
 *
 * <p>Priority: 60 (medium heuristic)
 * <p>Confidence: MEDIUM
 */
public final class ImmutableNoIdCriteria implements ClassificationCriteria<DomainKind>, IdentifiedCriteria {

    @Override
    public String id() {
        return "domain.structural.immutableNoId";
    }

    @Override
    public String name() {
        return "immutable-no-id";
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.VALUE_OBJECT;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Check if type has identity field (exclude entities)
        if (hasIdentityField(node, query)) {
            return MatchResult.noMatch();
        }

        // Check immutability
        ImmutabilityResult immutability = checkImmutability(node, query);
        if (!immutability.isImmutable) {
            return MatchResult.noMatch();
        }

        List<Evidence> evidences = new ArrayList<>();
        evidences.add(Evidence.fromStructure(immutability.reason, List.of(node.id())));

        // Additional evidence if no id field
        evidences.add(Evidence.fromStructure("No identity field present", List.of()));

        return MatchResult.match(ConfidenceLevel.MEDIUM, immutability.reason + " and has no identity field", evidences);
    }

    private boolean hasIdentityField(TypeNode node, GraphQuery query) {
        return query.fieldsOf(node).stream()
                .anyMatch(f -> f.simpleName().equals("id") || f.simpleName().endsWith("Id"));
    }

    private ImmutabilityResult checkImmutability(TypeNode node, GraphQuery query) {
        // Records are always immutable
        if (node.form() == JavaForm.RECORD) {
            return new ImmutabilityResult(true, "Is a record (inherently immutable)");
        }

        // For classes, check if all fields are private final
        if (node.form() == JavaForm.CLASS) {
            List<FieldNode> fields = query.fieldsOf(node);

            // Empty classes are not value objects
            if (fields.isEmpty()) {
                return new ImmutabilityResult(false, "Has no fields");
            }

            boolean allPrivateFinal = fields.stream().allMatch(this::isPrivateFinal);

            if (allPrivateFinal) {
                return new ImmutabilityResult(true, "All fields are private final");
            }
        }

        return new ImmutabilityResult(false, "Not immutable");
    }

    private boolean isPrivateFinal(FieldNode field) {
        return field.modifiers().contains(JavaModifier.PRIVATE)
                && field.modifiers().contains(JavaModifier.FINAL);
    }

    private record ImmutabilityResult(boolean isImmutable, String reason) {}
}
