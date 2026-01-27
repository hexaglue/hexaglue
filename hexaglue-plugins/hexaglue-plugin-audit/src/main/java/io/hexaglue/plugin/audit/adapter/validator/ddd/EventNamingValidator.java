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

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates that domain events are named in past tense.
 *
 * <p>DDD Principle: Domain events represent facts that have already occurred
 * in the business domain. Their names should reflect this by using past-tense
 * verbs, making the timeline of events explicit in the code.
 *
 * <p>This validator identifies domain events and checks that they follow
 * past-tense naming conventions.
 *
 * <p><strong>Valid Past-Tense Patterns:</strong>
 * <ul>
 *   <li>Regular past tense with "ed" suffix (e.g., OrderPlaced, PaymentProcessed)</li>
 *   <li>Irregular past tense forms (e.g., OrderSent, PaymentReceived, UserDeleted)</li>
 *   <li>Common event verbs (Created, Updated, Deleted, Completed, Failed, etc.)</li>
 * </ul>
 *
 * <p><strong>Invalid Examples:</strong>
 * <ul>
 *   <li>Present tense: OrderPlace, PaymentProcess</li>
 *   <li>Gerund form: OrderPlacing, PaymentProcessing</li>
 *   <li>Noun form: OrderEvent, PaymentEvent</li>
 * </ul>
 *
 * <p><strong>Constraint:</strong> ddd:event-naming<br>
 * <strong>Severity:</strong> MINOR<br>
 * <strong>Rationale:</strong> Consistent event naming improves code readability
 * and makes the domain model more expressive. While not critical to functionality,
 * it reflects DDD best practices and aids in understanding event flow.
 *
 * @since 1.0.0
 */
public class EventNamingValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:event-naming");

    /**
     * Pattern matching valid past-tense event names.
     *
     * <p>This pattern matches:
     * <ul>
     *   <li>Names ending with "ed" (standard past tense)</li>
     *   <li>Names ending with common past-tense event verbs</li>
     * </ul>
     *
     * <p>The pattern is deliberately permissive to avoid false positives
     * with irregular verb forms while catching common violations.
     */
    private static final Pattern PAST_TENSE_PATTERN =
            Pattern.compile(".*(?:ed|Created|Deleted|Updated|Placed|Received|Sent|Completed|Failed|Started|Finished|"
                    + "Cancelled|Approved|Rejected|Confirmed|Validated|Submitted|Published|Archived|"
                    + "Activated|Deactivated|Registered|Unregistered|Added|Removed|Modified|Changed|"
                    + "Assigned|Unassigned|Granted|Revoked|Enabled|Disabled|Locked|Unlocked|Opened|Closed|"
                    + "Expired|Renewed|Scheduled|Rescheduled|Found|Lost|Won|Built|Sold|Bought|Paid|Refunded)$");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates event naming using the v5 ArchitecturalModel API.
     *
     * @param model the architectural model containing domain types
     * @param codebase the codebase (not used in v5)
     * @param query the architecture query (not used in v5)
     * @return list of violations
     * @since 5.0.0
     */
    @Override
    public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Check if domain index is available
        if (model.domainIndex().isEmpty()) {
            return violations; // Cannot validate without domain index
        }

        var domainIndex = model.domainIndex().get();

        // Get all domain events directly from the domain index
        List<DomainEvent> domainEvents = domainIndex.domainEvents().toList();

        for (DomainEvent domainEvent : domainEvents) {
            String simpleName = domainEvent.id().simpleName();

            // Extract the event name without "Event" suffix for validation
            String eventName = extractEventName(simpleName);

            // Check if the event name is in past tense
            if (!isPastTense(eventName)) {
                violations.add(Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.MINOR)
                        .message("Domain event '%s' should be named in past tense".formatted(simpleName))
                        .affectedType(domainEvent.id().qualifiedName())
                        .location(SourceLocation.of(domainEvent.id().qualifiedName(), 1, 1))
                        .evidence(StructuralEvidence.of(
                                "Domain events represent facts that have already occurred. "
                                        + "Use past-tense verbs (e.g., OrderPlaced, UserCreated, PaymentCompleted)",
                                domainEvent.id().qualifiedName()))
                        .build());
            }
        }

        return violations;
    }

    /**
     * Extracts the core event name for validation.
     *
     * <p>Removes common suffixes like "Event" to get the actual
     * event description (e.g., "OrderPlacedEvent" → "OrderPlaced").
     *
     * @param simpleName the simple name of the event
     * @return the extracted event name
     */
    private String extractEventName(String simpleName) {
        // Remove "Event" suffix if present
        if (simpleName.endsWith("Event")) {
            return simpleName.substring(0, simpleName.length() - 5);
        }
        // Remove "Notification" suffix if present
        if (simpleName.endsWith("Notification")) {
            return simpleName.substring(0, simpleName.length() - 12);
        }
        return simpleName;
    }

    /**
     * Checks if an event name is in past tense.
     *
     * <p>This validation uses a regex pattern to match common past-tense
     * endings and irregular verb forms commonly used in domain events.
     *
     * @param eventName the event name to check
     * @return true if the event name is in past tense
     */
    private boolean isPastTense(String eventName) {
        return PAST_TENSE_PATTERN.matcher(eventName).matches();
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MINOR;
    }
}
