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
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Matches enum types in domain packages and classifies them as VALUE_OBJECT.
 *
 * <p>In Domain-Driven Design, enums are typically Value Objects because:
 * <ul>
 *   <li>They are immutable by definition</li>
 *   <li>They have no identity - equality is based on their value</li>
 *   <li>They represent a fixed set of domain concepts (e.g., OrderStatus, PaymentMethod)</li>
 * </ul>
 *
 * <p>This criteria classifies all enums as VALUE_OBJECT to avoid layer-isolation
 * false positives where "DOMAIN depends on UNCLASSIFIED" violations are reported
 * for domain types using domain enums.
 *
 * <p>Priority: 72 (between inherited classification and embedded value object)
 * <p>Confidence: HIGH (enums are structurally VALUE_OBJECT by definition)
 *
 * @since 5.0.0
 */
public final class DomainEnumCriteria implements ClassificationCriteria<ElementKind>, IdentifiedCriteria {

    @Override
    public String id() {
        return "domain.structural.enum";
    }

    @Override
    public String name() {
        return "domain-enum";
    }

    @Override
    public int priority() {
        return 72;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.VALUE_OBJECT;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be an enum
        if (node.form() != JavaForm.ENUM) {
            return MatchResult.noMatch();
        }

        // All enums are classified as VALUE_OBJECT
        // They are immutable by definition and have no identity
        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Enum type - immutable, identity-less, represents fixed domain concepts",
                List.of(Evidence.fromStructure(
                        "Java enum: immutable by definition, equality by value", List.of(node.id()))));
    }

    @Override
    public String description() {
        return "Classifies enum types as VALUE_OBJECT (immutable, identity-less domain concepts)";
    }
}
