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

import java.util.Objects;
import java.util.Optional;

/**
 * Immediate action required to address blockers.
 *
 * @param required whether an immediate action is required
 * @param message description of what needs to be done
 * @param reference link to the related issue (e.g., "#issue-aggregate-cycle-1")
 * @since 5.0.0
 */
public record ImmediateAction(boolean required, String message, String reference) {

    /**
     * Creates an immediate action with validation.
     */
    public ImmediateAction {
        if (required) {
            Objects.requireNonNull(message, "message is required when action is required");
        }
    }

    /**
     * Creates an immediate action that is required.
     *
     * @param message what needs to be done
     * @param reference link to related issue
     * @return the immediate action
     */
    public static ImmediateAction required(String message, String reference) {
        return new ImmediateAction(true, message, reference);
    }

    /**
     * Creates an indication that no immediate action is required.
     *
     * @return empty immediate action
     */
    public static ImmediateAction none() {
        return new ImmediateAction(false, null, null);
    }

    /**
     * Returns the message as an optional.
     *
     * @return optional message
     */
    public Optional<String> messageOpt() {
        return Optional.ofNullable(message);
    }

    /**
     * Returns the reference as an optional.
     *
     * @return optional reference
     */
    public Optional<String> referenceOpt() {
        return Optional.ofNullable(reference);
    }
}
