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

package io.hexaglue.plugin.audit.adapter.report.model;

import java.util.List;

/**
 * Summary of constraints evaluated during the audit.
 *
 * @param totalConstraints number of constraints evaluated
 * @param constraintNames  list of constraint IDs that were checked
 * @since 1.0.0
 */
public record ConstraintsSummary(int totalConstraints, List<String> constraintNames) {

    public ConstraintsSummary {
        if (totalConstraints < 0) {
            throw new IllegalArgumentException("totalConstraints cannot be negative");
        }
        constraintNames = constraintNames != null ? List.copyOf(constraintNames) : List.of();
    }
}
