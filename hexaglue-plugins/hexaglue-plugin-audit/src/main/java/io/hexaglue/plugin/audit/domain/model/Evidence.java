/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.domain.model;

import java.util.List;

/**
 * Evidence supporting a constraint violation.
 *
 * <p>Evidence provides detailed proof of why a violation was detected.
 * Different types of evidence capture different aspects of the violation:
 * <ul>
 *   <li>{@link StructuralEvidence} - Type structure issues (missing fields, wrong types)</li>
 *   <li>{@link BehavioralEvidence} - Method behavior issues (setters, wrong logic)</li>
 *   <li>{@link RelationshipEvidence} - Dependency issues (wrong direction, cycles)</li>
 * </ul>
 *
 * <p>This is a sealed interface using Java 17 sealed types for exhaustive pattern matching.
 *
 * @since 1.0.0
 */
public sealed interface Evidence permits StructuralEvidence, BehavioralEvidence, RelationshipEvidence {

    /**
     * Returns a human-readable description of the evidence.
     *
     * @return the evidence description
     */
    String description();

    /**
     * Returns the qualified names of types involved in this evidence.
     *
     * @return list of fully qualified type names
     */
    List<String> involvedTypes();
}
