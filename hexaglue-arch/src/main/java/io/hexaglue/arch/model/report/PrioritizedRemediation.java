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

package io.hexaglue.arch.model.report;

import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import java.util.List;
import java.util.Objects;

/**
 * A prioritized remediation suggestion for an unclassified type.
 *
 * <p>Remediation suggestions are ordered by priority (1 = most urgent, 5 = optional).
 * Priority is determined by the {@link UnclassifiedCategory} of the type:</p>
 *
 * <ul>
 *   <li>Priority 1: CONFLICTING - requires immediate attention</li>
 *   <li>Priority 2: AMBIGUOUS - needs clarification</li>
 *   <li>Priority 3: UNKNOWN - should be investigated</li>
 *   <li>Priority 4: TECHNICAL - infrastructure class, may not need action</li>
 *   <li>Priority 5: UTILITY/OUT_OF_SCOPE - intentionally not classified</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * List<PrioritizedRemediation> remediations = report.remediations()
 *     .stream()
 *     .sorted()
 *     .limit(10)
 *     .toList();
 *
 * for (PrioritizedRemediation r : remediations) {
 *     System.out.printf("[P%d] %s: %s\n", r.priority(), r.typeName(), r.suggestion());
 *     if (!r.possibleAnnotations().isEmpty()) {
 *         System.out.println("  Possible annotations: " + r.possibleAnnotations());
 *     }
 * }
 * }</pre>
 *
 * @param priority the priority level (1 = urgent, 5 = optional)
 * @param typeId the identifier of the type
 * @param typeName the simple name of the type
 * @param category the unclassified category
 * @param suggestion the remediation suggestion
 * @param possibleAnnotations possible annotations to fix the classification
 * @since 4.1.0
 */
public record PrioritizedRemediation(
        int priority,
        TypeId typeId,
        String typeName,
        UnclassifiedCategory category,
        String suggestion,
        List<String> possibleAnnotations)
        implements Comparable<PrioritizedRemediation> {

    /**
     * Creates a new PrioritizedRemediation.
     *
     * @param priority the priority (1-5)
     * @param typeId the type id, must not be null
     * @param typeName the type name, must not be null
     * @param category the category, must not be null
     * @param suggestion the suggestion, must not be null
     * @param possibleAnnotations the possible annotations, must not be null
     * @throws NullPointerException if any reference argument is null
     * @throws IllegalArgumentException if priority is not between 1 and 5
     */
    public PrioritizedRemediation {
        Objects.requireNonNull(typeId, "typeId must not be null");
        Objects.requireNonNull(typeName, "typeName must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(suggestion, "suggestion must not be null");
        Objects.requireNonNull(possibleAnnotations, "possibleAnnotations must not be null");
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("priority must be between 1 and 5, was: " + priority);
        }
        possibleAnnotations = List.copyOf(possibleAnnotations);
    }

    /**
     * Creates a PrioritizedRemediation for an {@link UnclassifiedType}.
     *
     * <p>The priority is determined by the unclassified category:</p>
     * <ul>
     *   <li>CONFLICTING → 1 (urgent)</li>
     *   <li>AMBIGUOUS → 2</li>
     *   <li>UNKNOWN → 3</li>
     *   <li>TECHNICAL → 4</li>
     *   <li>UTILITY, OUT_OF_SCOPE → 5 (optional)</li>
     * </ul>
     *
     * @param unclassified the unclassified type
     * @return a new PrioritizedRemediation
     * @throws NullPointerException if unclassified is null
     */
    public static PrioritizedRemediation forUnclassified(UnclassifiedType unclassified) {
        Objects.requireNonNull(unclassified, "unclassified must not be null");

        int priority = priorityForCategory(unclassified.category());
        String suggestion = suggestionForCategory(unclassified.category());
        List<String> annotations = annotationsForCategory(unclassified.category());

        return new PrioritizedRemediation(
                priority,
                unclassified.id(),
                unclassified.simpleName(),
                unclassified.category(),
                suggestion,
                annotations);
    }

    private static int priorityForCategory(UnclassifiedCategory category) {
        return switch (category) {
            case CONFLICTING -> 1;
            case AMBIGUOUS -> 2;
            case UNKNOWN -> 3;
            case TECHNICAL -> 4;
            case UTILITY, OUT_OF_SCOPE -> 5;
        };
    }

    private static String suggestionForCategory(UnclassifiedCategory category) {
        return switch (category) {
            case CONFLICTING -> "Resolve the classification conflict by adding an explicit annotation";
            case AMBIGUOUS -> "Add explicit annotation to clarify the architectural role";
            case UNKNOWN -> "Investigate and classify this type by adding an appropriate annotation";
            case TECHNICAL -> "Consider if this technical class should be classified or excluded";
            case UTILITY -> "This utility class is intentionally not classified. No action required.";
            case OUT_OF_SCOPE -> "This type is out of scope (test/mock/generated). No action required.";
        };
    }

    private static List<String> annotationsForCategory(UnclassifiedCategory category) {
        return switch (category) {
            case CONFLICTING, AMBIGUOUS, UNKNOWN ->
                List.of("@AggregateRoot", "@Entity", "@ValueObject", "@DomainService", "@DrivingPort", "@DrivenPort");
            case TECHNICAL, UTILITY, OUT_OF_SCOPE -> List.of();
        };
    }

    /**
     * Returns true if this remediation is urgent (priority 1).
     *
     * @return true if urgent
     */
    public boolean isUrgent() {
        return priority == 1;
    }

    /**
     * Returns true if this remediation requires action.
     *
     * <p>Actionable remediations are those that are not utility or out-of-scope
     * categories, and have possible annotations to apply.</p>
     *
     * @return true if actionable
     */
    public boolean isActionable() {
        return switch (category) {
            case UTILITY, OUT_OF_SCOPE -> false;
            default -> true;
        };
    }

    @Override
    public int compareTo(PrioritizedRemediation other) {
        int priorityCompare = Integer.compare(this.priority, other.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        return this.typeName.compareTo(other.typeName);
    }
}
