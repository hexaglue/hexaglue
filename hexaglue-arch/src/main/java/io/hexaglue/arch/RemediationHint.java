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

package io.hexaglue.arch;

import java.util.Objects;
import java.util.Optional;

/**
 * A hint for corrective action to resolve classification ambiguity.
 *
 * <p>Provides actionable suggestions that users can follow to make
 * classification explicit and remove ambiguity.</p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Observation mode: Generate auto-actionable reports</li>
 *   <li>IDE: Quick-fix suggestions</li>
 *   <li>CLI: Interactive recommendations</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * RemediationHint hint = new RemediationHint(
 *     RemediationAction.ADD_ANNOTATION,
 *     "Add @AggregateRoot annotation to Order class",
 *     RemediationImpact.explicit(ElementKind.AGGREGATE_ROOT),
 *     Optional.of("@AggregateRoot\npublic class Order { }")
 * );
 * }</pre>
 *
 * @param actionType the type of remediation action
 * @param description human-readable description of the action
 * @param impact the expected impact on classification
 * @param codeSnippet optional code snippet showing the change
 * @since 4.0.0
 */
public record RemediationHint(
        RemediationAction actionType, String description, RemediationImpact impact, Optional<String> codeSnippet) {

    /**
     * Creates a new RemediationHint instance.
     *
     * @param actionType the action type, must not be null
     * @param description the description, must not be null or blank
     * @param impact the impact, must not be null
     * @param codeSnippet the optional code snippet
     * @throws NullPointerException if any required field is null
     * @throws IllegalArgumentException if description is blank
     */
    public RemediationHint {
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(impact, "impact must not be null");
        Objects.requireNonNull(codeSnippet, "codeSnippet must not be null (use Optional.empty())");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }

    /**
     * Returns the action description.
     *
     * <p>Shortcut for {@link #description()}.</p>
     *
     * @return the action description
     */
    public String action() {
        return description;
    }

    /**
     * Creates a hint for adding an annotation.
     *
     * @param annotationName the annotation to add
     * @param targetKind the resulting element kind
     * @return a new RemediationHint
     */
    public static RemediationHint addAnnotation(String annotationName, ElementKind targetKind) {
        return new RemediationHint(
                RemediationAction.ADD_ANNOTATION,
                "Add @" + annotationName + " annotation",
                RemediationImpact.explicit(targetKind),
                Optional.empty());
    }

    /**
     * Creates a hint for explicit configuration.
     *
     * @param typeName the type to configure
     * @param targetKind the resulting element kind
     * @return a new RemediationHint
     */
    public static RemediationHint configure(String typeName, ElementKind targetKind) {
        String snippet = "classification:\n  explicit:\n    " + typeName + ": " + targetKind;
        return new RemediationHint(
                RemediationAction.CONFIGURE_EXPLICIT,
                "Configure in hexaglue.yaml",
                RemediationImpact.explicit(targetKind),
                Optional.of(snippet));
    }
}
