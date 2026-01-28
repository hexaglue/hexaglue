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

/**
 * Remediation section of the report containing actions to fix issues.
 *
 * @param summary human-readable summary of the remediation plan
 * @param actions ordered list of remediation actions
 * @param totalEffort total estimated effort
 * @since 5.0.0
 */
public record RemediationPlan(String summary, List<RemediationAction> actions, TotalEffort totalEffort) {

    /**
     * Creates a remediation plan with validation.
     */
    public RemediationPlan {
        Objects.requireNonNull(summary, "summary is required");
        actions = actions != null ? List.copyOf(actions) : List.of();
        Objects.requireNonNull(totalEffort, "totalEffort is required");
    }

    /**
     * Creates an empty remediation plan.
     *
     * @return empty plan
     */
    public static RemediationPlan empty() {
        return new RemediationPlan("No actions required. All checks passed.", List.of(), TotalEffort.days(0));
    }

    /**
     * Returns the number of actions.
     *
     * @return action count
     */
    public int actionCount() {
        return actions.size();
    }

    /**
     * Checks if there are any actions.
     *
     * @return true if there are actions
     */
    public boolean hasActions() {
        return !actions.isEmpty();
    }
}
