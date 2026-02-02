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
 * Matches types whose name ends with "Event" and classifies them as DOMAIN_EVENT.
 *
 * <p>In Domain-Driven Design, domain events follow a strong naming convention:
 * {@code <Noun><Verb>Event} or {@code <Noun><Adjective>Event}, such as
 * {@code OrderCreatedEvent}, {@code PaymentReceivedEvent}, {@code ShipmentDeliveredEvent}.
 *
 * <p>Detection conditions:
 * <ul>
 *   <li>Type name ends with "Event"</li>
 *   <li>Type is a class or record (not interface or enum)</li>
 * </ul>
 *
 * <p>Note: Marker interfaces like {@code DomainEvent} are excluded because they are
 * interfaces, not classes/records. Framework event types (Spring, JDK) are excluded
 * because they are external and never reach the domain classifier.
 *
 * <p>Priority: 68 (below EmbeddedValueObjectCriteria at 70 — naming is a weaker signal
 * than structural analysis, but stronger than no heuristic at all)
 * <p>Confidence: MEDIUM (naming convention only)
 *
 * @since 5.0.0
 */
public final class DomainEventNamingCriteria implements ClassificationCriteria<ElementKind>, IdentifiedCriteria {

    @Override
    public String id() {
        return "domain.naming.domainEvent";
    }

    @Override
    public String name() {
        return "domain-event-naming";
    }

    @Override
    public int priority() {
        return 68;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.DOMAIN_EVENT;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a class or record (not interface or enum)
        if (node.form() == JavaForm.INTERFACE || node.form() == JavaForm.ENUM) {
            return MatchResult.noMatch();
        }

        // Name must end with "Event"
        String simpleName = node.simpleName();
        if (!simpleName.endsWith("Event")) {
            return MatchResult.noMatch();
        }

        // Exclude types whose name is exactly "Event" (too generic, likely a marker type)
        if (simpleName.equals("Event") || simpleName.equals("DomainEvent")) {
            return MatchResult.noMatch();
        }

        return MatchResult.match(
                ConfidenceLevel.MEDIUM,
                "Type name '%s' follows domain event naming convention (*Event)".formatted(simpleName),
                List.of(Evidence.fromNaming("*Event", simpleName)));
    }

    @Override
    public String description() {
        return "Classifies classes/records ending with 'Event' as DOMAIN_EVENT";
    }
}
