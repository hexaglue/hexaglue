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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Suggestion for fixing an issue, including steps and code example.
 *
 * <p>This record contains all the information needed to guide a developer
 * in resolving a violation, including actionable steps and example code.
 *
 * @param action short description of the fix action
 * @param steps ordered list of steps to perform the fix
 * @param codeExample example code showing the fix (may be null)
 * @param effort estimated effort (e.g., "2 days", "0.5 days")
 * @since 5.0.0
 */
public record Suggestion(
        String action,
        List<String> steps,
        String codeExample,
        String effort) {

    /**
     * Creates a suggestion with validation.
     */
    public Suggestion {
        Objects.requireNonNull(action, "action is required");
        steps = steps != null ? List.copyOf(steps) : List.of();
    }

    /**
     * Creates a simple suggestion with just an action.
     *
     * @param action the action to take
     * @return the suggestion
     */
    public static Suggestion simple(String action) {
        return new Suggestion(action, List.of(), null, null);
    }

    /**
     * Creates a suggestion with action and effort.
     *
     * @param action the action to take
     * @param effort estimated effort
     * @return the suggestion
     */
    public static Suggestion withEffort(String action, String effort) {
        return new Suggestion(action, List.of(), null, effort);
    }

    /**
     * Creates a complete suggestion.
     *
     * @param action the action to take
     * @param steps steps to follow
     * @param codeExample example code
     * @param effort estimated effort
     * @return the suggestion
     */
    public static Suggestion complete(String action, List<String> steps, String codeExample, String effort) {
        return new Suggestion(action, steps, codeExample, effort);
    }

    /**
     * Returns the code example as optional.
     *
     * @return optional code example
     */
    public Optional<String> codeExampleOpt() {
        return Optional.ofNullable(codeExample);
    }

    /**
     * Returns the effort as optional.
     *
     * @return optional effort
     */
    public Optional<String> effortOpt() {
        return Optional.ofNullable(effort);
    }

    /**
     * Checks if this suggestion has detailed steps.
     *
     * @return true if steps are provided
     */
    public boolean hasSteps() {
        return !steps.isEmpty();
    }

    /**
     * Checks if this suggestion has a code example.
     *
     * @return true if code example is provided
     */
    public boolean hasCodeExample() {
        return codeExample != null && !codeExample.isBlank();
    }
}
